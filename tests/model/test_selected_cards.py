"""Public selected-card state must be available without recurrent history."""

from dataclasses import replace

import pytest
import torch

from sls.backends.original.adapter import _selected_cards
from sls.backends.simulator import SimulatorBackend
from sls.contracts import Decision
from sls.model import encode_decision


@pytest.mark.parametrize("screen,combat,source", [
    ("HAND_SELECT", {}, "HAND"),
    ("GRID", {"card_select": {"source": "DISCARD_PILE"}}, "DISCARD"),
    ("GRID", {"card_in_play": {"id": "Exhume"}}, "EXHAUST"),
    ("GRID", {}, "MASTER_DECK"),
])
def test_selected_cards_keep_order_and_public_properties_without_remaining_choices(screen, combat, source):
    cards = [{"id": "Ritual Dagger", "upgrades": 1, "base_damage": 45,
              "base_cost": 1, "cost_for_turn": 0, "retain": True},
             {"id": "Bash", "upgrades": 0, "base_cost": 2, "cost_for_turn": 2}]
    state = {"selected" if screen == "HAND_SELECT" else "selected_cards": cards}
    entities = _selected_cards({"screen_type": screen, "combat_state": combat}, state, ())
    assert [c.content_id for c in entities] == ["RITUAL_DAGGER", "BASH"]
    for i, entity in enumerate(entities):
        props = dict(entity.properties)
        assert props["source"] == source and props["selected"] and props["selected_order"] == i
    props = dict(entities[0].properties)
    assert props["base_damage"] == 45 and props["current_cost"] == 0 and props["retain"]


def test_selected_card_identity_and_order_change_model_input():
    decision = SimulatorBackend().reset(0)
    cards = [{"id": "Bash", "upgrades": 0}, {"id": "Strike_R", "upgrades": 0}]
    entities = _selected_cards({"screen_type": "HAND_SELECT"}, {"selected": cards}, ())
    def encoded(selected):
        return encode_decision(Decision(replace(decision.observation, selected_cards=selected), decision.actions))
    baseline = encoded(entities)
    changed_order = tuple(replace(c, properties=tuple(sorted({
        **dict(c.properties), "selected_order": 1 - i,
    }.items()))) for i, c in enumerate(entities))
    assert not torch.equal(baseline.entity_numeric, encoded(changed_order).entity_numeric)
    changed_card = (replace(entities[0], content_id="ANGER"), entities[1])
    assert not torch.equal(baseline.entity_content, encoded(changed_card).entity_content)
