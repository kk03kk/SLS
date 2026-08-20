"""Decision-boundary contracts shared by Original and Simulator backends."""

from __future__ import annotations

from dataclasses import dataclass, field
from typing import Any, Mapping

from sls.contracts.action import Action, validate_candidate_set
from sls.contracts.observation import Observation


@dataclass(frozen=True, slots=True)
class Decision:
    observation: Observation
    actions: tuple[Action, ...]
    terminal: bool = False

    def __post_init__(self) -> None:
        validate_candidate_set(self.actions)
        if self.terminal and self.actions:
            raise ValueError("a terminal decision cannot expose legal actions")
        if not self.terminal and not self.actions:
            raise ValueError("a non-terminal decision must expose a legal action")


@dataclass(frozen=True, slots=True)
class Transition:
    decision: Decision
    reward: float
    terminated: bool
    truncated: bool = False
    info: Mapping[str, Any] = field(default_factory=dict)

    def __post_init__(self) -> None:
        if self.terminated != self.decision.terminal:
            raise ValueError("transition termination must match the returned decision")
