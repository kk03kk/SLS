"""Run a compact, decision-level A20 Act1 policy diagnostic.

The report intentionally omits individual combat actions.  It records macro
decisions and their complete policy distributions, combat entry/exit summaries,
inventory changes, terminal outcomes, and basic simulator invariants.
"""

from __future__ import annotations

import argparse
import json
import math
import os
import signal
import sys
from collections import Counter, defaultdict
from dataclasses import asdict
from pathlib import Path
from typing import Any, Iterable, Mapping

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "src"))
os.environ.setdefault("CUBLAS_WORKSPACE_CONFIG", ":4096:8")

import torch  # noqa: E402

from sls.backends.simulator import SimulatorBackend  # noqa: E402
from sls.contracts import Action, Observation  # noqa: E402
from sls.curriculum import IRONCLAD_A20_ACT1  # noqa: E402
from sls.rl.training_contract import native_artifact, sha256_file  # noqa: E402
from sls.runtime import AgentRuntime, load_policy_artifact  # noqa: E402

SCHEMA = "sls-a20-act1-macro-diagnostic-v2"
DEFAULT_SEED_START = 4_000_000_000_000
MACRO_SCREENS = frozenset({
    "NEOW", "MAP", "CARD_REWARD", "COMBAT_REWARD", "EVENT", "SHOP",
    "TREASURE", "REST", "BOSS_REWARD", "ACT_TRANSITION",
})


def _parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("artifact", type=Path)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--seed-start", type=int, default=DEFAULT_SEED_START)
    parser.add_argument("--episodes", type=int, default=100)
    parser.add_argument("--device", default="cpu")
    parser.add_argument("--max-actions", type=int, default=4096)
    return parser


def _properties(value: Any) -> dict[str, Any]:
    return dict(getattr(value, "properties", ()) or ())


def _option_index(observation: Observation) -> dict[str, dict[str, Any]]:
    result: dict[str, dict[str, Any]] = {}
    collections = (
        ("choice", observation.choice_options),
        ("reward", observation.reward_options),
        ("event", observation.event_options),
        ("rest", observation.rest_options),
        ("boss_relic", observation.boss_relic_options),
        ("deck", observation.deck),
        ("hand", observation.hand),
        ("potion", observation.potions),
        ("relic", observation.relics),
    )
    for source, values in collections:
        for item in values:
            identifier = getattr(item, "instance_id", None)
            content = getattr(item, "content_id", None) or getattr(item, "card_id", None)
            if identifier:
                result[str(identifier)] = {
                    "source": source,
                    "content_id": str(content) if content is not None else None,
                    "properties": _properties(item),
                }
    for item in observation.shop_items:
        result[item.instance_id] = {
            "source": "shop",
            "content_id": item.content_id,
            "item_type": item.item_type,
            "price": item.price,
            "properties": _properties(item),
        }
    for item in observation.map_nodes:
        result[item.node_id] = {
            "source": "map",
            "content_id": item.visible_room_type,
            "x": item.x,
            "y": item.y,
            "reachable": item.reachable,
        }
    return result


def describe_action(observation: Observation, action: Action) -> dict[str, Any]:
    """Resolve an observation-scoped action ID to its public semantic option."""

    index = _option_index(observation)
    references = [
        value for value in (
            action.subject_id, action.option_id, action.node_id,
            action.reward_id, action.target_id,
        ) if value is not None
    ]
    resolved = next((index[value] for value in references if value in index), None)
    return {
        "kind": action.kind.value,
        "subject_id": action.subject_id,
        "target_id": action.target_id,
        "option_id": action.option_id,
        "node_id": action.node_id,
        "reward_id": action.reward_id,
        "metadata": dict(action.metadata),
        "resolved": resolved,
    }


def _state(observation: Observation) -> dict[str, Any]:
    return {
        "screen": observation.screen.value,
        "act": observation.run.act,
        "floor": observation.run.floor,
        "hp": observation.player.current_hp,
        "max_hp": observation.player.max_hp,
        "gold": observation.run.gold,
        "boss": observation.run.visible_boss_id,
        "deck_size": len(observation.deck),
        "upgraded_cards": sum(card.upgrades > 0 for card in observation.deck),
        "relics": [item.content_id for item in observation.relics],
        "potions": [item.content_id for item in observation.potions],
    }


def _inventory(observation: Observation) -> dict[str, Counter[str]]:
    return {
        "cards": Counter(
            f"{card.card_id}+{card.upgrades}" if card.upgrades else card.card_id
            for card in observation.deck
        ),
        "relics": Counter(item.content_id for item in observation.relics),
        "potions": Counter(item.content_id for item in observation.potions),
    }


def _expanded(counter: Counter[str]) -> list[str]:
    return sorted(item for item, count in counter.items() for _ in range(count))


