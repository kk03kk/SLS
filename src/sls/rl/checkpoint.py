"""Atomic exact-resume checkpoints for canonical FullRun PPO."""

from __future__ import annotations

import os
import random
from pathlib import Path
from typing import Any, Mapping

import torch

from sls.content.scope import IRONCLAD_A0_SCOPE_ID, ironclad_a0_scope_hash
from sls.model.encoding import ENCODING_SCHEMA, vocabulary_hash
from sls.rl.ppo import PPOTrainer
from sls.rl.training_contract import TRAINING_CHECKPOINT_SCHEMA, runtime_contract

CHECKPOINT_SCHEMA = TRAINING_CHECKPOINT_SCHEMA


def _contract(trainer: PPOTrainer) -> dict[str, Any]:
    return {
        "model": trainer.model.config.to_dict(),
        "ppo": trainer.config.to_dict(),
        "profile": trainer.workers.profile,
        "curriculum_version": trainer.workers.profile.version,
        "workers": trainer.workers.size,
        "worker_shards": getattr(trainer.workers, "shard_count", 1),
        "encoding_schema": ENCODING_SCHEMA,
        "vocabulary_sha256": vocabulary_hash(),
        "content_scope_id": IRONCLAD_A0_SCOPE_ID,
        "content_scope_sha256": ironclad_a0_scope_hash(),
        "native_source_sha256": trainer.native_contract_digest,
        "runtime": runtime_contract(torch),
        "simulator_only": True,
        "git_commit": trainer.git_commit,
        "training_config_sha256": trainer.training_config_digest,
        "training_seed_limit": trainer.training_seed_limit,
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
            "environment_steps": trainer.environment_steps,
            "next_seed": trainer.next_seed,
            "random": trainer.random.getstate(),
            "episode_limits": [item.to_dict() for item in trainer.episode_limits],
            "termination_counts": dict(trainer.termination_counts),
            "memory": trainer.memory.detach().cpu(),
            "episode_starts": trainer.episode_starts.detach().cpu(),
            "previous_action_types": trainer.previous_action_types.detach().cpu(),
            "previous_rewards": trainer.previous_rewards.detach().cpu(),
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
    trainer.environment_steps = int(state["environment_steps"])
    trainer.next_seed = int(state["next_seed"])
    trainer.random.setstate(state["random"])
    from sls.rl.episode_limit import TERMINATION_REASONS, EpisodeLimitState
    limits = state.get("episode_limits")
    if not isinstance(limits, list) or len(limits) != trainer.workers.size:
        raise ValueError("checkpoint episode limiter state does not match worker count")
    trainer.episode_limits = [EpisodeLimitState.from_dict(item) for item in limits]
    counts = state.get("termination_counts")
    if not isinstance(counts, Mapping) or set(counts) != set(TERMINATION_REASONS):
        raise ValueError("checkpoint termination counters are invalid")
    trainer.termination_counts = {key: int(counts[key]) for key in TERMINATION_REASONS}
    trainer.last_collect_terminations = {key: 0 for key in TERMINATION_REASONS}
    memory = state.get("memory")
    starts = state.get("episode_starts")
    previous_actions = state.get("previous_action_types")
    previous_rewards = state.get("previous_rewards")
    expected_memory = (trainer.workers.size, trainer.model.config.recurrent_hidden_dim)
    if not isinstance(memory, torch.Tensor) or tuple(memory.shape) != expected_memory:
        raise ValueError("checkpoint recurrent memory is invalid")
    if not isinstance(starts, torch.Tensor) or tuple(starts.shape) != (trainer.workers.size,):
        raise ValueError("checkpoint episode-start mask is invalid")
    if not isinstance(previous_actions, torch.Tensor) or tuple(previous_actions.shape) != (trainer.workers.size,):
        raise ValueError("checkpoint previous-action state is invalid")
    if not isinstance(previous_rewards, torch.Tensor) or tuple(previous_rewards.shape) != (trainer.workers.size,):
        raise ValueError("checkpoint previous-reward state is invalid")
    trainer.memory = memory.to(trainer.device)
    trainer.episode_starts = starts.to(trainer.device, dtype=torch.bool)
    trainer.previous_action_types = previous_actions.to(trainer.device, dtype=torch.long)
    trainer.previous_rewards = previous_rewards.to(trainer.device, dtype=torch.float32)
    random.setstate(payload["python_rng"])
    torch.set_rng_state(payload["torch_rng"])
    if torch.cuda.is_available() and payload.get("cuda_rng") is not None:
        torch.cuda.set_rng_state_all(payload["cuda_rng"])
    trainer.decisions = trainer.workers.load_checkpoints(payload["environments"])
    return payload


def load_checkpoint_environment_migration(
    path: str | Path, trainer: PPOTrainer,
) -> Mapping[str, Any]:
    """Resume learning state while deliberately abandoning in-flight episodes.

    This is narrower than a warm start: every learning and RNG field is retained,
    and only Git/native simulator provenance may differ. Worker environments,
    episode-limit state, and recurrent episode memory are reset together.
    """

    payload = torch.load(Path(path), map_location="cpu", weights_only=False)
    if payload.get("schema") != CHECKPOINT_SCHEMA:
        raise ValueError("unsupported training checkpoint")
    actual = payload.get("contract")
    expected = _contract(trainer)
    if not isinstance(actual, Mapping):
        raise ValueError("checkpoint contract is missing")
    allowed_changes = {
        "git_commit", "native_source_sha256", "profile", "curriculum_version",
    }
    incompatible = {
        key for key in set(actual) | set(expected)
        if key not in allowed_changes and actual.get(key) != expected.get(key)
    }
    if incompatible:
        raise ValueError(
            "environment migration checkpoint has incompatible contract fields: "
            + ", ".join(sorted(incompatible))
        )
    changed = {
        key for key in allowed_changes if actual.get(key) != expected.get(key)
    }
    if not changed:
        raise ValueError("checkpoint does not require environment migration")

    trainer.model.load_state_dict(payload["model"])
    trainer.optimizer.load_state_dict(payload["optimizer"])
    state = payload["trainer"]
    trainer.update = int(state["update"])
    trainer.episodes = int(state["episodes"])
    trainer.environment_steps = int(state["environment_steps"])
    trainer.next_seed = int(state["next_seed"])
    trainer.random.setstate(state["random"])
    from sls.rl.episode_limit import TERMINATION_REASONS, EpisodeLimitState
    counts = state.get("termination_counts")
    if not isinstance(counts, Mapping) or set(counts) != set(TERMINATION_REASONS):
        raise ValueError("checkpoint termination counters are invalid")
    trainer.termination_counts = {key: int(counts[key]) for key in TERMINATION_REASONS}
    trainer.last_collect_terminations = {key: 0 for key in TERMINATION_REASONS}

    first_seed = trainer.next_seed
    next_seed = first_seed + trainer.workers.size
    if trainer.training_seed_limit is not None and next_seed > trainer.training_seed_limit:
        raise RuntimeError("training seed namespace reached held-out evaluation seeds")
    trainer.decisions = trainer.workers.reset(range(first_seed, next_seed))
    trainer.next_seed = next_seed
    trainer.episode_limits = [EpisodeLimitState.initial(item) for item in trainer.decisions]
    trainer.memory = trainer.model.initial_memory(trainer.workers.size, trainer.device)
    trainer.episode_starts = torch.ones(
        trainer.workers.size, dtype=torch.bool, device=trainer.device,
    )
    trainer.previous_action_types = torch.zeros(
        trainer.workers.size, dtype=torch.long, device=trainer.device,
    )
    trainer.previous_rewards = torch.zeros(
        trainer.workers.size, dtype=torch.float32, device=trainer.device,
    )

    random.setstate(payload["python_rng"])
    torch.set_rng_state(payload["torch_rng"])
    if torch.cuda.is_available() and payload.get("cuda_rng") is not None:
        torch.cuda.set_rng_state_all(payload["cuda_rng"])
    return payload
