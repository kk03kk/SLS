"""Choose the smallest simulator worker count within 95% of peak throughput."""

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
from sls.curriculum import IRONCLAD_A0_ACT1
from sls.model import ENCODING_SCHEMA, ModelConfig, Policy, vocabulary_hash
from sls.rl import PPOConfig, PPOTrainer, ShardedWorkerPool
from sls.rl.training_contract import (
    canonical_digest,
    git_state,
    native_artifact,
    native_source_digest,
)


def _parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser()
    parser.add_argument("--workers", type=int, nargs="+", default=(8, 16, 24, 32))
    parser.add_argument("--rounds", type=int, default=3)
    parser.add_argument("--rollout-steps", type=int, default=64)
    parser.add_argument("--output", type=Path, default=ROOT / "runs" / "worker-benchmark.json")
    parser.add_argument("--baseline-dps", type=float, default=72.0)
    parser.add_argument("--target-multiplier", type=float, default=4.0)
    parser.add_argument("--require-target", action="store_true")
    parser.add_argument("--allow-dirty", action="store_true", help="development/test only")
    return parser


def main() -> int:
    args = _parser().parse_args()
    if args.baseline_dps <= 0 or args.target_multiplier <= 0:
        raise SystemExit("benchmark baseline and multiplier must be positive")
    if not torch.cuda.is_available():
        raise SystemExit("worker benchmark requires one CUDA GPU")
    torch.set_float32_matmul_precision("high")
    repository = git_state()
    if bool(repository["dirty"]) and not args.allow_dirty:
        raise ValueError("worker benchmark requires a clean Git worktree")
    if ENCODING_SCHEMA != "sls-policy-input-v3":
        raise RuntimeError("worker benchmark requires the policy v3 encoding contract")
    vocabulary_digest = vocabulary_hash()
    source_digest = native_source_digest()
    artifact = native_artifact()
    if artifact is None:
        raise RuntimeError("worker benchmark requires the compiled native simulator")
    benchmark_config_digest = canonical_digest({
        "workers": sorted(set(args.workers)),
        "rounds": args.rounds,
        "rollout_steps": args.rollout_steps,
        "baseline_dps": args.baseline_dps,
        "target_multiplier": args.target_multiplier,
    })
    rows = []
    for count in sorted(set(args.workers)):
        model = Policy(ModelConfig()).to("cuda")
        with ShardedWorkerPool(IRONCLAD_A0_ACT1, count) as pool:
            trainer = PPOTrainer(
                model, pool, PPOConfig(rollout_steps=args.rollout_steps), device="cuda", seed=0,
                native_contract_digest=source_digest,
                git_commit=str(repository["commit"]),
                training_config_digest=benchmark_config_digest,
            )
            trainer.collect()  # warmup
            torch.cuda.synchronize()
            started = time.perf_counter()
            for _ in range(args.rounds):
                trainer.collect()
            torch.cuda.synchronize()
            elapsed = time.perf_counter() - started
        rows.append({"workers": count, "seconds": elapsed, "decisions_per_second": count * args.rollout_steps * args.rounds / elapsed})
    peak = max(float(row["decisions_per_second"]) for row in rows)
    selected = min(int(row["workers"]) for row in rows if float(row["decisions_per_second"]) >= 0.95 * peak)
    throughput_target = args.baseline_dps * args.target_multiplier
    result = {
        "schema": "sls-worker-benchmark-v1", "selected_workers": selected,
        "simulator_only": True,
        "selection_threshold": 0.95, "results": rows, "git": repository,
        "baseline_decisions_per_second": args.baseline_dps,
        "target_multiplier": args.target_multiplier,
        "target_decisions_per_second": throughput_target,
        "target_met": peak >= throughput_target,
        "encoding_schema": ENCODING_SCHEMA,
        "vocabulary_sha256": vocabulary_digest,
        "benchmark_config_sha256": benchmark_config_digest,
        "content_scope_id": IRONCLAD_A0_SCOPE_ID,
        "content_scope_sha256": ironclad_a0_scope_hash(),
        "native_source_sha256": source_digest, "native_artifact": artifact,
        "torch": torch.__version__, "cuda": torch.version.cuda, "gpu": torch.cuda.get_device_name(0),
    }
    args.output.parent.mkdir(parents=True, exist_ok=True)
    temporary = args.output.with_suffix(args.output.suffix + ".tmp")
    temporary.write_text(json.dumps(result, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    temporary.replace(args.output)
    print(json.dumps(result, indent=2, sort_keys=True))
    return 0 if result["target_met"] or not args.require_target else 2


if __name__ == "__main__":
    raise SystemExit(main())
