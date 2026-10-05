"""Tests for the auditable per-domain advantage normalization diagnostics.

The training objective is silently reweighted when each screen domain is divided
by its own advantage standard deviation, so the reweighting has to be visible in
the produced metrics rather than only in a code reading.
"""

from __future__ import annotations

from dataclasses import dataclass

import torch

from sls.rl.ppo import (
    DOMAIN_NAMES,
    advantage_domain_diagnostics,
    decision_domains,
    normalize_advantages_by_domain,
)


@dataclass(frozen=True)
class _Encoded:
    screen_type: int


def _rollout_domains() -> tuple[tuple[_Encoded, ...], ...]:
    # Two time steps x three environments, one domain per environment.
    return (
        (_Encoded(0), _Encoded(1), _Encoded(2)),
        (_Encoded(0), _Encoded(1), _Encoded(2)),
    )


def test_decision_domains_matches_rollout_shape() -> None:
    domains = decision_domains(_rollout_domains())  # type: ignore[arg-type]
    assert tuple(domains.shape) == (2, 3)
    assert domains[0].tolist() == [0, 1, 2]


def test_domain_normalization_gives_each_domain_unit_standard_deviation() -> None:
    encoded = _rollout_domains()
    advantages = torch.tensor([
        [1.0, 10.0, 100.0],
        [3.0, 20.0, 200.0],
    ])
    normalized = normalize_advantages_by_domain(advantages, encoded)  # type: ignore[arg-type]
    for domain in range(len(DOMAIN_NAMES)):
        mask = decision_domains(encoded)[:, :] == domain  # type: ignore[arg-type]
        assert abs(float(normalized[mask].mean())) < 1e-6
        assert abs(float(normalized[mask].std(unbiased=False)) - 1.0) < 1e-5

    metrics = advantage_domain_diagnostics(
        advantages.reshape(-1), normalized.reshape(-1),
        decision_domains(encoded).reshape(-1),  # type: ignore[arg-type]
    )
    # The scale is the multiplier the normalization applies, so it must invert
    # the raw spread: the widest domain is scaled down the most.
    assert metrics["advantage_std_combat"] == 1.0
    assert metrics["advantage_std_run"] == 5.0
    assert metrics["advantage_std_choice"] == 50.0
    assert metrics["advantage_scale_combat"] > metrics["advantage_scale_choice"]
    for name in DOMAIN_NAMES:
        assert abs(metrics[f"advantage_normalized_std_{name}"] - 1.0) < 1e-5
        assert abs(metrics[f"samples_{name}_fraction"] - 1 / 3) < 1e-6


def test_empty_domain_reports_zeros_instead_of_diverging() -> None:
    encoded = ((_Encoded(0),), (_Encoded(0),))
    advantages = torch.tensor([[1.0], [2.0]])
    normalized = normalize_advantages_by_domain(advantages, encoded)  # type: ignore[arg-type]
    metrics = advantage_domain_diagnostics(
        advantages, normalized, decision_domains(encoded),  # type: ignore[arg-type]
    )
    assert metrics["samples_choice_fraction"] == 0.0
    assert metrics["advantage_std_choice"] == 0.0
    assert metrics["advantage_scale_choice"] == 0.0
