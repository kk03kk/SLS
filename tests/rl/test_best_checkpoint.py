from __future__ import annotations

import json
from pathlib import Path

from sls.rl.best_checkpoint import (
    BEST_CHECKPOINT_SCHEMA,
    best_checkpoint_record,
    evaluation_rank,
    update_best_checkpoint,
)


def _evaluation(**changes: object) -> dict[str, object]:
    value: dict[str, object] = {
        "episodes": 100, "successes": 11, "mean_reward": -0.5,
        "mean_steps": 200.0, "self_loops": 1, "timeouts": 0,
        "step_limits": 2, "cycle_limits": 1, "backend_truncations": 0,
        "median_failure_floor": 12.0,
        "boss_success_rate": {"HEXAGHOST": 0.1, "SLIME_BOSS": 0.2, "THE_GUARDIAN": 0.05},
    }
    value.update(changes)
    return value


def test_rank_penalizes_stalling_before_failure_floor_or_reward() -> None:
    baseline = _evaluation()
    stalled = _evaluation(step_limits=3, median_failure_floor=15.0, mean_reward=0.5)
    assert evaluation_rank(baseline) > evaluation_rank(stalled)
    boss_balanced = _evaluation(
        boss_success_rate={"HEXAGHOST": 0.1, "SLIME_BOSS": 0.2, "THE_GUARDIAN": 0.1},
    )
    assert evaluation_rank(boss_balanced) > evaluation_rank(baseline)


def test_best_checkpoint_keeps_earlier_tie_and_replaces_strict_improvement(tmp_path: Path) -> None:
    saved: list[Path] = []
    first = best_checkpoint_record(_evaluation(), update=10)
    assert update_best_checkpoint(tmp_path, first, save=lambda path: saved.append(path))
    assert saved == [tmp_path / "best_success.pt"]
    assert not update_best_checkpoint(
        tmp_path,
        best_checkpoint_record(_evaluation(), update=20),
        save=lambda path: saved.append(path),
    )
    improved = best_checkpoint_record(
        _evaluation(successes=12), update=30,
    )
    assert update_best_checkpoint(tmp_path, improved, save=lambda path: saved.append(path))
    stored = json.loads((tmp_path / "best_success.json").read_text(encoding="utf-8"))
    assert stored["schema"] == BEST_CHECKPOINT_SCHEMA
    assert stored["update"] == 30
    assert stored["mean_reward"] == -0.5
    assert stored["selection_excludes_mean_reward"] is True


def test_nus_pilot_history_selects_update_200_not_latest() -> None:
    """Freeze the decisive ranks from NUS jobs 759012 and 760804."""

    common = {
        "episodes": 100, "timeouts": 0, "backend_truncations": 0,
    }
    evaluations = {
        200: {
            **common, "successes": 11, "step_limits": 15, "cycle_limits": 1,
            "self_loops": 1, "median_failure_floor": 10, "mean_steps": 273.07,
            "mean_reward": -0.62, "boss_success_rate": {
                "HEXAGHOST": 2 / 13, "SLIME_BOSS": 0.2, "THE_GUARDIAN": 1 / 44,
            },
        },
        280: {
            **common, "successes": 11, "step_limits": 26, "cycle_limits": 4,
            "self_loops": 4, "median_failure_floor": 10, "mean_steps": 308.47,
            "mean_reward": -0.48, "boss_success_rate": {
                "HEXAGHOST": 2 / 13, "SLIME_BOSS": 1 / 6, "THE_GUARDIAN": 2 / 44,
            },
        },
        330: {
            **common, "successes": 11, "step_limits": 25, "cycle_limits": 1,
            "self_loops": 1, "median_failure_floor": 8, "mean_steps": 314.32,
            "mean_reward": -0.52, "boss_success_rate": {
                "HEXAGHOST": 3 / 26, "SLIME_BOSS": 4 / 15, "THE_GUARDIAN": 0.0,
            },
        },
        440: {
            **common, "successes": 0, "step_limits": 47, "cycle_limits": 6,
            "self_loops": 6, "median_failure_floor": 5, "mean_steps": 365.81,
            "mean_reward": -0.47, "boss_success_rate": {
                "HEXAGHOST": 0.0, "SLIME_BOSS": 0.0, "THE_GUARDIAN": 0.0,
            },
        },
    }
    records = {
        update: best_checkpoint_record(value, update=update)
        for update, value in evaluations.items()
    }
    assert max(records, key=lambda update: evaluation_rank(records[update])) == 200
