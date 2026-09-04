"""Atomic exact-resume checkpoints for canonical FullRun PPO."""

from __future__ import annotations

import json
import os
import random
from dataclasses import asdict, is_dataclass
from enum import Enum
from pathlib import Path
from typing import Any, Mapping

import torch

from sls.content.scope import IRONCLAD_A0_SCOPE_ID, ironclad_a0_scope_hash
from sls.model import ModelConfig, Policy
from sls.model.encoding import ENCODING_SCHEMA, vocabulary_hash
from sls.rl.ppo import PPOTrainer
from sls.rl.training_contract import TRAINING_CHECKPOINT_SCHEMA, runtime_contract

CHECKPOINT_SCHEMA = TRAINING_CHECKPOINT_SCHEMA


class CheckpointContractMismatch(ValueError):
    """A checkpoint contract differs from the requested trainer contract."""

    def __init__(self, differences: list[dict[str, object]]) -> None:
        self.differences = differences
        super().__init__(
            "checkpoint contract does not match the current trainer: "
            + json.dumps(differences, sort_keys=True)
        )


def _require_sha256(value: object, field: str) -> None:
    if not isinstance(value, str) or len(value) != 64:
        raise ValueError(f"training checkpoint {field} is invalid")
    try:
        int(value, 16)
    except ValueError as error:
        raise ValueError(f"training checkpoint {field} is invalid") from error


def policy_from_training_checkpoint(
    payload: Mapping[str, Any], *, device: str | torch.device = "cpu",
) -> Policy:
    """Load policy weights after strict checkpoint and input-identity validation.

    Native/content version hashes may describe an older approved environment,
    but must be present and valid. Input meanings and content-scope identity
    remain exact, which is the compatibility boundary for evaluation/migration.
    """

    if payload.get("schema") != CHECKPOINT_SCHEMA:
        raise ValueError("unsupported training checkpoint")
    contract = payload.get("contract")
    state = payload.get("model")
    if not isinstance(contract, Mapping) or not isinstance(state, Mapping):
        raise ValueError("training checkpoint has no model transfer contract")
    expected = {
        "encoding_schema": ENCODING_SCHEMA,
        "vocabulary_sha256": vocabulary_hash(),
        "content_scope_id": IRONCLAD_A0_SCOPE_ID,
        "simulator_only": True,
    }
    incompatible = [
        key for key, value in expected.items() if contract.get(key) != value
    ]
    if incompatible:
        raise ValueError(
            "training checkpoint policy identity is incompatible: "
            + ", ".join(sorted(incompatible))
        )
    _require_sha256(contract.get("content_scope_sha256"), "content scope digest")
    _require_sha256(contract.get("native_source_sha256"), "native source digest")
    config_payload = contract.get("model")
    if not isinstance(config_payload, Mapping):
        raise ValueError("training checkpoint model config is missing")
    model = Policy(ModelConfig.from_dict(config_payload))
    model.load_state_dict(state, strict=True)
    model.eval().to(device)
    return model


def checkpoint_contract(trainer: PPOTrainer) -> dict[str, Any]:
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


def _contract_value(value: object) -> object:
    if is_dataclass(value) and not isinstance(value, type):
        return _contract_value(asdict(value))
    if isinstance(value, Mapping):
        return {str(key): _contract_value(item) for key, item in value.items()}
    if isinstance(value, (list, tuple)):
        return [_contract_value(item) for item in value]
    if isinstance(value, Enum):
        return value.value
    return value


