"""Deterministic FullRun policy evaluation."""

from __future__ import annotations

from dataclasses import dataclass
import statistics

import torch

from sls.backends.simulator import SimulatorBackend
from sls.curriculum import CurriculumProfile
from sls.model import Policy, PolicyBatch


@dataclass(frozen=True, slots=True)
class EvaluationResult:
    episodes: int
    successes: int
    mean_reward: float
    mean_steps: float
    self_loops: int
    timeouts: int
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
) -> EvaluationResult:
    model.eval().to(device)
    seed_values = list(seeds)
    backends = [SimulatorBackend(profile) for _ in seed_values]
    decisions = [backend.reset(seed) for backend, seed in zip(backends, seed_values)]
    bosses = [decision.observation.run.visible_boss_id or "UNKNOWN" for decision in decisions]
    active = list(range(len(seed_values)))
    episode_rewards = [0.0] * len(seed_values)
    episode_steps = [0] * len(seed_values)
    successes = 0
    self_loops = 0
    timeouts = 0
    failure_floors: list[int] = []
    boss_results: dict[str, list[bool]] = {}
    for boss in bosses:
        boss_results.setdefault(boss, [])
    for _ in range(max_steps):
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
            checkpoint = backend.checkpoint()
            transition = backend.step(action)
            episode_rewards[index] += transition.reward
            episode_steps[index] += 1
            decisions[index] = transition.decision
            if transition.terminated or transition.truncated:
                success = bool(transition.info.get("success"))
                successes += int(success)
                boss_results[bosses[index]].append(success)
                if not success:
                    failure_floors.append(transition.decision.observation.run.floor)
                continue
            # Greedy evaluation will choose the same action forever when the
            # complete native state did not change (for example repeatedly
            # reopening and skipping an optional card reward).  Count that as
            # an evaluation failure instead of hiding it behind a huge timeout.
            if backend.checkpoint() == checkpoint:
                self_loops += 1
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
        statistics.median(failure_floors) if failure_floors else None,
        {
            boss: sum(results) / len(results)
            for boss, results in sorted(boss_results.items())
        },
    )
