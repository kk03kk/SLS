"""Coverage summary over canonical parity traces."""

from __future__ import annotations

from dataclasses import dataclass
from typing import Iterable

from sls.validation.trace import ParityTrace


@dataclass(frozen=True, slots=True)
class CoverageSummary:
    seeds: int
    complete_runs: int
    victory_runs: int
    matching_runs: int
    semantic_steps: int
    max_act: int
    screens: tuple[str, ...]
    candidate_action_kinds: tuple[str, ...]
    selected_action_kinds: tuple[str, ...]


def summarize(traces: Iterable[ParityTrace]) -> CoverageSummary:
    values = tuple(traces)
    screens = {step.screen for trace in values for step in trace.steps}
    selected_action_kinds = {
        str(step.action["kind"])
        for trace in values
        for step in trace.steps
        if step.action is not None
    }
    candidate_action_kinds = {
        kind
        for trace in values
        for step in trace.steps
        for kind in step.candidate_kinds
    }
    return CoverageSummary(
        len(values),
        sum(trace.complete for trace in values),
        sum(
            bool(trace.steps) and trace.steps[-1].terminal_kind == "VICTORY"
            for trace in values
        ),
        sum(trace.matches for trace in values),
        sum(len(trace.steps) for trace in values),
        max((step.act for trace in values for step in trace.steps), default=0),
        tuple(sorted(screens)),
        tuple(sorted(candidate_action_kinds)),
        tuple(sorted(selected_action_kinds)),
    )