def inventory_delta(before: Observation, after: Observation) -> dict[str, dict[str, list[str]]]:
    old, new = _inventory(before), _inventory(after)
    result: dict[str, dict[str, list[str]]] = {}
    for category in old:
        added = _expanded(new[category] - old[category])
        removed = _expanded(old[category] - new[category])
        if added or removed:
            result[category] = {"added": added, "removed": removed}
    return result


def _normalized_entropy(probabilities: Iterable[float]) -> float:
    values = [float(value) for value in probabilities]
    if len(values) <= 1:
        return 0.0
    return -sum(value * math.log(value) for value in values if value > 0.0) / math.log(len(values))


def _invariants(before: Observation, after: Observation) -> list[str]:
    issues = []
    if not 0 <= after.player.current_hp <= after.player.max_hp:
        issues.append("player HP is outside [0, max_hp]")
    if after.run.gold < 0:
        issues.append("gold is negative")
    if after.run.floor < before.run.floor:
        issues.append("floor moved backwards")
    if after.run.ascension != 20:
        issues.append("ascension changed from A20")
    return issues


def _macro_record(decision, score, selected_index: int, step: int) -> dict[str, Any]:
    ranked = sorted(score.actions, key=lambda item: item.probability, reverse=True)
    return {
        "step": step,
        "state": _state(decision.observation),
        "value": score.value,
        "confidence": score.actions[selected_index].probability,
        "normalized_entropy": _normalized_entropy(item.probability for item in score.actions),
        "selected": describe_action(decision.observation, decision.actions[selected_index]),
        "alternatives": [
            {
                "rank": rank,
                "probability": item.probability,
                "action": describe_action(decision.observation, decision.actions[item.index]),
            }
            for rank, item in enumerate(ranked, start=1)
        ],
    }


def run_episode(artifact, seed: int, *, device: str, max_actions: int) -> dict[str, Any]:
    backend = SimulatorBackend(IRONCLAD_A20_ACT1)
    decision = backend.reset(seed)
    runtime = AgentRuntime(backend, artifact, device=device)
    macro: list[dict[str, Any]] = []
    combats: list[dict[str, Any]] = []
    changes: list[dict[str, Any]] = []
    anomalies: list[dict[str, Any]] = []
    combat: dict[str, Any] | None = None
    transition = None
    steps = 0
    while not decision.terminal and steps < max_actions:
        observation = decision.observation
        score = runtime.score(decision)
        selected = score.recommended.index
        if observation.screen.value in MACRO_SCREENS:
            macro.append(_macro_record(decision, score, selected, steps))
        if observation.screen.value == "COMBAT" and combat is None:
            combat = {
                "floor": observation.run.floor,
                "enemies": [enemy.monster_id for enemy in observation.enemies],
                "entry": _state(observation),
                "start_step": steps,
            }
        before = observation
        transition = runtime.execute_scored_action(decision, score, selected)
        decision = transition.decision
        after = decision.observation
        delta = inventory_delta(before, after)
        if delta:
            changes.append({"step": steps, "floor": before.run.floor, **delta})
        for issue in _invariants(before, after):
            anomalies.append({"step": steps, "issue": issue, "before": _state(before), "after": _state(after)})
        if combat is not None and after.screen.value != "COMBAT":
            combat["exit"] = _state(after)
            combat["end_step"] = steps
            combat["terminal"] = transition.terminated
            combats.append(combat)
            combat = None
        steps += 1
    if combat is not None:
        combat["exit"] = _state(decision.observation)
        combat["end_step"] = steps
        combat["terminal"] = decision.terminal
        combats.append(combat)
    if not decision.terminal:
        anomalies.append({"step": steps, "issue": "episode action limit reached"})
    info = dict(transition.info) if transition is not None else {}
    final = decision.observation
    return {
        "seed": seed,
        "success": bool(info.get("success")),
        "reason": info.get("reason"),
        "steps": steps,
        "final": _state(final),
        "final_deck": [
            {"card_id": card.card_id, "upgrades": card.upgrades}
            for card in final.deck
        ],
        "macro_decisions": macro,
        "combats": combats,
        "inventory_changes": changes,
        "anomalies": anomalies,
    }


def _resolved_content(record: Mapping[str, Any]) -> str:
    resolved = record.get("resolved") or {}
    return str(resolved.get("content_id") or record.get("option_id")
               or record.get("subject_id") or record.get("node_id")
               or record.get("reward_id") or record.get("kind"))


def _rates(offered: Counter[str], chosen: Counter[str]) -> dict[str, dict[str, float | int]]:
    return {
        item: {
            "offered": count,
            "chosen": chosen[item],
            "chosen_when_offered": chosen[item] / count,
        }
        for item, count in sorted(offered.items())
    }


