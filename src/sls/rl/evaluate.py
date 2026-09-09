"""Deterministic FullRun policy evaluation."""

from __future__ import annotations

import statistics
from collections import Counter, deque
from dataclasses import dataclass, field
from pathlib import Path
from typing import Callable

import torch

from sls.backends.simulator import SimulatorBackend
from sls.curriculum import CurriculumProfile, EpisodeHorizon
from sls.model import Policy, PolicyBatch
from sls.model.encoding import ACTION_TYPE_IDS
from sls.rl.episode_limit import EpisodeLimitState
from sls.rl.reward import (
    DEFAULT_FAILURE_PROGRESS_SCALE,
    curriculum_terminal_reward,
)
from sls.rl.workers import ShardedWorkerPool

_BOSS_MONSTERS = {
    "AUTOMATON": frozenset({"BRONZE_AUTOMATON"}),
    "CHAMP": frozenset({"THE_CHAMP"}),
    "COLLECTOR": frozenset({"THE_COLLECTOR"}),
    "DONU_AND_DECA": frozenset({"DONU", "DECA"}),
    # The encounter continues after the large slime splits.
    "SLIME_BOSS": frozenset({
        "SLIME_BOSS", "ACID_SLIME_L", "SPIKE_SLIME_L", "ACID_SLIME_M", "SPIKE_SLIME_M",
    }),
}


