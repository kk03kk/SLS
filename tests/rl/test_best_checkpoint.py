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
        "episodes": 100, "successes": 11, "success_rate": 0.11,
        "reached_act2": 40, "reached_act3": 20,
        "reached_act2_rate": 0.4, "reached_act3_rate": 0.2,
        "mean_reward": -0.5,
        "mean_steps": 200.0, "self_loops": 1, "timeouts": 0,
        "step_limits": 2, "cycle_limits": 1, "backend_truncations": 0,
        "backend_errors": 0,
        "median_failure_floor": 12.0,
        "boss_success_rate": {"HEXAGHOST": 0.1, "SLIME_BOSS": 0.2, "THE_GUARDIAN": 0.05},
    }
    value.update(changes)
    return value


def test_rank_prefers_act_progress_then_failure_floor_and_stability() -> None:
    baseline = _evaluation()
    progressed = _evaluation(reached_act3=21, step_limits=99)
    assert evaluation_rank(progressed) > evaluation_rank(baseline)
    later_floor = _evaluation(median_failure_floor=15.0, step_limits=3)
    assert evaluation_rank(later_floor) > evaluation_rank(baseline)


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
