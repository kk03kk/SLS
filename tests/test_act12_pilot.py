"""Act1-2 transfer preparation gates; synthetic unit fixtures are not training results."""

import copy
import json
from pathlib import Path
from types import SimpleNamespace

import pytest

from sls.curriculum import IRONCLAD_A20_ACT1, IRONCLAD_A20_ACT2, IRONCLAD_A20_ACT3
from tools import prepare_act12_pilot as prepare
from tools import submit_act12_pilot as submit
from tools import train_full_run as train

ROOT = Path(__file__).resolve().parents[1]


@pytest.fixture
def original():
    from sls.rl.preparation import read_config
    return read_config(ROOT / "configs/train/ironclad_a20_act1_win_90m_continuation.toml")


@pytest.fixture
def recipe():
    return json.loads(prepare.RECIPE.read_text())


@pytest.fixture
def parent():
    return {"checkpoint": "local/runs/parent/final.pt", "sha256": "a" * 64,
            "environment_steps": 90_013_696}


def test_build_pilot_has_new_goal_fresh_transfer_and_unchanged_ppo(original, recipe, parent):
    initial = copy.deepcopy(original)
    config = prepare.build_configuration(original, parent, recipe, run_name="act12-test")
    parsed = prepare.tomllib.loads(prepare.configuration_toml(config))
    assert parsed == config
    assert original == initial
    assert config["run"]["profile"] == "IRONCLAD_A20_ACT2"
    assert config["stages"]["train"]["target_environment_steps"] == 92_013_696
    assert "continuation_from" not in config["run"]
    assert config["model"] == original["model"] and config["ppo"] == original["ppo"]
    assert config["warm_start"]["transfer_kind"] == "curriculum-stage"
    assert config["run"]["development_reference_sha256"] == config["warm_start"]["checkpoint_sha256"]
    assert config["run"]["final_evaluation_role"] == "development-confirmation"
    assert config["run"]["final_evaluation_seed_start"] < recipe["reserved_final_seeds"][0]


@pytest.mark.parametrize("bad", ["../escape", "A20", "", "a/b"])
def test_bad_run_names_rejected(original, recipe, parent, bad):
    with pytest.raises(ValueError, match="run name"):
        prepare.build_configuration(original, parent, recipe, run_name=bad)


def test_progress_reward_cannot_enter_pilot(original, recipe, parent):
    original["ppo"]["failure_progress_scale"] = 0.8
    with pytest.raises(ValueError, match="Win objective"):
        prepare.build_configuration(original, parent, recipe, run_name="act12")


def test_overlapping_development_seeds_rejected(original, recipe, parent):
    recipe["confirmation_seed_start"] = recipe["periodic_seed_start"]
    with pytest.raises(ValueError, match="overlap"):
        prepare.build_configuration(original, parent, recipe, run_name="act12")


def test_cross_horizon_reference_is_exact_adjacent_parent(original, recipe, parent, monkeypatch):
    monkeypatch.setattr(train, "native_source_digest", lambda: "b" * 64)
    config = prepare.build_configuration(original, parent, recipe, run_name="act12")
    payload = {"contract": {"profile": IRONCLAD_A20_ACT1, "native_source_sha256": "b" * 64}}
    assert train._validate_frozen_reference_profile(payload, IRONCLAD_A20_ACT2, config)
    with pytest.raises(ValueError, match="adjacent"):
        train._validate_frozen_reference_profile(payload, IRONCLAD_A20_ACT3, config)
    config["warm_start"]["checkpoint_sha256"] = "c" * 64
    with pytest.raises(ValueError, match="pinned"):
        train._validate_frozen_reference_profile(payload, IRONCLAD_A20_ACT2, config)


