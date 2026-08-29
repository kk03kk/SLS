"""Atomic exact-resume checkpoints for canonical FullRun PPO."""

from __future__ import annotations

import os
from pathlib import Path
import random
from typing import Any, Mapping

import torch

from sls.content.scope import IRONCLAD_A0_SCOPE_ID, ironclad_a0_scope_hash
from sls.model.encoding import ENCODING_SCHEMA, vocabulary_hash
from sls.rl.ppo import PPOTrainer
from sls.rl.training_contract import TRAINING_CHECKPOINT_SCHEMA, runtime_contract
from sls.rl.training_mode import TrainingMode, parse_training_mode, require_artifact_mode


CHECKPOINT_SCHEMA = TRAINING_CHECKPOINT_SCHEMA


def _contract(trainer: PPOTrainer) -> dict[str, Any]:
    return {
        "model": trainer.model.config.to_dict(),
        "ppo": trainer.config.to_dict(),
        "profile": trainer.workers.profile,
        "curriculum_version": trainer.workers.profile.version,
        "workers": trainer.workers.size,
        "encoding_schema": ENCODING_SCHEMA,
        "vocabulary_sha256": vocabulary_hash(),
        "content_scope_id": IRONCLAD_A0_SCOPE_ID,
        "content_scope_sha256": ironclad_a0_scope_hash(),
        "readiness_lock_sha256": trainer.readiness_lock_digest,
        "native_source_sha256": trainer.native_contract_digest,
        "checkpoint_reservoir_sha256": trainer.checkpoint_reservoir_digest,
        "runtime": runtime_contract(torch),
        "training_mode": trainer.training_mode.value,
        "policy_transfer_verified": trainer.policy_transfer_verified,
        "git_commit": trainer.git_commit,
        "training_config_sha256": trainer.training_config_digest,
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


def load_model_weights(
    path: str | Path, model: torch.nn.Module, *,
    target_training_mode: TrainingMode | str = TrainingMode.EXPERIMENTAL,
) -> Mapping[str, Any]:
    """Load only policy weights for a compatible curriculum transfer.

    Unlike exact resume, this intentionally ignores the source profile,
    workers, PPO optimizer, readiness lock, native digest, RNG, and environment
    state.  Architecture and policy vocabulary remain strict contracts.
    """

    payload = torch.load(Path(path), map_location="cpu", weights_only=False)
    target_mode = parse_training_mode(target_training_mode, field="target_training_mode")
    if payload.get("schema") == "sls-behavior-pretrain-v1":
        provenance = payload.get("provenance")
        if not isinstance(provenance, Mapping):
            raise ValueError("behavior-pretrained model has no safety provenance")
        require_artifact_mode(
            provenance, production=target_mode is TrainingMode.PRODUCTION,
        )
        config = getattr(model, "config", None)
        if config is None or payload.get("model_config") != config.to_dict():
            raise ValueError("behavior-pretrained model architecture is incompatible")
        model.load_state_dict(payload["model"], strict=True)
        return {
            "schema": payload["schema"], "profile": "IRONCLAD_A0_ACT1_TEACHER",
            "corpus": payload.get("corpus"), "provenance": dict(provenance),
        }
    if payload.get("schema") != CHECKPOINT_SCHEMA:
        raise ValueError("unsupported training checkpoint")
    contract = payload.get("contract")
    if not isinstance(contract, Mapping):
        raise ValueError("training checkpoint has no transfer contract")
    config = getattr(model, "config", None)
    if config is None or contract.get("model") != config.to_dict():
        raise ValueError("checkpoint model architecture is incompatible with warm start")
    if contract.get("encoding_schema") != ENCODING_SCHEMA:
        raise ValueError("checkpoint encoding schema is incompatible with warm start")
    if contract.get("vocabulary_sha256") != vocabulary_hash():
        raise ValueError("checkpoint vocabulary is incompatible with warm start")
    require_artifact_mode(
        contract, production=target_mode is TrainingMode.PRODUCTION,
    )
    model.load_state_dict(payload["model"], strict=True)
    source_profile = contract.get("profile")
    trainer_state = payload.get("trainer") or {}
    return {
        "schema": payload["schema"],
        "update": int(trainer_state.get("update", 0)),
        "profile": getattr(source_profile, "profile_id", str(source_profile)),
        "readiness_lock_sha256": contract.get("readiness_lock_sha256"),
        "native_source_sha256": contract.get("native_source_sha256"),
        "training_mode": contract.get("training_mode"),
        "policy_transfer_verified": contract.get("policy_transfer_verified"),
        "git_commit": contract.get("git_commit"),
        "training_config_sha256": contract.get("training_config_sha256"),
    }
