from __future__ import annotations

import copy
import hashlib
import json
import subprocess
import sys
from pathlib import Path

import pytest

from sls.rl.preparation import read_config
from sls.rl.training_contract import training_implementation_digest
from tools import submit_win70m as submission
from tools.analyze_win_continuation import paired_evaluations


def evaluation(wins: list[bool]) -> dict:
    rows = [{"seed": 100 + i, "success": win, "steps": 5, "floor": 16,
             "bosses": {"1": "SLIME_BOSS"}, "entered_bosses": ["ACT_1:SLIME_BOSS"]}
            for i, win in enumerate(wins)]
    return {"seeds": [100, 100 + len(wins)], "evaluation_role": "development-confirmation",
            "runtime": {"torch": "same", "cpu_threads": 16},
            "simulator": {"native_source_sha256": "same"},
            "result": {"seed_results": rows, "successes": sum(wins), "episodes": len(wins),
                       "success_rate": sum(wins) / len(wins),
                       "boss_successes": {"ACT_1:SLIME_BOSS": sum(wins)},
                       "boss_attempts": {"ACT_1:SLIME_BOSS": len(wins)},
                       **{k: 0 for k in ("backend_errors", "backend_truncations", "step_limits",
                                         "cycle_limits", "timeouts")}}}


def test_paired_continuation_recomputes_raw_changes():
    left = evaluation([True, True, False, False])
    right = evaluation([True, False, True, True])
    result = paired_evaluations(left, right, label="test")
    assert result["net"] == 1
    assert result["discordant"] == {"reference_win": 1, "candidate_win": 2}
    assert result["exact_mcnemar_p"] == 1.0
    assert result["boss_pairs"]["SLIME_BOSS"]["net"] == 1


@pytest.mark.parametrize("key", ["runtime", "simulator", "seeds", "evaluation_role"])
def test_paired_continuation_refuses_contract_mismatch(key):
    left = evaluation([True, False])
    right = copy.deepcopy(left)
    right[key] = "changed"
    with pytest.raises(ValueError):
        paired_evaluations(left, right, label="test")


def test_paired_continuation_refuses_aggregate_or_boss_corruption():
    left = evaluation([True, False])
    right = copy.deepcopy(left)
    right["result"]["successes"] = 2
    with pytest.raises(ValueError, match="counts"):
        paired_evaluations(left, right, label="test")
    right = copy.deepcopy(left)
    right["result"]["seed_results"][0]["bosses"]["1"] = "HEXAGHOST"
    with pytest.raises((ValueError, KeyError)):
        paired_evaluations(left, right, label="test")


def test_90m_plan_preserves_recipe_and_rotates_development_only():
    root = Path(__file__).resolve().parents[1]
    old = read_config(root / "configs/train/ironclad_a20_act1_win_70m_continuation.toml")
    path = root / "configs/train/ironclad_a20_act1_win_90m_continuation.toml"
    new = read_config(path)
    plan = json.loads((root / "configs/experiments/win-90m-20261001.json").read_text())
    assert hashlib.sha256(path.read_bytes().replace(b"\r\n", b"\n")).hexdigest() == plan["config_sha256"]
    assert new["ppo"] == old["ppo"]
    assert new["model"] == old["model"]
    assert new["run"]["worker_layout"] == old["run"]["worker_layout"]
    assert new["run"]["seed"] == old["run"]["seed"]
    assert new["run"]["training_seed_limit"] == old["run"]["training_seed_limit"]
    assert new["run"]["continuation_from"] == old["run"]["output"]
    assert new["run"]["continuation_selection_evidence"] == "completed-endpoint"
    assert new["run"]["continuation_from_training_implementation_sha256"] == training_implementation_digest()
    assert new["run"]["continuation_to_training_implementation_sha256"] == training_implementation_digest()
    assert new["stages"]["train"]["target_environment_steps"] == 90_000_000
    assert new["stages"]["train"]["evaluate_every_steps"] == 4_000_000
    for field in ("periodic", "final"):
        start = new["run"][f"{field}_evaluation_seed_start"]
        assert start >= old["run"]["final_evaluation_seed_start"] + 2048
        assert start + new["run"][f"{field}_evaluation_seed_count"] < plan["reserved_final_seeds"][0]


def test_90m_submission_direct_script_bootstrap():
    root = Path(__file__).resolve().parents[1]
    subprocess.run([sys.executable, "-I", str(root / "tools/submit_win90m.py"), "--help"],
                   cwd=root, check=True, capture_output=True)


@pytest.mark.parametrize("corrupt", [None, "run-manifest.json", "training-config.toml", "latest.pt"])
def test_submission_checks_source_bundle_before_gpu_queue(tmp_path, monkeypatch, corrupt):
    monkeypatch.setattr(submission, "training_implementation_digest", lambda **_: "new")
    monkeypatch.setattr(submission, "native_source_digest", lambda: "native")
    source = tmp_path / "source"
    source.mkdir()
    (source / "latest.pt").write_bytes(b"checkpoint")
    (tmp_path / "reference.pt").write_bytes(b"reference")
    (source / "run-manifest.json").write_text(json.dumps({
        "status": "COMPLETE", "native_source_sha256": "native",
        "training_implementation_sha256": "old"}))
    (source / "training-config.toml").write_text("parent configuration")
    hashes = {p.name: hashlib.sha256(p.read_bytes()).hexdigest() for p in source.iterdir()}
    (source / "training-bundle.json").write_text(json.dumps({"files": hashes}))
    path = tmp_path / "config.toml"
    path.write_text('\n'.join([
        '[run]', 'continuation_from = "source"',
        'continuation_from_training_implementation_sha256 = "old"',
        'continuation_to_training_implementation_sha256 = "new"',
        f'continuation_checkpoint_sha256 = "{hashes["latest.pt"]}"',
        'development_reference_checkpoint = "reference.pt"',
        f'development_reference_sha256 = "{hashlib.sha256(b"reference").hexdigest()}"',
    ]))
    plan = {"config": "config.toml", "config_sha256": hashlib.sha256(
        path.read_bytes().replace(b"\r\n", b"\n")
    ).hexdigest()}
    if corrupt:
        with (source / corrupt).open("ab") as stream:
            stream.write(b" ")
        with pytest.raises(ValueError, match="mismatch"):
            submission.validate_plan(plan, root=tmp_path)
    else:
        assert submission.validate_plan(plan, root=tmp_path) == path
