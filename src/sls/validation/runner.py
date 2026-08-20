"""Paired Original/Simulator FullRun differential runner."""

from __future__ import annotations

from typing import Callable

from sls.backends.original import OriginalBackend
from sls.backends.simulator import SimulatorBackend
from sls.contracts import Action, Decision
from sls.validation.compare import parity_differences
from sls.validation.diff import differences
from sls.validation.policies import action_ids, deterministic_action
from sls.validation.trace import ParityTrace, TraceStep


ActionSelector = Callable[[Decision, Decision], Action]


def run_paired(
    original: OriginalBackend,
    simulator: SimulatorBackend,
    *,
    seed: int,
    max_steps: int = 10_000,
    include_rng: bool = True,
    selector: ActionSelector = deterministic_action,
    stop_on_difference: bool = True,
) -> ParityTrace:
    steps: list[TraceStep] = []
    error: str | None = None
    complete = False
    try:
        original_decision = original.reset(seed)
        simulator_decision = simulator.reset(seed)
        for sequence in range(max_steps + 1):
            observation_diff = differences(
                original_decision.observation.to_dict(),
                simulator_decision.observation.to_dict(),
            )
            action_diff = differences(
                action_ids(original_decision.actions),
                action_ids(simulator_decision.actions),
            )
            state_diff = parity_differences(
                original.raw_payload,
                simulator.raw_state,
                include_rng=include_rng,
            )
            terminal = original_decision.terminal or simulator_decision.terminal
            terminal_kind = None
            if terminal:
                if original_decision.terminal != simulator_decision.terminal:
                    terminal_kind = "TERMINAL_MISMATCH"
                else:
                    original_alive = original_decision.observation.player.current_hp > 0
                    simulator_alive = simulator_decision.observation.player.current_hp > 0
                    if original_alive != simulator_alive:
                        terminal_kind = "OUTCOME_MISMATCH"
                    else:
                        terminal_kind = "VICTORY" if original_alive else "DEATH"
            action = None if terminal else selector(original_decision, simulator_decision)
            step = TraceStep(
                sequence,
                simulator_decision.observation.screen.value,
                simulator_decision.observation.run.act,
                simulator_decision.observation.run.floor,
                terminal_kind,
                tuple(sorted({action.kind.value for action in simulator_decision.actions})),
                None if action is None else action.to_dict(),
                observation_diff,
                action_diff,
                state_diff,
            )
            steps.append(step)
            if stop_on_difference and not step.matches:
                break
            if terminal:
                complete = original_decision.terminal and simulator_decision.terminal
                break
            if sequence == max_steps:
                error = f"step limit exceeded: {max_steps}"
                break
            assert action is not None
            original_decision = original.step(action).decision
            simulator_decision = simulator.step(action).decision
    except Exception as exception:
        error = f"{type(exception).__name__}: {exception}"
    return ParityTrace(seed, simulator.profile.profile_id, tuple(steps), complete, error)
