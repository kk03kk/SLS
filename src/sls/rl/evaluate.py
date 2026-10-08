"""Deterministic FullRun policy evaluation."""

from __future__ import annotations

import statistics
from collections import Counter, deque
from dataclasses import asdict, dataclass, field
from pathlib import Path
from typing import Callable

import torch

from sls.backends.simulator import SimulatorBackend
from sls.contracts import Decision
from sls.curriculum import CurriculumProfile, EpisodeHorizon
from sls.model import Policy, PolicyBatch
from sls.model.encoding import ACTION_TYPE_IDS
from sls.rl.episode_limit import EpisodeLimitState
from sls.rl.reward import (
    DEFAULT_FAILURE_PROGRESS_SCALE,
    curriculum_potential,
    curriculum_terminal_reward,
)
from sls.rl.workers import ShardedWorkerPool

_BOSS_MONSTERS = {
    "AUTOMATON": frozenset({"BRONZE_AUTOMATON"}),
    "CHAMP": frozenset({"THE_CHAMP"}),
    "COLLECTOR": frozenset({"THE_COLLECTOR"}),
    "DONU_AND_DECA": frozenset({"DONU", "DECA"}),
    "THE_HEART": frozenset({"CORRUPT_HEART"}),
    # The encounter continues after the large slime splits.
    "SLIME_BOSS": frozenset({
        "SLIME_BOSS", "ACID_SLIME_L", "SPIKE_SLIME_L", "ACID_SLIME_M", "SPIKE_SLIME_M",
    }),
}


def _act2_elite_entry(observation, selected_room: tuple[int, str | None] | None) -> list[str]:
    """Coverage evidence requires a selected elite map room, excluding Colosseum."""
    if (observation.run.act != 2 or observation.screen.value != "COMBAT"
            or selected_room is None or selected_room[0] != 2
            or selected_room[1] not in {"ELITE", "BURNING_ELITE"}):
        return []
    return sorted({enemy.monster_id for enemy in observation.enemies}
                  & {"BOOK_OF_STABBING", "GREMLIN_LEADER", "TASKMASTER"})


