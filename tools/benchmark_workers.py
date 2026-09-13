"""Benchmark and select the smallest near-peak FullRun worker layout."""

from __future__ import annotations

import argparse
import gc
import json
import math
import os
import random
import sys
import time
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "src"))
os.environ.setdefault("CUBLAS_WORKSPACE_CONFIG", ":4096:8")

import torch

from sls.content.scope import ironclad_scope_contract
from sls.curriculum import CURRICULUM_PROFILES_BY_ID, IRONCLAD_A0_FULLRUN
from sls.model import ENCODING_SCHEMA, ModelConfig, Policy, vocabulary_hash
from sls.rl import PPOConfig, PPOTrainer, ShardedWorkerPool
from sls.rl.preparation import read_config, workload_contract
from sls.rl.training_contract import (
    canonical_digest,
    git_state,
    native_artifact,
    native_source_digest,
)

DEFAULT_LAYOUTS = ((16, 8), (24, 8), (32, 8), (32, 16), (48, 16))
BENCHMARK_SCHEMA = "sls-worker-benchmark-v2"


def _rss_bytes(pids: list[int]) -> int | None:
    """Sample combined parent/shard RSS on Linux, including shared pages."""
    total = 0
    try:
        for pid in pids:
            for line in Path(f"/proc/{pid}/status").read_text().splitlines():
                if line.startswith("VmRSS:"):
                    total += int(line.split()[1]) * 1024
        return total
    except OSError:
        return None


def _layout(value: str) -> tuple[int, int]:
    try:
        workers, shards = (int(item) for item in value.split(":", 1))
    except (TypeError, ValueError) as error:
        raise argparse.ArgumentTypeError("layout must be WORKERS:SHARDS") from error
    if workers <= 0 or shards <= 0 or shards > workers:
        raise argparse.ArgumentTypeError("layout counts are invalid")
    return workers, shards


def _parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--layouts", type=_layout, nargs="+",
        default=DEFAULT_LAYOUTS,
    )
    parser.add_argument("--config", type=Path)
    parser.add_argument("--rounds", type=int, default=3)
    parser.add_argument("--rollout-steps", type=int, default=64)
    parser.add_argument(
        "--output", type=Path,
        default=ROOT / "local" / "runs" / "worker-benchmark.json",
    )
    parser.add_argument(
        "--allow-dirty", action="store_true",
        help="deprecated; local source digests are authoritative",
    )
    return parser


def select_layout(rows: list[dict[str, float | int]]) -> tuple[int, int]:
    if not rows:
        raise ValueError("worker benchmark produced no results")
    peak = max(float(row["decisions_per_second"]) for row in rows)
    eligible = [
        row for row in rows
        if float(row["decisions_per_second"]) >= 0.95 * peak
    ]
    selected = min(
        eligible, key=lambda row: (int(row["workers"]), int(row["shards"])),
    )
    return int(selected["workers"]), int(selected["shards"])


