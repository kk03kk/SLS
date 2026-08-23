"""Atomic exact-resume checkpoints for canonical FullRun PPO."""

from __future__ import annotations

import os
from pathlib import Path
import random
from typing import Any, Mapping

import torch

from sls.model.encoding import ENCODING_SCHEMA, vocabulary_hash
from sls.rl.ppo import PPOTrainer
from sls.rl.training_contract import runtime_contract


CHECKPOINT_SCHEMA = "sls-full-run-ppo-v3"


def _contract(trainer: PPOTrainer) -> dict[str, Any]:
    return {
        "model": trainer.model.config.to_dict(),
        "ppo": trainer.config.to_dict(),
        "profile": trainer.workers.profile,
        "curriculum_version": trainer.workers.profile.version,
        "workers": trainer.workers.size,
        "encoding_schema": ENCODING_SCHEMA,
        "vocabulary_sha256": vocabulary_hash(),
        "readiness_lock_sha256": trainer.readiness_lock_digest,
        "native_source_sha256": trainer.native_contract_digest,
        "runtime": runtime_contract(torch),
    }


def save_checkpoint(path: str | Path, trainer: PPOTrainer) -> Path:
    target = Path(path)
    target.parent.mkdir(parents=True, exist_ok=True)
    temporary = target.with_suffix(target.suffix + ".tmp")
    payload = {
        "schema": CHECKPOINT_SCHEMA,
        "contract": _contract(trainer),
        "model": trainer.model.state_dict(),
        "optimizer": trainer.optimizer.state_dict(),
        "trainer": {
            "update": trainer.update,
            "episodes": trainer.episodes,
            "next_seed": trainer.next_seed,
            "random": trainer.random.getstate(),
            "episode_limits": [item.to_dict() for item in trainer.episode_limits],
            "termination_counts": dict(trainer.termination_counts),
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
    # RNG states are CPU ByteTensors even when the trainer runs on CUDA.
    # Loading the whole payload directly onto the trainer device corrupts that
    # contract; model and optimizer loaders already move their own tensors.
    payload = torch.load(Path(path), map_location="cpu", weights_only=False)
    if payload.get("schema") != CHECKPOINT_SCHEMA:
        raise ValueError("unsupported training checkpoint")
    expected = _contract(trainer)
    if payload.get("contract") != expected:
        raise ValueError("checkpoint contract does not match the current trainer")
    trainer.model.load_state_dict(payload["model"])
    trainer.optimizer.load_state_dict(payload["optimizer"])
    state = payload["trainer"]
    trainer.update = int(state["update"])
    trainer.episodes = int(state["episodes"])
    trainer.next_seed = int(state["next_seed"])
    trainer.random.setstate(state["random"])
    from sls.rl.episode_limit import EpisodeLimitState, TERMINATION_REASONS
    limits = state.get("episode_limits")
    if not isinstance(limits, list) or len(limits) != trainer.workers.size:
        raise ValueError("checkpoint episode limiter state does not match worker count")
    trainer.episode_limits = [EpisodeLimitState.from_dict(item) for item in limits]
    counts = state.get("termination_counts")
    if not isinstance(counts, Mapping) or set(counts) != set(TERMINATION_REASONS):
        raise ValueError("checkpoint termination counters are invalid")
    trainer.termination_counts = {key: int(counts[key]) for key in TERMINATION_REASONS}
    trainer.last_collect_terminations = {key: 0 for key in TERMINATION_REASONS}
    random.setstate(payload["python_rng"])
    torch.set_rng_state(payload["torch_rng"])
    if torch.cuda.is_available() and payload.get("cuda_rng") is not None:
        torch.cuda.set_rng_state_all(payload["cuda_rng"])
    trainer.decisions = trainer.workers.load_checkpoints(payload["environments"])
    return payload