@dataclass(frozen=True, slots=True)
class EvaluationResult:
    """boss_* rates group act completion by scheduled boss, including earlier deaths.

    They are not conditional win rates given entry into the boss combat. Actual
    combat entries are recorded separately in boss_action_metrics[*].entries.

    Counter semantics, since selection ranks these fields:

    * ``self_loops`` counts decisions after which no policy-visible state changed.
    * ``cycle_limits`` counts episodes stopped by the repeated-boundary guard.
      These are different conditions; the two were previously the same counter.
    * ``step_limits`` counts episodes stopped by the decision budget.
    * ``timeouts`` is a hard-bound safety net: it can only be nonzero if an
      episode survived ``max_steps`` decision-limit checks without the limiter
      firing, which would indicate a limiter defect.
    * ``backend_errors`` is always zero today, because a backend failure aborts
      the evaluation (see the wrapped ``RuntimeError`` sites) instead of being
      counted. It is retained so that a future recording change cannot silently
      pass promotion.
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


def _boundary_signature(decision: Decision, *, exhaustive: bool = False) -> tuple:
    """Policy-visible state used to detect a decision that changed nothing.

    The cheap form is built for every step. The exhaustive form is only built
    once the cheap form has already matched, so ordinary steps pay one tuple
    construction while an apparent no-op is confirmed against the full public
    state before it is counted.
    """

    observation = decision.observation
    cheap = (
        observation.screen.value,
        observation.run.act,
        observation.run.floor,
        observation.player.current_hp,
        observation.player.block,
        observation.player.energy,
        len(observation.hand),
        len(observation.draw_pile),
        len(observation.discard_pile),
        tuple((enemy.monster_id, enemy.current_hp) for enemy in observation.enemies),
    )
    if not exhaustive:
        return cheap
    return cheap + (
        tuple(card.instance_id for card in observation.hand),
        tuple(card.instance_id for card in observation.deck),
        len(observation.exhaust_pile),
        len(observation.selected_cards),
        len(observation.choice_options),
        len(observation.reward_options),
        len(observation.shop_items),
        len(observation.event_options),
        # Effects that are only visible through powers, relics or potions must be
        # included, otherwise using a buff potion would be miscounted as a no-op.
        tuple(
            (power.instance_id, power.content_id, power.properties)
            for power in observation.powers
        ),
        tuple(
            (relic.instance_id, relic.content_id, relic.properties)
            for relic in observation.relics
        ),
        tuple(potion.content_id for potion in observation.potions),
        observation.run.gold,
        observation.public_context,
    )


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
    # Diagnostics only: one anchor at the first policy decision in each act.
    # Values predict the stochastic training policy, whereas evaluation is greedy.
    act_entries = [{} for _ in seed_values]
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
    # Coverage diagnostics only. Taskmaster also appears in the Colosseum event.
    act2_elite_entries = [dict() for _ in seed_values]
    selected_rooms: list[tuple[int, str | None] | None] = [None for _ in seed_values]
    # One iteration beyond the step limit. Every live slot is normally removed by
    # the limiter on the max_steps-th decision, so the extra iteration only runs
    # when the limiter failed to fire; that path is what makes `timeouts` a real
    # safety net instead of a counter that cannot become nonzero.
    for _ in range(max_steps + 1):
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
            elite_ids = _act2_elite_entry(observation, selected_rooms[index])
            if elite_ids:
                act2_elite_entries[index].setdefault(str(observation.run.floor), elite_ids)
            act_key = str(observation.run.act)
            if act_key not in act_entries[index]:
                act_entries[index][act_key] = {
                    "schema": "sls-act-entry-value-diagnostic-v1",
                    "floor": observation.run.floor, "screen": observation.screen.value,
                    "hp": observation.player.current_hp, "max_hp": observation.player.max_hp,
                    "gold": observation.run.gold,
                    "deck": [c.card_id for c in observation.deck],
                    "relics": [r.content_id for r in observation.relics],
                    "potions": [p.content_id for p in observation.potions],
                    "value_shaped": float(output.value[batch_index].cpu()),
                    "potential": curriculum_potential(observation, profile),
                }
            previous_decision = decisions[index]
            previous_signature = _boundary_signature(previous_decision)
            if observation.enemies:
                contexts[index] = {"enemy_ids": [e.monster_id for e in observation.enemies]}
            elif observation.screen.value == "EVENT":
                contexts[index] = {"event_options": [e.content_id for e in observation.event_options]}
            if action.kind.value == "CHOOSE_MAP_NODE":
                routes[index].append(action.node_id)
                node = next((node for node in observation.map_nodes if node.node_id == action.node_id), None)
                selected_rooms[index] = (observation.run.act, node.visible_room_type if node else None)
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
            # A self-loop is a decision after which no policy-visible state
            # changed. It is distinct from the cycle limit, which counts repeated
            # boundary fingerprints anywhere in the episode.
            if _boundary_signature(decisions[index]) == previous_signature and (
                _boundary_signature(decisions[index], exhaustive=True)
                == _boundary_signature(previous_decision, exhaustive=True)
            ):
                self_loops += 1
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
         "terminal_screen": decisions[i].observation.screen.value,
         "last_action": traces[i][-1]["action"] if traces[i] else None,
         "terminal_selection_context": {
             "selected_cards": [asdict(c) for c in decisions[i].observation.selected_cards],
             "choice_options": [asdict(c) for c in decisions[i].observation.choice_options],
             "reward_options": [asdict(c) for c in decisions[i].observation.reward_options],
         } if reasons[i] == "cycle_limit" else None,
         "act2_elite_entries": act2_elite_entries[i],
         "act2_entry_diagnostics_contract": "sls-act2-map-room-coverage-v1",
         "route": routes[i], "deck": [c.card_id for c in decisions[i].observation.deck],
         "act_entries": act_entries[i]}
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
