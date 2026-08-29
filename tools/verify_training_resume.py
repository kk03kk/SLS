"""Verify that a saved PPO checkpoint produces an exact next update."""

from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "src"))

import torch

from sls.model import ModelConfig, Policy
from sls.rl import PPOConfig, PPOTrainer, ShardedWorkerPool, load_checkpoint


def _next_update(
    checkpoint: Path, device: str,
) -> tuple[dict[str, float], dict[str, torch.Tensor], int, int, int, torch.Tensor]:
    payload = torch.load(checkpoint, map_location="cpu", weights_only=False)
    contract = payload["contract"]
    model_config = dict(contract["model"])
    model_config.pop("encoding_schema", None)
    model_config.pop("vocabulary_hash", None)
    model = Policy(ModelConfig(**model_config))
    ppo = PPOConfig(**contract["ppo"])
    with ShardedWorkerPool(
        contract["profile"], int(contract["workers"]),
        shard_count=int(contract["worker_shards"]),
    ) as workers:
        trainer = PPOTrainer(
            model, workers, ppo, device=device, seed=0,
            native_contract_digest=str(contract["native_source_sha256"]),
            git_commit=str(contract["git_commit"]),
            training_config_digest=str(contract["training_config_sha256"]),
            training_seed_limit=contract["training_seed_limit"],
        )
        load_checkpoint(checkpoint, trainer)
        metrics = trainer.train_update()
        state = {
            key: value.detach().cpu().clone()
            for key, value in trainer.model.state_dict().items()
        }
        return (
            metrics, state, trainer.next_seed, trainer.episodes,
            trainer.environment_steps, trainer.memory.detach().cpu().clone(),
        )


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("checkpoint", type=Path)
    parser.add_argument("--device", default="auto")
    args = parser.parse_args()
    device = "cuda" if args.device == "auto" and torch.cuda.is_available() else (
        "cpu" if args.device == "auto" else args.device
    )
    first = _next_update(args.checkpoint, device)
    second = _next_update(args.checkpoint, device)
    metric_match = first[0] == second[0]
    model_match = first[1].keys() == second[1].keys() and all(
        torch.equal(first[1][key], second[1][key]) for key in first[1]
    )
    state_match = first[2:5] == second[2:5] and torch.equal(first[5], second[5])
    result = {
        "schema": "sls-training-exact-resume-check-v1",
        "checkpoint": str(args.checkpoint.resolve()),
        "device": device,
        "metrics_exact": metric_match,
        "model_exact": model_match,
        "trainer_state_exact": state_match,
        "next_update_metrics": first[0],
    }
    print(json.dumps(result, indent=2, sort_keys=True))
    return 0 if metric_match and model_match and state_match else 1


if __name__ == "__main__":
    raise SystemExit(main())
