"""Strict, first-divergence comparisons for controlled A20 stock captures."""

from __future__ import annotations

import json
from typing import Any, Mapping

from sls.audit.card_parity import structured_differences
from sls.audit.relic_parity import _combat_projection
from sls.audit.semantic_actions import resolve_target
from sls.backends.original.adapter import adapt_original
from sls.content.normalize import (
    normalize_card_id,
    normalize_content_id,
    normalize_monster_id,
    normalize_potion_id,
    normalize_power_amount,
    normalize_power_id,
    normalize_relic_counter,
)
from sls.rl.training_contract import native_source_digest


def production_combat_projection(battle: Any) -> dict[str, Any]:
    """Read production presentation while retaining the separate raw serializer."""
    payload = battle.snapshot()
    if "combat_state" not in payload["game_state"]:
        raise ValueError("production combat projection requires a combat boundary; use an explicit ending comparator")
    public = battle.public_combat_probe_snapshot()
    combat = payload['game_state']['combat_state']
    payload['_lossless_monsters'] = combat['monsters']
    combat['monsters'] = [dict(row, id=row['content_id'], monster_id=row['content_id'],
                              move_adjusted_damage=row['intent_damage'],
                              move_hits=row['intent_hits'], half_dead=row.get('half_dead', False))
                          for row in public['monsters']]
    payload['_native_comparison_view'] = 'FULL_RUN_PUBLIC_COMBAT'
    return payload


def heart_terminal_resources(payload: Mapping[str, Any], *, stock: bool) -> dict[str, Any]:
    """Explicit ending resources, not a combat-pile or whole-run certificate."""
    game = payload["game_state"]
    if (game["ascension_level"], game["act"]) != (20, 4):
        raise ValueError("Heart ending evidence requires actual A20 Act4")
    if stock:
        direct = payload["_stock_direct"]
        hp = direct["player"]["current_hp"]
        if (direct["act"] != 4 or direct["ascension"] != 20
                or direct["dungeon_class"] != "com.megacrit.cardcrawl.dungeons.TheEnding"
                or direct["room_class"] != "com.megacrit.cardcrawl.rooms.MonsterRoomBoss"
                or hp != game["current_hp"]):
            raise ValueError("independent stock Heart ending context mismatch")
        monsters = direct["monsters"]
        if len(monsters) != 1 or normalize_monster_id(monsters[0]["id"]) != "CORRUPT_HEART":
            raise ValueError("terminal Heart proof cannot use another encounter")
        if (game["screen_type"] in {"DEATH", "GAME_OVER"} and hp == 0
                and payload["_continuation"]["continuation_kind"] == "DEATH"):
            outcome = "PLAYER_LOSS"
        elif (game["screen_type"] == "COMPLETE" and game["room_phase"] == "COMPLETE"
              and hp > 0
              and monsters[0]["current_hp"] == 0 and monsters[0]["dying"] is True):
            outcome = "PLAYER_VICTORY"
        else:
            raise ValueError("missing independent Heart terminal proof")
        decision = adapt_original(payload).decision
        if not decision.terminal or decision.actions:
            raise ValueError("stock Heart terminal must have no policy actions")
        deck = game["deck"]
    else:
        outcome = payload["outcome"]
        if outcome not in {"PLAYER_LOSS", "PLAYER_VICTORY"} or payload["_legal_actions"]:
            raise ValueError("native Heart terminal must have no policy actions")
        deck = game["master_deck"]
    return {
        "outcome": outcome, "hp": game["current_hp"], "max_hp": game["max_hp"],
        "gold": game["gold"],
        "deck": [{"id": normalize_card_id(c["id"]), "upgrades": c["upgrades"]} for c in deck],
        "relics": [{"id": normalize_content_id(r["id"]),
                    "counter": normalize_relic_counter(r["counter"])} for r in game["relics"]],
        "potions": [{"id": normalize_potion_id(p["id"]),
                     "slot": index if normalize_potion_id(p["id"]) == "EMPTY_POTION_SLOT"
                     else p["slot"]} for index, p in enumerate(game["potions"])],
        "rng": payload["_rng"], "policy_actions": [],
    }


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