def _card_reward_options(decision: Mapping[str, Any]) -> tuple[tuple[str, str], ...]:
    """Return one stable identity for the card offer visible at this boundary."""

    options = []
    for item in decision["alternatives"]:
        action = item["action"]
        if action["kind"] != "CHOOSE_CARD_REWARD":
            continue
        options.append((str(action.get("subject_id")), _resolved_content(action)))
    return tuple(sorted(options))


def _card_reward_summary(
    episodes: Iterable[Mapping[str, Any]],
) -> tuple[Counter[str], Counter[str], Counter[str]]:
    """Count each reward offer once despite other rewards being taken first."""

    offered: Counter[str] = Counter()
    chosen: Counter[str] = Counter()
    outcomes: Counter[str] = Counter()
    for episode in episodes:
        active: tuple[tuple[str, str], ...] | None = None
        for decision in episode["macro_decisions"]:
            if decision["state"]["screen"] != "COMBAT_REWARD":
                if active is not None:
                    outcomes["unresolved"] += 1
                    active = None
                continue
            options = _card_reward_options(decision)
            if not options:
                continue
            if active != options:
                if active is not None:
                    outcomes["unresolved"] += 1
                active = options
                offered.update(content for _identifier, content in options)
            selected = decision["selected"]
            kind = selected["kind"]
            if kind == "CHOOSE_CARD_REWARD":
                chosen[_resolved_content(selected)] += 1
                outcomes["picked"] += 1
                active = None
            elif kind == "TAKE_SINGING_BOWL":
                outcomes["singing_bowl"] += 1
                active = None
            elif kind == "SKIP_REWARD":
                outcomes["skipped"] += 1
                active = None
        if active is not None:
            outcomes["unresolved"] += 1
    return offered, chosen, outcomes


def summarize(episodes: list[dict[str, Any]], errors: list[dict[str, Any]]) -> dict[str, Any]:
    outcomes = Counter("win" if row["success"] else "loss" for row in episodes)
    death_floors = Counter(str(row["final"]["floor"]) for row in episodes if not row["success"])
    bosses = defaultdict(Counter)
    screens, action_kinds = Counter(), Counter()
    neow_offered, neow_chosen = Counter(), Counter()
    card_offered, card_chosen, card_outcomes = _card_reward_summary(episodes)
    event_choices, event_followups = Counter(), Counter()
    rooms, rest_choices, shop_choices = Counter(), Counter(), Counter()
    acquired = {name: Counter() for name in ("cards", "relics", "potions")}
    removed = {name: Counter() for name in ("cards", "relics", "potions")}
    confidence: dict[str, list[float]] = defaultdict(list)
    entropy: dict[str, list[float]] = defaultdict(list)
    anomaly_count = 0
    for episode in episodes:
        boss = str(episode["final"].get("boss") or "UNKNOWN")
        bosses[boss]["attempts"] += 1
        bosses[boss]["wins"] += int(episode["success"])
        bosses[boss]["entries"] += int(any(c["floor"] == 16 for c in episode["combats"]))
        anomaly_count += len(episode["anomalies"])
        for delta in episode["inventory_changes"]:
            for category in acquired:
                change = delta.get(category) or {}
                acquired[category].update(change.get("added", ()))
                removed[category].update(change.get("removed", ()))
        for decision in episode["macro_decisions"]:
            screen = decision["state"]["screen"]
            selected = decision["selected"]
            kind = selected["kind"]
            content = _resolved_content(selected)
            screens[screen] += 1
            action_kinds[kind] += 1
            confidence[screen].append(float(decision["confidence"]))
            entropy[screen].append(float(decision["normalized_entropy"]))
            if screen == "NEOW":
                neow_chosen[content] += 1
                for item in decision["alternatives"]:
                    neow_offered[_resolved_content(item["action"])] += 1
            elif screen == "EVENT":
                if ":OPTION:" in content:
                    event_choices[content] += 1
                else:
                    event_followups[content] += 1
            elif screen == "MAP":
                rooms[content] += 1
            elif screen == "REST":
                rest_choices[content] += 1
            elif screen == "SHOP" and kind != "LEAVE_SHOP":
                shop_choices[f"{kind}:{content}"] += 1
    def distribution(values: Mapping[str, list[float]]) -> dict[str, dict[str, float | int]]:
        return {
            key: {
                "n": len(items),
                "mean": sum(items) / len(items),
                "min": min(items),
                "max": max(items),
                "below_0_55": sum(item < 0.55 for item in items),
            }
            for key, items in sorted(values.items()) if items
        }
    return {
        "schema": SCHEMA,
        "episodes_completed": len(episodes),
        "errors": errors,
        "outcomes": dict(outcomes),
        "success_rate": outcomes["win"] / len(episodes) if episodes else None,
        "death_floors": dict(sorted(death_floors.items(), key=lambda item: int(item[0]))),
        "bosses": {key: dict(value) for key, value in sorted(bosses.items())},
        "macro_screen_counts": dict(screens),
        "macro_action_counts": dict(action_kinds),
        "confidence_by_screen": distribution(confidence),
        "normalized_entropy_by_screen": distribution(entropy),
        "neow_options": _rates(neow_offered, neow_chosen),
        "card_reward_opportunities": sum(card_outcomes.values()),
        "card_reward_outcomes": dict(card_outcomes),
        "card_reward_cards": _rates(card_offered, card_chosen),
        "event_choices": dict(event_choices),
        "event_followup_choices": dict(event_followups),
        "map_room_choices": dict(rooms),
        "rest_choices": dict(rest_choices),
        "shop_choices": dict(shop_choices),
        "inventory_added_deltas": {key: dict(value) for key, value in acquired.items()},
        "inventory_removed_deltas": {key: dict(value) for key, value in removed.items()},
        "simulator_anomalies": anomaly_count,
        "qualification": (
            "Deterministic argmax diagnostic on a predeclared seed range. Macro choices "
            "and simulator invariants are recorded; individual combat actions are omitted. "
            "Observed behavior can identify suspicious states but does not by itself prove "
            "a difference from the original game. Inventory fields are raw state deltas, so "
            "card upgrades and transforms appear as paired removals/additions."
        ),
    }


