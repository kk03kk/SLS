"""Golden battle trace recording, replay and differential comparison."""

from __future__ import annotations

import json
from dataclasses import asdict, dataclass
from pathlib import Path
from typing import Any, Callable

from spirecomm.envs.base_sts_env import BaseSTSEnv
from spirecomm.envs.codec import (
    actions_info,
    generate_legal_actions,
    is_combat_payload,
    rich_battle_state,
)


TRACE_VERSION = 3
SUPPORTED_TRACE_VERSIONS = {1, 2, TRACE_VERSION}

ACTION_FIELDS = (
    "kind", "card_index", "potion_index", "target_index", "choice_index"
)


def semantic_action(action: dict[str, Any]) -> dict[str, Any]:
    return {
        key: action.get(key)
        for key in ACTION_FIELDS
    }


def semantic_actions(actions: list[dict[str, Any]]) -> list[dict[str, Any]]:
    return [semantic_action(action) for action in actions]


def replay_options_from_payload(payload: dict[str, Any]) -> dict[str, Any]:
    game = payload["game_state"]
    combat = game["combat_state"]
    player = combat["player"]
    return {
        "encounter": infer_act1_encounter(combat["monsters"]),
        "ascension": int(game.get("ascension_level", 0)),
        "deck": [
            {"id": card.get("id"), "upgrades": int(card.get("upgrades", 0))}
            for card in game.get("deck") or []
        ],
        "relics": [
            {"id": relic.get("id"), "counter": int(relic.get("counter", -1))}
            for relic in game.get("relics") or []
        ],
        "potions": [potion.get("id") for potion in game.get("potions") or []],
        "current_hp": int(player.get("current_hp", game.get("current_hp", 80))),
        "max_hp": int(player.get("max_hp", game.get("max_hp", 80))),
    }


def _name(value: Any) -> str:
    result = "".join(char for char in str(value or "").upper() if char.isalnum())
    return {
        "STRIKER": "STRIKERED",
        "DEFENDR": "DEFENDRED",
        # Base-game power IDs differ from the enum names used by lightspeed.
        "WEAKENED": "WEAK",
    }.get(result, result)


def _powers(values: list[dict[str, Any]]) -> dict[str, int]:
    return {
        _name(item.get("id") or item.get("name")): int(item.get("amount") or 0)
        for item in values
    }


def _card(value: dict[str, Any]) -> dict[str, Any]:
    return {
        "name": _name(value.get("id") or value.get("name")),
        "cost": value.get("cost"),
        "base_cost": value.get("base_cost", value.get("cost")),
        "upgrades": int(value.get("upgrades") or 0),
        "special_data": int(value.get("special_data") or 0),
        "free_to_play_once": bool(value.get("free_to_play_once", False)),
        "retain": bool(value.get("retain", False)),
        "ethereal": bool(value.get("ethereal", False)),
        "is_playable": bool(value.get("is_playable", False)),
        "has_target": bool(value.get("has_target", False)),
        "selected": bool(value.get("selected", False)),
    }


def _pile_card(value: dict[str, Any]) -> dict[str, Any]:
    card = _card(value)
    card.pop("is_playable")
    card.pop("has_target")
    card.pop("selected")
    return card


ORIGINAL_MOVE_IDS = {
    "JAWWORMCHOMP": 1,
    "JAWWORMBELLOW": 2,
    "JAWWORMTHRASH": 3,
    "CULTISTINCANTATION": 3,
    "CULTISTDARKSTRIKE": 1,
    "ACIDSLIMESLICK": 2,
    "SPIKESLIMESTACKLE": 1,
    "SPIKESLIMEMFLAMETACKLE": 1,
    "SPIKESLIMEMLICK": 4,
    "ACIDSLIMEMLICK": 4,
    "ACIDSLIMEMTACKLE": 2,
    "ACIDSLIMEMCORROSIVESPIT": 1,
    "GREENLOUSEBITE": 3,
    "GREENLOUSESPITWEB": 4,
    "REDLOUSEBITE": 3,
    "REDLOUSEGROW": 4,
}


def _enemy_name(value: dict[str, Any]) -> str:
    name = _name(value.get("id") or value.get("name"))
    return {
        "FUZZYLOUSENORMAL": "REDLOUSE",
        "FUZZYLOUSEDEFENSIVE": "GREENLOUSE",
    }.get(name, name)


