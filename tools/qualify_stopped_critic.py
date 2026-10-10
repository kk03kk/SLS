"""Read-only paired development evaluation; never train or restore optimizer state."""
from __future__ import annotations

import argparse
import json
import os
import sys
import time
from dataclasses import asdict
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path[:0] = [str(ROOT / "src"), str(ROOT)]

from sls.diagnostics.critic_archive import paired_binary, require, sha


def validate_plan(plan, run, parent):
    require(plan["schema"] == "sls-stopped-critic-qualification-v1", "wrong qualification schema")
    require(plan["role"] == "STOPPED_CHECKPOINT_DEVELOPMENT_CONFIRMATION", "wrong evaluation role")
    require(plan["seed_range"] == [8000013000000, 8000013004096], "sealed or unregistered seeds")
    require(plan["profile"] == "IRONCLAD_A20_ACT2", "wrong horizon")
    require(type(plan["batch_size"]) is int and 1 <= plan["batch_size"] <= 512
            and type(plan["environment_shards"]) is int and 1 <= plan["environment_shards"] <= plan["batch_size"],
            "invalid fixed evaluation layout")
    paths, labels = {}, set()
    for model in plan["models"]:
        label = model["label"]
        require(label.isidentifier() and label not in labels, "invalid/duplicate model label")
        labels.add(label)
        if model["path"] == "FROZEN_PARENT":
            path = parent
        else:
            path = (run / model["path"]).resolve()
            require(path.is_relative_to(run.resolve()), "checkpoint outside historical run")
        require(sha(path) == model["sha256"], "historical checkpoint hash mismatch: " + label)
        paths[label] = path
    require("parent90" in paths, "reference missing")
    return paths


def combine_chunks(chunks, seeds):
    rows = [s for c in chunks for s in c["result"]["seed_results"]]
    require([s["seed"] for s in rows] == list(seeds), "missing/duplicate evaluation seeds")
    require(all(not any(c["result"].get(k, 0) for k in ("backend_errors", "backend_truncations", "timeouts"))
                for c in chunks), "evaluation execution failed")
    n = len(rows)
    return {"episodes": n, "successes": sum(s["success"] for s in rows),
            "reached_act2": sum("2" in s["act_entries"] for s in rows),
            "cycle_limits": sum(s["reason"] == "cycle_limit" for s in rows),
            "step_limits": sum(s["reason"] == "step_limit" for s in rows), "seed_results": rows}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--plan", type=Path, required=True)
    parser.add_argument("--run", type=Path, required=True)
    parser.add_argument("--parent", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--device", choices=("cpu", "cuda"), default="cpu")
    parser.add_argument("--smoke", action="store_true", help="Two research seeds; never confirmation")
    parser.add_argument("--dry-run", action="store_true")
    args = parser.parse_args()
    plan = json.loads(args.plan.read_text(encoding="utf-8"))
    paths = validate_plan(plan, args.run, args.parent)
    if args.dry_run:
        print(json.dumps({"status": "VALIDATED_INPUT_HASHES", "models": list(paths), "plan_sha256": sha(args.plan)}))
        return
    if args.device == "cpu":
        os.environ["CUDA_VISIBLE_DEVICES"] = "-1"
    os.environ["CUBLAS_WORKSPACE_CONFIG"] = ":4096:8"
    import torch

    from sls.curriculum import IRONCLAD_A20_ACT2
    from sls.rl.checkpoint import policy_from_training_checkpoint
    from sls.rl.evaluate import evaluate
    from sls.rl.training_contract import (
        evaluation_identity,
        git_state,
        native_artifact,
        native_source_digest,
        training_implementation_digest,
    )

    require(native_source_digest() == plan["native_source_sha256"], "wrong diagnostic native source")
    artifact = native_artifact()
    require(artifact["source_sha256"] == plan["native_source_sha256"], "stale native build")
    if args.device == "cuda":
        require(torch.cuda.is_available(), "GPU evaluation requested without CUDA")
    torch.set_num_threads(1)
    torch.use_deterministic_algorithms(True)
    torch.backends.cudnn.benchmark = False
    torch.set_float32_matmul_precision("high")
    require(not args.output.exists(), "qualification output exists; use a new run id")
    args.output.mkdir(parents=True)
    # Smoke namespace is research only, disjoint from confirmation and prior CPU seeds.
    seeds = range(132200000, 132200002) if args.smoke else range(*plan["seed_range"])
    batch = 2 if args.smoke else plan["batch_size"]
    shards = 0 if args.device == "cpu" else plan["environment_shards"]
    runtime = evaluation_identity(device=args.device, environment_shards=shards, ascension=20)["runtime"]
    env = {"native_source_sha256": native_source_digest(), "native_artifact": artifact,
           "training_implementation_sha256": training_implementation_digest(), "git": git_state(), "runtime": runtime}
    totals = {}
    for label, path in paths.items():
        payload = torch.load(path, map_location="cpu", weights_only=False)
        policy = policy_from_training_checkpoint(payload, device=args.device)
        require(payload["trainer"]["environment_steps"] == next(m["steps"] for m in plan["models"] if m["label"] == label),
                "checkpoint steps differ from plan")
        chunks = []
        for offset in range(0, len(seeds), batch):
            chunk_seeds = tuple(seeds[offset:offset + batch])
            started = time.monotonic()
            result = asdict(evaluate(policy, IRONCLAD_A20_ACT2, chunk_seeds, device=args.device,
                max_steps=4096, max_boundary_visits=4, failure_progress_scale=0., environment_shards=shards,
                crash_dump_dir=args.output / "crashes"))
            record = {"model": label, "checkpoint_sha256": sha(path), **env,
                      "seed_range": [chunk_seeds[0], chunk_seeds[-1] + 1], "elapsed_seconds": time.monotonic() - started,
                      "result": result}
            with (args.output / f"{label}-{offset:04d}.json").open("x", encoding="utf-8") as stream:
                json.dump(record, stream, indent=2, allow_nan=False)
            chunks.append(record)
            print(json.dumps({"model": label, "completed": offset + len(chunk_seeds), "wins": result["successes"]}), flush=True)
        totals[label] = combine_chunks(chunks, seeds)
        require(sha(path) == next(m["sha256"] for m in plan["models"] if m["label"] == label), "historical weights changed")
    reference = {s["seed"]: {**s, "reached_act2": "2" in s["act_entries"]} for s in totals["parent90"]["seed_results"]}
    pairs = {}
    for label, total in totals.items():
        candidate = {s["seed"]: {**s, "reached_act2": "2" in s["act_entries"]} for s in total["seed_results"]}
        pairs[label] = {key: paired_binary(reference, candidate, key) for key in ("success", "reached_act2")}
    report = {"schema": "sls-stopped-critic-confirmation-v1", "role": "CPU_SMOKE_NOT_CONFIRMATION" if args.smoke else plan["role"],
              "plan_sha256": sha(args.plan), **env, "batch_size": batch, "seed_range": [seeds[0], seeds[-1] + 1],
              "totals": totals, "paired": pairs,
              "limitations": ["Stopped checkpoints, not the registered 20M fixed endpoint.",
                  "Development confirmation; sealed final holdout remains unused.",
                  "Batch size is fixed across models but differs from historical 512-seed periodic evaluation.",
                  "One training seed; multiple checkpoint comparisons are exploratory; no warmup causal claim."]}
    with (args.output / "summary.json").open("x", encoding="utf-8") as stream:
        json.dump(report, stream, indent=2, allow_nan=False)


if __name__ == "__main__":
    main()
