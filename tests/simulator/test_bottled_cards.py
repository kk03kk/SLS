"""Bottled-card identity survives public projection and combat state changes."""

import json
from dataclasses import replace

import pytest
import torch

from sls.backends.original.adapter import _cards
from sls.backends.simulator import SimulatorBackend
from sls.backends.simulator.environment import _combat_cards
from sls.content import load_content_registry
from sls.contracts import ActionKind, Decision
from sls.model import encode_decision


def _ordinal(category, name):
    return next(item["ordinal"] for item in load_content_registry().categories[category] if item["id"] == name)


@pytest.mark.parametrize("bottle,flag,index", [
    ("BOTTLED_FLAME", "bottled_flame", 0),
    ("BOTTLED_LIGHTNING", "bottled_lightning", 1),
    ("BOTTLED_TORNADO", "bottled_tornado", 2),
])
def test_bottle_card_projects_into_deck_combat_and_checkpoint(bottle, flag, index):
    backend = SimulatorBackend()
    decision = backend.reset(0)
    while decision.observation.screen.value != "MAP":
        decision = backend.step(decision.actions[0]).decision
    checkpoint = backend.checkpoint()
    player = checkpoint["player_state"]
    player["relics"].append({"id": _ordinal("relics", bottle), "data": 0})
    card_name = ["STRIKE_RED", "DEFEND_RED", "INFLAME"][index]
    if index == 2:
        player["deck"].append({"id": _ordinal("cards", card_name), "upgraded": False, "misc": 0})
    card_index = next(i for i, c in enumerate(player["deck"]) if c["id"] == _ordinal("cards", card_name))
    bottles = list(player["bottle_indices"])
    bottles[index] = card_index
    player["bottle_indices"] = tuple(bottles)
    checkpoint["replay_actions"] = []
    checkpoint["replay_required"] = False
    decision = backend.load_checkpoint(checkpoint)
    assert [c.card_id for c in decision.observation.deck if dict(c.properties)[flag]] == [card_name]
    decision = backend.step(decision.actions[0]).decision
    bottled = next(c for c in decision.observation.hand if dict(c.properties)[flag])
    assert bottled.card_id == card_name
    restored = SimulatorBackend()
    assert restored.load_checkpoint(json.loads(json.dumps(backend.checkpoint()))) == decision
    action = next(a for a in decision.actions if a.kind is ActionKind.PLAY_CARD and a.subject_id == bottled.instance_id)
    expected = backend.step(action).decision
    assert restored.step(action).decision == expected
    if index != 2:  # Powers leave combat rather than going to the discard pile.
        assert any(dict(c.properties)[flag] for c in expected.observation.discard_pile)


def test_bottle_marker_changes_encoding_without_revealing_internal_card_id():
    backend = SimulatorBackend()
    decision = backend.reset(0)
    card = decision.observation.deck[0]
    properties = dict(card.properties)
    properties["bottled_flame"] = True
    changed = replace(card, properties=tuple(sorted(properties.items())))
    marked = Decision(replace(decision.observation, deck=(changed, *decision.observation.deck[1:])), decision.actions)
    assert not torch.equal(encode_decision(decision).entity_numeric, encode_decision(marked).entity_numeric)
    raw = {"id": "STRIKE_RED", "content_id": "STRIKE_RED", "upgrades": 0,
           "base_cost": 1, "cost": 1, "is_playable": True, "bottled_flame": True,
           "bottled_lightning": False, "bottled_tornado": False}
    assert _cards([raw], "HAND") == _combat_cards([raw], "HAND")
