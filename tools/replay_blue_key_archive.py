"""Replay actual chest opening and linked reward choices from stock initial data."""
from __future__ import annotations

import argparse
import copy
import hashlib
import json
import os
from dataclasses import asdict
from pathlib import Path

from sls.backends.original.adapter import adapt_original
from sls.content.normalize import normalize_content_id
from sls.contracts import ActionKind
from sls.curriculum import IRONCLAD_A20_HEART
from tools.audit_stock_key_room_capture import audit
from tools.replay_rest_key_archive import (
    differences,
    native_resources,
    prepare,
    stock_resources,
)
from tools.reproduce_double_boss_entry import enum_ids
from tools.run_key_room_batch import scene_floor

ROOT = Path(__file__).resolve().parents[1]


def stock_rewards(payload):
    rows = payload["_stock_reward_state"]["screen_rewards"]
    if any(row["type"] not in {"GOLD", "RELIC", "SAPPHIRE_KEY"} or row["done"] or row["ignored"] for row in rows):
        raise ValueError("unexpected active stock reward in frozen chest script")
    return dict(gold=[row["gold"] + row["bonus_gold"] for row in rows if row["type"] == "GOLD"],
                relics=[normalize_content_id(row["relic"]) for row in rows if row["type"] == "RELIC"],
                sapphire_key=any(row["type"] == "SAPPHIRE_KEY" for row in rows))


def native_rewards(state, names):
    rewards = state["screen_info"]["rewards"]
    if rewards["card_rewards"] or rewards["potions"] or rewards["emerald_key"]:
        raise ValueError("unexpected native reward in frozen chest script")
    return dict(gold=rewards["gold"], relics=[names[value] for value in rewards["relics"]],
                sapphire_key=rewards["sapphire_key"])


