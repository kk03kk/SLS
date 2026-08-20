"""Fixed-horizon FullRun rollout and generalized advantage estimation."""

from __future__ import annotations

from dataclasses import dataclass

import torch

from sls.contracts import Decision


@dataclass(frozen=True, slots=True)
class RolloutBatch:
    decisions: tuple[Decision, ...]
    action_indices: torch.Tensor
    old_log_probabilities: torch.Tensor
    old_values: torch.Tensor
    advantages: torch.Tensor
    returns: torch.Tensor


def generalized_advantage_estimate(
    rewards: torch.Tensor,
    values: torch.Tensor,
    terminated: torch.Tensor,
    bootstrap_values: torch.Tensor,
    gamma: float,
    gae_lambda: float,
) -> tuple[torch.Tensor, torch.Tensor]:
    """Compute [time, env] advantages without crossing terminal boundaries."""

    if rewards.shape != values.shape or rewards.shape != terminated.shape:
        raise ValueError("rollout tensors must have the same [time, env] shape")
    advantages = torch.zeros_like(rewards)
    next_advantage = torch.zeros_like(bootstrap_values)
    next_value = bootstrap_values
    for time in range(rewards.shape[0] - 1, -1, -1):
        continuation = (~terminated[time]).to(rewards.dtype)
        delta = rewards[time] + gamma * next_value * continuation - values[time]
        next_advantage = delta + gamma * gae_lambda * continuation * next_advantage
        advantages[time] = next_advantage
        next_value = values[time]
    return advantages, advantages + values