def infer_act1_encounter(monsters: list[dict[str, Any]]) -> str:
    """Map a CommunicationMod Act 1 monster group to the simulator catalog."""

    ids = tuple(_name(monster.get("id") or monster.get("name")) for monster in monsters)
    singles = {
        "CULTIST": "CULTIST", "JAWWORM": "JAW_WORM",
        "BLUESLAVER": "BLUE_SLAVER", "LOOTER": "LOOTER",
        "REDSLAVER": "RED_SLAVER", "GREMLINNOB": "GREMLIN_NOB",
        "LAGAVULIN": "LAGAVULIN", "SLIMEBOSS": "SLIME_BOSS",
        "THEGUARDIAN": "THE_GUARDIAN", "HEXAGHOST": "HEXAGHOST",
    }
    if len(ids) == 1:
        if ids[0] in {"ACIDSLIMEL", "SPIKESLIMEL"}:
            return "LARGE_SLIME"
        if ids[0] in singles:
            return singles[ids[0]]

    # The Java base game exposes these internal IDs through CommunicationMod;
    # RedLouse/GreenLouse are simulator-facing aliases used by older fixtures.
    louses = {
        "REDLOUSE", "GREENLOUSE",
        "FUZZYLOUSENORMAL", "FUZZYLOUSEDEFENSIVE",
    }
    if len(ids) == 2 and set(ids) <= louses:
        return "TWO_LOUSE"
    if len(ids) == 3 and set(ids) <= louses:
        return "THREE_LOUSE"
    if ids == ("FUNGIBEAST", "FUNGIBEAST"):
        return "TWO_FUNGI_BEASTS"
    if set(ids) == {"SPIKESLIMES", "ACIDSLIMEM"} or set(ids) == {
        "ACIDSLIMES", "SPIKESLIMEM"
    }:
        return "SMALL_SLIMES"
    if len(ids) == 5 and ids.count("SPIKESLIMES") == 3 and ids.count("ACIDSLIMES") == 2:
        return "LOTS_OF_SLIMES"
    gremlins = {
        "MADGREMLIN", "SNEAKYGREMLIN", "FATGREMLIN",
        "SHIELDGREMLIN", "GREMLINWIZARD",
    }
    if len(ids) == 4 and set(ids) <= gremlins:
        return "GREMLIN_GANG"
    weak_wildlife = louses | {"SPIKESLIMEM", "ACIDSLIMEM"}
    if len(ids) == 2 and ids[0] in weak_wildlife and ids[1] in {
        "CULTIST", "REDSLAVER", "BLUESLAVER", "LOOTER"
    }:
        return "EXORDIUM_THUGS"
    if len(ids) == 2 and ids[0] in {"FUNGIBEAST", "JAWWORM"} and ids[1] in weak_wildlife:
        return "EXORDIUM_WILDLIFE"
    if ids == ("SENTRY", "SENTRY", "SENTRY"):
        return "THREE_SENTRIES"
    raise ValueError(f"Unsupported or ambiguous Act 1 monster group: {ids}")


def _move_id(value: dict[str, Any]) -> str | None:
    raw = value.get("move_id")
    if raw is None:
        return None
    monster = _enemy_name(value)
    if isinstance(raw, int):
        return f"{monster}:{raw}"
    move_name = str(raw).upper()
    numeric = ORIGINAL_MOVE_IDS.get(_name(move_name))
    return f"{monster}:{numeric}" if numeric is not None else _name(move_name)


def _enemy(value: dict[str, Any]) -> dict[str, Any]:
    raw_intent = value.get("intent")
    debug_attack = raw_intent == "DEBUG" and (value.get("move_base_damage") or 0) > 0
    intent = "ATTACK" if debug_attack else raw_intent
    is_attack = str(intent or "").startswith("ATTACK")
    damage = value.get("move_damage")
    if damage == -1:
        damage = value.get("move_base_damage")
    return {
        "name": _enemy_name(value),
        "hp": value.get("hp"),
        "max_hp": value.get("max_hp"),
        "block": value.get("block", 0),
        "intent": intent,
        "move_id": _move_id(value),
        "move_damage": damage if is_attack else None,
        "move_hits": value.get("move_hits") if is_attack else None,
        "powers": _powers(value.get("powers") or []),
        "half_dead": bool(value.get("half_dead", False)),
        "is_gone": bool(value.get("is_gone", False)),
    }