def _direct_cards_and_items(raw: Mapping[str, Any], items: Mapping[str, Any]) -> dict[str, Any]:
    # Ordered controlled piles are compared as read, including hidden order.
    # UUIDs differ by construction and are never used as card identities.
    result = {zone: [dict(id=normalize_card_id(card['id']), **{
        key: card[key] for key in ('base_cost', 'cost', 'upgrades',
                                  'free_to_play_once', 'retain', 'self_retain')
    }) for card in raw[zone]]
        for zone in ('hand', 'draw_pile', 'discard_pile', 'exhaust_pile')}
    result['relics'] = [{'id': normalize_content_id(row['id']),
                         'counter': normalize_relic_counter(row['counter'])}
                        for row in items['relics']]
    result['potions'] = [{'id': normalize_potion_id(row['id']),
                          # Stock PotionSlot.slot=-1 is a placeholder, while
                          # container position is its actual UI slot. Occupied
                          # potion.slot is essential to destroyPotion and stays raw.
                          'slot': index if normalize_potion_id(row['id']) == 'EMPTY_POTION_SLOT'
                          else row['slot'], 'requires_target': row['requires_target']}
                         for index, row in enumerate(items['potions'])]
    return result


def direct_projection(payload: Mapping[str, Any], *, stock: bool,
                      extended: bool = False) -> dict[str, Any]:
    if stock:
        direct = payload["_stock_direct"]
        if direct["schema"] not in {"sls-stock-direct-v1", "sls-stock-direct-v2"}:
            raise ValueError("unsupported independent stock projection")
        result = {"player": _creature(direct["player"]), "energy": direct["energy"],
                "monsters": [dict(_creature(m), id=normalize_monster_id(m["id"]))
                             for m in direct["monsters"]]}
        if direct['schema'] == 'sls-stock-direct-v2':
            result.update(_direct_cards_and_items(direct, direct))
        return result
    combat = payload["game_state"]["combat_state"]
    result = {"player": _creature(combat["player"]), "energy": combat["player"]["energy"],
            "monsters": [dict(_creature(m), id=normalize_monster_id(m["id"]))
                         for m in combat["monsters"]]}
    if extended:
        result.update(_direct_cards_and_items(combat, payload['game_state']))
    return result


def comparison_projection(payload: Mapping[str, Any], *, stock: bool,
                          extended_direct: bool = False) -> dict[str, Any]:
    game = payload["game_state"]
    # Combat loss is an explicit terminal boundary, not a waiting-for-player
    # exception. Retain direct piles/powers/RNG even when the public adapter
    # intentionally hides them on GAME_OVER. Victory/rewards remain unsupported.
    hp = (payload['_stock_direct']['player']['current_hp'] if stock
          else game.get('combat_state', {}).get('player', {}).get('current_hp'))
    loss = (str(game.get('screen_type', '')).upper() in {'DEATH', 'GAME_OVER'}
            and hp == 0) if stock else game.get('outcome') == 'PLAYER_LOSS' and hp == 0
    if stock and str(game.get('screen_type', '')).upper() in {'DEATH', 'GAME_OVER', 'VICTORY'} and not loss:
        raise ValueError('unsupported terminal boundary; victory needs a separate flow comparison')
    if not stock and game.get('outcome') in {'PLAYER_VICTORY', 'ESCAPED'}:
        raise ValueError('unsupported terminal boundary; victory needs a separate flow comparison')
    if not stock and game.get('outcome') == 'PLAYER_LOSS' and hp != 0:
        raise ValueError('combat loss is not a stable zero-HP terminal boundary')
    stable = game.get("action_phase") == "WAITING_ON_USER" if stock else (
        game.get("input_state") == "PLAYER_NORMAL")
    if not stable and not loss:
        raise ValueError("capture is not a stable waiting-for-player boundary")
    if stock and pending_stock_intents(payload):
        raise ValueError('stock intent not materialized at a player boundary')
    if int(game["ascension_level"]) != 20:
        raise ValueError("A20 qualification refuses other ascensions")
    if "_rng" not in payload:
        raise ValueError("controlled capture lacks RNG evidence")
    if stock:
        adapted = adapt_original(payload)
        if loss and (not adapted.decision.terminal or adapted.decision.actions):
            raise ValueError('combat loss must be terminal with no legal actions')
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
        if loss and actual_actions:
            raise ValueError('combat loss must have no legal actions')
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
    # The isolated native battle serializer declares its outcome rather than
    # a full-run UI screen. Translate only the independently checked loss for
    # this audit adapter; retain the original payload and all direct evidence.
    adapted_payload = (dict(payload, game_state=dict(game, screen_type='GAME_OVER'))
                       if loss and not stock else payload)
    return {"adapted": _combat_projection(adapted_payload),
            "actual_actions": sorted(json.dumps(row, sort_keys=True) for row in actual_actions),
            "direct": direct_projection(payload, stock=stock, extended=extended_direct),
            "rng": payload["_rng"]}


