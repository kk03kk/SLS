"""Public availability and native acquisition semantics for limited-use relics."""

import json
from dataclasses import replace

import pytest
import torch

from sls.backends.original.adapter import adapt_original
from sls.backends.simulator import SimulatorBackend
from sls.content import load_content_registry
from sls.content.normalize import normalize_relic_counter
from sls.contracts import ActionKind, Decision
from sls.model import encode_decision

native = pytest.importorskip("sls.backends.simulator.native", exc_type=ImportError)


def _set_lethal_hp(battle):
    checkpoint = battle.snapshot()
    checkpoint["game_state"]["combat_state"]["player"]["current_hp"] = 3
    checkpoint["rng"] = checkpoint.pop("_rng")
    battle.load_checkpoint(checkpoint)


def test_newly_acquired_lizard_tail_revives_once_and_stays_spent_after_restore():
    battle = native.LightspeedBattle()
    battle.reset(0, "CULTIST", relics=["Lizard Tail"], replace_relics=True)
    battle.set_card_piles(["Bloodletting", "Bloodletting"], [], [], [])
    _set_lethal_hp(battle)
    assert battle.snapshot()["game_state"]["relics"][0]["counter"] == -1
    battle.step("play", card_index=1)
    snapshot = battle.snapshot()
    assert snapshot["game_state"]["combat_state"]["player"]["current_hp"] == 40
    assert snapshot["game_state"]["relics"][0]["counter"] == -2
    _set_lethal_hp(battle)
    battle.step("play", card_index=1)
    assert battle.snapshot()["game_state"]["combat_state"]["player"]["current_hp"] == 0


@pytest.mark.parametrize("relic", ["Lizard Tail", "Maw Bank", "Ancient Tea Set"])
def test_relic_sentinels_change_encoded_observation(relic):
    battle = native.LightspeedBattle()
    battle.reset(0, "CULTIST")
    payload = battle.snapshot()
    payload["game_state"]["relics"] = [{"id": relic, "counter": -1}]
    first = adapt_original(payload).decision
    payload["game_state"]["relics"][0]["counter"] = -2
    second = adapt_original(payload).decision
    assert dict(first.observation.relics[0].properties)["counter"] == 0
    assert dict(second.observation.relics[0].properties)["counter"] == -2
    # Hold legal actions constant: the difference must survive observation encoding.
    second = Decision(replace(first.observation, relics=second.observation.relics), first.actions)
    assert not torch.equal(encode_decision(first).entity_numeric, encode_decision(second).entity_numeric)


def test_only_neutral_relic_counter_is_collapsed():
    assert [normalize_relic_counter(x) for x in (-2, -1, 0, 1, 3)] == [-2, 0, 0, 1, 3]


def _map_with_tea_set(data=0):
    backend = SimulatorBackend()
    decision = backend.reset(0)
    while decision.observation.screen.value != "MAP":
        decision = backend.step(decision.actions[0]).decision
    checkpoint = backend.checkpoint()
    relic_id = next(
        r["ordinal"] for r in load_content_registry().categories["relics"]
        if r["id"] == "ANCIENT_TEA_SET"
    )
    checkpoint["player_state"]["relics"].append({"id": relic_id, "data": data})
    checkpoint["replay_actions"] = []
    checkpoint["replay_required"] = False
    return backend, checkpoint


def _tea_counter(decision):
    return dict(next(
        r for r in decision.observation.relics if r.content_id == "ANCIENT_TEA_SET"
    ).properties)["counter"]


@pytest.mark.parametrize("previous_room", [0, 1, 2])
def test_uncharged_tea_set_does_not_infer_charge_from_previous_room(previous_room):
    backend, checkpoint = _map_with_tea_set()
    checkpoint["progress_state"]["current_room"] = previous_room
    decision = backend.load_checkpoint(checkpoint)
    decision = backend.step(decision.actions[0]).decision
    assert decision.observation.screen.value == "COMBAT"
    assert decision.observation.player.energy == 3


def test_tea_set_charge_survives_event_and_restore_and_is_consumed_once():
    backend, checkpoint = _map_with_tea_set()
    # Place a settled run at an actual parent of the seed-0 campfire. Subsequent
    # room entry, campfire, event, combat and escape use real FullRun actions.
    parent = next(n for n in checkpoint["public_map"] if "map:2:5" in n["outgoing_node_ids"])
    checkpoint["progress_state"].update(current_map_x=parent["x"], current_map_y=parent["y"])
    checkpoint["run_state"]["floor"] = parent["y"] + 1
    checkpoint["player_state"]["potions"] = [37, 1, 1]  # Smoke Bomb, empty, empty.
    checkpoint["player_state"]["potion_count"] = 1
    decision = backend.load_checkpoint(checkpoint)
    decision = backend.step(next(a for a in decision.actions if a.node_id == "map:2:5")).decision
    assert decision.observation.screen.value == "REST"
    assert _tea_counter(decision) == -2
    restored = SimulatorBackend()
    assert restored.load_checkpoint(json.loads(json.dumps(backend.checkpoint()))) == decision
    backend = restored
    saw_event = False
    for _ in range(12):
        if decision.observation.screen.value == "COMBAT":
            break
        saw_event |= decision.observation.screen.value == "EVENT"
        assert _tea_counter(decision) == -2
        decision = backend.step(next(
            a for a in decision.actions if a.kind not in {ActionKind.USE_POTION, ActionKind.DISCARD_POTION}
        )).decision
    assert saw_event and decision.observation.screen.value == "COMBAT"
    assert decision.observation.player.energy == 5
    assert _tea_counter(decision) == 0
    decision = backend.step(next(a for a in decision.actions if a.kind is ActionKind.USE_POTION)).decision
    assert decision.observation.screen.value == "MAP" and _tea_counter(decision) == 0
    for _ in range(16):
        if decision.observation.screen.value == "COMBAT":
            break
        assert decision.observation.screen.value != "REST"
        monster_nodes = {n.node_id for n in decision.observation.map_nodes if n.visible_room_type == "MONSTER"}
        action = next((a for a in decision.actions if a.node_id in monster_nodes), decision.actions[0])
        decision = backend.step(action).decision
    assert decision.observation.screen.value == "COMBAT"
    assert decision.observation.player.energy == 3