def normalize_battle(battle: dict[str, Any]) -> dict[str, Any]:
    """Remove transport-only identifiers while retaining gameplay state."""

    player = battle.get("player") or {}
    return {
        "turn": battle.get("turn"),
        "potions": [
            {
                "id": _name(potion.get("id") or potion.get("name")),
                "can_use": bool(potion.get("can_use", False)),
                "can_discard": bool(potion.get("can_discard", False)),
                "requires_target": bool(potion.get("requires_target", False)),
            }
            for potion in battle.get("potions") or []
        ],
        "relics": [
            {
                "id": _name(relic.get("id") or relic.get("name")),
                "counter": int(relic.get("counter", -1) or 0),
            }
            for relic in battle.get("relics") or []
        ],
        "player": {
            "hp": player.get("hp"),
            "max_hp": player.get("max_hp"),
            "block": player.get("block", 0),
            "energy": player.get("energy", 0),
            "energy_per_turn": player.get("energy_per_turn", 3),
            "card_draw_per_turn": player.get("card_draw_per_turn", 5),
            "powers": _powers(player.get("powers") or []),
            "max_orbs": int(player.get("max_orbs", len(player.get("orbs") or [])) or 0),
            "orbs": [
                {
                    "name": _name(orb.get("id") or orb.get("name")),
                    "passive_amount": int(orb.get("passive_amount") or 0),
                    "evoke_amount": int(orb.get("evoke_amount") or 0),
                }
                for orb in player.get("orbs") or []
            ],
        },
        "hand": [_card(card) for card in battle.get("hand") or []],
        "draw_pile": [_pile_card(card) for card in battle.get("draw_pile") or []],
        "discard_pile": [_pile_card(card) for card in battle.get("discard_pile") or []],
        "exhaust_pile": [_pile_card(card) for card in battle.get("exhaust_pile") or []],
        "choice": {
            "task": _name((battle.get("choice") or {}).get("task")),
            "source": _name((battle.get("choice") or {}).get("source")),
            "options": [
                _card(card) for card in (battle.get("choice") or {}).get("options") or []
            ],
        },
        "enemies": [_enemy(enemy) for enemy in battle.get("enemies") or []],
    }


@dataclass(frozen=True)
class Difference:
    path: str
    expected: Any
    actual: Any


def _diff(expected: Any, actual: Any, path: str, output: list[Difference]) -> None:
    if type(expected) is not type(actual):
        output.append(Difference(path, expected, actual))
        return
    if isinstance(expected, dict):
        for key in sorted(set(expected) | set(actual)):
            if key not in expected or key not in actual:
                output.append(Difference(f"{path}.{key}", expected.get(key), actual.get(key)))
            else:
                _diff(expected[key], actual[key], f"{path}.{key}", output)
    elif isinstance(expected, list):
        if len(expected) != len(actual):
            output.append(Difference(f"{path}.length", len(expected), len(actual)))
        for index, (left, right) in enumerate(zip(expected, actual)):
            _diff(left, right, f"{path}[{index}]", output)
    elif expected != actual:
        output.append(Difference(path, expected, actual))


def compare_battles(expected: dict[str, Any], actual: dict[str, Any]) -> list[Difference]:
    normalized_expected = normalize_battle(expected)
    normalized_actual = normalize_battle(actual)
    for optional_key in ("potions", "relics", "choice"):
        if optional_key not in expected:
            normalized_expected.pop(optional_key, None)
            normalized_actual.pop(optional_key, None)
    for index, enemy in enumerate(expected.get("enemies") or []):
        if "move_id" not in enemy and index < len(normalized_actual["enemies"]):
            normalized_expected["enemies"][index].pop("move_id", None)
            normalized_actual["enemies"][index].pop("move_id", None)
    differences: list[Difference] = []
    _diff(normalized_expected, normalized_actual, "battle", differences)
    return differences


def compare_rng_states(
    expected: dict[str, Any], actual: dict[str, Any], *, path: str = "rng"
) -> list[Difference]:
    """Return the first stream-state divergence in stable stream/field order."""

    for stream in sorted(set(expected) | set(actual)):
        if stream not in expected or stream not in actual:
            return [Difference(
                f"{path}.{stream}", expected.get(stream), actual.get(stream)
            )]
        left = expected[stream]
        right = actual[stream]
        for field in ("counter", "seed0", "seed1"):
            if left.get(field) != right.get(field):
                return [Difference(
                    f"{path}.{stream}.{field}", left.get(field), right.get(field)
                )]
    return []


