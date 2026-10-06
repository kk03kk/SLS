"""Audit an Act1-2 download; explicitly allow ONLY the omitted policy export.

Original evidence stays read-only. No simulator execution, training or holdout
evaluation. Checkpoints must be from the user's trusted local training archive.
"""
from __future__ import annotations

import argparse
import contextlib
import io
import json
import statistics
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))

from tools.analyze_act12_pilot import (
    late_success_windows,
    paired_act12,
    require_pilot_health,
    value_diagnostics,
)
from tools.analyze_reward_screen import analyze_run, outcomes, read
from tools.compare_run_arms import _from_seed_results, report


def binary_pair(left: list[dict], right: list[dict], *, reach: bool) -> dict:
    def rows(items):
        return [{**row, "success": "2" in row["bosses"] if reach else row["success"]}
                for row in items]
    with contextlib.redirect_stdout(io.StringIO()):
        return report(_from_seed_results("reference", "development", rows(left)),
                      _from_seed_results("candidate", "development", rows(right)), 256)


def entry_summary(rows: list[dict], act: str) -> dict:
    anchors = [row["act_entries"][act] for row in rows if act in row.get("act_entries", {})]
    if not anchors:
        return {"samples": 0}
    return {"samples": len(anchors), **{
        name: statistics.mean(fn(anchor) for anchor in anchors)
        for name, fn in {
            "hp_fraction": lambda a: a["hp"] / a["max_hp"],
            "gold": lambda a: a["gold"], "deck_size": lambda a: len(a["deck"]),
            "relic_count": lambda a: len(a["relics"]),
            "potion_count": lambda a: len(a["potions"]),
            "value_shaped": lambda a: a["value_shaped"],
        }.items()}}