def main() -> int:
    args = _parser().parse_args()
    if args.rounds <= 0 or args.rollout_steps <= 0:
        raise ValueError("benchmark rounds and rollout steps must be positive")
    if args.rollout_steps % 32:
        raise ValueError("benchmark rollout steps must be divisible by 32")
    if not torch.cuda.is_available():
        raise SystemExit("worker benchmark requires one CUDA GPU")
    torch.set_float32_matmul_precision("high")
    repository = git_state()
    source_digest = native_source_digest()
    artifact = native_artifact()
    if artifact is None:
        raise RuntimeError("worker benchmark requires the compiled native simulator")
    payload = read_config(args.config) if args.config else None
    profile = CURRICULUM_PROFILES_BY_ID[payload["run"]["profile"]] if payload else IRONCLAD_A0_FULLRUN
    model_config = ModelConfig(**payload["model"]) if payload else ModelConfig()
    config = PPOConfig(**payload["ppo"]) if payload else PPOConfig(
        rollout_steps=args.rollout_steps, recurrent_sequence_length=32,
    )
    torch.use_deterministic_algorithms(bool(payload["run"].get("deterministic", True)) if payload else True)
    torch.backends.cudnn.benchmark = False
    layouts = sorted(set(args.layouts))
    benchmark_digest = canonical_digest({
        "layouts": layouts,
        "rounds": args.rounds,
        "rollout_steps": config.rollout_steps,
        "profile": profile.profile_id,
    })
    rows: list[dict[str, float | int]] = []
    failures = []
    for workers_count, shards in layouts:
        print(json.dumps({"starting_layout": [workers_count, shards]}), flush=True)
        random.seed(918273)
        torch.manual_seed(918273)
        model = Policy(model_config).to("cuda")
        try:
            torch.cuda.reset_peak_memory_stats()
            with ShardedWorkerPool(profile, workers_count, shard_count=shards) as pool:
                trainer = PPOTrainer(
                    model, pool, config, device="cuda", seed=918273,
                    native_contract_digest=source_digest,
                    git_commit=str(repository["commit"]),
                    training_config_digest=benchmark_digest,
                    training_seed_limit=1_000_000_000_000,
                )
                trainer.train_update()
                torch.cuda.synchronize()
                started = time.perf_counter()
                collect_seconds = update_seconds = 0.0
                rss_samples = []
                for _ in range(args.rounds):
                    before = time.perf_counter()
                    batch = trainer.collect()
                    torch.cuda.synchronize()
                    middle = time.perf_counter()
                    metrics = trainer.optimize(batch)
                    if not all(math.isfinite(value) for value in metrics.values()):
                        raise FloatingPointError("non-finite PPO benchmark metrics")
                    torch.cuda.synchronize()
                    collect_seconds += middle - before
                    update_seconds += time.perf_counter() - middle
                    rss_samples.append(_rss_bytes([os.getpid(), *[p.pid for p in pool._processes]]))
                elapsed = time.perf_counter() - started
            rows.append({
                "workers": workers_count, "shards": shards, "seconds": elapsed,
                "collect_seconds": collect_seconds, "update_seconds": update_seconds,
                "cuda_peak_memory_bytes": torch.cuda.max_memory_allocated(),
                "sampled_peak_process_rss_bytes": max((v for v in rss_samples if v is not None), default=None),
                "decisions_per_second": workers_count * config.rollout_steps * args.rounds / elapsed,
            })
        except torch.cuda.OutOfMemoryError as error:
            failures.append({"workers": workers_count, "shards": shards, "error": str(error)})
        finally:
            if "batch" in locals():
                del batch
            if "trainer" in locals():
                del trainer
            del model
            gc.collect()
            torch.cuda.empty_cache()
        print(json.dumps({"completed_layout": [workers_count, shards], "results": rows,
                          "failures": failures}), flush=True)
    selected_workers, selected_shards = select_layout(rows)
    result = {
        "schema": BENCHMARK_SCHEMA,
        "selected_workers": selected_workers,
        "selected_shards": selected_shards,
        "selection_threshold": 0.95,
        "results": rows,
        "failures": failures,
        "measurement": "collect-and-ppo-update",
        "workload_contract": workload_contract(payload) if payload else None,
        "git": repository,
        "profile": profile.profile_id,
        "encoding_schema": ENCODING_SCHEMA,
        "vocabulary_sha256": vocabulary_hash(),
        "benchmark_config_sha256": benchmark_digest,
        **ironclad_scope_contract(profile.ascension),
        "native_source_sha256": source_digest,
        "native_artifact": artifact,
        "torch": torch.__version__,
        "cuda": torch.version.cuda,
        "gpu": torch.cuda.get_device_name(0),
        "torch_cpu_threads": torch.get_num_threads(),
        "allocated_cpus": os.environ.get("SLURM_CPUS_PER_TASK"),
    }
    args.output.parent.mkdir(parents=True, exist_ok=True)
    temporary = args.output.with_suffix(args.output.suffix + ".tmp")
    temporary.write_text(
        json.dumps(result, indent=2, sort_keys=True) + "\n", encoding="utf-8",
    )
    temporary.replace(args.output)
    print(json.dumps(result, indent=2, sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
