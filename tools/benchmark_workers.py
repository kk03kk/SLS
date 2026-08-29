"""Choose the smallest simulator worker count within 95% of peak throughput."""

from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path
import sys
import time

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "src"))

import torch

from sls.content.scope import IRONCLAD_A0_SCOPE_ID, ironclad_a0_scope_hash

from sls.curriculum import IRONCLAD_A0_ACT1
from sls.model import ModelConfig, Policy
from sls.rl import PPOConfig, PPOTrainer, ShardedWorkerPool
from sls.rl.training_contract import git_state, native_artifact, native_source_digest
from sls.validation.transfer_gate import verify_policy_transfer_gate

TRANSFER_GATE = ROOT / "runs" / "policy_transfer_v1.json"


def _parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser()
    parser.add_argument("--workers", type=int, nargs="+", default=(8, 16, 24, 32))
    parser.add_argument("--rounds", type=int, default=3)
    parser.add_argument("--rollout-steps", type=int, default=64)
    parser.add_argument("--output", type=Path, default=ROOT / "runs" / "worker-benchmark.json")
    parser.add_argument("--transfer-gate", type=Path, default=TRANSFER_GATE)
    parser.add_argument("--baseline-dps", type=float, default=72.0)
    parser.add_argument("--target-multiplier", type=float, default=4.0)
    parser.add_argument("--require-target", action="store_true")
    parser.add_argument("--allow-dirty", action="store_true", help="development/test only")
    return parser


def _verify_benchmark_readiness(args: argparse.Namespace) -> dict[str, object]:
    return verify_policy_transfer_gate(
        args.transfer_gate, profile_id=IRONCLAD_A0_ACT1.profile_id,
        require_canary=False,
    )


def main() -> int:
    args = _parser().parse_args()
    if args.baseline_dps <= 0 or args.target_multiplier <= 0:
        raise SystemExit("benchmark baseline and multiplier must be positive")
    if not torch.cuda.is_available():
        raise SystemExit("worker benchmark requires one CUDA GPU")
    torch.set_float32_matmul_precision("high")
    transfer_gate = _verify_benchmark_readiness(args)
    transfer_digest = hashlib.sha256(args.transfer_gate.read_bytes()).hexdigest()
    rows = []
    for count in sorted(set(args.workers)):
        model = Policy(ModelConfig()).to("cuda")
        with ShardedWorkerPool(IRONCLAD_A0_ACT1, count) as pool:
            trainer = PPOTrainer(
                model, pool, PPOConfig(rollout_steps=args.rollout_steps), device="cuda", seed=0,
                readiness_lock_digest=transfer_digest,
                native_contract_digest=native_source_digest(),
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
        "selection_threshold": 0.95, "results": rows, "git": git_state(),
        "baseline_decisions_per_second": args.baseline_dps,
        "target_multiplier": args.target_multiplier,
        "target_decisions_per_second": throughput_target,
        "target_met": peak >= throughput_target,
        "transfer_gate_sha256": transfer_digest,
        "transfer_gate_schema": transfer_gate["schema"],
        "content_scope_id": IRONCLAD_A0_SCOPE_ID,
        "content_scope_sha256": ironclad_a0_scope_hash(),
        "native_source_sha256": native_source_digest(), "native_artifact": native_artifact(),
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
