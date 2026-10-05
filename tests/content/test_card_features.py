from dataclasses import replace

import pytest
import torch

from sls.backends.original.adapter import _cards
from sls.backends.original.adapter import _screen_entities as original_screen_entities
from sls.backends.simulator.environment import _combat_cards
from sls.backends.simulator.environment import (
    _screen_entities as native_screen_entities,
)
from sls.contracts import (
    Action,
    ActionKind,
    Decision,
    Observation,
    Player,
    RunContext,
    ScreenType,
)
from sls.model import encode_decision


def card(damage=15):
    return {"id": "RITUAL_DAGGER", "content_id": "RITUAL_DAGGER", "upgrades": 0,
            "base_cost": 1, "cost": 1, "is_playable": True, "base_damage": damage,
            "free_to_play_once": False, "retain": False, "self_retain": False}


def test_dynamic_damage_and_flags_agree_between_adapters():
    for raw in (card(), card(45), {**card(), "free_to_play_once": True, "retain": True}):
        assert _cards([raw], "HAND") == _combat_cards([raw], "HAND")


def test_dynamic_damage_changes_model_input():
    observation = Observation(
        Player("IRONCLAD", 70, 80, 0, 3, 3),
        RunContext(0, 1, 1, 99, False, False, False), ScreenType.COMBAT,
        hand=_combat_cards([card()], "HAND"),
    )
    actions = (Action(ActionKind.END_TURN),)
    first = encode_decision(Decision(observation, actions))
    second = encode_decision(Decision(replace(
        observation, hand=_combat_cards([card(45)], "HAND"),
    ), actions))
    assert not torch.equal(first.entity_numeric, second.entity_numeric)


def test_hidden_draw_order_does_not_leak_through_dynamic_card_ties():
    cards = [card(45), card(15)]
    assert _combat_cards(cards, "DRAW") == _combat_cards(list(reversed(cards)), "DRAW")
    assert _cards(cards, "DRAW") == _cards(list(reversed(cards)), "DRAW")


def test_rampage_never_interprets_stock_misc_as_native_damage_growth():
    raw = {**card(), "id": "RAMPAGE", "content_id": "RAMPAGE", "special_data": 0,
           "base_damage": 23}
    original = _cards([raw], "HAND")[0]
    simulator = _combat_cards([{**raw, "special_data": 15}], "HAND")[0]
    assert original == simulator
    assert dict(original.properties)["base_damage"] == 23
    del raw["base_damage"]
    with pytest.raises(ValueError, match="requires public base_damage"):
        _cards([raw], "HAND")


@pytest.mark.parametrize("screen", ["HAND_SELECT", "GRID", "CARD_REWARD", "SHOP"])
def test_action_referenced_card_keeps_visible_mutable_state(screen):
    raw_card = {**card(45), "upgrades": 1, "cost": 0, "price": 99,
                "free_to_play_once": True, "retain": True, "deck_index": 0,
                "instance_id": "select-card:0"}
    game = {"screen_type": screen}
    combat = {"hand": [raw_card]} if screen == "HAND_SELECT" else {}
    state = {"cards": [raw_card]}
    canonical_screen = {
        "HAND_SELECT": ScreenType.COMBAT, "GRID": ScreenType.CARD_REWARD,
        "CARD_REWARD": ScreenType.COMBAT_REWARD, "SHOP": ScreenType.SHOP,
    }[screen]
    public_screen = {}
    public_combat = {}
    if screen == "HAND_SELECT":
        public_combat = {"choice": {"task": "DUAL_WIELD", "source": "HAND",
                                     "options": [raw_card]}}
        group, screen_state = "choice", 9
    elif screen == "GRID":
        public_screen = {"card_options": [raw_card]}
        group, screen_state = "reward", 4
    elif screen == "CARD_REWARD":
        state = {"rewards": [{"reward_type": "CARD", "cards": [raw_card]}]}
        public_screen = {"card_rewards": [[raw_card]], "gold": [],
                         "emerald_key": False, "sapphire_key": False}
        group, screen_state = "reward", 2
    else:
        public_screen = {"cards": [raw_card], "prices": [99], "relics": [], "potions": []}
        group, screen_state = "shop", 8
    original = original_screen_entities({}, game, combat, state, canonical_screen)[group]
    native = native_screen_entities({
        "public_run": {"outcome": 1, "screen_state": screen_state, "current_event_id": "INVALID"},
        "public_screen": public_screen, "public_combat": public_combat, "legal_actions": [],
    })[group]
    assert original == native
    props = dict(original[0].properties)
    assert props["base_damage"] == 45
    assert props["base_cost"] == 1 and props["current_cost"] == 0
    assert props["upgrades"] == 1
    assert props["free_to_play_once"] is True and props["retain"] is True


@pytest.mark.parametrize("source,expected", [
    ("DRAW_PILE", "DRAW"), ("EXHAUST_PILE", "EXHAUST"),
    ("DISCARD_PILE", "DISCARD"), ("DRAW", "DRAW"), ("EXHAUST", "EXHAUST"),
])
def test_original_choice_preserves_the_public_pile_source(source, expected):
    result = original_screen_entities(
        {}, {"screen_type": "GRID", "choice_list": ["Ritual Dagger"]},
        {"card_select": {"source": source}}, {"cards": [card()]}, ScreenType.COMBAT,
    )
    assert dict(result["choice"][0].properties)["source"] == expected


@pytest.mark.parametrize("played,action,expected", [
    ("Exhume", "", "EXHAUST"), ("Secret Technique", "", "DRAW"),
    ("Secret Weapon", "", "DRAW"), ("Headbutt", "", "DISCARD"),
    ("", "com.megacrit.cardcrawl.actions.common.BetterDiscardPileToHandAction", "DISCARD"),
    ("", "com.megacrit.cardcrawl.actions.unique.ExhumeAction", "EXHAUST"),
])
def test_stock_grid_source_comes_from_the_visible_operation(played, action, expected):
    result = original_screen_entities(
        {}, {"screen_type": "GRID", "choice_list": ["Ritual Dagger"], "current_action": action},
        {"card_in_play": {"id": played}}, {"cards": [card()]}, ScreenType.COMBAT,
    )
    assert dict(result["choice"][0].properties)["source"] == expected