def record_episode(
    env: BaseSTSEnv,
    policy: Callable[[dict[str, Any], dict[str, Any]], int],
    path: str | Path,
    *,
    seed: int | None = None,
    options: dict[str, Any] | None = None,
    max_steps: int = 1000,
) -> dict[str, Any]:
    observation, info = env.reset(seed=seed, options=options)
    if options is None and env.payload is not None and is_combat_payload(env.payload):
        options = replay_options_from_payload(env.payload)
        seed = (env.payload.get("game_state") or {}).get("seed", seed)
    trace: dict[str, Any] = {
        "trace_version": TRACE_VERSION,
        "environment": type(env).__name__,
        "seed": info.get("seed", seed),
        "options": options or {},
        "initial": info["battle"],
        "initial_legal_actions": semantic_actions(info["legal_actions"]),
        "checks": {"legal_actions": True, "rewards": True, "outcome": True},
        "steps": [],
    }
    if env.payload is not None and isinstance(env.payload.get("_rng"), dict):
        trace["initial_rng"] = env.payload["_rng"]
    for step_number in range(max_steps):
        action_index = int(policy(observation, info))
        if not 0 <= action_index < len(info["legal_actions"]):
            raise ValueError(f"Policy selected illegal action index {action_index}")
        action = semantic_action(info["legal_actions"][action_index])
        observation, reward, terminated, truncated, info = env.step(action_index)
        recorded_step = {
                "step": step_number + 1,
                "action": action,
                "reward": reward,
                "terminated": terminated,
                "truncated": truncated,
                "after": info["battle"],
                "legal_actions": semantic_actions(info["legal_actions"]),
            }
        if env.payload is not None and isinstance(env.payload.get("_rng"), dict):
            recorded_step["rng"] = env.payload["_rng"]
        trace["steps"].append(recorded_step)
        if terminated or truncated:
            trace["outcome"] = info.get("outcome")
            break
    else:
        raise RuntimeError(f"Episode did not finish in {max_steps} steps")

    target = Path(path)
    target.parent.mkdir(parents=True, exist_ok=True)
    # Original-game localized strings may contain lone UTF-16 surrogates.
    # ASCII escaping preserves them as valid JSON without making UTF-8 encoding
    # of the trace file fail after an otherwise successful battle.
    target.write_text(json.dumps(trace, ensure_ascii=True, indent=2), encoding="utf-8")
    return trace


def load_trace(path: str | Path) -> dict[str, Any]:
    trace = json.loads(Path(path).read_text(encoding="utf-8"))
    if trace.get("trace_version") not in SUPPORTED_TRACE_VERSIONS:
        raise ValueError(f"Unsupported trace version: {trace.get('trace_version')}")
    return trace


def replay_trace(env: BaseSTSEnv, trace: dict[str, Any]) -> list[Difference]:
    options = dict(trace.get("options") or {})
    # A combat-only reset is a checkpoint boundary: run-level RNG streams may
    # already have advanced through Neow and other pre-combat setup.  Restore
    # the oracle boundary just as we already restore deck, relics, HP and
    # potions; combat transitions must then advance every stream identically.
    if "initial_rng" in trace:
        options["rng"] = trace["initial_rng"]
    _, info = env.reset(seed=trace.get("seed"), options=options)
    differences = compare_battles(trace["initial"], info["battle"])
    if differences:
        return differences
    if "initial_rng" in trace and env.payload is not None:
        differences = compare_rng_states(
            trace["initial_rng"], env.payload.get("_rng") or {}, path="initial_rng"
        )
        if differences:
            return differences
    checks = trace.get("checks") or {}
    if checks.get("legal_actions") and "initial_legal_actions" in trace:
        expected_actions = semantic_actions(trace["initial_legal_actions"])
        actual_actions = semantic_actions(info["legal_actions"])
        differences = []
        _diff(expected_actions, actual_actions, "initial_legal_actions", differences)
        if differences:
            return differences

    for recorded in trace["steps"]:
        wanted = semantic_action(recorded["action"])
        candidates = [semantic_action(item) for item in info["legal_actions"]]
        try:
            action_index = candidates.index(wanted)
        except ValueError:
            return [Difference("legal_actions", wanted, candidates)]
        _, reward, terminated, truncated, info = env.step(action_index)
        if "rng" in recorded and env.payload is not None:
            differences = compare_rng_states(
                recorded["rng"], env.payload.get("_rng") or {},
                path=f"step[{recorded['step']}].rng",
            )
            if differences:
                return differences
        if terminated != bool(recorded["terminated"]):
            return [Difference(
                f"step[{recorded['step']}].terminated",
                bool(recorded["terminated"]), terminated,
            )]
        if truncated != bool(recorded.get("truncated", False)):
            return [Difference(
                f"step[{recorded['step']}].truncated",
                bool(recorded.get("truncated", False)), truncated,
            )]
        if checks.get("rewards") and reward != recorded.get("reward"):
            return [Difference(
                f"step[{recorded['step']}].reward", recorded.get("reward"), reward
            )]
        differences = compare_battles(recorded["after"], info["battle"])
        if differences:
            prefix = f"step[{recorded['step']}]"
            return [Difference(prefix + item.path[6:], item.expected, item.actual) for item in differences]
        if checks.get("legal_actions") and "legal_actions" in recorded:
            expected_actions = semantic_actions(recorded["legal_actions"])
            actual_actions = semantic_actions(info["legal_actions"])
            differences = []
            _diff(
                expected_actions, actual_actions,
                f"step[{recorded['step']}].legal_actions", differences,
            )
            if differences:
                return differences
        if terminated:
            break
    if (
        checks.get("outcome")
        and trace.get("outcome") is not None
        and trace.get("outcome") != info.get("outcome")
    ):
        return [Difference("outcome", trace.get("outcome"), info.get("outcome"))]
    return []


