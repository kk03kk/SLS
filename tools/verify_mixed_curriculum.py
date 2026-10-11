"""Real mixed-start PPO soak and same-runtime exact next-update recovery."""
from __future__ import annotations

import argparse
import hashlib
import json
import math
from dataclasses import asdict
from pathlib import Path

import torch

from sls.curriculum import IRONCLAD_A20_ACT2
from sls.rl import (
    PPOConfig,
    PPOTrainer,
    ShardedWorkerPool,
    load_checkpoint,
    save_checkpoint,
)
from sls.rl.start_curriculum import StartCurriculum
from sls.rl.training_contract import native_source_digest, training_validation_digest
from sls.runtime import load_policy_artifact


def equal(left, right):
    if isinstance(left, torch.Tensor):
        return isinstance(right, torch.Tensor) and torch.equal(left, right)
    if isinstance(left, dict):
        return left.keys() == right.keys() and all(equal(v, right[k]) for k, v in left.items())
    if isinstance(left, (list, tuple)):
        return len(left) == len(right) and all(equal(a, b) for a, b in zip(left, right))
    return left == right


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--bank", type=Path, required=True)
    parser.add_argument("--artifact", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--device", default="cuda")
    parser.add_argument("--updates", type=int, default=32)
    parser.add_argument("--workers", type=int, default=8)
    parser.add_argument("--shards", type=int, default=2)
    parser.add_argument("--rollout-steps", type=int, default=32)
    parser.add_argument("--sequence-length", type=int, default=16)
    parser.add_argument("--minibatch-sequences", type=int, default=8)
    args = parser.parse_args()
    if args.output.exists() or args.updates < 4:
        raise ValueError("fresh output and at least four updates required")
    torch.set_num_threads(2)
    torch.manual_seed(133000000)
    torch.use_deterministic_algorithms(True)
    torch.backends.cudnn.benchmark = False
    artifact = load_policy_artifact(args.artifact, device=args.device)
    bank_sha = hashlib.sha256(args.bank.read_bytes()).hexdigest()
    source, implementation = native_source_digest(), training_validation_digest()
    curriculum = StartCurriculum(args.bank, bank_sha)
    config = PPOConfig(rollout_steps=args.rollout_steps, recurrent_sequence_length=args.sequence_length,
                       minibatch_sequences=args.minibatch_sequences,
                       epochs=2, gamma=1.0, gae_lambda=0.98, learning_rate=3.125e-5,
                       failure_progress_scale=0.0, reward_schema="sls-curriculum-win-v1",
                       entropy_coefficient=0.002, entropy_final=0.002)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    checkpoint = args.output.with_suffix(".resume.pt")
    with ShardedWorkerPool(IRONCLAD_A20_ACT2, args.workers, shard_count=args.shards) as workers:
        trainer = PPOTrainer(artifact.model, workers, config, seed=133000000, device=args.device,
                             start_curriculum=curriculum, training_seed_limit=2_000_000_000_000)
        for _ in range(args.updates):
            metrics = trainer.train_update()
            if any(not math.isfinite(v) for v in metrics.values()):
                raise FloatingPointError("soak metrics nonfinite")
            print(json.dumps({"update": trainer.update, "decisions": trainer.environment_steps,
                              "curriculum": curriculum.state_dict()}), flush=True)
        save_checkpoint(checkpoint, trainer)
        expected_metrics = trainer.train_update()
        expected = save_checkpoint(args.output.with_suffix(".expected.pt"), trainer)
        load_checkpoint(checkpoint, trainer)
        actual_metrics = trainer.train_update()
        actual = save_checkpoint(args.output.with_suffix(".actual.pt"), trainer)
        expected_payload = torch.load(expected, map_location="cpu", weights_only=False)
        actual_payload = torch.load(actual, map_location="cpu", weights_only=False)
        checks = {key: equal(expected_payload[key], actual_payload[key]) for key in expected_payload}
        checks["metrics"] = expected_metrics == actual_metrics
        if not all(checks.values()):
            raise ValueError(f"mixed PPO exact-resume mismatch: {checks}")
        if native_source_digest() != source or training_validation_digest() != implementation:
            raise ValueError("training source changed during mixed soak")
        report = {"schema": "sls-mixed-curriculum-soak-v1", "passed": True,
                  "native_source_sha256": source, "training_validation_sha256": implementation,
                  "model_sha256": artifact.metadata.model_sha256, "bank_sha256": bank_sha,
                  "device": args.device, "updates": args.updates,
                  "layout": [args.workers, args.shards], "ppo": asdict(config),
                  "cuda_peak_memory_bytes": torch.cuda.max_memory_allocated(args.device)
                  if args.device.startswith("cuda") else 0,
                  "new_decisions": trainer.environment_steps, "curriculum": curriculum.state_dict(),
                  "exact_resume_checks": checks, "policy_exported": False,
                  "limits": "Local same-runtime diagnostic; Linux server preparation remains mandatory"}
        args.output.write_text(json.dumps(report, indent=2) + "\n")
        print(json.dumps({"passed": True, "exact_resume_checks": checks}), flush=True)


if __name__ == "__main__":
    main()
