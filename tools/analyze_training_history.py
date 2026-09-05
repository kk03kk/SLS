"""Summarize archived FullRun logs without executing a model evaluation."""

from __future__ import annotations

import argparse
import hashlib
import json
import math
import statistics
from pathlib import Path


def wilson(successes: int, episodes: int) -> list[float]:
    z = 1.959963984540054
    p = successes / episodes
    centre = p + z * z / (2 * episodes)
    radius = z * math.sqrt(p * (1 - p) / episodes + z * z / (4 * episodes * episodes))
    denominator = 1 + z * z / episodes
    return [(centre - radius) / denominator, (centre + radius) / denominator]


def analyze(run: Path, stdout: Path, stderr: Path) -> dict:
    metrics = run / "stages/train/metrics.jsonl"
    records = [json.loads(line) for line in metrics.read_text().splitlines()]
    updates = [r for r in records if "loss" in r]
    evaluations = []
    for record in records:
        for kind in ("evaluation", "diagnostic_evaluation"):
            if kind not in record:
                continue
            result = record[kind]
            selected = {key: result[key] for key in (
                "episodes", "successes", "success_rate", "reached_act2_rate", "reached_act3_rate",
                "mean_reward", "median_failure_floor", "mean_steps", "backend_errors",
                "backend_truncations", "cycle_limits", "step_limits",
            )}
            evaluations.append({"kind": kind, "steps": record["environment_steps"], **selected,
                                "success_wilson_95": wilson(result["successes"], result["episodes"])})
    fields = ("entropy", "entropy_coefficient", "approx_kl_final", "clip_fraction", "gradient_norm",
              "value_explained_variance", "decisions_per_second", "kl_early_stop")
    windows = {}
    for name, rows in (("first50", updates[:50]), ("last50", updates[-50:]), ("all", updates)):
        windows[name] = {key: {"mean": statistics.mean(r[key] for r in rows),
                              "min": min(r[key] for r in rows), "max": max(r[key] for r in rows)}
                         for key in fields}
    parsed, other = [], []
    for line in stdout.read_text().splitlines():
        try:
            parsed.append(json.loads(line))
        except ValueError:
            other.append(line)
    manifest = json.loads((run / "run-manifest.json").read_text())
    return {
        "schema": "sls-training-history-analysis-v1", "updates": len(updates),
        "first_update": updates[0]["update"], "last_update": updates[-1]["update"],
        "last_steps": updates[-1]["environment_steps"],
        "continuous_updates": all(b["update"] == a["update"] + 1 for a, b in zip(updates, updates[1:])),
        "step_increments": sorted({b["environment_steps"] - a["environment_steps"]
                                   for a, b in zip(updates, updates[1:])}),
        "nonfinite_metrics": sum(not math.isfinite(v) for r in updates for v in r.values()
                                 if isinstance(v, (int, float))),
        "updates_with_fewer_than_two_epochs": sum(r["epochs_completed"] < 2 for r in updates),
        "termination_totals": {key: sum(r.get(key, 0) for r in updates)
                               for key in updates[0] if key.startswith("terminations_")},
        "windows": windows, "evaluations": evaluations,
        "stdout_non_json": other, "stdout_last_record": parsed[-1], "stderr_verbatim": stderr.read_text(),
        "final_evaluation_present": (run / "final-evaluation.json").exists(),
        "manifest_status": manifest.get("status"), "manifest_stale_error": manifest.get("error"),
        "log_sha256": {str(p): hashlib.sha256(p.read_bytes()).hexdigest()
                       for p in (metrics, stdout, stderr)},
    }


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--run", type=Path, required=True)
    parser.add_argument("--stdout", type=Path, required=True)
    parser.add_argument("--stderr", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    result = analyze(args.run, args.stdout, args.stderr)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(result, indent=2) + "\n")
    print(json.dumps({"output": str(args.output), "updates": result["updates"],
                      "continuous_updates": result["continuous_updates"]}))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