def _command_action(command: str) -> dict[str, Any] | None:
    parts = command.split()
    if not parts:
        return None
    if parts[0] == "play":
        return semantic_action(
            {
                "kind": "play",
                "card_index": int(parts[1]),
                "target_index": int(parts[2]) if len(parts) > 2 else None,
            }
        )
    if parts[0] == "end":
        return semantic_action({"kind": "end_turn"})
    if parts[0] == "choose":
        return semantic_action({"kind": "choose", "choice_index": int(parts[1])})
    if parts[0] == "potion":
        if len(parts) >= 3 and parts[1] == "use":
            return semantic_action({
                "kind": "potion",
                "potion_index": int(parts[2]),
                "target_index": int(parts[3]) if len(parts) > 3 else None,
            })
        if len(parts) >= 3 and parts[1] == "discard":
            return semantic_action({
                "kind": "discard_potion", "potion_index": int(parts[2])
            })
        return None
    if parts[0] in {"proceed", "confirm"}:
        return semantic_action({"kind": "proceed"})
    if parts[0] in {"cancel", "return", "leave", "skip"}:
        return semantic_action({"kind": "cancel"})
    return None


def import_protocol_log(source: str | Path, target: str | Path) -> dict[str, Any]:
    """Convert an existing StdioTransport JSONL log into a golden trace."""

    records = [
        json.loads(line)
        for line in Path(source).read_text(encoding="utf-8").splitlines()
        if line.strip()
    ]
    start = next(
        index
        for index, record in enumerate(records)
        if record.get("direction") == "rx" and is_combat_payload(record.get("data") or {})
    )
    initial_payload = records[start]["data"]
    game = initial_payload["game_state"]
    trace: dict[str, Any] = {
        "trace_version": TRACE_VERSION,
        "environment": "OriginalSTSEnv",
        "seed": game.get("seed"),
        "options": replay_options_from_payload(initial_payload),
        "initial": rich_battle_state(initial_payload),
        "initial_legal_actions": semantic_actions(
            actions_info(generate_legal_actions(initial_payload))
        ),
        "checks": {"legal_actions": True, "rewards": False, "outcome": True},
        "steps": [],
    }

    index = start + 1
    step_number = 0
    while index < len(records):
        record = records[index]
        index += 1
        if record.get("direction") != "tx":
            continue
        action = _command_action(str(record.get("data", "")))
        if action is None:
            continue
        while index < len(records) and records[index].get("direction") != "rx":
            index += 1
        if index >= len(records):
            break
        after_payload = records[index]["data"]
        index += 1
        step_number += 1
        terminated = not is_combat_payload(after_payload)
        trace["steps"].append(
            {
                "step": step_number,
                "action": action,
                "reward": 0.0,
                "terminated": terminated,
                "truncated": False,
                "after": rich_battle_state(after_payload),
                "legal_actions": (
                    semantic_actions(actions_info(generate_legal_actions(after_payload)))
                    if not terminated else []
                ),
            }
        )
        if terminated:
            final_game = after_payload.get("game_state") or {}
            trace["outcome"] = (
                "PLAYER_VICTORY" if final_game.get("current_hp", 0) > 0 else "PLAYER_LOSS"
            )
            break

    destination = Path(target)
    destination.parent.mkdir(parents=True, exist_ok=True)
    destination.write_text(json.dumps(trace, ensure_ascii=False, indent=2), encoding="utf-8")
    return trace
