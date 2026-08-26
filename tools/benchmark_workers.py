"""Choose the smallest simulator worker count within 95% of peak throughput."""

from __future__ import annotations

import argparse
import json
from pathlib import Path
import sys
import time

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "src"))

import torch

from sls.content.scope import IRONCLAD_A0_SCOPE_ID, ironclad_a0_scope_hash
from sls.content.semantic_audit import semantic_audit_hash

from sls.curriculum import IRONCLAD_A0_ACT1
from sls.model import ModelConfig, Policy
from sls.rl import PPOConfig, PPOTrainer, WorkerPool
from sls.rl.training_contract import git_state, native_artifact, native_source_digest
from sls.rl.training_contract import (  # noqa: E402
    ACT1_PRODUCTION_READINESS_LEVEL, ACT1_PRODUCTION_READINESS_LOCK,
)
from sls.validation.readiness_lock import verify_readiness_lock


def _parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser()
    parser.add_argument("--workers", type=int, nargs="+", default=(8, 16, 24, 32))
    parser.add_argument("--rounds", type=int, default=3)
    parser.add_argument("--rollout-steps", type=int, default=64)
    parser.add_argument("--output", type=Path, default=ROOT / "runs" / "worker-benchmark.json")
    parser.add_argument(
        "--readiness-lock", type=Path, default=ACT1_PRODUCTION_READINESS_LOCK,
    )
    parser.add_argument(
        "--readiness-level", choices=(ACT1_PRODUCTION_READINESS_LEVEL,),
        default=ACT1_PRODUCTION_READINESS_LEVEL,
    )
    parser.add_argument("--allow-dirty", action="store_true", help="development/test only")
    return parser


def _verify_benchmark_readiness(args: argparse.Namespace) -> dict[str, object]:
    return verify_readiness_lock(
        args.readiness_lock,
        require_clean=not args.allow_dirty,
        expected_level=args.readiness_level,
    )


def main() -> int:
    args = _parser().parse_args()
    if not torch.cuda.is_available():
        raise SystemExit("worker benchmark requires one CUDA GPU")
    readiness = _verify_benchmark_readiness(args)
    rows = []
    for count in sorted(set(args.workers)):
        model = Policy(ModelConfig()).to("cuda")
        with WorkerPool(IRONCLAD_A0_ACT1, count) as pool:
            trainer = PPOTrainer(
                model, pool, PPOConfig(rollout_steps=args.rollout_steps), device="cuda", seed=0,
                readiness_lock_digest=str(readiness["lock_sha256"]),
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
    result = {
        "schema": "sls-worker-benchmark-v1", "selected_workers": selected,
        "selection_threshold": 0.95, "results": rows, "git": git_state(),
        "readiness_lock_sha256": readiness["lock_sha256"],
        "readiness_level": readiness["level"],
        "content_scope_id": IRONCLAD_A0_SCOPE_ID,
        "content_scope_sha256": ironclad_a0_scope_hash(),
        "semantic_audit_sha256": semantic_audit_hash(),
        "native_source_sha256": native_source_digest(), "native_artifact": native_artifact(),
        "torch": torch.__version__, "cuda": torch.version.cuda, "gpu": torch.cuda.get_device_name(0),
    }
    args.output.parent.mkdir(parents=True, exist_ok=True)
    temporary = args.output.with_suffix(args.output.suffix + ".tmp")
    temporary.write_text(json.dumps(result, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    temporary.replace(args.output)
    print(json.dumps(result, indent=2, sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
