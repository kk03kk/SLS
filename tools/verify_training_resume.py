"""Verify that a saved PPO checkpoint produces an exact next update."""

from __future__ import annotations

import argparse
import json
from pathlib import Path
import sys
import tomllib


ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "src"))

import torch

from sls.curriculum import CURRICULUM_PROFILES_BY_ID
from sls.model import ModelConfig, Policy
from sls.rl import PPOConfig, PPOTrainer, WorkerPool, load_checkpoint
from sls.rl.training_contract import native_source_digest, readiness_settings
from sls.validation.readiness_lock import verify_readiness_lock


PROFILES = CURRICULUM_PROFILES_BY_ID


def _next_update(
    checkpoint: Path, payload: dict[str, object], device: str,
) -> tuple[dict[str, float], dict[str, torch.Tensor], int, int]:
    run = payload["run"]
    assert isinstance(run, dict)
    profile = PROFILES[str(run["profile"])]
    model = Policy(ModelConfig(**payload.get("model", {})))
    ppo = PPOConfig(**payload.get("ppo", {}))
    readiness_digest = "UNVERIFIED"
    if bool(run.get("require_readiness", False)):
        readiness_path, readiness_level = readiness_settings(run)
        readiness_digest = str(verify_readiness_lock(
            readiness_path, expected_level=readiness_level,
        )["lock_sha256"])
    with WorkerPool(profile, int(run["workers"])) as workers:
        trainer = PPOTrainer(
            model, workers, ppo, device=device, seed=int(run["seed"]),
            readiness_lock_digest=readiness_digest,
            native_contract_digest=native_source_digest(),
        )
        load_checkpoint(checkpoint, trainer)
        metrics = trainer.train_update()
        state = {
            key: value.detach().cpu().clone()
            for key, value in trainer.model.state_dict().items()
        }
        return metrics, state, trainer.next_seed, trainer.episodes


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("checkpoint", type=Path)
    parser.add_argument("--config", type=Path, required=True)
    parser.add_argument("--device", default="auto")
    args = parser.parse_args()
    with args.config.open("rb") as stream:
        payload = tomllib.load(stream)
    device = "cuda" if args.device == "auto" and torch.cuda.is_available() else (
        "cpu" if args.device == "auto" else args.device
    )
    first = _next_update(args.checkpoint, payload, device)
    second = _next_update(args.checkpoint, payload, device)
    metric_match = first[0] == second[0]
    model_match = first[1].keys() == second[1].keys() and all(
        torch.equal(first[1][key], second[1][key]) for key in first[1]
    )
    state_match = first[2:] == second[2:]
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
