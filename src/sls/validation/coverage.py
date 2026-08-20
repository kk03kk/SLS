"""Coverage summary over canonical parity traces."""

from __future__ import annotations

from dataclasses import dataclass
from typing import Iterable

from sls.validation.trace import ParityTrace


@dataclass(frozen=True, slots=True)
class CoverageSummary:
    seeds: int
    complete_runs: int
    matching_runs: int
    semantic_steps: int
    screens: tuple[str, ...]
    action_kinds: tuple[str, ...]


def summarize(traces: Iterable[ParityTrace]) -> CoverageSummary:
    values = tuple(traces)
    screens = {step.screen for trace in values for step in trace.steps}
    action_kinds = {
        str(step.action["kind"])
        for trace in values
        for step in trace.steps
        if step.action is not None
    }
    return CoverageSummary(
        len(values),
        sum(trace.complete for trace in values),
        sum(trace.matches for trace in values),
        sum(len(trace.steps) for trace in values),
        tuple(sorted(screens)),
        tuple(sorted(action_kinds)),
    )
