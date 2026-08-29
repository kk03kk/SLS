from __future__ import annotations

import signal
import tomllib
from pathlib import Path

import pytest

from tools.train_full_run import (
    StopController,
    _archive_pre_migration_best,
    _positive_int,
    _progress_from_baseline,
    _seed_range,
    _training_identity,
    _validate_seed_namespaces,
)


def test_stop_controller_defers_signal_to_safe_boundary() -> None:
    controller = StopController()
    controller.handler(signal.SIGTERM, None)
    assert controller.requested is True
    assert controller.signal_name == "SIGTERM"


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


def test_stage_targets_do_not_change_training_identity() -> None:
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
    assert _training_identity(payload, workers=32, shards=8) == first


def test_positive_training_intervals_fail_before_the_training_loop() -> None:
    assert _positive_int({"target": 20}, "target") == 20
    with pytest.raises(ValueError, match="must be positive"):
        _positive_int({"target": 0}, "target")


def test_canonical_fullrun_config_freezes_stage_and_recurrent_contract() -> None:
    path = Path(__file__).resolve().parents[1] / "configs/train/ironclad_a0_fullrun.toml"
    with path.open("rb") as stream:
        payload = tomllib.load(stream)
    assert payload["run"]["profile"] == "IRONCLAD_A0_FULLRUN"
    assert payload["stages"]["smoke"]["target_environment_steps"] == 100_000
    assert payload["stages"]["pilot"]["target_environment_steps"] == 2_000_000
    assert payload["stages"]["pilot"]["evaluate_every_steps"] == 250_000
    assert payload["stages"]["train"]["target_environment_steps"] == 50_000_000
    assert payload["model"]["architecture"] == "sls-recurrent-relational-policy-v4"
    assert payload["model"]["recurrent_hidden_dim"] == 256
    assert payload["run"]["seed"] == 10_000_000
    assert payload["run"]["output"] == "runs/ironclad-a0-fullrun-v2"
    assert payload["ppo"]["gamma"] == 1.0
    assert payload["ppo"]["failure_progress_scale"] == 0.8
    assert payload["ppo"]["reward_schema"] == "sls-curriculum-progress-v3"
    assert payload["ppo"]["recurrent_sequence_length"] == 32
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


def test_environment_migration_archives_stale_best_without_deleting_it(
    tmp_path: Path,
) -> None:
    (tmp_path / "best_success.pt").write_bytes(b"checkpoint")
    (tmp_path / "best_success.json").write_text("{}\n", encoding="utf-8")

    archived = _archive_pre_migration_best(tmp_path)

    assert archived == [
        "best_success.pre-environment-migration.pt",
        "best_success.pre-environment-migration.json",
    ]
    assert (tmp_path / archived[0]).read_bytes() == b"checkpoint"
    assert (tmp_path / archived[1]).read_text(encoding="utf-8") == "{}\n"
    assert _archive_pre_migration_best(tmp_path) == archived
