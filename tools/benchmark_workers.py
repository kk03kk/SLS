"""Benchmark and select the smallest near-peak FullRun worker layout."""

from __future__ import annotations

import argparse
import json
import sys
import time
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "src"))

import torch

from sls.content.scope import IRONCLAD_A0_SCOPE_ID, ironclad_a0_scope_hash
from sls.curriculum import IRONCLAD_A0_FULLRUN
from sls.model import ENCODING_SCHEMA, ModelConfig, Policy, vocabulary_hash
from sls.rl import PPOConfig, PPOTrainer, ShardedWorkerPool
from sls.rl.training_contract import (
    canonical_digest,
    git_state,
    native_artifact,
    native_source_digest,
)

DEFAULT_LAYOUTS = ((16, 8), (24, 8), (32, 8), (32, 16), (48, 16))
BENCHMARK_SCHEMA = "sls-worker-benchmark-v2"


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
    layouts = sorted(set(args.layouts))
    benchmark_digest = canonical_digest({
        "layouts": layouts,
        "rounds": args.rounds,
        "rollout_steps": args.rollout_steps,
        "profile": IRONCLAD_A0_FULLRUN.profile_id,
    })
    rows: list[dict[str, float | int]] = []
    for workers_count, shards in layouts:
        model = Policy(ModelConfig()).to("cuda")
        config = PPOConfig(
            rollout_steps=args.rollout_steps,
            recurrent_sequence_length=32,
        )
        with ShardedWorkerPool(
            IRONCLAD_A0_FULLRUN, workers_count, shard_count=shards,
        ) as pool:
            trainer = PPOTrainer(
                model, pool, config, device="cuda", seed=0,
                native_contract_digest=source_digest,
                git_commit=str(repository["commit"]),
                training_config_digest=benchmark_digest,
                training_seed_limit=1_000_000_000_000,
            )
            trainer.collect()
            torch.cuda.synchronize()
            started = time.perf_counter()
            for _ in range(args.rounds):
                trainer.collect()
            torch.cuda.synchronize()
            elapsed = time.perf_counter() - started
        rows.append({
            "workers": workers_count,
            "shards": shards,
            "seconds": elapsed,
            "decisions_per_second": (
                workers_count * args.rollout_steps * args.rounds / elapsed
            ),
        })
    selected_workers, selected_shards = select_layout(rows)
    result = {
        "schema": BENCHMARK_SCHEMA,
        "selected_workers": selected_workers,
        "selected_shards": selected_shards,
        "selection_threshold": 0.95,
        "results": rows,
        "git": repository,
        "profile": IRONCLAD_A0_FULLRUN.profile_id,
        "encoding_schema": ENCODING_SCHEMA,
        "vocabulary_sha256": vocabulary_hash(),
        "benchmark_config_sha256": benchmark_digest,
        "content_scope_id": IRONCLAD_A0_SCOPE_ID,
        "content_scope_sha256": ironclad_a0_scope_hash(),
        "native_source_sha256": source_digest,
        "native_artifact": artifact,
        "torch": torch.__version__,
        "cuda": torch.version.cuda,
        "gpu": torch.cuda.get_device_name(0),
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
