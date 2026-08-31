from __future__ import annotations

import json
import signal
import tomllib
from pathlib import Path

import pytest

from tools.train_full_run import (
    StopController,
    _load_benchmark,
    _positive_int,
    _progress_from_baseline,
    _promotion_passes,
    _require_interrupted_smoke_resume,
    _require_predecessor_promotion,
    _seed_range,
    _training_identity,
    _validate_seed_namespaces,
)


def test_worker_benchmark_is_bound_to_local_source_and_binary_not_git(
    tmp_path: Path,
) -> None:
    path = tmp_path / "benchmark.json"
    path.write_text(json.dumps({
        "schema": "sls-worker-benchmark-v2",
        "selected_workers": 16,
        "selected_shards": 8,
        "git": {"commit": "unrelated-optional-metadata"},
        "native_source_sha256": "source",
        "native_artifact": {"sha256": "binary"},
    }), encoding="utf-8")

    assert _load_benchmark(
        path, native_digest="source", native_binary_sha256="binary",
    ) == (16, 8)


def _interrupted_smoke_manifest() -> dict[str, object]:
    return {
        "schema": "sls-recurrent-ppo-run-v2",
        "training_identity_sha256": "identity",
        "status": "INTERRUPTED",
        "stages": {
            "smoke": {
                "status": "INTERRUPTED",
                "profile": "IRONCLAD_A0_ACT1",
            },
        },
    }


def test_stop_controller_defers_signal_to_safe_boundary() -> None:
    controller = StopController()
    controller.handler(signal.SIGTERM, None)
    assert controller.requested is True
    assert controller.signal_name == "SIGTERM"


def test_interrupted_smoke_chain_is_exactly_resumable() -> None:
    _require_interrupted_smoke_resume(_interrupted_smoke_manifest(), "identity")


@pytest.mark.parametrize(
    ("field", "value"),
    (
        ("status", "RUNNING"),
        ("status", "COMPLETE"),
        ("training_identity_sha256", "other"),
    ),
)
def test_smoke_resume_rejects_non_interrupted_or_incompatible_chains(
    field: str, value: str,
) -> None:
    manifest = _interrupted_smoke_manifest()
    manifest[field] = value
    with pytest.raises(FileExistsError, match="non-interrupted or incompatible"):
        _require_interrupted_smoke_resume(manifest, "identity")


def test_evaluation_seed_namespaces_are_disjoint_from_training() -> None:
    run = {
        "periodic_evaluation_seed_start": 10**12,
        "periodic_evaluation_seed_count": 128,
        "final_evaluation_seed_start": 2 * 10**12,
        "final_evaluation_seed_count": 1000,
    }
    periodic, final = _validate_seed_namespaces(run)
    assert periodic == _seed_range(10**12, 128)
    assert final == _seed_range(2 * 10**12, 1000)
    with pytest.raises(ValueError, match="overlap"):
        _validate_seed_namespaces({
            **run,
            "final_evaluation_seed_start": 10**12 + 100,
        })


def test_curriculum_and_stage_targets_are_part_of_training_identity() -> None:
    payload = {
        "run": {
            "profile": "IRONCLAD_A0_FULLRUN", "seed": 0,
            "worker_backend": "sharded-vector",
            "periodic_evaluation_seed_start": 10**12,
            "periodic_evaluation_seed_count": 128,
            "final_evaluation_seed_start": 2 * 10**12,
            "final_evaluation_seed_count": 1000,
        },
        "model": {"architecture": "v4"},
        "ppo": {"gamma": 0.999},
        "stages": {"smoke": {"target_environment_steps": 1}},
    }
    first = _training_identity(payload, workers=32, shards=8)
    payload["stages"] = {"train": {"target_environment_steps": 50_000_000}}
    assert _training_identity(payload, workers=32, shards=8) != first


def test_positive_training_intervals_fail_before_the_training_loop() -> None:
    assert _positive_int({"target": 20}, "target") == 20
    with pytest.raises(ValueError, match="must be positive"):
        _positive_int({"target": 0}, "target")


def test_canonical_fullrun_config_freezes_stage_and_recurrent_contract() -> None:
    path = Path(__file__).resolve().parents[1] / "configs/train/ironclad_a0_fullrun.toml"
    with path.open("rb") as stream:
        payload = tomllib.load(stream)
    assert payload["run"]["profile"] == "IRONCLAD_A0_FULLRUN"
    assert payload["stages"]["smoke"]["profile"] == "IRONCLAD_A0_ACT1"
    assert payload["stages"]["smoke"]["target_environment_steps"] == 5_000_000
    assert payload["stages"]["pilot"]["profile"] == "IRONCLAD_A0_ACT2"
    assert payload["stages"]["pilot"]["target_environment_steps"] == 25_000_000
    assert payload["stages"]["train"]["target_environment_steps"] == 100_000_000
    assert payload["model"]["architecture"] == "sls-recurrent-relational-policy-v5"
    assert payload["model"]["recurrent_hidden_dim"] == 256
    assert payload["run"]["seed"] == 10_000_000
    assert payload["run"]["output"] == "local/runs/ironclad-a0-fullrun-v2"
    assert payload["ppo"]["gamma"] == 1.0
    assert payload["ppo"]["failure_progress_scale"] == 0.8
    assert payload["ppo"]["reward_schema"] == "sls-curriculum-progress-v3"
    assert payload["ppo"]["recurrent_sequence_length"] == 64
    assert payload["ppo"]["max_episode_steps"] == 4096


def test_progress_report_is_relative_to_update_zero() -> None:
    progress = _progress_from_baseline(
        {"reached_act2_rate": 0.1, "reached_act3_rate": 0.0, "median_failure_floor": 5},
        {"reached_act2_rate": 0.3, "reached_act3_rate": 0.1, "median_failure_floor": 8},
    )
    assert progress == {
        "reached_act2_rate_delta": pytest.approx(0.2),
        "reached_act3_rate_delta": pytest.approx(0.1),
        "median_failure_floor_delta": pytest.approx(3.0),
    }


def test_policy_promotion_requires_quality_and_zero_runtime_failures() -> None:
    stage = {
        "minimum_success_rate": 0.1,
        "minimum_reached_act2_rate": 0.5,
        "minimum_reached_act3_rate": 0.2,
        "minimum_evaluation_episodes": 100,
    }
    result = {
        "episodes": 128, "success_rate": 0.2,
        "reached_act2_rate": 0.7, "reached_act3_rate": 0.3,
        "backend_errors": 0, "backend_truncations": 0,
        "step_limits": 0, "cycle_limits": 0, "timeouts": 0,
    }
    assert _promotion_passes(result, stage)
    assert not _promotion_passes({**result, "backend_errors": 1}, stage)
    assert not _promotion_passes({**result, "self_loops": 1}, stage)
    assert not _promotion_passes({**result, "success_rate": 0.01}, stage)


def test_curriculum_stage_requires_successful_predecessor_promotion() -> None:
    manifest = {
        "stages": {
            "smoke": {"status": "COMPLETE", "promotion_passed": True},
            "pilot": {"status": "COMPLETE", "promotion_passed": True},
        },
    }
    _require_predecessor_promotion(manifest, "smoke")
    _require_predecessor_promotion(manifest, "pilot")
    _require_predecessor_promotion(manifest, "train")
    manifest["stages"]["pilot"]["promotion_passed"] = False
    with pytest.raises(ValueError, match="promotion gate"):
        _require_predecessor_promotion(manifest, "train")
