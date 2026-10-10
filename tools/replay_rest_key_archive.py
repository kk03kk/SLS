"""Replay stock-bound rest choices and resources with full native restore checks.

Map continuation and disabled-final-act engine support remain separate findings.
"""
from __future__ import annotations

import argparse
import copy
import hashlib
import json
import os
from dataclasses import asdict
from pathlib import Path

from sls.backends.original.adapter import adapt_original
from sls.content.normalize import (
    normalize_card_id,
    normalize_content_id,
    normalize_potion_id,
    normalize_relic_counter,
)
from sls.curriculum import IRONCLAD_A20_HEART
from tools.audit_stock_key_room_capture import audit
from tools.reproduce_double_boss_entry import enum_ids

ROOT = Path(__file__).resolve().parents[1]


def differences(expected, actual, path=""):
    if isinstance(expected, dict) and isinstance(actual, dict):
        result = []
        for key in sorted(set(expected) | set(actual)):
            if key not in expected or key not in actual:
                result.append(dict(path=f"{path}.{key}", expected=expected.get(key), actual=actual.get(key)))
            else:
                result.extend(differences(expected[key], actual[key], f"{path}.{key}"))
        return result
    if isinstance(expected, (list, tuple)) and isinstance(actual, (list, tuple)):
        if len(expected) != len(actual):
            return [dict(path=path, expected=expected, actual=actual)]
        return [item for index, (left, right) in enumerate(zip(expected, actual, strict=True))
                for item in differences(left, right, f"{path}[{index}]" )]
    return [] if expected == actual else [dict(path=path, expected=expected, actual=actual)]


def stock_resources(payload, *, canonical_counters=True):
    direct, game, keys = payload["_stock_direct"], payload["game_state"], payload["_parity_run"]
    if (direct["player"]["current_hp"] != game["current_hp"]
            or direct["player"]["max_hp"] != game["max_hp"]
            or [(card["id"], card["upgrades"]) for card in direct["master_deck"]]
            != [(card["id"], card["upgrades"]) for card in game["deck"]]):
        raise ValueError("independent stock resource projections disagree")
    return dict(hp=direct["player"]["current_hp"], max_hp=direct["player"]["max_hp"], gold=game["gold"],
                ruby_key=keys["ruby_key"], emerald_key=keys["emerald_key"], sapphire_key=keys["sapphire_key"],
                deck=[dict(id=normalize_card_id(card["id"]), upgrades=card["upgrades"], special_data=card["special_data"])
                      for card in game["deck"]],
                relics=[dict(id=normalize_content_id(relic["id"]),
                             counter=normalize_relic_counter(relic["counter"]) if canonical_counters else relic["counter"])
                        for relic in direct["relics"]],
                potions=[dict(id=normalize_potion_id(potion["id"]), slot=index)
                         for index, potion in enumerate(direct["potions"])])


def native_resources(state, *, canonical_counters=True):
    player, inventory = state["player_state"], state["public_inventory"]
    return dict(hp=player["current_hp"], max_hp=player["max_hp"], gold=player["gold"],
                ruby_key=player["red_key"], emerald_key=player["green_key"], sapphire_key=player["blue_key"],
                deck=[dict(id=card["content_id"], upgrades=card["upgrades"], special_data=card["special_data"])
                      for card in inventory["deck"]],
                relics=[dict(id=relic["content_id"],
                             counter=normalize_relic_counter(relic["counter"]) if canonical_counters else relic["counter"])
                        for relic in inventory["relics"]],
                potions=[dict(id=potion["content_id"], slot=potion["slot"]) for potion in inventory["potions"]])