def chest_creation(row, initial, native):
    """Execute actual native map entry; never add an edge to match an injected room."""
    stock = row["boundaries"][0]
    x, y = stock["_parity_run"]["current_map_x"], stock["_parity_run"]["current_map_y"]
    parents = [node for node in initial["public_map"] if node["y"] == y - 1
               and f"map:{x}:{y}" in node["outgoing_node_ids"]]
    if not parents:
        return dict(status="UNSUPPORTED_CONTROLLED_NODE_NOT_REACHABLE", stock_node=[x, y])
    state = copy.deepcopy(initial)
    state["run_state"]["floor"] = scene_floor(row["scene"], stock) - 1
    state["progress_state"].update(screen_state=5, current_map_x=parents[0]["x"], current_map_y=parents[0]["y"])
    state["screen_info"] = dict(screen_state=5, complete=True)
    state["rng"]["treasure"] = copy.deepcopy(row["before"]["_rng"]["treasure"])
    run = native.LightspeedRunState()
    run.load_state(state)
    if x not in {action["bits"] for action in run.snapshot()["legal_actions"]}:
        raise ValueError("stock chest is not a native-legal map entry")
    run.step(x)
    actual = run.snapshot()
    expected = initial["screen_info"]
    fields = ("screen_state", "complete", "chest_size", "have_gold", "relic_tier", "continuation")
    return dict(status="CHECKED_CHEST_METADATA_AND_TREASURE_STREAM_ONLY", stock_node=[x, y],
                differences=differences({key: expected[key] for key in fields},
                                        {key: actual["screen_info"][key] for key in fields}),
                treasure_rng_differences=differences(stock["_rng"]["treasure"], actual["rng"]["treasure"]),
                all_rng_differences=differences(stock["_rng"], actual["rng"]),
                native_map_parent=parents[0], native_before=state, native_after=actual)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--capture", type=Path, required=True)
    parser.add_argument("--oracle-build", type=Path, required=True)
    parser.add_argument("--manifest", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    if args.output.exists():
        raise FileExistsError("refuse to overwrite evidence")
    os.environ["CUDA_VISIBLE_DEVICES"] = "-1"
    scenes = ["sapphire-take-key", "sapphire-take-relic", "sapphire-already-held"]
    source = audit(args.capture, args.oracle_build, args.manifest, scenes)
    data = json.loads(args.capture.read_text(encoding="utf-8"))
    from sls.backends.simulator import SimulatorBackend, native
    from sls.rl.training_contract import native_source_digest

    if native.NATIVE_SOURCE_SHA256 != native_source_digest():
        raise ValueError("stale native binary")
    constants = ROOT / "native/simulator/include/constants"
    cards = enum_ids(constants / "Cards.h", "enum class CardId : std::uint16_t {")
    relics = enum_ids(constants / "Relics.h", "enum class RelicId : std::uint8_t {")
    names = {value: key for key, value in relics.items()}
    encounters = enum_ids(constants / "MonsterEncounters.h", "enum class MonsterEncounter : std::int8_t {")
    results = []
    for row in data["runs"]:
        if row["scene"]["id"] not in scenes:
            continue
        run = prepare(row, native, cards, relics, encounters)
        backend = SimulatorBackend(profile=IRONCLAD_A20_HEART)
        initial = run.snapshot()
        decision = backend._adapt(initial)
        original = adapt_original(row["boundaries"][0]).decision
        result = dict(scene=row["scene"]["id"], seed=row["seed"],
                      initial_observation_differences=differences(asdict(original.observation), asdict(decision.observation)),
                      initial_resource_differences=differences(stock_resources(row["boundaries"][0]), native_resources(initial)),
                      chest_creation=chest_creation(row, initial, native), boundaries=[])
        results.append(result)
        for index, action in enumerate(row["actions"]):
            before = run.snapshot()
            decision = backend._adapt(before)
            original_before = adapt_original(row["boundaries"][index]).decision
            matches = [candidate for candidate in decision.actions if
                       (candidate.kind.value == action["requested"] if action["requested"] != "TAKE_RELIC" else
                        candidate.kind == ActionKind.TAKE_REWARD and
                        (candidate.reward_id or "").startswith("reward-relic:"))]
            if len(matches) != 1:
                raise ValueError("recorded chest action is not uniquely native-legal")
            bits = backend._candidate_bits[matches[0].candidate_id]
            clone = native.LightspeedRunState()
            clone.load_state(before)
            run.step(bits)
            clone.step(bits)
            after = run.snapshot()
            restored = native.LightspeedRunState()
            restored.load_state(after)
            stock = row["boundaries"][index + 1]
            original_after = adapt_original(stock).decision
            native_after = backend._adapt(after)
            pools = stock["_stock_direct"]["ordered_relic_pools"]
            result["boundaries"].append(dict(
                action=action, native_bits=bits,
                legal_action_differences=differences([asdict(a) for a in original_before.actions],
                                                     [asdict(a) for a in decision.actions]),
                observation_differences=differences(asdict(original_after.observation), asdict(native_after.observation)),
                resource_differences=differences(stock_resources(stock), native_resources(after)),
                raw_resource_differences=differences(stock_resources(stock, canonical_counters=False),
                                                     native_resources(after, canonical_counters=False)),
                reward_differences=differences(stock_rewards(stock), native_rewards(after, names)),
                rng_differences=differences(stock["_rng"], after["rng"]),
                pool_differences=differences({name: [normalize_content_id(value) for value in values]
                                              for name, values in pools.items()},
                                             {name: [names[value] for value in after["ordered_pools"][name]]
                                              for name in pools}),
                restored_suffix_equal=clone.snapshot() == after, post_restore_equal=restored.snapshot() == after,
                native_before=before, native_after=after))
    report = dict(schema="sls-blue-key-replay-v1", stock_capture_integrity=source,
                  scope="CHEST_OPEN_REWARDS_AND_LINKED_SELECTION_FROM_SAME_INITIAL_CHEST_AND_POOLS",
                  chest_room_creation="SEE_PER_CASE_METADATA_AND_TREASURE_STREAM_SCOPE", natural_trajectory=False,
                  native_source_sha256=native.NATIVE_SOURCE_SHA256,
                  native_binary_sha256=hashlib.sha256(Path(native.__file__).read_bytes()).hexdigest(), runs=results)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    with args.output.open("x", encoding="utf-8") as stream:
        json.dump(report, stream, indent=2)
        stream.write("\n")
    boundaries = [boundary for result in results for boundary in result["boundaries"]]
    print(json.dumps(dict(cases=len(results), boundaries=len(boundaries),
                          resource_matches=sum(not row["resource_differences"] for row in boundaries),
                          reward_matches=sum(not row["reward_differences"] for row in boundaries),
                          rng_matches=sum(not row["rng_differences"] for row in boundaries),
                          pool_matches=sum(not row["pool_differences"] for row in boundaries),
                          observation_matches=sum(not row["observation_differences"] for row in boundaries),
                          restore_matches=sum(row["restored_suffix_equal"] and row["post_restore_equal"] for row in boundaries))))


if __name__ == "__main__":
    main()
