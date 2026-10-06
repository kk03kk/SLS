"""Verify a completed Act1-2 pilot and recompute normal-start paired outcomes."""

from __future__ import annotations

import argparse
import contextlib
import io
import json
import math
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))

from tools.analyze_reward_screen import analyze_run, outcomes, read
from tools.compare_run_arms import _from_seed_results, report


def paired_act12(reference: dict, candidate: dict, *,
                 labels: tuple[str, str] = ("frozen-parent", "candidate")) -> dict:
    interval = tuple(reference["seeds"])
    if tuple(candidate["seeds"]) != interval:
        raise ValueError("Act1-2 paired seeds differ")
    for key in ("runtime", "simulator"):
        if not reference.get(key) or reference[key] != candidate.get(key):
            raise ValueError(f"Act1-2 evaluation {key} differs or is missing")
    for key in ("environment", "reward", "decoding"):
        if (key in reference or key in candidate) and reference.get(key) != candidate.get(key):
            raise ValueError(f"Act1-2 evaluation {key} differs")
    for evaluation in (reference, candidate):
        if evaluation.get("evaluation_role") != "development-confirmation":
            raise ValueError("pilot confirmation must remain development data")
        outcomes(evaluation["result"], interval, horizon=2)
        if any(row["success"] != (row["reason"] == "ACT_2_CLEARED")
               for row in evaluation["result"]["seed_results"]):
            raise ValueError("success does not mean complete Act1-2 clear")
    left, right = reference["result"]["seed_results"], candidate["result"]["seed_results"]
    indexed = {row["seed"]: row for row in left}
    if any(row["bosses"]["1"] != indexed[row["seed"]]["bosses"]["1"] for row in right):
        raise ValueError("initial seed-to-boss assignment changed")
    # Act2 boss information is absent when the policy never reaches Act2.
    # Unequal reach rates are legitimate; never pair only surviving subsets.
    with contextlib.redirect_stdout(io.StringIO()):
        result = report(_from_seed_results(labels[0], "Act1-2", left),
                        _from_seed_results(labels[1], "Act1-2", right), 256)
    n = result["paired_seeds"]
    delta = result["net"] / n
    discordant = sum(result["discordant"].values()) / n
    se = math.sqrt(max(0, discordant - delta * delta) / n)
    result["paired_difference_ci95_normal"] = [delta - 1.96 * se, delta + 1.96 * se]
    return result


def value_diagnostics(evaluation: dict, ppo: dict) -> dict:
    """Descriptive off-policy anchors; not a test of stochastic-policy calibration."""
    if evaluation.get("source_profile") == "IRONCLAD_A20_ACT1":
        return {"schema": "sls-greedy-value-anchor-summary-v1", "acts": {},
                "limitation": "Frozen parent critic predicts Act1 returns. Do not reinterpret it as an Act2 success probability; raw anchors remain in evaluation evidence."}
    if ppo["gamma"] != 1 or ppo["failure_progress_scale"] != 0:
        raise ValueError("Win probability conversion requires gamma=1 and terminal +/-1")
    groups = {}
    scale = ppo["potential_scale"] if ppo["potential_shaping"] else 0
    for row in evaluation["result"]["seed_results"]:
        for act, anchor in row.get("act_entries", {}).items():
            probability = (anchor["value_shaped"] + scale * anchor["potential"] + 1) / 2
            groups.setdefault(act, []).append((probability, int(row["success"])))
    return {"schema": "sls-greedy-value-anchor-summary-v1",
            "limitation": "Critic predicts stochastic training policy; outcomes use greedy evaluation. Reached-act populations are policy dependent. Parent critic was trained for Act1, not Act2.",
            "acts": {act: {"samples": len(rows),
                            "mean_prediction_unclipped": sum(p for p, _ in rows) / len(rows),
                            "observed_full_horizon_rate": sum(y for _, y in rows) / len(rows),
                            "mean_squared_error_unclipped": sum((p-y)**2 for p, y in rows) / len(rows),
                            "out_of_range": sum(not 0 <= p <= 1 for p, _ in rows)}
                     for act, rows in groups.items()}}


def late_success_windows(metrics: list[dict], parent_steps: int) -> list[dict]:
    windows = [{"additional_step_range": [2_000_000, 3_000_000], "successes": 0},
               {"additional_step_range": [3_000_000, None], "successes": 0}]
    for row in metrics:
        step = row["environment_steps"] - parent_steps
        if "terminations_success" not in row or step < 2_000_000:
            continue
        windows[int(step >= 3_000_000)]["successes"] += int(row["terminations_success"])
    return windows