def encounter_terminal_subset(payload: Mapping[str, Any], *, stock: bool,
                              retained: Mapping[str, Any] | None = None) -> dict[str, Any]:
    """Explicit selected victory fields; no reward, item or corpse-cleanup claim."""
    game = payload['game_state']
    if stock:
        direct = payload['_stock_direct']
        monsters = direct['monsters']
        if (game.get('room_phase') != 'COMPLETE' or direct['player']['current_hp'] <= 0
                or direct['player']['current_hp'] != game['current_hp']
                or direct['player']['max_hp'] != game['max_hp']
                or not monsters or not all(
                    (m['current_hp'] == 0 and m['dying'] is True) or m['escaped'] is True for m in monsters)):
            raise ValueError('missing independent stock encounter victory proof')
        rows = [{'id': normalize_monster_id(m['id']), 'hp': m['current_hp'], 'max_hp': m['max_hp']}
                for m in monsters]
    else:
        if (payload.get('outcome') != 'PLAYER_VICTORY' or payload['_legal_actions'] or retained is None
                or game['current_hp'] <= 0):
            raise ValueError('native ending subset requires completed encounter and retained monsters')
        rows = [{'id': normalize_monster_id(m['content_id']), 'hp': m['current_hp'], 'max_hp': m['max_hp']}
                for m in retained['monsters']]
    return {'encounter_won': True, 'hp': game['current_hp'], 'max_hp': game['max_hp'],
            'rng': payload['_rng'], 'monsters': rows}