def analyze(run: Path) -> dict:
    summary, evaluated, selected = analyze_run(run, horizon=2, allow_missing_export=True)
    config = summary["config"]
    manifest = read(run / "run-manifest.json")
    reference = read(run / "reference-evaluation.json")
    endpoint = read(run / "endpoint-evaluation.json")
    warm = config["warm_start"]
    if (config["run"]["profile"] != "IRONCLAD_A20_ACT2"
            or warm["transfer_kind"] != "curriculum-stage"
            or reference["schema"] != "sls-frozen-reference-evaluation-v2"
            or reference["source_profile"] != "IRONCLAD_A20_ACT1"
            or reference["evaluation_profile"] != "IRONCLAD_A20_ACT2"
            or reference["checkpoint_sha256"] != warm["checkpoint_sha256"]
            or reference["checkpoint_environment_steps"] != warm["parent_environment_steps"]
            or endpoint["checkpoint_sha256"] != summary["checkpoints"]["final.pt"]["sha256"]
            or endpoint["checkpoint_environment_steps"] != summary["checkpoints"]["final.pt"]["steps"]
            or endpoint["simulator"]["native_source_sha256"] != summary["native_source_sha256"]
            or tuple(endpoint["seeds"]) != tuple(manifest["final_evaluation_seeds"])):
        raise ValueError("pilot parent/endpoint/config identity mismatch")
    target = config["stages"]["train"]["target_environment_steps"]
    steps = summary["checkpoints"]["final.pt"]["steps"]
    if not target <= steps < target + config["ppo"]["rollout_steps"] * manifest["workers"]:
        raise ValueError("budget incomplete or excessive overshoot")
    metrics = [json.loads(line) for line in (run / "stages/train/metrics.jsonl").read_text().splitlines()]
    updates = [row for row in metrics if "update_seconds" in row]
    health = require_pilot_health([reference["result"], endpoint["result"], selected["result"]]
                                  + [row["evaluation"] for row in evaluated], metrics)
    termination_counts = {name: int(sum(row[f"terminations_{name}"] for row in updates))
                          for name in manifest["termination_counts"]}
    if termination_counts != manifest["termination_counts"] or len(updates) != manifest["updates"]:
        raise ValueError("training aggregate counts disagree")
    records = {"frozen_parent": reference, "endpoint": endpoint, "selected": selected}
    interval = tuple(reference["seeds"])
    primary = paired_act12(reference, endpoint)
    windows = late_success_windows(metrics, warm["parent_environment_steps"])
    gain = primary["net"] / primary["paired_seeds"]
    supports = gain >= .02 and primary["paired_difference_ci95_normal"][0] > 0 and all(w["successes"] for w in windows)
    by_phase = []
    for start, stop in ((0, 1_000_000), (1_000_000, 2_000_000), (2_000_000, 3_000_000), (3_000_000, 5_000_000)):
        subset = [row for row in updates if start <= row["environment_steps"] - warm["parent_environment_steps"] < stop]
        by_phase.append({"new_decisions_window": [start, min(stop, summary["new_decisions"])],
                         "updates": len(subset),
                         "successes": sum(row["terminations_success"] for row in subset),
                         "deaths": sum(row["terminations_death"] for row in subset),
                         "means": {key: statistics.mean(row[key] for row in subset) for key in (
                             "entropy", "approx_kl_final", "clip_fraction", "gradient_clip_fraction",
                             "value_explained_variance", "value", "advantage_scale_combat",
                             "advantage_scale_run", "advantage_scale_choice", "advantage_mean_run",
                             "neow_mean_normalized_entropy", "neow_mean_swap_probability")}})
    lrows = reference["result"]["seed_results"]
    reach_pairs, entry_stats, boss_pairs = {}, {}, {}
    for name, record in records.items():
        rows = record["result"]["seed_results"]
        reach_pairs[name] = binary_pair(lrows, rows, reach=True)
        common = {r["seed"] for r in lrows if "2" in r["bosses"]} & {r["seed"] for r in rows if "2" in r["bosses"]}
        entry_stats[name] = {"all_reached": entry_summary(rows, "2"),
                             "both_reached_descriptive": entry_summary([r for r in rows if r["seed"] in common], "2"),
                             "common_population": len(common)}
        boss_pairs[name] = {}
        for boss in sorted({r["bosses"]["1"] for r in lrows}):
            left = [r for r in lrows if r["bosses"]["1"] == boss]
            right = [r for r in rows if r["bosses"]["1"] == boss]
            boss_pairs[name][boss] = {"joint": binary_pair(left, right, reach=False),
                                      "act2_reach": binary_pair(left, right, reach=True)}
    timing = summary["timing_seconds"]
    return {"schema": "sls-act12-download-analysis-v1", "run": summary,
            "primary_metric": "normal-Neow-start Act1+Act2 joint clear probability",
            "endpoint_vs_parent": primary, "selected_vs_parent": paired_act12(reference, selected),
            "selected_vs_endpoint": paired_act12(endpoint, selected),
            "research_decision": "SUPPORTS_REGISTERED_EXTENSION" if supports else "INCONCLUSIVE" if gain > 0 else "NO_DEVELOPMENT_GAIN",
            "health_gate": health, "late_success_windows": windows,
            "training_termination_counts": termination_counts, "training_phases": by_phase,
            "act2_reach_pairs": reach_pairs, "initial_boss_pairs": boss_pairs,
            "confirmation_diagnostics": {name: outcomes(record["result"], interval, horizon=2) for name, record in records.items()},
            "value_diagnostics": {name: value_diagnostics(record, config["ppo"]) for name, record in records.items()},
            "entry_state_descriptive": entry_stats,
            "update_decisions_per_second": summary["new_decisions"] / timing["update_seconds"],
            "missing_checkpoint_history": "Intermediate checkpoint files are not included; periodic outcomes remain verifiable in hashed metrics. No reconstruction attempted.",
            "limitations": "One training seed; development-only simulator evidence. Reach-conditioned and common-reach populations do not isolate causal combat/deck effects. Greedy critic anchors are off-policy. No final holdout accessed."}


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--run", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    result = analyze(args.run)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    with args.output.open("x", encoding="utf-8") as stream:
        stream.write(json.dumps(result, indent=2, ensure_ascii=False) + "\n")
    print(json.dumps({"decision": result["research_decision"], "paired": result["endpoint_vs_parent"]}))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
