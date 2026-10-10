"""Summarize a globally stratified natural corpus without estimating win rates."""
from __future__ import annotations

import argparse
import hashlib
import json
import statistics
from collections import Counter, defaultdict
from pathlib import Path


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--corpus", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    paths = [args.corpus / name for name in ("manifest.json", "comparison.json", "returns.json")]
    manifest, comparison, returns = [json.loads(p.read_text(encoding="utf-8")) for p in paths]
    hashes = {p.name: hashlib.sha256(p.read_bytes()).hexdigest() for p in paths}
    if comparison["corpus_sha256"] != hashes["manifest.json"]:
        raise ValueError("comparison/corpus identity mismatch")
    grouped = defaultdict(dict)
    for row in comparison["states"]:
        grouped[row["state"]][row["model"]] = row
    expected = set(manifest["models"])
    if len(grouped) != len(manifest["states"]) or any(set(g) != expected for g in grouped.values()):
        raise ValueError("incomplete model/state comparison")
    paired = {}
    for label in sorted(expected - {"parent90"}):
        pairs = [(g["parent90"], g[label]) for g in grouped.values()]
        choices = [(a, b) for a, b in pairs if len(a["actions"]) > 1]
        combat = [(a, b) for a, b in pairs if a["stratum"].startswith("act2:COMBAT:")]
        paired[label] = {
            "nonforced_states": len(choices),
            "different_argmax": sum(a["chosen_action"] != b["chosen_action"] for a, b in choices),
            "mean_probability_total_variation": statistics.mean(
                sum(abs(p - q) for p, q in zip(a["probabilities"], b["probabilities"])) / 2
                for a, b in choices) if choices else None,
            "act2_combat_states": len(combat),
            "act2_combat_continuations": [
                {"state": a["state"], "stratum": a["stratum"],
                 "parent": a["continuation"], "candidate": b["continuation"]}
                for a, b in combat],
        }
    selection = [r for r in comparison["states"] if ":MASTER_DECK:" in r["stratum"]]
    summary = {
        "schema": "sls-act2-global-mechanism-evidence-v1",
        "claim": "Failure-selected natural CPU diagnostics, not win-rate estimates or causal attribution",
        "input_sha256": hashes,
        "corpus_git": manifest.get("git"),
        "implementation_sha256": manifest["implementation_sha256"],
        "native": manifest.get("native"),
        "models": manifest["models"],
        "seeds": len(manifest["seed_ids"]), "trajectories": len(manifest["trajectories"]),
        "states": len(grouped), "model_state_scores": len(comparison["states"]),
        "unfinished_continuations": sum(not r["continuation"]["complete"] for r in comparison["states"]),
        "continuation_reasons": dict(Counter(r["continuation"]["reason"] for r in comparison["states"])),
        "selected_strata": dict(Counter(s["stratum"] for s in manifest["states"])),
        "missing_natural_bosses": manifest["missing_boss_contexts"],
        "missing_selected_bosses": manifest["missing_selected_boss_contexts"],
        "missing_known_choice_sources": manifest["missing_known_choice_sources"],
        "same_state_comparisons": paired,
        "selected_card_branch_scores": [
            {k: row[k] for k in ("state", "stratum", "model", "chosen_action", "actions", "probabilities", "continuation")}
            for row in selection],
        "complete_behavior_returns": returns["trajectories"],
        "training_eligible": False,
        "provenance_note": "Public histories retain original digests and contemporaneous identities; global replay/private boundaries and comparisons have their own clean committed source identity.",
    }
    args.output.parent.mkdir(parents=True, exist_ok=True)
    with args.output.open("x", encoding="utf-8") as stream:
        json.dump(summary, stream, indent=2, ensure_ascii=False)


if __name__ == "__main__":
    main()