def replay_controlled_run(run: Mapping[str, Any], *, production_projection: bool = False,
                          continue_after_divergence: bool = False,
                          heart_ending_resources: bool = False,
                          encounter_ending_subset: bool = False) -> dict[str, Any]:
    from sls.backends.simulator import native

    if heart_ending_resources and encounter_ending_subset:
        raise ValueError('choose exactly one explicit ending comparison scope')

    if native.NATIVE_SOURCE_SHA256 != native_source_digest():
        raise ValueError("stale native artifact; rebuild before differential replay")
    if not run.get("before", {}).get("_rng"):
        raise ValueError("missing pre-constructor initial state")
    boundaries = run["boundaries"]
    actions = run["actions"]
    if len(boundaries) != len(actions) + 1:
        raise ValueError("missing action or boundary")
    if 'resolved_actions' in run:
        if len(run['resolved_actions']) != len(actions):
            raise ValueError('missing resolved semantic action evidence')
        for index, action in enumerate(actions):
            resolved = resolve_target(action, boundaries[index]['game_state']['combat_state']['monsters'])
            if resolved != run['resolved_actions'][index]:
                raise ValueError('stock semantic target resolution differs from recorded command')
    for payload in boundaries:
        if payload.get("_oracle_mode") != "validation":
            raise ValueError("controlled run must use validation mode")
        direct = payload["_stock_direct"]
        if (direct["ascension"], direct["act"], direct["floor"]) != (20, run["act"], run["floor"]):
            raise ValueError("declared difficulty or context differs from actual stock state")
        if run.get('scene', {}).get('actual_dungeon_required'):
            expected = {3: 'TheBeyond', 4: 'TheEnding'}[run['act']]
            room = {'MONSTER': 'MonsterRoom', 'ELITE': 'MonsterRoomElite', 'BOSS': 'MonsterRoomBoss'}[
                run['scene']['room']]
            if (direct.get('dungeon_id') != expected
                    or direct.get('dungeon_class') != 'com.megacrit.cardcrawl.dungeons.' + expected
                    or direct.get('room_class') != 'com.megacrit.cardcrawl.rooms.' + room):
                raise ValueError('actual stock dungeon or room context mismatch')
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
        # set_potions is a legacy A0 probe helper with three-slot default.
        # These A20 scenes explicitly have only Burning Blood, no Potion Belt.
        # Restore the two-slot controlled initial condition through the existing
        # lossless checkpoint API; do not change production potion semantics.
        snapshot = battle.snapshot()
        snapshot['game_state']['combat_state']['_internal']['potion_capacity'] = 2
        if heart_ending_resources:
            # Match independently captured initial permanent resources, not
            # the final desired result. Controlled start may include Neow's
            # upgrades/removals; isolated reset() alone uses a starter deck.
            stock_initial = boundaries[0]['game_state']
            snapshot['game_state']['gold'] = stock_initial['gold']
            snapshot['game_state']['combat_state']['player']['_internal']['gold'] = stock_initial['gold']
            snapshot['game_state']['master_deck'] = [
                {'id': normalize_card_id(card['id']), 'upgrades': card['upgrades'],
                 'misc': card.get('misc', 0)} for card in stock_initial['deck']]
        battle.load_checkpoint({'game_state': snapshot['game_state'], 'rng': snapshot['_rng']})
    results = []
    first = None
    for index, original in enumerate(boundaries):
        if index:
            prior = production_combat_projection(battle) if production_projection else battle.snapshot()
            action = resolve_target(actions[index - 1], prior['game_state']['combat_state']['monsters'])
            if "target_index" in action:
                target = prior["game_state"]["combat_state"]["monsters"][action["target_index"]]
                action["target_index"] = int(target["instance_id"].split(":")[1])
            battle.step(action.pop("kind"), **action)
        ending = heart_ending_resources and index == len(boundaries) - 1
        subset_ending = encounter_ending_subset and index == len(boundaries) - 1
        if ending:
            current = battle.snapshot()
            differences = structured_differences(heart_terminal_resources(original, stock=True),
                                                heart_terminal_resources(current, stock=False))
        elif subset_ending:
            current = battle.snapshot()
            differences = structured_differences(
                encounter_terminal_subset(original, stock=True),
                encounter_terminal_subset(current, stock=False, retained=battle.public_combat_probe_snapshot()))
        else:
            current = production_combat_projection(battle) if production_projection else battle.snapshot()
            stock_projection = comparison_projection(original, stock=True)
            native_projection = comparison_projection(current, stock=False,
                extended_direct=original['_stock_direct']['schema'] == 'sls-stock-direct-v2')
            differences = structured_differences(stock_projection, native_projection)
        classification = None
        if differences and not ending and not subset_ending:
            from sls.audit.corpse_cleanup import classify_controlled_corpse_cleanup
            classification = classify_controlled_corpse_cleanup(stock_projection, native_projection, original)
        results.append({"boundary": index, "differences": differences, "classification": classification})
        if differences and first is None:
            first = {"boundary": index, "differences": differences, "classification": classification}
            if not continue_after_divergence:
                break
    return {"encounter": run["encounter"], "seed": run["seed"],
            "scene_id": run.get("scene", {}).get("id"),
            "status": ("CLASSIFIED_CORPSE_ANIMATION_DIFFERENCE" if first and
                all(not row['differences'] or row['classification'] for row in results)
                else "SEMANTIC_DIFFERENCE") if first else (
                    "CONTROLLED_PREFIX_AND_ENDING_SUBSET_MATCH" if encounter_ending_subset else "HARNESS_MATCH"),
            "first_divergence": first, "boundaries": results,
            "native_comparison_view": 'FULL_RUN_PUBLIC_COMBAT' if production_projection else 'LOSSLESS_COMBAT_SERIALIZER',
            "terminal_comparison_scope": ("HEART_ENDING_RESOURCES_NOT_COMBAT_CLEANUP" if heart_ending_resources
                else "SELECTED_VICTORY_FIELDS_NOT_REWARD_FLOW_OR_CLEANUP" if encounter_ending_subset else None),
            "qualification": False}