@dataclass(frozen=True, slots=True)
class EvaluationResult:
    """boss_* rates group act completion by scheduled boss, including earlier deaths.

    They are not conditional win rates given entry into the boss combat. Actual
    combat entries are recorded separately in boss_action_metrics[*].entries.
    """
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
    boss_successes: dict[str, int]
    boss_attempts: dict[str, int]
    boss_action_metrics: dict[str, dict[str, int | float]]
    success_rate_ci95: tuple[float, float] = (0.0, 1.0)
    boss_entry_success_rate: dict[str, float] = field(default_factory=dict)
    death_floor_distribution: dict[str, int] = field(default_factory=dict)
    seed_results: list[dict] = field(default_factory=list)
    failure_traces: list[dict] = field(default_factory=list)


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
def _evaluate_impl(
    model: Policy,
    profile: CurriculumProfile,
    seeds: tuple[int, ...],
    *,
    device: str | torch.device = "cpu",
    max_steps: int = 512,
    max_boundary_visits: int = 4,
    failure_progress_scale: float = DEFAULT_FAILURE_PROGRESS_SCALE,
    stop_requested: Callable[[], bool] | None = None,
    environment_pool: ShardedWorkerPool | None = None,
    progress_callback: Callable[[int, int, int], None] | None = None,
) -> EvaluationResult:
    model.eval().to(device)
    seed_values = list(seeds)
    if not seed_values:
        raise ValueError("evaluation requires at least one seed")
    backends = (
        [SimulatorBackend(profile) for _ in seed_values]
        if environment_pool is None else []
    )
    decisions = (
        [backend.reset(seed) for backend, seed in zip(backends, seed_values)]
        if environment_pool is None else environment_pool.reset(seed_values)
    )
    memory = model.initial_memory(len(seed_values), device)
    episode_starts = torch.ones(len(seed_values), dtype=torch.bool, device=device)
    previous_action_types = torch.zeros(len(seed_values), dtype=torch.long, device=device)
    previous_rewards = torch.zeros(len(seed_values), dtype=torch.float32, device=device)
    bosses_by_act: list[dict[int, str]] = [dict() for _ in seed_values]
    for index, decision in enumerate(decisions):
        bosses_by_act[index][decision.observation.run.act] = (
            decision.observation.run.visible_boss_id or "UNKNOWN"
        )
    limits = [EpisodeLimitState.initial(decision) for decision in decisions]
    active = list(range(len(seed_values)))
    episode_rewards = [0.0] * len(seed_values)
    episode_steps = [0] * len(seed_values)
    won = [False] * len(seed_values)
    reasons = ["timeout"] * len(seed_values)
    contexts = [{} for _ in seed_values]
    routes = [[] for _ in seed_values]
    traces = [deque(maxlen=32) for _ in seed_values]
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
    boss_action_counts: dict[str, dict[str, int]] = {}
    entered_bosses: list[set[str]] = [set() for _ in seed_values]
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
            previous_action_types=previous_action_types[active],
            previous_rewards=previous_rewards[active],
        )
        memory[active] = output.next_memory
        episode_starts[active] = False
        action_indices = output.logits.argmax(dim=1).cpu().tolist()
        parallel_transitions = None
        if environment_pool is not None:
            sparse_actions: list[str | None] = [None] * len(seed_values)
            for batch_index, index in enumerate(active):
                sparse_actions[index] = decisions[index].actions[
                    int(action_indices[batch_index])
                ].candidate_id
            try:
                parallel_transitions = environment_pool.step_sparse(sparse_actions)
            except Exception as error:
                active_seeds = [seed_values[index] for index in active]
                raise RuntimeError(
                    "parallel canonical evaluation backend failed for active "
                    f"seed range [{min(active_seeds)}, {max(active_seeds)}]"
                ) from error
        still_active = []
        for batch_index, index in enumerate(active):
            action = decisions[index].actions[int(action_indices[batch_index])]
            observation = decisions[index].observation
            if observation.enemies:
                contexts[index] = {"enemy_ids": [e.monster_id for e in observation.enemies]}
            elif observation.screen.value == "EVENT":
                contexts[index] = {"event_options": [e.content_id for e in observation.event_options]}
            if action.kind.value == "CHOOSE_MAP_NODE":
                routes[index].append(action.node_id)
            traces[index].append({"step": episode_steps[index], "floor": observation.run.floor,
                                  "hp": observation.player.current_hp, "screen": observation.screen.value,
                                  "action": action.to_dict()})
            visible_boss = observation.run.visible_boss_id or "UNKNOWN"
            fighting_boss = (
                observation.screen.value == "COMBAT"
                and any(
                    enemy.monster_id in _BOSS_MONSTERS.get(visible_boss, {visible_boss})
                    for enemy in observation.enemies
                )
                and (
                    visible_boss != "SLIME_BOSS"
                    or any(enemy.monster_id == "SLIME_BOSS" for enemy in observation.enemies)
                    or f"ACT_{observation.run.act}:{visible_boss}" in entered_bosses[index]
                )
            )
            if fighting_boss:
                boss_key = f"ACT_{observation.run.act}:{visible_boss}"
                metrics = boss_action_counts.setdefault(boss_key, {
                    "entries": 0,
                    "potions_at_entry": 0,
                    "decisions": 0,
                    "play_card_actions": 0,
                    "defend_red_actions": 0,
                    "use_potion_actions": 0,
                    "discard_potion_actions": 0,
                    "block_deficit_decisions": 0,
                    "defend_red_on_block_deficit": 0,
                    "targeted_card_plays_while_sharp_hide": 0,
                })
                if boss_key not in entered_bosses[index]:
                    entered_bosses[index].add(boss_key)
                    metrics["entries"] += 1
                    metrics["potions_at_entry"] += len(observation.potions)
                metrics["decisions"] += 1
                selected_card = next(
                    (
                        card for card in observation.hand
                        if card.instance_id == action.subject_id
                    ),
                    None,
                )
                if action.kind.value == "PLAY_CARD":
                    metrics["play_card_actions"] += 1
                if selected_card is not None and selected_card.card_id == "DEFEND_RED":
                    metrics["defend_red_actions"] += 1
                if action.kind.value == "USE_POTION":
                    metrics["use_potion_actions"] += 1
                if action.kind.value == "DISCARD_POTION":
                    metrics["discard_potion_actions"] += 1
                incoming = sum(
                    enemy.intent_damage * enemy.intent_hits
                    for enemy in observation.enemies if enemy.current_hp > 0
                )
                block_deficit = incoming > observation.player.block
                if block_deficit:
                    metrics["block_deficit_decisions"] += 1
                    if selected_card is not None and selected_card.card_id == "DEFEND_RED":
                        metrics["defend_red_on_block_deficit"] += 1
                if (
                    action.kind.value == "PLAY_CARD"
                    and action.target_id is not None
                    and any(power.content_id == "SHARP_HIDE" for power in observation.powers)
                ):
                    metrics["targeted_card_plays_while_sharp_hide"] += 1
            previous_act = decisions[index].observation.run.act
            try:
                if parallel_transitions is None:
                    transition = backends[index].step(action)
                else:
                    transition = parallel_transitions[index]
                    if transition is None:
                        raise RuntimeError("active evaluation slot was not stepped")
            except Exception as error:
                raise RuntimeError(
                    "canonical evaluation backend failed at "
                    f"seed={seed_values[index]} step={episode_steps[index]} "
                    f"action={action.candidate_id}"
                ) from error
            episode_steps[index] += 1
            if transition.info.get("automatic_actions"):
                traces[index][-1]["automatic_actions"] = transition.info["automatic_actions"]
            previous_action_types[index] = ACTION_TYPE_IDS[action.kind.value] + 1
            previous_rewards[index] = float(transition.reward)
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
                episode_rewards[index] += (
                    curriculum_terminal_reward(
                        transition.decision.observation,
                        profile,
                        success=success,
                        failure_progress_scale=failure_progress_scale,
                    )
                    if transition.terminated
                    else -1.0
                )
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
                won[index] = success
                reasons[index] = str(transition.info.get("reason") or "backend_truncation")
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
                reasons[index] = limit_reason
                episode_rewards[index] += -1.0
                step_limits += int(limit_reason == "step_limit")
                cycle_limits += int(limit_reason == "cycle_limit")
                self_loops += int(limit_reason == "cycle_limit")
                failure_floors.append(transition.decision.observation.run.floor)
                boss = bosses_by_act[index].get(current_act, "UNKNOWN")
                boss_results.setdefault(f"ACT_{current_act}:{boss}", []).append(False)
                continue
            still_active.append(index)
        active = still_active
        if progress_callback is not None:
            progress_callback(
                len(seed_values) - len(active),
                len(seed_values),
                sum(episode_steps),
            )
    timeouts = len(active)
    for index in active:
        episode_rewards[index] += -1.0
        act = decisions[index].observation.run.act
        boss = bosses_by_act[index].get(act, "UNKNOWN")
        boss_results.setdefault(f"ACT_{act}:{boss}", []).append(False)
        failure_floors.append(decisions[index].observation.run.floor)
    if active and progress_callback is not None:
        progress_callback(len(seed_values), len(seed_values), sum(episode_steps))
    count = len(seed_values)
    reached_act2 = sum(act >= 2 for act in max_acts)
    reached_act3 = sum(act >= 3 for act in max_acts)
    from sls.rl.best_checkpoint import _wilson_interval
    seed_results = [
        {"seed": seed, "success": won[i], "reason": reasons[i], "steps": episode_steps[i],
         "floor": decisions[i].observation.run.floor, "bosses": bosses_by_act[i],
         "entered_bosses": sorted(entered_bosses[i]), "last_context": contexts[i],
         "route": routes[i], "deck": [c.card_id for c in decisions[i].observation.deck]}
        for i, seed in enumerate(seed_values)
    ]
    sampled = set()
    failure_traces = []
    for i, row in enumerate(seed_results):
        key = str(row["last_context"]) + str(row["reason"])
        if not row["success"] and key not in sampled and len(failure_traces) < 16:
            sampled.add(key)
            failure_traces.append({"seed": row["seed"], "tail": list(traces[i])})
    return EvaluationResult(
        success_rate_ci95=_wilson_interval(successes, count),
        boss_entry_success_rate={
            boss: sum(won[i] and boss in entered_bosses[i] for i in range(count)) / metrics["entries"]
            for boss, metrics in boss_action_counts.items() if metrics["entries"]
        } if profile.horizon is EpisodeHorizon.ACT_1 else {},
        death_floor_distribution=dict(sorted(Counter(
            str(decisions[i].observation.run.floor) for i in range(count)
            if not won[i] and reasons[i] == "DEATH"
        ).items())),
        seed_results=seed_results, failure_traces=failure_traces,
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
        boss_successes={
            boss: sum(results) for boss, results in sorted(boss_results.items())
        },
        boss_attempts={
            boss: len(results) for boss, results in sorted(boss_results.items())
        },
        boss_action_metrics={
            boss: {
                **metrics,
                "mean_potions_at_entry": (
                    metrics["potions_at_entry"] / metrics["entries"]
                    if metrics["entries"] else 0.0
                ),
                "defend_red_on_block_deficit_rate": (
                    metrics["defend_red_on_block_deficit"]
                    / metrics["block_deficit_decisions"]
                    if metrics["block_deficit_decisions"] else 0.0
                ),
            }
            for boss, metrics in sorted(boss_action_counts.items())
        },
    )


