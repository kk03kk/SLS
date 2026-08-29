"""Canonical FullRun reinforcement-learning implementation.

Torch-backed modules are imported lazily so simulator and content utilities do
not require the model runtime merely because Python imports :mod:`sls.rl`.
"""

from __future__ import annotations

from importlib import import_module
from typing import Any

from sls.rl.episode_limit import (
    EPISODE_LIMIT_SCHEMA,
    EpisodeLimitState,
    policy_boundary_fingerprint,
)

__all__ = [
    "EvaluationResult",
    "EPISODE_LIMIT_SCHEMA",
    "EpisodeLimitState",
    "PPOConfig",
    "PPOTrainer",
    "ShardedWorkerPool",
    "VectorWorkerPool",
    "WorkerPool",
    "evaluate",
    "load_checkpoint",
    "load_checkpoint_environment_migration",
    "policy_boundary_fingerprint",
    "save_checkpoint",
]


_LAZY_EXPORTS = {
    "EvaluationResult": ("sls.rl.evaluate", "EvaluationResult"),
    "PPOConfig": ("sls.rl.ppo", "PPOConfig"),
    "PPOTrainer": ("sls.rl.ppo", "PPOTrainer"),
    "ShardedWorkerPool": ("sls.rl.workers", "ShardedWorkerPool"),
    "VectorWorkerPool": ("sls.rl.workers", "VectorWorkerPool"),
    "WorkerPool": ("sls.rl.workers", "WorkerPool"),
    "evaluate": ("sls.rl.evaluate", "evaluate"),
    "load_checkpoint": ("sls.rl.checkpoint", "load_checkpoint"),
    "load_checkpoint_environment_migration": (
        "sls.rl.checkpoint", "load_checkpoint_environment_migration",
    ),
    "save_checkpoint": ("sls.rl.checkpoint", "save_checkpoint"),
}


def __getattr__(name: str) -> Any:
    try:
        module_name, attribute = _LAZY_EXPORTS[name]
    except KeyError as error:
        raise AttributeError(f"module {__name__!r} has no attribute {name!r}") from error
    value = getattr(import_module(module_name), attribute)
    globals()[name] = value
    return value
