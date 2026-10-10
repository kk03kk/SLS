"""Reproducible descriptive summary; selected diagnostics are never win rates."""
from __future__ import annotations

import argparse
import gzip
import hashlib
import json
import statistics
from collections import Counter, defaultdict
from pathlib import Path


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--corpus", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    report = json.loads((args.corpus / "report.json").read_text())
    results, returns, inputs = [], [], []
    strata, natural = Counter(), Counter()
    seen_bosses, selected_bosses, implementations = set(), set(), set()
    forks, loops = [], []
    required_bosses = {"THE_GUARDIAN", "HEXAGHOST", "SLIME_BOSS", "THE_CHAMP", "THE_COLLECTOR", "BRONZE_AUTOMATON"}
    for entry in report["diagnostic_seeds"]:
        root = args.corpus / str(entry["seed"])
        manifest = json.loads((root / "manifest.json").read_text())
        comparison = json.loads((root / "comparison.json").read_text())
        if comparison["corpus_sha256"] != sha(root / "manifest.json"):
            raise ValueError("comparison belongs to another corpus")
        inputs.extend({"path": p.relative_to(args.corpus).as_posix(), "sha256": sha(p)}
                      for p in (root / "manifest.json", root / "comparison.json", root / "returns.json"))
        results.extend(comparison["states"])
        returns.extend(json.loads((root / "returns.json").read_text())["trajectories"])
        implementations.add(manifest["implementation_sha256"])
        strata.update(state["stratum"] for state in manifest["states"])
        natural.update(manifest["natural_strata"])
        seen_bosses.update(required_bosses - set(manifest["missing_boss_contexts"]))
        selected_bosses.update(required_bosses - set(manifest["missing_selected_boss_contexts"]))
        histories = {}
        for trajectory in manifest["trajectories"]:
            path = root / trajectory["public_path"]
            if sha(path) != trajectory["sha256"]:
                raise ValueError("public history digest mismatch")
            with gzip.open(path, "rt", encoding="utf-8") as stream:
                records = [json.loads(line) for line in stream]
            histories[trajectory["model"]] = records[1:-1]
            if trajectory["outcome"]["reason"] == "cycle_limit":
                tail = trajectory["outcome"]["cycle_tail"]
                loops.append({"seed": entry["seed"], "model": trajectory["model"],
                    "floor": trajectory["outcome"]["final_observation"]["run"]["floor"],
                    "selected_card_counts": [len(row["selected_cards"]) for row in tail[-8:]],
                    "last_actions": [row["action"] for row in tail[-8:]]})
        for label, rows in histories.items():
            if label == "parent90":
                continue
            for index, (reference, candidate) in enumerate(zip(histories["parent90"], rows)):
                if any(reference[k] != candidate[k] for k in ("observation", "actions")):
                    break
                if reference["chosen_action"] != candidate["chosen_action"]:
                    forks.append({"seed": entry["seed"], "model": label, "first_differing_decision": index,
                        "act": reference["observation"]["run"]["act"], "screen": reference["observation"]["screen"],
                        "parent_action": reference["chosen_action"], "candidate_action": candidate["chosen_action"]})
                    break
    grouped = defaultdict(dict)
    for row in results:
        grouped[row["state"]][row["model"]] = row
    paired = {}
    for label in ("criticbest", "critic94", "criticlatest"):
        pairs = [(g["parent90"], g[label]) for g in grouped.values()]
        choice_pairs = [(a, b) for a, b in pairs if len(a["actions"]) > 1]
        combat_pairs = [(a, b) for a, b in pairs if a["stratum"].startswith("act2:COMBAT:")]
        paired[label] = {"states": len(pairs), "nonforced_states": len(choice_pairs),
            "different_argmax": sum(a["chosen_action"] != b["chosen_action"] for a, b in choice_pairs),
            "mean_probability_total_variation": statistics.mean(sum(abs(p - q) for p, q in zip(a["probabilities"], b["probabilities"])) / 2
                                                                 for a, b in choice_pairs) if choice_pairs else None,
            "act2_combat_states": len(combat_pairs),
            "act2_combat_continuations": [{"state": a["state"], "stratum": a["stratum"],
                                           "parent": a["continuation"], "candidate": b["continuation"]} for a, b in combat_pairs]}
    summary = {"schema": "sls-act2-mechanism-evidence-v1", "input_manifest": inputs,
        "seeds": len(report["diagnostic_seeds"]), "trajectories": len(report["outcomes"]),
        "same_states": len(grouped), "model_state_scores": len(results),
        "unfinished_continuations": sum(not r["continuation"]["complete"] for r in results),
        "runtime_differences": report["runtime_differences"], "selected_strata": dict(strata),
        "natural_decision_strata": dict(natural), "missing_natural_bosses": sorted(required_bosses - seen_bosses),
        "missing_selected_bosses": sorted(required_bosses - selected_bosses),
        "first_policy_forks": forks, "cycle_evidence": loops, "same_state_comparisons": paired,
        "complete_behavior_returns": returns, "recorded_training_implementation_digests": sorted(implementations),
        "claim": "Failure-selected CPU diagnostics; not formal win rates or causal attribution",
        "provenance_note": "Captured while unrelated research training modules were developed. Read-only inference, model, encoder, reward, limiter and native sources stayed fixed; each corpus preserves its contemporaneous implementation digest."}
    args.output.parent.mkdir(parents=True, exist_ok=True)
    with args.output.open("x", encoding="utf-8") as stream:
        json.dump(summary, stream, indent=2)


if __name__ == "__main__":
    main()
