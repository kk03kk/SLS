"""Deterministic FullRun policy evaluation."""

from __future__ import annotations

import statistics
from dataclasses import dataclass
from typing import Callable

import torch

from sls.backends.simulator import SimulatorBackend
from sls.curriculum import CurriculumProfile
from sls.model import Policy, PolicyBatch
from sls.rl.episode_limit import EpisodeLimitState


@dataclass(frozen=True, slots=True)
class EvaluationResult:
    episodes: int
    successes: int
    mean_reward: float
    mean_steps: float
    self_loops: int
    timeouts: int
    step_limits: int
    cycle_limits: int
    backend_truncations: int
    median_failure_floor: float | None
    boss_success_rate: dict[str, float]


@torch.no_grad()
def evaluate(
    model: Policy,
    profile: CurriculumProfile,
    seeds: tuple[int, ...],
    *,
    device: str | torch.device = "cpu",
    max_steps: int = 512,
    max_boundary_visits: int = 4,
    stop_requested: Callable[[], bool] | None = None,
) -> EvaluationResult:
    model.eval().to(device)
    seed_values = list(seeds)
    backends = [SimulatorBackend(profile) for _ in seed_values]
    decisions = [backend.reset(seed) for backend, seed in zip(backends, seed_values)]
    bosses = [decision.observation.run.visible_boss_id or "UNKNOWN" for decision in decisions]
    limits = [EpisodeLimitState.initial(decision) for decision in decisions]
    active = list(range(len(seed_values)))
    episode_rewards = [0.0] * len(seed_values)
    episode_steps = [0] * len(seed_values)
    successes = 0
    self_loops = 0
    timeouts = 0
    step_limits = 0
    cycle_limits = 0
    backend_truncations = 0
    failure_floors: list[int] = []
    boss_results: dict[str, list[bool]] = {}
    for boss in bosses:
        boss_results.setdefault(boss, [])
    for _ in range(max_steps):
        if stop_requested is not None and stop_requested():
            raise InterruptedError("evaluation interrupted at a safe inference boundary")
        if not active:
            break
        batch = PolicyBatch.from_decisions(
            (decisions[index] for index in active), model.config,
        ).to(device)
        output = model(*batch.model_inputs())
        action_indices = output.logits.argmax(dim=1).cpu().tolist()
        still_active = []
        for batch_index, index in enumerate(active):
            backend = backends[index]
            action = decisions[index].actions[int(action_indices[batch_index])]
            transition = backend.step(action)
            episode_rewards[index] += transition.reward
            episode_steps[index] += 1
            decisions[index] = transition.decision
            if transition.terminated or transition.truncated:
                success = bool(transition.info.get("success"))
                successes += int(success)
                backend_truncations += int(transition.truncated and not transition.terminated)
                boss_results[bosses[index]].append(success)
                if not success:
                    failure_floors.append(transition.decision.observation.run.floor)
                continue
            limit_reason = limits[index].observe(
                transition.decision,
                max_steps=max_steps,
                max_boundary_visits=max_boundary_visits,
            )
            if limit_reason is not None:
                step_limits += int(limit_reason == "step_limit")
                cycle_limits += int(limit_reason == "cycle_limit")
                self_loops += int(limit_reason == "cycle_limit")
                boss_results[bosses[index]].append(False)
                failure_floors.append(transition.decision.observation.run.floor)
                continue
            still_active.append(index)
        active = still_active
    timeouts = len(active)
    for index in active:
        boss_results[bosses[index]].append(False)
        failure_floors.append(decisions[index].observation.run.floor)
    count = len(seed_values)
    return EvaluationResult(
        count, successes, sum(episode_rewards) / count,
        sum(episode_steps) / count, self_loops, timeouts,
        step_limits, cycle_limits, backend_truncations,
        statistics.median(failure_floors) if failure_floors else None,
        {
            boss: sum(results) / len(results)
            for boss, results in sorted(boss_results.items())
        },
    )