@pytest.mark.parametrize("change", ["checkpoint", "transfer_kind", "native", "declaration"])
def test_cross_horizon_reference_rejects_unregistered_changes(original, recipe, parent, monkeypatch, change):
    monkeypatch.setattr(train, "native_source_digest", lambda: "b" * 64)
    config = prepare.build_configuration(original, parent, recipe, run_name="act12")
    payload = {"contract": {"profile": IRONCLAD_A20_ACT1, "native_source_sha256": "b" * 64}}
    if change in {"checkpoint", "transfer_kind"}:
        config["warm_start"][change] = "other"
    elif change == "native":
        payload["contract"]["native_source_sha256"] = "c" * 64
    else:
        del config["run"]["development_reference_profile"]
    with pytest.raises(ValueError, match="simulator" if change == "native" else "adjacent"):
        train._validate_frozen_reference_profile(payload, IRONCLAD_A20_ACT2, config)


def test_existing_same_profile_reference_remains_valid():
    assert not train._validate_frozen_reference_profile(
        {"contract": {"profile": IRONCLAD_A20_ACT1}}, IRONCLAD_A20_ACT1, {"run": {}},
    )


def test_paths_cannot_escape_repository(tmp_path):
    with pytest.raises(ValueError, match="within"):
        prepare.repository_path(tmp_path, "../outside")


@pytest.fixture
def completed_parent_fixture(tmp_path, monkeypatch, original):
    """Small fake bytes exercise operator gates, never written under real runs."""
    folder = tmp_path / "parent"
    folder.mkdir()
    native = "b" * 64
    monkeypatch.setattr(prepare, "native_source_digest", lambda: native)
    (folder / "final.pt").write_bytes(b"synthetic-unit-fixture")
    digest = prepare.sha256_file(folder / "final.pt")
    # Reuse a real TOML's structure; no synthetic checkpoint is deserialized.
    config_path = ROOT / "configs/train/ironclad_a20_act1_win_90m_continuation.toml"
    (folder / "training-config.toml").write_bytes(config_path.read_bytes())
    manifest = {"status": "COMPLETE", "native_source_sha256": native,
                "training_identity_sha256": "e" * 64,
                "training_implementation_sha256": "c" * 64,
                "stages": {"train": {"status": "COMPLETE", "profile": "IRONCLAD_A20_ACT1",
                                      "completed_environment_steps": 90_013_696}}}
    evaluation = {"checkpoint_sha256": digest, "checkpoint_environment_steps": 90_013_696,
                  "evaluation_role": "development-confirmation",
                  "simulator": {"native_source_sha256": native},
                  "result": dict.fromkeys(prepare.HEALTH, 0)}
    (folder / "run-manifest.json").write_text(json.dumps(manifest))
    (folder / "endpoint-evaluation.json").write_text(json.dumps(evaluation))
    def rebundle():
        files = {name: prepare.sha256_file(folder / name) for name in (
            "run-manifest.json", "training-config.toml", "final.pt", "endpoint-evaluation.json",
        )}
        (folder / "training-bundle.json").write_text(json.dumps({"files": files}))
    rebundle()
    monkeypatch.setattr(prepare.torch, "load", lambda *a, **k: {
        "contract": {"profile": IRONCLAD_A20_ACT1, "native_source_sha256": native,
                     "ppo": prepare.PPOConfig(**original["ppo"]).to_dict(),
                     "training_config_sha256": "e" * 64},
        "trainer": {"environment_steps": 90_013_696},
    })
    monkeypatch.setattr(prepare, "policy_from_training_checkpoint", lambda _: SimpleNamespace(
        config=prepare.ModelConfig(**original["model"]),
    ))
    return folder, rebundle


def test_complete_parent_identity_and_evidence_are_pinned(tmp_path, completed_parent_fixture):
    folder, _ = completed_parent_fixture
    _, record, _ = prepare.inspect_parent(folder, "endpoint", root=tmp_path)
    assert record["environment_steps"] == 90_013_696
    assert record["sha256"] == prepare.sha256_file(folder / "final.pt")


@pytest.mark.parametrize("problem", ["running", "short", "health", "hash"])
def test_parent_gate_rejects_incomplete_or_corrupt_sources(tmp_path, completed_parent_fixture, problem):
    folder, rebundle = completed_parent_fixture
    if problem in {"running", "short"}:
        path = folder / "run-manifest.json"
        data = json.loads(path.read_text())
        if problem == "running":
            data["status"] = "RUNNING"
        else:
            data["stages"]["train"]["completed_environment_steps"] = 89_000_000
        path.write_text(json.dumps(data))
        rebundle()
    elif problem == "health":
        path = folder / "endpoint-evaluation.json"
        data = json.loads(path.read_text())
        data["result"]["timeouts"] = 1
        path.write_text(json.dumps(data))
        rebundle()
    else:
        (folder / "final.pt").write_bytes(b"corrupt")
    with pytest.raises(ValueError):
        prepare.inspect_parent(folder, "endpoint", root=tmp_path)


