"""Deterministic selection and metadata for evaluation-selected checkpoints."""

from __future__ import annotations

import hashlib
import json
import math
import os
from pathlib import Path
from typing import Any, Callable, Mapping

BEST_CHECKPOINT_SCHEMA = "sls-best-progress-v4"
_LEGACY_BEST_CHECKPOINT_SCHEMAS = {"sls-best-progress-v3"}


def _digest(path: Path) -> str:
    with path.open("rb") as stream:
        return hashlib.file_digest(stream, "sha256").hexdigest()


def recover_best_checkpoint(output: Path) -> None:
    """Roll forward an interrupted promotion before reading either public file.

    A single training writer owns this directory. The pending record is published
    only after its staged weights are complete; removing it commits the pair.
    """
    pending = output / "best_progress.pending.json"
    if not pending.exists():
        return
    transaction = json.loads(pending.read_text(encoding="utf-8"))
    staged = output / "best_progress.staged.pt"
    checkpoint = output / "best_progress.pt"
    source = staged if staged.exists() else checkpoint
    if _digest(source) != transaction["checkpoint_sha256"]:
        raise ValueError("interrupted best checkpoint has invalid weights")
    if staged.exists():
        os.replace(staged, checkpoint)
    temporary = output / "best_progress.json.tmp"
    temporary.write_text(json.dumps(transaction["record"], indent=2) + "\n", encoding="utf-8")
    os.replace(temporary, output / "best_progress.json")
    pending.unlink()


def _wilson_interval(k: int, n: int) -> tuple[float, float]:
    if not 0 <= k <= n or n <= 0:
        raise ValueError("invalid checkpoint selection counts")
    z = 1.959963984540054
    p = k / n
    centre = p + z * z / (2 * n)
    margin = z * math.sqrt(p * (1 - p) / n + z * z / (4 * n * n))
    denominator = 1 + z * z / n
    return (centre - margin) / denominator, (centre + margin) / denominator


def passes_progress_guard(candidate: Mapping[str, Any], incumbent: Mapping[str, Any]) -> bool:
    """Conservative marginal-interval guard, not a paired significance test.

    One extra rare win must not mask an unambiguous collapse in act reach.
    All snapshots remain saved even when this guard rejects best promotion.
    """
    n = int(candidate["episodes"])
    if n != int(incumbent["episodes"]):
        raise ValueError("guarded checkpoint selection requires the same fixed seed count")
    if any(int(candidate.get(k, 0)) for k in ("backend_errors", "backend_truncations")):
        return False
    def interval(record, field):
        return _wilson_interval(int(record[field]), n)
    if interval(candidate, "successes")[0] > interval(incumbent, "successes")[1]:
        return True
    return all(interval(candidate, field)[1] >= interval(incumbent, field)[0]
               for field in ("reached_act2", "reached_act3"))


def evaluation_rank(record: Mapping[str, Any]) -> tuple[float, ...]:
    """Rank progress while penalizing non-progress before reward magnitude."""

    if record.get("selection_objective") in {"ACT1_CLEAR_COUNT", "HORIZON_CLEAR_COUNT"}:
        return (float(record["successes"]),)
    failure_floor = record.get("median_failure_floor")
    rates = dict(record.get("boss_success_rate") or {})
    successes = dict(record.get("boss_successes") or {})
    attempts = dict(record.get("boss_attempts") or {})

    def lower_bound(name: str, rate: object) -> float:
        n = int(attempts.get(name, 0))
        if n <= 0:
            return float(rate)
        p = int(successes.get(name, round(float(rate) * n))) / n
        z = 1.959963984540054
        denominator = 1.0 + z * z / n
        centre = p + z * z / (2.0 * n)
        margin = z * math.sqrt((p * (1.0 - p) + z * z / (4.0 * n)) / n)
        return (centre - margin) / denominator

    minimum_boss_lcb = min(
        (lower_bound(name, rate) for name, rate in rates.items()),
        default=-1.0,
    )
    return (
        float(record["successes"]),
        float(record["reached_act3"]),
        minimum_boss_lcb,
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
        "boss_successes": {
            str(key): int(value)
            for key, value in sorted(dict(evaluation.get("boss_successes") or {}).items())
        },
        "boss_attempts": {
            str(key): int(value)
            for key, value in sorted(dict(evaluation.get("boss_attempts") or {}).items())
        },
        "minimum_boss_success_rate": min(
            (float(value) for value in evaluation["boss_success_rate"].values()),
            default=0.0,
        ),
        "boss_action_metrics": {
            str(key): dict(value)
            for key, value in sorted(
                dict(evaluation.get("boss_action_metrics") or {}).items()
            )
        },
        "selection_excludes_mean_reward": True,
    }


def update_best_checkpoint(
    output: Path,
    record: Mapping[str, Any],
    *,
    save: Callable[[Path], object],
    progress_guard: bool = False,
) -> bool:
    """Save only a strict deterministic-evaluation improvement."""

    output.mkdir(parents=True, exist_ok=True)
    recover_best_checkpoint(output)
    metadata_path = output / "best_progress.json"
    if metadata_path.exists():
        existing = json.loads(metadata_path.read_text(encoding="utf-8"))
        if existing.get("schema") not in {
            BEST_CHECKPOINT_SCHEMA, *_LEGACY_BEST_CHECKPOINT_SCHEMAS,
        }:
            raise ValueError("unsupported best-checkpoint metadata")
        if progress_guard and not passes_progress_guard(record, existing):
            return False
        if evaluation_rank(record) <= evaluation_rank(existing):
            return False
    staged = output / "best_progress.staged.pt"
    save(staged)
    digest = _digest(staged)
    pending = output / "best_progress.pending.json"
    temporary = pending.with_suffix(".tmp")
    temporary.write_text(
        json.dumps({"checkpoint_sha256": digest,
                    "record": {**record, "checkpoint_sha256": digest}}, indent=2) + "\n",
        encoding="utf-8",
    )
    os.replace(temporary, pending)
    recover_best_checkpoint(output)
    return True
