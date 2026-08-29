"""Policy-transfer validation: contracts, local semantics, and outcomes."""

from __future__ import annotations

from collections import Counter
from dataclasses import asdict, dataclass
import math
import random
from typing import Hashable, Iterable, Mapping

from sls.contracts import Decision
from sls.validation.diff import differences


POLICY_TRANSFER_SCHEMA = "sls-policy-transfer-v1"


def public_decision(value: Decision) -> dict[str, object]:
    return {
        "observation": value.observation.to_dict(),
        "actions": sorted((action.to_dict() for action in value.actions), key=str),
        "terminal": value.terminal,
    }


def contract_differences(original: Decision, simulator: Decision) -> dict[str, object]:
    """Exact policy-facing differences; RNG and continuation are absent by construction."""

    return differences(public_decision(original), public_decision(simulator))


@dataclass(frozen=True, slots=True)
class DistributionComparison:
    original_samples: int
    simulator_samples: int
    total_variation: float
    maximum_probability_delta: float
    confidence: float
    bootstrap_resamples: int
    total_variation_upper_bound: float
    accepted: bool


def compare_distributions(
    original: Iterable[Hashable], simulator: Iterable[Hashable], *,
    maximum_total_variation: float = 0.05,
    confidence: float = 0.95,
    bootstrap_resamples: int = 2_000,
    bootstrap_seed: int = 0,
) -> DistributionComparison:
    original_values, simulator_values = tuple(original), tuple(simulator)
    first, second = Counter(original_values), Counter(simulator_values)
    first_count, second_count = sum(first.values()), sum(second.values())
    if first_count == 0 or second_count == 0:
        raise ValueError("distribution samples must be non-empty")
    if not 0.0 < confidence < 1.0:
        raise ValueError("confidence must be between zero and one")
    if bootstrap_resamples < 1:
        raise ValueError("bootstrap_resamples must be positive")
    keys = set(first) | set(second)
    deltas = [
        abs(first[key] / first_count - second[key] / second_count) for key in keys
    ]
    total_variation = 0.5 * sum(deltas)
    maximum_delta = max(deltas, default=0.0)
    ordered_keys = sorted(keys, key=repr)
    try:
        import numpy as np
    except ImportError:  # pragma: no cover - model/validation environments provide NumPy
        rng = random.Random(bootstrap_seed)
        bootstrapped = []
        for _ in range(bootstrap_resamples):
            sampled_first = Counter(rng.choices(original_values, k=first_count))
            sampled_second = Counter(rng.choices(simulator_values, k=second_count))
            bootstrapped.append(0.5 * sum(
                abs(sampled_first[key] / first_count - sampled_second[key] / second_count)
                for key in ordered_keys
            ))
    else:
        rng = np.random.default_rng(bootstrap_seed)
        first_probabilities = np.asarray(
            [first[key] / first_count for key in ordered_keys], dtype=float,
        )
        second_probabilities = np.asarray(
            [second[key] / second_count for key in ordered_keys], dtype=float,
        )
        first_draws = rng.multinomial(
            first_count, first_probabilities, size=bootstrap_resamples,
        ) / first_count
        second_draws = rng.multinomial(
            second_count, second_probabilities, size=bootstrap_resamples,
        ) / second_count
        bootstrapped = (0.5 * np.abs(first_draws - second_draws).sum(axis=1)).tolist()
    bootstrapped.sort()
    upper_index = min(
        bootstrap_resamples - 1,
        max(0, math.ceil(confidence * bootstrap_resamples) - 1),
    )
    upper_bound = bootstrapped[upper_index]
    return DistributionComparison(
        first_count, second_count, total_variation, maximum_delta,
        confidence, bootstrap_resamples, upper_bound,
        upper_bound <= maximum_total_variation,
    )


@dataclass(frozen=True, slots=True)
class BackendPolicySummary:
    backend: str
    episodes: int
    successes: int
    mean_floor: float
    mean_actions: float
    action_distribution: Mapping[str, int]
    floor_distribution: Mapping[str, int]
    termination_reasons: Mapping[str, int]
    elapsed_seconds: float
    invalid_actions: int = 0
    empty_decisions: int = 0
    backend_truncations: int = 0

    @property
    def success_rate(self) -> float:
        return self.successes / self.episodes


@dataclass(frozen=True, slots=True)
class PolicyTransferReport:
    original: BackendPolicySummary
    simulator: BackendPolicySummary
    schema: str = POLICY_TRANSFER_SCHEMA

    @staticmethod
    def _distribution_tv(first: Mapping[str, int], second: Mapping[str, int]) -> float:
        first_total, second_total = sum(first.values()), sum(second.values())
        if not first_total or not second_total:
            return 1.0
        keys = set(first) | set(second)
        return 0.5 * sum(
            abs(first.get(key, 0) / first_total - second.get(key, 0) / second_total)
            for key in keys
        )

    def to_dict(self) -> dict[str, object]:
        value = asdict(self)
        value["success_rate_delta"] = self.original.success_rate - self.simulator.success_rate
        value["mean_floor_delta"] = self.original.mean_floor - self.simulator.mean_floor
        value["action_distribution_total_variation"] = self._distribution_tv(
            self.original.action_distribution, self.simulator.action_distribution,
        )
        return value