def evaluate(
    model: Policy,
    profile: CurriculumProfile,
    seeds: tuple[int, ...],
    *,
    device: str | torch.device = "cpu",
    max_steps: int = 512,
    max_boundary_visits: int = 4,
    failure_progress_scale: float = DEFAULT_FAILURE_PROGRESS_SCALE,
    stop_requested: Callable[[], bool] | None = None,
    environment_shards: int = 0,
    crash_dump_dir: str | Path | None = None,
    progress_callback: Callable[[int, int, int], None] | None = None,
) -> EvaluationResult:
    """Evaluate deterministically, optionally stepping native states in shards.

    Model inference remains centralized and uses the same active seed order.
    Sharding changes only where independent native environment transitions are
    computed, so policy batching and metric reduction stay identical.
    """

    if not seeds:
        raise ValueError("evaluation requires at least one seed")
    if environment_shards < 0:
        raise ValueError("environment_shards cannot be negative")
    if environment_shards <= 1:
        return _evaluate_impl(
            model, profile, seeds, device=device, max_steps=max_steps,
            max_boundary_visits=max_boundary_visits,
            failure_progress_scale=failure_progress_scale,
            stop_requested=stop_requested,
            progress_callback=progress_callback,
        )
    with ShardedWorkerPool(
        profile, len(seeds), shard_count=environment_shards,
        crash_dump_dir=crash_dump_dir,
    ) as environment_pool:
        return _evaluate_impl(
            model, profile, seeds, device=device, max_steps=max_steps,
            max_boundary_visits=max_boundary_visits,
            failure_progress_scale=failure_progress_scale,
            stop_requested=stop_requested,
            environment_pool=environment_pool,
            progress_callback=progress_callback,
        )
