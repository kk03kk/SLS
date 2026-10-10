"""Globally stratify already collected histories, with explicit selection branches."""
from __future__ import annotations

import argparse
import gzip
import hashlib
import json
import shutil
import sys
from collections import Counter, defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path[:0] = [str(ROOT / "src"), str(ROOT)]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    from sls.backends.simulator import SimulatorBackend
    from sls.contracts import Action
    from sls.diagnostics.cpu import (
        PROFILE,
        analyze_returns,
        compare,
        cpu_runtime,
        decision_record,
        digest,
        identity,
        load_models,
        read_history,
        selection_sources,
        stratum,
        validated_corpus,
        write_json,
    )
    from sls.rl.episode_limit import EpisodeLimitState
    from sls.rl.training_contract import sha256_file

    runtime = cpu_runtime()
    report = json.loads((args.source / "report.json").read_text())
    if args.output.exists():
        raise FileExistsError("global diagnostic namespace already exists")
    args.output.mkdir(parents=True)
    trajectories, source_identities = [], []
    models, groups = None, defaultdict(list)
    natural, choices, encountered = Counter(), Counter(), set()
    sources = set()
    for selected in report["diagnostic_seeds"]:
        source = args.source / str(selected["seed"])
        manifest = validated_corpus(source)
        if models is None:
            models = load_models({k: Path(v["checkpoint"]) for k, v in manifest["models"].items()})
        if any(v["sha256"] != models[k]["identity"]["sha256"] for k, v in manifest["models"].items()):
            raise ValueError("input corpora use different models")
        source_identities.append({"path": str(source), "manifest_sha256": sha256_file(source / "manifest.json"),
                                  "implementation_sha256": manifest["implementation_sha256"]})
        for original in manifest["trajectories"]:
            trajectory = dict(original)
            target = args.output / "public" / Path(original["public_path"]).name
            target.parent.mkdir(exist_ok=True)
            shutil.copyfile(source / original["public_path"], target)
            trajectory["public_path"] = target.relative_to(args.output).as_posix()
            trajectories.append(trajectory)
            rows, _ = read_history(target)
            for row in rows:
                base = stratum(row)
                branch = sorted(selection_sources(row))
                key = base + (":" + "+".join(branch) + f":selected{len(row['observation']['selected_cards'])}" if branch else "")
                state_id = hashlib.sha256(f"{trajectory['model']}:{trajectory['seed']}:{row['step']}".encode()).hexdigest()
                groups[key].append({"id": state_id, "trajectory": trajectory["id"], "step": row["step"],
                    "stratum": key, "public_sha256": digest({k: row[k] for k in ("observation", "actions")})})
                natural[base] += 1
                encountered.update(e["monster_id"] for e in row["observation"]["enemies"])
                if branch:
                    choices[key] += 1
                    sources.update(branch)
    for values in groups.values():
        values.sort(key=lambda state: state["id"])
    keys = sorted(groups, key=lambda key: hashlib.sha256(key.encode()).hexdigest())
    selected, depth = [], 0
    while len(selected) < 64:
        extra = [groups[key][depth] for key in keys if len(groups[key]) > depth]
        if not extra:
            break
        selected.extend(extra[:64 - len(selected)])
        depth += 1
    by_trajectory = defaultdict(list)
    for state in selected:
        by_trajectory[state["trajectory"]].append(state)
    selected_enemies = set()
    for trajectory in trajectories:
        wanted = {s["step"]: s for s in by_trajectory[trajectory["id"]]}
        if not wanted:
            continue
        rows, _ = read_history(args.output / trajectory["public_path"])
        backend = SimulatorBackend(PROFILE)
        decision = backend.reset(trajectory["seed"])
        limits = EpisodeLimitState.initial(decision)
        for row in rows[:max(wanted) + 1]:
            if decision_record(decision) != {k: row[k] for k in ("observation", "actions")}:
                raise ValueError("global boundary differs from natural public history")
            if row["step"] in wanted:
                state = wanted[row["step"]]
                selected_enemies.update(e.monster_id for e in decision.observation.enemies)
                target = args.output / "private" / f"{state['id']}.json.gz"
                target.parent.mkdir(exist_ok=True)
                with gzip.open(target, "xt", encoding="utf-8") as stream:
                    json.dump({"native": backend.checkpoint(), "limits": limits.to_dict()}, stream)
                state["private_path"] = target.relative_to(args.output).as_posix()
                state["private_sha256"] = sha256_file(target)
            transition = backend.step(Action.from_dict(row["chosen_action"]))
            decision = transition.decision
            if not transition.terminated and not transition.truncated:
                limits.observe(decision, max_steps=4096, max_boundary_visits=4)
    bosses = {"THE_GUARDIAN", "HEXAGHOST", "SLIME_BOSS", "THE_CHAMP", "THE_COLLECTOR", "BRONZE_AUTOMATON"}
    manifest = {"schema": "sls-cpu-natural-corpus-v1", **identity(runtime), "seed_range": None,
        "seed_ids": [s["seed"] for s in report["diagnostic_seeds"]], "seed_scan_files": [],
        "models": {k: v["identity"] for k, v in models.items()}, "trajectories": trajectories,
        "states": sorted(selected, key=lambda s: s["id"]), "natural_strata": dict(natural),
        "selection_branch_coverage": dict(choices), "missing_known_choice_sources": sorted({"HAND", "MASTER_DECK", "GENERATED", "DISCARD", "EXHAUST", "DRAW"} - sources),
        "missing_boss_contexts": sorted(bosses - encountered), "missing_selected_boss_contexts": sorted(bosses - selected_enemies),
        "selection": "global SHA256 round-robin act/screen/enemies plus public selection source/count; no future labels",
        "input_corpora": source_identities, "training_eligible": False}
    write_json(args.output / "manifest.json", manifest)
    analyze_returns(args.output, args.output / "returns.json")
    compare(args.output, args.output / "comparison.json", models, runtime, max_steps=256)


if __name__ == "__main__":
    main()
