from __future__ import annotations

import math

import pytest

torch = pytest.importorskip("torch")

from sls.rl.ppo import (
    PPOConfig,
    clipped_policy_loss,
    normalize_advantages,
    policy_distance_metrics,
)


def test_advantages_are_normalized_once_over_the_complete_rollout() -> None:
    normalized = normalize_advantages(torch.tensor([1.0, 2.0, 3.0, 4.0]))
    assert float(normalized.mean()) == pytest.approx(0.0, abs=1e-7)
    assert float(normalized.std(unbiased=False)) == pytest.approx(1.0, abs=1e-7)


def test_clipped_objective_handles_positive_and_negative_advantages() -> None:
    ratios = torch.tensor([1.5, 0.5])
    advantages = torch.tensor([2.0, -2.0])
    loss = clipped_policy_loss(ratios, advantages, 0.2)
    # min(3.0, 2.4) and min(-1.0, -1.6), then negate the mean.
    assert float(loss) == pytest.approx(-0.4)


def test_policy_distance_is_sampled_forward_kl_and_clip_fraction() -> None:
    old = torch.tensor([math.log(0.8)] * 8 + [math.log(0.2)] * 2)
    new = torch.tensor([math.log(0.6)] * 8 + [math.log(0.4)] * 2)
    approximate_kl, clip_fraction = policy_distance_metrics(old, new, 0.2)
    expected = 0.8 * math.log(0.8 / 0.6) + 0.2 * math.log(0.2 / 0.4)
    assert float(approximate_kl) == pytest.approx(expected)
    assert float(clip_fraction) == pytest.approx(1.0)
    zero_kl, zero_clip = policy_distance_metrics(old, old, 0.2)
    assert float(zero_kl) == pytest.approx(0.0)
    assert float(zero_clip) == pytest.approx(0.0)


@pytest.mark.parametrize("field,value", [
    ("learning_rate", 0.0), ("clip_ratio", 0.0),
    ("value_coefficient", -1.0), ("entropy_coefficient", -1.0),
    ("max_gradient_norm", 0.0),
])
def test_invalid_ppo_optimization_parameters_are_rejected(field: str, value: float) -> None:
    with pytest.raises(ValueError):
        PPOConfig(**{field: value})