def _atomic_json(path: Path, value: object) -> None:
    temporary = path.with_suffix(path.suffix + ".tmp")
    temporary.write_text(json.dumps(value, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    temporary.replace(path)


def main(argv: list[str] | None = None) -> int:
    args = _parser().parse_args(argv)
    if args.episodes <= 0 or args.max_actions <= 0:
        raise SystemExit("--episodes and --max-actions must be positive")
    if args.output.exists():
        raise SystemExit(f"output directory already exists: {args.output}")
    args.output.mkdir(parents=True)
    artifact = load_policy_artifact(args.artifact, device=args.device)
    profile = artifact.metadata.environment_profile or {}
    if (
        profile.get("profile_id") != IRONCLAD_A20_ACT1.profile_id
        or artifact.metadata.goal != "ACT1"
        or artifact.metadata.ascension_min > 20
        or artifact.metadata.ascension_max < 20
    ):
        raise ValueError("diagnostic requires an explicit IRONCLAD_A20_ACT1 artifact")
    torch.set_num_threads(min(8, os.cpu_count() or 1))
    torch.use_deterministic_algorithms(True)
    torch.backends.cudnn.benchmark = False
    torch.set_float32_matmul_precision("high")
    stopped = False

    def stop(_number: int, _frame: object) -> None:
        nonlocal stopped
        stopped = True

    signal.signal(signal.SIGINT, stop)
    signal.signal(signal.SIGTERM, stop)
    seeds = list(range(args.seed_start, args.seed_start + args.episodes))
    _atomic_json(args.output / "metadata.json", {
        "schema": SCHEMA,
        "artifact": str(args.artifact.resolve()),
        "artifact_sha256": sha256_file(args.artifact),
        "artifact_metadata": asdict(artifact.metadata),
        "native": native_artifact(),
        "device": str(args.device),
        "seed_range": [seeds[0], seeds[-1] + 1],
        "episodes_requested": args.episodes,
        "inference": "deterministic eval-mode argmax with recurrent memory",
        "combat_logging": "entry/exit summaries only; individual combat actions omitted",
    })
    episodes: list[dict[str, Any]] = []
    errors: list[dict[str, Any]] = []
    output = args.output / "episodes.jsonl"
    with output.open("a", encoding="utf-8") as stream:
        for index, seed in enumerate(seeds, start=1):
            if stopped:
                break
            try:
                record = run_episode(
                    artifact, seed, device=args.device, max_actions=args.max_actions,
                )
                episodes.append(record)
            except Exception as error:  # preserve the other seeds and a reproducible identity
                record = {
                    "seed": seed,
                    "error_type": type(error).__name__,
                    "error": str(error),
                }
                errors.append(record)
            stream.write(json.dumps(record, ensure_ascii=False, separators=(",", ":")) + "\n")
            stream.flush()
            os.fsync(stream.fileno())
            print(json.dumps({
                "completed": index,
                "requested": len(seeds),
                "wins": sum(item["success"] for item in episodes),
                "errors": len(errors),
                "seed": seed,
            }), flush=True)
    summary = summarize(episodes, errors)
    summary["interrupted"] = stopped
    _atomic_json(args.output / "summary.json", summary)
    print(json.dumps({
        "summary": str((args.output / "summary.json").resolve()),
        "episodes_completed": len(episodes),
        "errors": len(errors),
        "interrupted": stopped,
    }))
    return int(bool(errors) or stopped)


if __name__ == "__main__":
    raise SystemExit(main())
