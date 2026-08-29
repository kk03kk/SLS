"""Training-only finite episode and policy-visible cycle limits."""

from __future__ import annotations

import hashlib
import json
from dataclasses import dataclass, field
from typing import Any, Mapping

from sls.contracts import Decision

EPISODE_LIMIT_SCHEMA = "sls-act1-episode-limit-v1"
TERMINATION_REASONS = ("success", "death", "backend_truncated", "step_limit", "cycle_limit")


def _canonical(value: Any) -> Any:
    if isinstance(value, Mapping):
        return {str(key): _canonical(item) for key, item in sorted(value.items())}
    if isinstance(value, (tuple, list)):
        items = [_canonical(item) for item in value]
        return sorted(items, key=lambda item: json.dumps(item, sort_keys=True, separators=(",", ":")))
    return value


def policy_boundary_fingerprint(decision: Decision) -> str:
    """Hash only policy-visible state, independent of candidate/entity ordering."""

    payload = {
        "observation": _canonical(decision.observation.to_dict()),
        "actions": _canonical([action.to_dict() for action in decision.actions]),
    }
    encoded = json.dumps(payload, sort_keys=True, separators=(",", ":"), ensure_ascii=False)
    return hashlib.sha256(encoded.encode("utf-8")).hexdigest()


@dataclass(slots=True)
class EpisodeLimitState:
    steps: int = 0
    visits: dict[str, int] = field(default_factory=dict)

    @classmethod
    def initial(cls, decision: Decision) -> "EpisodeLimitState":
        state = cls()
        state.visits[policy_boundary_fingerprint(decision)] = 1
        return state

    def observe(
        self,
        decision: Decision,
        *,
        max_steps: int,
        max_boundary_visits: int,
    ) -> str | None:
        self.steps += 1
        if self.steps >= max_steps:
            return "step_limit"
        fingerprint = policy_boundary_fingerprint(decision)
        visits = self.visits.get(fingerprint, 0) + 1
        self.visits[fingerprint] = visits
        if visits > max_boundary_visits:
            return "cycle_limit"
        return None

    def to_dict(self) -> dict[str, Any]:
        return {"steps": self.steps, "visits": dict(sorted(self.visits.items()))}

    @classmethod
    def from_dict(cls, value: Mapping[str, Any]) -> "EpisodeLimitState":
        steps = int(value["steps"])
        visits = {str(key): int(item) for key, item in dict(value["visits"]).items()}
        if steps < 0 or any(item <= 0 for item in visits.values()):
            raise ValueError("invalid episode limiter state")
        return cls(steps=steps, visits=visits)
