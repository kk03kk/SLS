"""Strict, first-divergence comparisons for controlled A20 stock captures."""

from __future__ import annotations

import json
from typing import Any, Mapping

from sls.audit.card_parity import structured_differences
from sls.audit.relic_parity import _combat_projection
from sls.backends.original.adapter import adapt_original
from sls.content.normalize import (
    normalize_monster_id,
    normalize_power_amount,
    normalize_power_id,
)
from sls.rl.training_contract import native_source_digest


def pending_stock_intents(payload: Mapping[str, Any]) -> list[int]:
    """Read actual public stock intent, independently of Oracle move projection."""
    game = payload.get('game_state') or {}
    if str(game.get('screen_type') or '').upper() in {'DEATH', 'VICTORY', 'GAME_OVER', 'COMPLETE'}:
        return []
    monsters = (game.get('combat_state') or {}).get('monsters') or []
    return [i for i, monster in enumerate(monsters)
            if str(monster.get('intent') or '').upper() == 'DEBUG'
            and int(monster.get('current_hp', 1)) > 0
            and not monster.get('is_gone') and not monster.get('half_dead')]


def _creature(row: Mapping[str, Any]) -> dict[str, Any]:
    return {
        key: row[key] for key in ("current_hp", "max_hp", "block")
    } | {"powers": sorted([
        {"id": normalize_power_id(p["id"]),
         "amount": normalize_power_amount(p["id"], p["amount"])}
        for p in row["powers"]
    ], key=lambda item: item["id"])}


def direct_projection(payload: Mapping[str, Any], *, stock: bool) -> dict[str, Any]:
    if stock:
        direct = payload["_stock_direct"]
        if direct["schema"] != "sls-stock-direct-v1":
            raise ValueError("unsupported independent stock projection")
        return {"player": _creature(direct["player"]), "energy": direct["energy"],
                "monsters": [dict(_creature(m), id=normalize_monster_id(m["id"]))
                             for m in direct["monsters"]]}
    combat = payload["game_state"]["combat_state"]
    return {"player": _creature(combat["player"]), "energy": combat["player"]["energy"],
            "monsters": [dict(_creature(m), id=normalize_monster_id(m["id"]))
                         for m in combat["monsters"]]}


def comparison_projection(payload: Mapping[str, Any], *, stock: bool) -> dict[str, Any]:
    game = payload["game_state"]
    stable = game.get("action_phase") == "WAITING_ON_USER" if stock else (
        game.get("input_state") == "PLAYER_NORMAL")
    if not stable:
        raise ValueError("capture is not a stable waiting-for-player boundary")
    if stock and pending_stock_intents(payload):
        raise ValueError('stock intent not materialized at a player boundary')
    if int(game["ascension_level"]) != 20:
        raise ValueError("A20 qualification refuses other ascensions")
    if "_rng" not in payload:
        raise ValueError("controlled capture lacks RNG evidence")
    if stock:
        adapted = adapt_original(payload)
        actual_actions = []
        for action in adapted.decision.actions:
            commands = adapted.commands[action.candidate_id]
            if len(commands) != 1:
                raise ValueError("controlled combat action unexpectedly folds commands")
            words = commands[0].split()
            if words[0] == "end":
                semantic = {"kind": "end_turn"}
            elif words[0] == "play":
                semantic = {"kind": "play", "card_index": int(words[1])}
                if len(words) > 2:
                    semantic["target_index"] = int(words[2])
            elif words[0] == "potion":
                semantic = {"kind": "potion", "potion_index": int(words[2])}
                if words[1] == "discard":
                    semantic["kind"] = "discard_potion"
                elif len(words) > 3:
                    semantic["target_index"] = int(words[3])
            else:
                raise ValueError(f"unsupported controlled action: {commands}")
            actual_actions.append(semantic)
    else:
        actual_actions = [{k: v for k, v in row.items() if v is not None}
                          for row in payload["_legal_actions"]]
        # Native reserves slots for future summons. Its public monster list
        # omits INVALID slots (module.cpp combat_state), as does stock UI.
        # Match targets by their position in that complete public list, never
        # by content ID: identical monsters and retained corpses stay distinct.
        slots = {int(monster["instance_id"].split(":")[1]): index
                 for index, monster in enumerate(game["combat_state"]["monsters"])
                 if monster["instance_id"].split(":")[1].isdigit()}
        for action in actual_actions:
            if "target_index" in action:
                action["target_index"] = slots[action["target_index"]]
    return {"adapted": _combat_projection(payload),
            "actual_actions": sorted(json.dumps(row, sort_keys=True) for row in actual_actions),
            "direct": direct_projection(payload, stock=stock), "rng": payload["_rng"]}


def replay_controlled_run(run: Mapping[str, Any]) -> dict[str, Any]:
    from sls.backends.simulator import native

    if native.NATIVE_SOURCE_SHA256 != native_source_digest():
        raise ValueError("stale native artifact; rebuild before differential replay")
    if not run.get("before", {}).get("_rng"):
        raise ValueError("missing pre-constructor initial state")
    boundaries = run["boundaries"]
    actions = run["actions"]
    if len(boundaries) != len(actions) + 1:
        raise ValueError("missing action or boundary")
    for payload in boundaries:
        if payload.get("_oracle_mode") != "validation":
            raise ValueError("controlled run must use validation mode")
        direct = payload["_stock_direct"]
        if (direct["ascension"], direct["act"], direct["floor"]) != (20, run["act"], run["floor"]):
            raise ValueError("declared difficulty or context differs from actual stock state")
    battle = native.LightspeedBattle()
    battle.reset_encounter_probe(run["seed"], run["encounter"], run["before"]["_rng"],
                                 ascension=20, act=run["act"], floor=run["floor"],
                                 scenario_id=run.get("scene", {}).get("id", "harness"))
    if "scene" in run:
        initial = run["scene"]["initial"]
        battle.set_player_health(initial["hp"], initial.get("max_hp", initial["hp"]))
        snapshot = battle.snapshot()
        combat = snapshot["game_state"]["combat_state"]
        combat["player"]["energy"] = initial["energy"]
        combat["player"]["block"] = initial["block"]
        for monster in combat["monsters"]:
            identifier = monster["monster_id"]
            if identifier in initial.get("monster_hp", {}):
                monster["current_hp"] = initial["monster_hp"][identifier]
            if identifier in initial.get("moves", {}):
                monster["move_id"] = initial["moves"][identifier]["native"]
        battle.load_checkpoint({"game_state": snapshot["game_state"], "rng": snapshot["_rng"]})
        battle.set_card_piles(initial["hand"], initial["draw"], [], [])
        battle.set_potions([initial["potion"]] if "potion" in initial else [])
    results = []
    first = None
    for index, original in enumerate(boundaries):
        if index:
            action = dict(actions[index - 1])
            if "target_index" in action:
                target = battle.snapshot()["game_state"]["combat_state"]["monsters"][action["target_index"]]
                action["target_index"] = int(target["instance_id"].split(":")[1])
            battle.step(action.pop("kind"), **action)
        current = battle.snapshot()
        differences = structured_differences(comparison_projection(original, stock=True),
                                            comparison_projection(current, stock=False))
        results.append({"boundary": index, "differences": differences})
        if differences and first is None:
            first = {"boundary": index, "differences": differences}
            break
    return {"encounter": run["encounter"], "seed": run["seed"],
            "scene_id": run.get("scene", {}).get("id"),
            "status": "SEMANTIC_DIFFERENCE" if first else "HARNESS_MATCH",
            "first_divergence": first, "boundaries": results,
            "qualification": False}