def require_pilot_health(evaluations: list[dict], metrics: list[dict]) -> dict:
    health = ("backend_errors", "backend_truncations", "step_limits", "cycle_limits", "timeouts")
    for evaluation in evaluations:
        if any(evaluation.get(key, -1) != 0 for key in health):
            raise ValueError("pilot evaluation health gate failed")
    totals = {f"terminations_{key}": 0 for key in ("backend_truncated", "step_limit", "cycle_limit")}
    for row in metrics:
        if "update_seconds" not in row:
            continue
        for key in totals:
            if row.get(key, -1) != 0:
                raise ValueError("pilot training health gate failed")
        if any(isinstance(value, float) and not math.isfinite(value) for value in row.values()):
            raise ValueError("pilot training has non-finite metrics")
    return {"evaluations_checked": len(evaluations), "training_limit_counts": totals}


def analyze(run: Path) -> dict:
    summary, _, selected = analyze_run(run, horizon=2)
    config = summary["config"]
    if config["run"].get("profile") != "IRONCLAD_A20_ACT2":
        raise ValueError("not an A20 Act1-2 run")
    reference = read(run / "reference-evaluation.json")
    endpoint = read(run / "endpoint-evaluation.json")
    warm = config["warm_start"]
    if (warm["transfer_kind"] != "curriculum-stage"
            or reference.get("schema") != "sls-frozen-reference-evaluation-v2"
            or reference.get("source_profile") != "IRONCLAD_A20_ACT1"
            or reference.get("evaluation_profile") != "IRONCLAD_A20_ACT2"
            or reference.get("checkpoint_sha256") != warm["checkpoint_sha256"]
            or endpoint["checkpoint_sha256"] != summary["verified_bundle_files"]["final.pt"]
            or endpoint["checkpoint_environment_steps"] != summary["checkpoints"]["final.pt"]["steps"]
            or endpoint["simulator"]["native_source_sha256"] != summary["native_source_sha256"]):
        raise ValueError("cross-horizon control or endpoint identity mismatch")
    if summary["checkpoints"]["final.pt"]["steps"] < config["stages"]["train"]["target_environment_steps"]:
        raise ValueError("pilot budget incomplete")
    interval = tuple(reference["seeds"])
    primary = paired_act12(reference, endpoint)
    metrics = [json.loads(line) for line in (run / "stages/train/metrics.jsonl").read_text().splitlines()]
    health = require_pilot_health(
        [reference["result"], endpoint["result"], selected["result"]]
        + [row["evaluation"] for row in metrics if "evaluation" in row], metrics,
    )
    windows = late_success_windows(metrics, warm["parent_environment_steps"])
    gain = primary["net"] / primary["paired_seeds"]
    meets = (gain >= 0.02 and primary["paired_difference_ci95_normal"][0] > 0
             and all(window["successes"] > 0 for window in windows))
    return {"schema": "sls-act12-pilot-analysis-v3", "run": summary,
            "endpoint_vs_parent": primary,
            "late_training_success_windows": windows,
            "health_gate": health,
            "research_decision": "SUPPORTS_REGISTERED_EXTENSION" if meets else "INCONCLUSIVE" if gain > 0 else "NO_DEVELOPMENT_GAIN",
            "decision_limit": "Only completed healthy runs pass analyze_run; incomplete runs are not evidence of method failure. Approximate paired interval, one training seed.",
            "value_diagnostics": {name: value_diagnostics(record, config["ppo"])
                                  for name, record in (("frozen_parent", reference), ("endpoint", endpoint), ("selected", selected))},
            "selected_vs_parent": paired_act12(reference, selected),
            "selected_vs_endpoint": paired_act12(endpoint, selected),
            "primary_metric": "normal-start probability of clearing both Act1 and Act2",
            "confirmation_diagnostics": {name: outcomes(record["result"], interval, horizon=2)
                                         for name, record in (("frozen_parent", reference),
                                                              ("endpoint", endpoint), ("selected", selected))},
            "boss_metric_note": "Per-act clears and full-horizon wins are distinct; Act2 boss groups are policy-dependent reached populations.",
            "inference_limit": "Pilot development evidence, one training seed, simulator only; no reserved final test."}


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--run", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    result = analyze(args.run)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    with args.output.open("x", encoding="utf-8") as stream:
        stream.write(json.dumps(result, indent=2, ensure_ascii=False) + "\n")
    print(json.dumps({"endpoint_vs_parent": result["endpoint_vs_parent"]["net"]}))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
