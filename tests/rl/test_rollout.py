from __future__ import annotations

import pytest

torch = pytest.importorskip("torch")

from sls.rl.rollout import generalized_advantage_estimate


def test_gae_does_not_cross_terminal_boundaries() -> None:
    rewards = torch.tensor([[1.0], [10.0]])
    values = torch.zeros_like(rewards)
    terminated = torch.tensor([[True], [False]])
    advantages, returns = generalized_advantage_estimate(
        rewards, values, terminated, torch.tensor([2.0]), 1.0, 1.0,
    )
    assert advantages[:, 0].tolist() == [1.0, 12.0]
    assert torch.equal(advantages, returns)


def test_gae_bootstraps_only_the_open_rollout_boundary() -> None:
    rewards = torch.tensor([[1.0], [2.0]])
    values = torch.tensor([[0.5], [1.0]])
    advantages, returns = generalized_advantage_estimate(
        rewards, values, torch.tensor([[False], [False]]),
        torch.tensor([4.0]), 0.5, 1.0,
    )
    assert advantages[:, 0].tolist() == pytest.approx([2.5, 3.0])
    assert returns[:, 0].tolist() == pytest.approx([3.0, 4.0])
