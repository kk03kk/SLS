"""Recompute descriptive diagnostic metrics; these samples are never win-rate evals."""
from __future__ import annotations

import argparse
import gzip
import json
import math
import os
import statistics
import sys
from collections import Counter, defaultdict
from pathlib import Path

os.environ["CUDA_VISIBLE_DEVICES"] = "-1"
ROOT = Path(__file__).resolve().parents[1]
sys.path[:0] = [str(ROOT / "src"), str(ROOT)]

from sls.diagnostics.cpu import (
    cpu_runtime,
    read_json,
    selection_sources,
    sha256_file,
    validated_corpus,
    write_json,
)


def average(values):
    return statistics.mean(values) if values else None


def js_divergence(a, b):
    middle = [(x+y)/2 for x, y in zip(a, b, strict=True)]
    return sum(x * math.log(x/m) if x else 0 for x, m in zip(a, middle, strict=True))/2 + sum(
        x * math.log(x/m) if x else 0 for x, m in zip(b, middle, strict=True))/2


def summarize(corpus, comparison, returns, output):
    cpu_runtime()
    manifest = validated_corpus(corpus)
    compared, mc = read_json(comparison), read_json(returns)
    corpus_hash = sha256_file(corpus / "manifest.json")
    if compared["corpus_sha256"] != corpus_hash or mc["corpus_sha256"] != corpus_hash:
        raise ValueError("reports refer to different state banks")
    stats = {}
    for label in manifest["models"]:
        stats[label] = {"trajectories": 0, "reached_act2": 0, "successes": 0,
                        "reasons": Counter(), "decision_screens": Counter(),
                        "act2_entry_deck_sizes": [], "act2_entry_cards": Counter(),
                        "act2_entry_relics": Counter(), "cycles": [], "value_by_act": {}}
    branch_coverage, selected_bosses = Counter(), set()
    selected_ids = {(s["trajectory"], s["step"]) for s in manifest["states"]}
    for trajectory in manifest["trajectories"]:
        s = stats[trajectory["model"]]
        s["trajectories"] += 1
        outcome = trajectory["outcome"]
        s["successes"] += int(outcome["success"])
        s["reasons"][outcome["reason"]] += 1
        reached = False
        # Hash validation above binds these primitives to capture; no private state is read.
        with gzip.open(corpus / trajectory["public_path"], "rt", encoding="utf-8") as stream:
            for line in stream:
                row = json.loads(line)
                if row["record_type"] != "decision":
                    continue
                obs = row["observation"]
                s["decision_screens"][f"act{obs['run']['act']}:{obs['screen']}"] += 1
                sources = selection_sources(row)
                if sources:
                    branch_coverage[obs["screen"] + ":" + "+".join(sorted(sources))] += 1
                if (trajectory["id"], row["step"]) in selected_ids:
                    selected_bosses.update(e["monster_id"] for e in obs["enemies"])
                if obs["run"]["act"] == 2 and not reached:
                    reached = True
                    s["reached_act2"] += 1
                    s["act2_entry_deck_sizes"].append(len(obs["deck"]))
                    s["act2_entry_cards"].update(c["card_id"] for c in obs["deck"])
                    s["act2_entry_relics"].update(c["content_id"] for c in obs["relics"])
        if outcome["reason"] == "cycle_limit":
            s["cycles"].append({"trajectory": trajectory["id"], "tail": outcome["cycle_tail"]})
    for label, s in stats.items():
        s["act2_entry_deck_size_mean"] = average(s["act2_entry_deck_sizes"])
        reports = [t for t in mc["trajectories"] if t.get("model") == label and t["complete"]]
        s["initial_value_mean"] = average([t["initial_prediction"] for t in reports])
        s["initial_mc_mean"] = average([t["initial_mc_return"] for t in reports])
        for act in ("1", "2"):
            rows = [t["by_act"][act] for t in reports if act in t["by_act"]]
            n = sum(t["samples"] for t in rows)
            s["value_by_act"][act] = {"samples": n,
                                     "mse": sum(t["mse"] * t["samples"] for t in rows)/n if n else None}
    by_state = defaultdict(dict)
    for row in compared["states"]:
        by_state[row["state"]][row["model"]] = row
    pairwise = {}
    labels = sorted(compared["models"])
    for i, a in enumerate(labels):
        for b in labels[i+1:]:
            strata = defaultdict(list)
            for state, models in by_state.items():
                x, y = models[a], models[b]
                if x["actions"] != y["actions"]:
                    raise ValueError("action distribution alignment differs")
                strata[x["stratum"].split(":")[1]].append({
                    "state": state, "disagrees": x["chosen_action"] != y["chosen_action"],
                    "js": js_divergence(x["probabilities"], y["probabilities"]),
                    "value_difference": x["value_shaped"] - y["value_shaped"]})
            pairwise[a + ":" + b] = {screen: {"states": len(rows),
                "action_disagreements": sum(r["disagrees"] for r in rows),
                "mean_js_nats": average([r["js"] for r in rows]),
                "mean_shaped_value_difference": average([r["value_difference"] for r in rows])}
                for screen, rows in strata.items()}
    continuations = {}
    for label in labels:
        rows = [r["continuation"] for r in compared["states"] if r["model"] == label]
        continuations[label] = {"states": len(rows), "complete": sum(r["complete"] for r in rows),
                               "successes": sum(r["success"] for r in rows),
                               "unfinished": sum(not r["complete"] for r in rows),
                               "reasons": dict(Counter(r["reason"] for r in rows)),
                               "complete_return_mean": average([r["shaped_return"] for r in rows if r["complete"]])}
    boss_states = {state: models for state, models in by_state.items()
                   if next(iter(models.values()))["stratum"].startswith("act2:COMBAT:") and
                   any(boss in next(iter(models.values()))["stratum"] for boss in
                       ("THE_CHAMP", "THE_COLLECTOR", "BRONZE_AUTOMATON"))}
    result = {"schema": "sls-cpu-diagnostic-summary-v2", "corpus_sha256": corpus_hash,
              "comparison_sha256": sha256_file(comparison), "returns_sha256": sha256_file(returns),
              "native_source_sha256": manifest["native_source_sha256"],
              "models": manifest["models"], "natural_trajectories": stats,
              "natural_choice_branch_coverage": dict(branch_coverage),
              "selected_encounters": sorted(selected_bosses),
              "states_by_stratum": dict(Counter(s["stratum"] for s in manifest["states"])),
              "pairwise_by_screen": pairwise, "matched_continuations": continuations,
              "act2_boss_combat_controls": {
                  "states": len(boss_states),
                  "scope": "SAME_STARTING_DECK_RESOURCES_HISTORY_AND_NATIVE_STATE; ACT2_HORIZON_ENDS_AT_BOSS_CLEAR",
                  "models": {label: {"successes": sum(rows[label]["continuation"]["success"] for rows in boss_states.values()),
                                     "unfinished": sum(not rows[label]["continuation"]["complete"] for rows in boss_states.values())}
                             for label in labels},
                  "state_ids": sorted(boss_states)},
              "jointly_completed_states": sum(all(r["continuation"]["complete"] for r in models.values())
                                               for models in by_state.values()),
              "conditional_success_states": {state: [label for label, row in models.items()
                                                       if row["continuation"]["success"]]
                                              for state, models in by_state.items()
                                              if any(row["continuation"]["success"] for row in models.values())},
              "scope": "DESCRIPTIVE_MIGRATION_DIAGNOSTICS_NOT_FORMAL_WIN_RATES_OR_STOCHASTIC_CRITIC_CALIBRATION"}
    write_json(output, result)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--corpus", type=Path, required=True)
    parser.add_argument("--comparison", type=Path, required=True)
    parser.add_argument("--returns", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    summarize(args.corpus, args.comparison, args.returns, args.output)


if __name__ == "__main__":
    main()
