"""Atomic exact-resume checkpoints for canonical FullRun PPO."""

from __future__ import annotations

import os
from pathlib import Path
import random
from typing import Any, Mapping

import torch

from sls.rl.ppo import PPOTrainer


CHECKPOINT_SCHEMA = "sls-full-run-ppo-v1"


def save_checkpoint(path: str | Path, trainer: PPOTrainer) -> Path:
    target = Path(path)
    target.parent.mkdir(parents=True, exist_ok=True)
    temporary = target.with_suffix(target.suffix + ".tmp")
    payload = {
        "schema": CHECKPOINT_SCHEMA,
        "contract": {
            "model": trainer.model.config.to_dict(),
            "ppo": trainer.config.to_dict(),
            "profile": trainer.workers.profile,
            "workers": trainer.workers.size,
        },
        "model": trainer.model.state_dict(),
        "optimizer": trainer.optimizer.state_dict(),
        "trainer": {
            "update": trainer.update,
            "episodes": trainer.episodes,
            "next_seed": trainer.next_seed,
            "random": trainer.random.getstate(),
        },
        "python_rng": random.getstate(),
        "torch_rng": torch.get_rng_state(),
        "cuda_rng": torch.cuda.get_rng_state_all() if torch.cuda.is_available() else None,
        "environments": trainer.workers.checkpoints(),
    }
    torch.save(payload, temporary)
    os.replace(temporary, target)
    return target


def load_checkpoint(path: str | Path, trainer: PPOTrainer) -> Mapping[str, Any]:
    payload = torch.load(Path(path), map_location=trainer.device, weights_only=False)
    if payload.get("schema") != CHECKPOINT_SCHEMA:
        raise ValueError("unsupported training checkpoint")
    expected = {
        "model": trainer.model.config.to_dict(),
        "ppo": trainer.config.to_dict(),
        "profile": trainer.workers.profile,
        "workers": trainer.workers.size,
    }
    if payload.get("contract") != expected:
        raise ValueError("checkpoint contract does not match the current trainer")
    trainer.model.load_state_dict(payload["model"])
    trainer.optimizer.load_state_dict(payload["optimizer"])
    state = payload["trainer"]
    trainer.update = int(state["update"])
    trainer.episodes = int(state["episodes"])
    trainer.next_seed = int(state["next_seed"])
    trainer.random.setstate(state["random"])
    random.setstate(payload["python_rng"])
    torch.set_rng_state(payload["torch_rng"])
    if torch.cuda.is_available() and payload.get("cuda_rng") is not None:
        torch.cuda.set_rng_state_all(payload["cuda_rng"])
    trainer.decisions = trainer.workers.load_checkpoints(payload["environments"])
    return payload