def checkpoint_contract_diff(
    checkpoint: Mapping[str, Any],
    current: Mapping[str, Any],
    *,
    allowed_runtime_rebind_fields: frozenset[str] = frozenset(),
) -> list[dict[str, object]]:
    """Return deterministic, path-specific contract differences."""

    missing = object()
    differences: list[dict[str, object]] = []

    def visit(path: str, actual: object, expected: object, top_field: str) -> None:
        actual = _contract_value(actual)
        expected = _contract_value(expected)
        if isinstance(actual, Mapping) and isinstance(expected, Mapping):
            for key in sorted(set(actual) | set(expected), key=str):
                child = f"{path}.{key}" if path else str(key)
                visit(child, actual.get(key, missing), expected.get(key, missing), top_field)
            return
        if isinstance(actual, list) and isinstance(expected, list):
            for index in range(max(len(actual), len(expected))):
                visit(
                    f"{path}[{index}]",
                    actual[index] if index < len(actual) else missing,
                    expected[index] if index < len(expected) else missing,
                    top_field,
                )
            return
        if actual == expected:
            return
        differences.append({
            "path": path,
            "checkpoint": "<MISSING>" if actual is missing else actual,
            "current": "<MISSING>" if expected is missing else expected,
            "runtime_rebind_allowed": top_field in allowed_runtime_rebind_fields,
        })

    for key in sorted(set(checkpoint) | set(current)):
        visit(str(key), checkpoint.get(key, missing), current.get(key, missing), str(key))
    return differences


def save_checkpoint(path: str | Path, trainer: PPOTrainer) -> Path:
    target = Path(path)
    target.parent.mkdir(parents=True, exist_ok=True)
    temporary = target.with_suffix(target.suffix + ".tmp")
    payload = {
        "schema": CHECKPOINT_SCHEMA,
        "contract": checkpoint_contract(trainer),
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


def _load_checkpoint_exact(
    path: str | Path,
    trainer: PPOTrainer,
    *,
    allowed_contract_changes: frozenset[str] = frozenset(),
) -> Mapping[str, Any]:
    # RNG states are CPU ByteTensors even when the trainer runs on CUDA.
    # Loading the whole payload directly onto the trainer device corrupts that
    # contract; model and optimizer loaders already move their own tensors.
    payload = torch.load(Path(path), map_location="cpu", weights_only=False)
    if payload.get("schema") != CHECKPOINT_SCHEMA:
        raise ValueError("unsupported training checkpoint")
    expected = checkpoint_contract(trainer)
    actual = payload.get("contract")
    if not isinstance(actual, Mapping):
        raise ValueError("checkpoint contract is missing")
    differences = checkpoint_contract_diff(
        actual,
        expected,
        allowed_runtime_rebind_fields=allowed_contract_changes,
    )
    if any(not item["runtime_rebind_allowed"] for item in differences):
        raise CheckpointContractMismatch(differences)
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


def load_checkpoint(path: str | Path, trainer: PPOTrainer) -> Mapping[str, Any]:
    return _load_checkpoint_exact(path, trainer)


def load_checkpoint_runtime_rebind(
    path: str | Path, trainer: PPOTrainer,
) -> Mapping[str, Any]:
    """Exactly resume after an explicitly approved code/native rebuild.

    Only source provenance may be rebound. Model, PPO, curriculum, content,
    encoding, vocabulary, runtime, worker layout, training schedule, all RNG
    state, recurrent state, and serialized worker environments remain exact.
    """

    return _load_checkpoint_exact(
        path,
        trainer,
        allowed_contract_changes=frozenset({"git_commit", "native_source_sha256"}),
    )


def load_checkpoint_environment_migration(
    path: str | Path, trainer: PPOTrainer,
) -> Mapping[str, Any]:
    """Resume learning state while deliberately abandoning in-flight episodes.

    This is narrower than a warm start: every learning and RNG field is retained,
    and only approved environment provenance may differ. The content-scope ID,
    vocabulary, encoding, model, and PPO contracts must still match. Worker
    environments, episode-limit state, and recurrent episode memory are reset
    together.
    """

    payload = torch.load(Path(path), map_location="cpu", weights_only=False)
    if payload.get("schema") != CHECKPOINT_SCHEMA:
        raise ValueError("unsupported training checkpoint")
    actual = payload.get("contract")
    expected = checkpoint_contract(trainer)
    if not isinstance(actual, Mapping):
        raise ValueError("checkpoint contract is missing")
    allowed_changes = {
        "git_commit", "native_source_sha256", "content_scope_id",
        "content_scope_sha256",
        "profile", "curriculum_version", "training_config_sha256",
        "training_seed_limit",
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