def prepare(row, native, cards, relics, encounters):
    stock, scene = row["boundaries"][0], row["scene"]
    direct = stock["_stock_direct"]
    if any(card["upgrades"] or card["special_data"] for card in stock["game_state"]["deck"]):
        raise ValueError("this frozen replay only supports the unmodified starting deck")
    run = native.LightspeedRunState()
    run.reset(row["seed"], 20)
    state = run.snapshot()
    if not scene["initial"]["final_act_available"]:
        if any(node.visible_room_type == "BURNING_ELITE" for node in adapt_original(stock).decision.observation.map_nodes):
            raise ValueError("disabled-final-act fixture unexpectedly has a burning elite")
        burning_x, burning_y = -1, -1
    else:
        burning_x, burning_y = stock["_parity_run"]["burning_elite_x"], stock["_parity_run"]["burning_elite_y"]
    state["run_state"].update(act=2, floor=scene["floor"],
                              burning_elite_x=burning_x, burning_elite_y=burning_y)
    state["derived_rng"]["map"].update(act=2, derived_seed=row["seed"] + 200,
                                     assign_burning_elite=scene["initial"]["final_act_available"])
    rest = scene["room"] == "REST"
    state["progress_state"].update(screen_state=7 if rest else 6, current_room=1 if rest else 5,
                                  current_event=0,
                                  boss=encounters[adapt_original(stock).decision.observation.run.visible_boss_id],
                                  current_map_x=stock["_parity_run"]["current_map_x"],
                                  current_map_y=stock["_parity_run"]["current_map_y"])
    if rest:
        state["screen_info"] = dict(screen_state=7, complete=True, continuation="map")
    else:
        if direct.get("key_room_evidence_schema") != "sls-stock-key-room-v1":
            raise ValueError("chest replay requires independent stock pool/chest evidence")
        if set(direct["ordered_relic_pools"]) != {
                "common_relics", "uncommon_relics", "rare_relics", "shop_relics", "boss_relics"}:
            raise ValueError("missing or unexpected stock relic pool category")
        chest = direct["chest"]
        if chest["open"]:
            raise ValueError("chest fixture already opened")
        state["screen_info"] = dict(screen_state=6, complete=True, continuation="map",
                                    chest_size={"SmallChest": 0, "MediumChest": 1, "LargeChest": 2}[chest["class"]],
                                    have_gold=chest["gold_reward"],
                                    relic_tier={"COMMON_RELIC": 0, "UNCOMMON_RELIC": 1, "RARE_RELIC": 2}[chest["relic_reward"]])
        for name, pool in direct["ordered_relic_pools"].items():
            if name not in {"common_relics", "uncommon_relics", "rare_relics", "shop_relics", "boss_relics"}:
                raise ValueError("unexpected stock pool category")
            state["ordered_pools"][name] = [relics[normalize_content_id(identifier)] for identifier in pool]
    state["rng"] = copy.deepcopy(stock["_rng"])
    state["player_state"].update(
        current_hp=scene["initial"]["hp"], max_hp=scene["initial"]["max_hp"], gold=scene["initial"]["gold"],
        red_key=scene["initial"]["ruby_key"], blue_key=scene["initial"]["sapphire_key"], green_key=False,
        deck=[dict(id=cards[normalize_card_id(card["id"])], upgraded=False, misc=0)
              for card in direct["master_deck"]],
        relics=[dict(id=relics[normalize_content_id(relic["id"])], data=max(0, relic["counter"]))
                for relic in direct["relics"]])
    run.load_state(state)
    return run


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--capture", type=Path, required=True)
    parser.add_argument("--oracle-build", type=Path, required=True)
    parser.add_argument("--manifest", type=Path, required=True)
    parser.add_argument("--scenes", nargs="+", required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    if args.output.exists():
        raise FileExistsError("refuse to overwrite replay evidence")
    os.environ["CUDA_VISIBLE_DEVICES"] = "-1"
    evidence = audit(args.capture, args.oracle_build, args.manifest, args.scenes)
    data = json.loads(args.capture.read_text(encoding="utf-8"))
    from sls.backends.simulator import SimulatorBackend, native
    from sls.rl.training_contract import native_source_digest

    if native.NATIVE_SOURCE_SHA256 != native_source_digest():
        raise ValueError("stale native binary")
    constants = ROOT / "native/simulator/include/constants"
    cards = enum_ids(constants / "Cards.h", "enum class CardId : std::uint16_t {")
    relics = enum_ids(constants / "Relics.h", "enum class RelicId : std::uint8_t {")
    encounters = enum_ids(constants / "MonsterEncounters.h", "enum class MonsterEncounter : std::int8_t {")
    results = []
    for row in data["runs"]:
        if row["scene"]["id"] not in args.scenes:
            continue
        if row["scene"]["room"] != "REST" or len(row["actions"]) != 1:
            raise ValueError("only frozen single-choice rest branches supported")
        run = prepare(row, native, cards, relics, encounters)
        before = run.snapshot()
        clone = native.LightspeedRunState()
        clone.load_state(before)
        stock_before, stock_after = row["boundaries"]
        original = adapt_original(stock_before)
        backend = SimulatorBackend(profile=IRONCLAD_A20_HEART)
        decision = backend._adapt(before)
        original_actions = [asdict(action) for action in original.decision.actions]
        native_actions = [asdict(action) for action in decision.actions]
        native_bits = 2 if row["actions"][0]["requested"] == "RECALL" else 0
        if native_bits not in {action["bits"] for action in before["legal_actions"]}:
            raise ValueError("recorded action is not native-legal")
        run.step(native_bits)
        clone.step(native_bits)
        after = run.snapshot()
        restored = native.LightspeedRunState()
        restored.load_state(after)
        resource_diff = differences(stock_resources(stock_after), native_resources(after))
        results.append(dict(scene=row["scene"]["id"], seed=row["seed"],
                            final_act_flag_representable=row["scene"]["initial"]["final_act_available"],
                            initial_resource_differences=differences(stock_resources(stock_before), native_resources(before)),
                            legal_action_differences=differences(original_actions, native_actions),
                            initial_observation_differences=differences(asdict(original.decision.observation), asdict(decision.observation)),
                            resource_differences=resource_diff,
                            raw_resource_differences=differences(stock_resources(stock_after, canonical_counters=False),
                                                                 native_resources(after, canonical_counters=False)),
                            rng_differences=differences(stock_after["_rng"], after["rng"]),
                            restored_suffix_equal=clone.snapshot() == after,
                            post_restore_equal=restored.snapshot() == after,
                            native_before=before, native_after=after,
                            original_terminal_screen=adapt_original(stock_after).decision.observation.screen,
                            native_terminal_screen=backend._adapt(after).observation.screen))
        if "automatic_map_boundary" in row:
            stock_map = row["automatic_map_boundary"]
            results[-1]["map_observation_differences"] = differences(
                asdict(adapt_original(stock_map).decision.observation), asdict(backend._adapt(after).observation))
            results[-1]["map_action_differences"] = differences(
                [asdict(action) for action in adapt_original(stock_map).decision.actions],
                [asdict(action) for action in backend._adapt(after).actions])
            results[-1]["map_rng_differences"] = differences(stock_map["_rng"], after["rng"])
    report = dict(schema="sls-rest-key-replay-v1", stock_capture_integrity=evidence,
                  scope="REST_CHOICES_RESOURCES_RNG_NATIVE_RESTORE",
                  map_continuation="CAPTURED_IF_PRESENT_PER_CASE", natural_trajectory=False,
                  canonical_counter_contract="EXISTING_normalize_relic_counter",
                  initial_fields_not_qualified=["ordered_pools", "burning_elite_buff", "encounter_lists", "boss_sequence"],
                  native_source_sha256=native.NATIVE_SOURCE_SHA256,
                  native_binary_sha256=hashlib.sha256(Path(native.__file__).read_bytes()).hexdigest(), runs=results)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    with args.output.open("x", encoding="utf-8") as stream:
        json.dump(report, stream, indent=2)
        stream.write("\n")
    print(json.dumps({"cases": len(results), "resource_matches": sum(not r["resource_differences"] for r in results),
                      "rng_matches": sum(not r["rng_differences"] for r in results),
                      "legal_action_matches": sum(not r["legal_action_differences"] for r in results),
                      "restore_matches": sum(r["post_restore_equal"] and r["restored_suffix_equal"] for r in results)}))


if __name__ == "__main__":
    main()
