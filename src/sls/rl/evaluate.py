"""Deterministic FullRun policy evaluation."""

from __future__ import annotations

import statistics
from dataclasses import dataclass
from typing import Callable

import torch

from sls.backends.simulator import SimulatorBackend
from sls.curriculum import CurriculumProfile, EpisodeHorizon
from sls.model import Policy, PolicyBatch
from sls.rl.episode_limit import EpisodeLimitState


@dataclass(frozen=True, slots=True)
class EvaluationResult:
    episodes: int
    successes: int
    success_rate: float
    mean_reward: float
    mean_steps: float
    reached_act2: int
    reached_act3: int
    reached_act2_rate: float
    reached_act3_rate: float
    self_loops: int
    timeouts: int
    step_limits: int
    cycle_limits: int
    backend_truncations: int
    backend_errors: int
    median_failure_floor: float | None
    failure_floor_p25: float | None
    failure_floor_p75: float | None
    boss_success_rate: dict[str, float]


def _percentile(values: list[int], fraction: float) -> float | None:
    if not values:
        return None
    ordered = sorted(values)
    position = (len(ordered) - 1) * fraction
    lower = int(position)
    upper = min(len(ordered) - 1, lower + 1)
    weight = position - lower
    return ordered[lower] * (1.0 - weight) + ordered[upper] * weight


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
    if not seed_values:
        raise ValueError("evaluation requires at least one seed")
    backends = [SimulatorBackend(profile) for _ in seed_values]
    decisions = [backend.reset(seed) for backend, seed in zip(backends, seed_values)]
    memory = model.initial_memory(len(seed_values), device)
    episode_starts = torch.ones(len(seed_values), dtype=torch.bool, device=device)
    bosses_by_act: list[dict[int, str]] = [dict() for _ in seed_values]
    for index, decision in enumerate(decisions):
        bosses_by_act[index][decision.observation.run.act] = (
            decision.observation.run.visible_boss_id or "UNKNOWN"
        )
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
    backend_errors = 0
    failure_floors: list[int] = []
    max_acts = [decision.observation.run.act for decision in decisions]
    boss_results: dict[str, list[bool]] = {}
    for _ in range(max_steps):
        if stop_requested is not None and stop_requested():
            raise InterruptedError("evaluation interrupted at a safe inference boundary")
        if not active:
            break
        batch = PolicyBatch.from_decisions(
            (decisions[index] for index in active), model.config,
        ).to(device)
        active_memory = memory[active]
        output = model(
            *batch.model_inputs(),
            memory=active_memory,
            episode_start_mask=episode_starts[active],
        )
        memory[active] = output.next_memory
        episode_starts[active] = False
        action_indices = output.logits.argmax(dim=1).cpu().tolist()
        still_active = []
        for batch_index, index in enumerate(active):
            backend = backends[index]
            action = decisions[index].actions[int(action_indices[batch_index])]
            previous_act = decisions[index].observation.run.act
            try:
                transition = backend.step(action)
            except Exception:
                backend_errors += 1
                failure_floors.append(decisions[index].observation.run.floor)
                boss = bosses_by_act[index].get(previous_act, "UNKNOWN")
                boss_results.setdefault(f"ACT_{previous_act}:{boss}", []).append(False)
                continue
            episode_rewards[index] += transition.reward
            episode_steps[index] += 1
            decisions[index] = transition.decision
            current_act = transition.decision.observation.run.act
            max_acts[index] = max(max_acts[index], current_act)
            bosses_by_act[index][current_act] = (
                transition.decision.observation.run.visible_boss_id or "UNKNOWN"
            )
            if current_act > previous_act:
                boss = bosses_by_act[index].get(previous_act, "UNKNOWN")
                boss_results.setdefault(f"ACT_{previous_act}:{boss}", []).append(True)
            if transition.terminated or transition.truncated:
                success = bool(transition.info.get("success"))
                if (
                    success
                    and profile.horizon is EpisodeHorizon.FULL_RUN
                    and not (
                        current_act >= 3
                        and (
                            (
                                transition.info.get("reason") == "GAME_VICTORY"
                                and transition.info.get("terminal_outcome")
                                == "PLAYER_VICTORY"
                            )
                            or transition.info.get("reason") == "ACT_3_CLEARED"
                        )
                    )
                ):
                    raise RuntimeError(
                        "FullRun backend reported success without a real Act 3 victory"
                    )
                successes += int(success)
                backend_truncations += int(transition.truncated and not transition.terminated)
                if success and current_act == previous_act:
                    boss = bosses_by_act[index].get(current_act, "UNKNOWN")
                    boss_results.setdefault(f"ACT_{current_act}:{boss}", []).append(True)
                elif not success:
                    boss = bosses_by_act[index].get(current_act, "UNKNOWN")
                    boss_results.setdefault(f"ACT_{current_act}:{boss}", []).append(False)
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
                failure_floors.append(transition.decision.observation.run.floor)
                boss = bosses_by_act[index].get(current_act, "UNKNOWN")
                boss_results.setdefault(f"ACT_{current_act}:{boss}", []).append(False)
                continue
            still_active.append(index)
        active = still_active
    timeouts = len(active)
    for index in active:
        act = decisions[index].observation.run.act
        boss = bosses_by_act[index].get(act, "UNKNOWN")
        boss_results.setdefault(f"ACT_{act}:{boss}", []).append(False)
        failure_floors.append(decisions[index].observation.run.floor)
    count = len(seed_values)
    reached_act2 = sum(act >= 2 for act in max_acts)
    reached_act3 = sum(act >= 3 for act in max_acts)
    return EvaluationResult(
        episodes=count,
        successes=successes,
        success_rate=successes / count,
        mean_reward=sum(episode_rewards) / count,
        mean_steps=sum(episode_steps) / count,
        reached_act2=reached_act2,
        reached_act3=reached_act3,
        reached_act2_rate=reached_act2 / count,
        reached_act3_rate=reached_act3 / count,
        self_loops=self_loops,
        timeouts=timeouts,
        step_limits=step_limits,
        cycle_limits=cycle_limits,
        backend_truncations=backend_truncations,
        backend_errors=backend_errors,
        median_failure_floor=(
            statistics.median(failure_floors) if failure_floors else None
        ),
        failure_floor_p25=_percentile(failure_floors, 0.25),
        failure_floor_p75=_percentile(failure_floors, 0.75),
        boss_success_rate={
            boss: sum(results) / len(results)
            for boss, results in sorted(boss_results.items())
        },
    )