@pytest.mark.parametrize("seed", [9_000_000_000_000, 8_000_005_000_000, 8_000_004_000_000])
def test_pilot_does_not_consume_reserved_or_exposed_seeds(original, recipe, parent, seed):
    recipe["confirmation_seed_start"] = seed
    with pytest.raises(ValueError, match="overlap"):
        prepare.build_configuration(original, parent, recipe, run_name="act12")


def test_submission_checks_bound_config_and_parent(tmp_path, completed_parent_fixture, recipe, monkeypatch):
    folder, _ = completed_parent_fixture
    original, parent, _ = prepare.inspect_parent(folder, "endpoint", root=tmp_path)
    config = prepare.build_configuration(original, parent, recipe, run_name="act12")
    path = tmp_path / "pilot.toml"
    path.write_text(prepare.configuration_toml(config), encoding="utf-8")
    monkeypatch.setattr(submit, "training_implementation_digest", lambda **_: "d" * 64)
    plan = {"schema": "sls-act12-bound-plan-v1", "status": "READY_FOR_LOCAL_VALIDATION",
            "config": "pilot.toml", "config_sha256": prepare.source_sha256(path),
            "target_training_implementation_sha256": "d" * 64,
            "parent": parent, "recipe": recipe}
    assert submit.validate_plan(plan, root=tmp_path) == path
    path.write_text(path.read_text() + "\n# unregistered change\n")
    with pytest.raises(ValueError, match="configuration changed"):
        submit.validate_plan(plan, root=tmp_path)


def test_provisional_recipe_is_not_a_submittable_plan(recipe):
    with pytest.raises(ValueError, match="hash-bound"):
        submit.validate_plan(recipe)


def test_selected_parent_requires_registered_checkpoint(tmp_path, completed_parent_fixture):
    folder, _ = completed_parent_fixture
    selection = folder / "stages/train/selection"
    selection.mkdir(parents=True)
    (selection / "best_progress.pt").write_bytes((folder / "final.pt").read_bytes())
    best = {"selection_objective": "ACT1_CLEAR_COUNT", "environment_steps": 90_013_696,
            "checkpoint_sha256": prepare.sha256_file(selection / "best_progress.pt")}
    (selection / "best_progress.json").write_text(json.dumps(best))
    (folder / "final-evaluation.json").write_bytes((folder / "endpoint-evaluation.json").read_bytes())
    bundle_path = folder / "training-bundle.json"
    bundle = json.loads(bundle_path.read_text())
    for name in ("stages/train/selection/best_progress.pt", "stages/train/selection/best_progress.json",
                 "final-evaluation.json"):
        bundle["files"][name] = prepare.sha256_file(folder / name)
    bundle_path.write_text(json.dumps(bundle))
    assert prepare.inspect_parent(folder, "selected", root=tmp_path)[1]["role"] == "selected"
    best["checkpoint_sha256"] = "0" * 64
    (selection / "best_progress.json").write_text(json.dumps(best))
    bundle["files"]["stages/train/selection/best_progress.json"] = prepare.sha256_file(selection / "best_progress.json")
    bundle_path.write_text(json.dumps(bundle))
    with pytest.raises(ValueError, match="registered selection"):
        prepare.inspect_parent(folder, "selected", root=tmp_path)


def test_simulator_review_is_preserved_in_generated_toml(original, recipe, parent):
    parent["simulator_transition"] = {"evidence": "review.json", "evidence_sha256": "c" * 64}
    config = prepare.build_configuration(original, parent, recipe, run_name="act12")
    assert prepare.tomllib.loads(prepare.configuration_toml(config)) == config
    assert config["warm_start"]["simulator_transition"] == parent["simulator_transition"]
