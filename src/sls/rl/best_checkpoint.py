"""Deterministic selection and metadata for evaluation-selected checkpoints."""

from __future__ import annotations

import json
import os
from pathlib import Path
from typing import Any, Callable, Mapping

BEST_CHECKPOINT_SCHEMA = "sls-best-progress-v3"


def evaluation_rank(record: Mapping[str, Any]) -> tuple[float, ...]:
    """Rank progress while penalizing non-progress before reward magnitude."""

    failure_floor = record.get("median_failure_floor")
    return (
        float(record["successes"]),
        float(record["reached_act3"]),
        float(record["reached_act2"]),
        float("inf") if failure_floor is None else float(failure_floor),
        -float(record.get("backend_errors", 0)),
        -float(record["step_limits"]),
        -float(record["cycle_limits"]),
        -float(record["self_loops"]),
        -float(record["timeouts"]),
        -float(record["backend_truncations"]),
        -float(record["mean_steps"]),
    )


def best_checkpoint_record(
    evaluation: Mapping[str, Any],
    *,
    update: int,
) -> dict[str, Any]:
    return {
        "schema": BEST_CHECKPOINT_SCHEMA,
        "update": int(update),
        "successes": int(evaluation["successes"]),
        "success_rate": float(evaluation["success_rate"]),
        "reached_act2": int(evaluation["reached_act2"]),
        "reached_act3": int(evaluation["reached_act3"]),
        "reached_act2_rate": float(evaluation["reached_act2_rate"]),
        "reached_act3_rate": float(evaluation["reached_act3_rate"]),
        "episodes": int(evaluation["episodes"]),
        "mean_reward": float(evaluation["mean_reward"]),
        "mean_steps": float(evaluation["mean_steps"]),
        "median_failure_floor": evaluation["median_failure_floor"],
        "step_limits": int(evaluation["step_limits"]),
        "cycle_limits": int(evaluation["cycle_limits"]),
        "self_loops": int(evaluation["self_loops"]),
        "timeouts": int(evaluation["timeouts"]),
        "backend_truncations": int(evaluation["backend_truncations"]),
        "backend_errors": int(evaluation.get("backend_errors", 0)),
        "boss_success_rate": {
            str(key): float(value)
            for key, value in sorted(dict(evaluation["boss_success_rate"]).items())
        },
        "selection_excludes_mean_reward": True,
    }


def update_best_checkpoint(
    output: Path,
    record: Mapping[str, Any],
    *,
    save: Callable[[Path], object],
) -> bool:
    """Save only a strict deterministic-evaluation improvement."""

    output.mkdir(parents=True, exist_ok=True)
    metadata_path = output / "best_progress.json"
    if metadata_path.exists():
        existing = json.loads(metadata_path.read_text(encoding="utf-8"))
        if existing.get("schema") != BEST_CHECKPOINT_SCHEMA:
            raise ValueError("unsupported best-checkpoint metadata")
        if evaluation_rank(record) <= evaluation_rank(existing):
            return False
    save(output / "best_progress.pt")
    temporary = metadata_path.with_suffix(metadata_path.suffix + ".tmp")
    temporary.write_text(
        json.dumps(record, ensure_ascii=False, indent=2, sort_keys=True) + "\n",
        encoding="utf-8",
    )
    os.replace(temporary, metadata_path)
    return True
