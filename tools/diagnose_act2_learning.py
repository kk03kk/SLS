"""CPU re-collection of <=16 hash-stratified historical development seeds."""
from __future__ import annotations

import argparse
import hashlib
import json
import os
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path[:0] = [str(ROOT / "src"), str(ROOT)]


def select_seeds(totals):
    rows = {name: {r["seed"]: r for r in value["seed_results"]} for name, value in totals.items()}
    seeds = sorted(rows["parent90"])
    if len(seeds) != 4096 or any(sorted(v) != seeds for v in rows.values()):
        raise ValueError("historical four-model seed alignment is incomplete")
    elites = {"BOOK_OF_STABBING", "GREMLIN_LEADER", "TASKMASTER"}
    def reached(row):
        return "2" in row["act_entries"]
    def matches(seed, group):
        values = [r[seed] for r in rows.values()]
        if group == "act1_loss":
            return reached(rows["parent90"][seed]) and not reached(rows["criticlatest"][seed])
        if group == "cross_act_cycle":
            return any(r["floor"] == 17 and "cycle" in r["reason"].lower() for r in values)
        if group == "act2_nonboss_failure":
            return any(reached(r) and not r["success"] and "DEATH" == r["reason"] and
                       not any(b.startswith("ACT_2:") for b in r["entered_bosses"]) for r in values)
        return any(not r["success"] and any(b.startswith("ACT_2:") for b in r["entered_bosses"]) for r in values)
    chosen = []
    for group in ("act1_loss", "cross_act_cycle", "act2_nonboss_failure", "act2_boss_failure"):
        candidates = [seed for seed in seeds if seed not in {r["seed"] for r in chosen} and matches(seed, group)]
        candidates.sort(key=lambda seed: hashlib.sha256(f"act2-r1:{group}:{seed}".encode()).hexdigest())
        if group == "act2_nonboss_failure":
            # Reserve two seeds for actual elite-enemy failures, two for other non-boss failures.
            elite = [s for s in candidates if any(set(r[s]["last_context"].get("enemy_ids", [])) & elites
                                                for r in rows.values())]
            ordinary = [s for s in candidates if s not in elite]
            candidates = elite[:2] + ordinary[:2]
        chosen.extend({"seed": seed, "selection_stratum": group} for seed in candidates[:4])
    return chosen, rows


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--summary", type=Path, required=True)
    parser.add_argument("--parent", type=Path, required=True)
    parser.add_argument("--critic-run", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--device", choices=("cpu",), default="cpu")
    args = parser.parse_args()
    if os.environ.get("CUDA_VISIBLE_DEVICES") != "-1":
        raise RuntimeError("disable CUDA before importing Torch")
    from sls.diagnostics.cpu import (
        analyze_returns,
        capture,
        compare,
        cpu_runtime,
        load_models,
        write_json,
    )
    from sls.research.protocol import NATIVE_SHA256, PARENT_SHA256
    from sls.rl.training_contract import native_source_digest, sha256_file

    if sha256_file(args.summary) != "39d6f7aa6d123ffc54d0cd565301a512b547ea13a91afb40cfe98d0594c7a627":
        raise ValueError("historical summary is not the previously verified 927398 evidence")
    if native_source_digest() != NATIVE_SHA256:
        raise ValueError("diagnostic environment differs from historical evaluation")
    if args.output.exists():
        raise FileExistsError("diagnostic namespace already exists")
    summary = json.loads(args.summary.read_text(encoding="utf-8"))
    selected, original = select_seeds(summary["totals"])
    checkpoints = {"parent90": args.parent, "criticbest": args.critic_run / "stages/train/selection/best_progress.pt",
                   "critic94": args.critic_run / "checkpoint-steps-000094011392.pt",
                   "criticlatest": args.critic_run / "latest.pt"}
    expected = {"parent90": PARENT_SHA256,
                "criticbest": "a804334af0060351a5e83a946c37a42a1a30445d809b21de3a220f2617e82870",
                "critic94": "70103ae500ebf154360fbb6ec16f897f300cebcb16ba6b8244082d946c45def1",
                "criticlatest": "be05e515fc892e1b09a2d8a87e30cac430e2f295c7ce820f3f91c7f0a838693d"}
    for label, path in checkpoints.items():
        if sha256_file(path) != expected[label]:
            raise ValueError(f"historical model identity mismatch: {label}")
    runtime, models = cpu_runtime(), load_models(checkpoints)
    args.output.mkdir(parents=True)
    write_json(args.output / "selection.json", {"source_sha256": sha256_file(args.summary), "seeds": selected,
        "role": "diagnostics-only; historical development re-use; forbidden for training",
        "selection": "fixed SHA256 ordering by predefined failure strata"})
    differences, outcomes = [], []
    for entry in selected:
        seed = entry["seed"]
        root = args.output / str(seed)
        manifest = capture(root, models, runtime, seed_start=seed, seed_count=1, max_states=4)
        analyze_returns(root, root / "returns.json")
        compare(root, root / "comparison.json", models, runtime, max_steps=256)
        for trajectory in manifest["trajectories"]:
            old, new = original[trajectory["model"]][seed], trajectory["outcome"]
            fields = {"success": (old["success"], new["success"]),
                      "floor": (old["floor"], new["final_observation"]["run"]["floor"]),
                      "steps": (old["steps"], new["steps"]),
                      "reason": (old["reason"].lower(), str(new["reason"]).lower())}
            mismatch = {k: v for k, v in fields.items() if v[0] != v[1]}
            record = {"seed": seed, "model": trajectory["model"], "cpu_outcome": new,
                      "server_outcome": old, "runtime_differences": mismatch}
            outcomes.append(record)
            if mismatch:
                differences.append({"seed": seed, "model": trajectory["model"], "fields": mismatch})
    write_json(args.output / "report.json", {"diagnostic_seeds": selected, "outcomes": outcomes,
        "runtime_differences": differences, "selected_states_maximum": 4 * len(selected),
        "claim": "CPU re-collected conditional diagnostic evidence; never the original server trajectories or a win-rate estimate"})


if __name__ == "__main__":
    main()
