"""Serializable evidence emitted by paired differential runs."""

from __future__ import annotations

from dataclasses import asdict, dataclass, field
import json
from pathlib import Path
from typing import Any, Mapping


TRACE_SCHEMA = "sls-parity-trace-v2"


@dataclass(frozen=True, slots=True)
class TraceStep:
    sequence: int
    screen: str
    candidate_kinds: tuple[str, ...]
    action: Mapping[str, Any] | None
    observation_differences: Mapping[str, Any]
    action_differences: Mapping[str, Any]
    state_differences: Mapping[str, Any]

    @property
    def matches(self) -> bool:
        return not (
            self.observation_differences
            or self.action_differences
            or self.state_differences
        )


@dataclass(frozen=True, slots=True)
class ParityTrace:
    seed: int
    profile_id: str
    steps: tuple[TraceStep, ...]
    complete: bool
    error: str | None = None
    schema: str = field(default=TRACE_SCHEMA, init=False)

    @property
    def matches(self) -> bool:
        return self.error is None and all(step.matches for step in self.steps)

    def to_dict(self) -> dict[str, Any]:
        return asdict(self)

    def write(self, path: str | Path) -> Path:
        target = Path(path)
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(
            json.dumps(self.to_dict(), indent=2, sort_keys=True) + "\n",
            encoding="utf-8",
        )
        return target
