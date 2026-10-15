"""Source-derived revival boundaries; these are not live-game parity captures."""

import pytest

from sls.backends.simulator import native


def _battle(max_hp, relics, potions=()):
    battle = native.LightspeedBattle()
    battle.reset(0, "CULTIST", 20, relics=list(relics), replace_relics=True)
    battle.set_player_health(1, max_hp)
    battle.set_card_piles(["Bloodletting", "Bloodletting"], [], [], [])
    battle.set_potions(list(potions))
    return battle


@pytest.mark.parametrize("max_hp,expected", [(1, 1), (2, 1), (7, 3), (79, 39), (80, 40)])
def test_lizard_tail_revives_at_least_one_hp_and_spent_state_survives_restore(max_hp, expected):
    # Stock LizardTail.onTrigger: max(1, player.maxHealth / 2), then counter=-2.
    battle = _battle(max_hp, ["LIZARD_TAIL"])
    battle.step("play", card_index=1)
    state = battle.snapshot()
    assert state["game_state"]["current_hp"] == expected
    assert state["outcome"] == "UNDECIDED"
    assert state["game_state"]["relics"][0]["counter"] == -2
    restored = native.LightspeedBattle()
    restored.load_checkpoint({"game_state": state["game_state"], "rng": state["_rng"]})
    assert restored.snapshot() == state
    for current in (battle, restored):
        current.set_player_health(1, max_hp)
        current.step("play", card_index=1)
        assert current.snapshot()["outcome"] == "PLAYER_LOSS"
        assert current.snapshot()["_legal_actions"] == []
    assert restored.snapshot() == battle.snapshot()


def test_fairy_precedes_lizard_tail_then_tail_is_available_on_next_death():
    # AbstractPlayer.damage checks FairyPotion before the unused Lizard Tail.
    battle = _battle(80, ["LIZARD_TAIL"], ["FAIRY_POTION"])
    battle.step("play", card_index=1)
    state = battle.snapshot()
    assert state["game_state"]["current_hp"] == 24
    assert state["game_state"]["relics"][0]["counter"] == -1
    assert all(p["id"] == "Potion Slot" for p in state["game_state"]["potions"])
    battle.set_player_health(1, 80)
    battle.step("play", card_index=1)
    assert battle.snapshot()["game_state"]["current_hp"] == 40
    assert battle.snapshot()["game_state"]["relics"][0]["counter"] == -2


def test_mark_of_the_bloom_blocks_both_revival_sources_without_consuming_them():
    battle = _battle(80, ["LIZARD_TAIL", "MARK_OF_THE_BLOOM"], ["FAIRY_POTION"])
    battle.step("play", card_index=1)
    state = battle.snapshot()
    assert state["outcome"] == "PLAYER_LOSS"
    assert state["_legal_actions"] == []
    assert state["game_state"]["relics"][0]["counter"] == -1
    assert [p["id"] for p in state["game_state"]["potions"] if p["id"] != "Potion Slot"] == [
        "Fairy Potion",
    ]


@pytest.mark.parametrize("max_hp,relics,expected", [
    (1, [], 1),
    (7, [], 2),
    (80, ["SACRED_BARK"], 48),
    (80, ["MAGIC_FLOWER"], 36),
    (80, ["SACRED_BARK", "MAGIC_FLOWER"], 72),
])
def test_fairy_minimum_and_healing_modifiers(max_hp, relics, expected):
    # FairyPotion.use truncates its percentage then clamps to 1; MagicFlower
    # applies MathUtils.round(healAmount * 1.5f) while the room is in combat.
    battle = _battle(max_hp, relics, ["FAIRY_POTION"])
    battle.step("play", card_index=1)
    state = battle.snapshot()
    assert state["game_state"]["current_hp"] == expected
    assert state["outcome"] == "UNDECIDED"


@pytest.mark.parametrize("artifact", [False, True])
def test_red_skull_healing_removes_strength_without_consuming_artifact(artifact):
    # RedSkull.onNotBloodied creates StrengthPower(-3), whose type is BUFF;
    # ApplyPowerAction only consumes Artifact for DEBUFF-typed powers.
    relics = ["RED_SKULL"] + (["CLOCKWORK_SOUVENIR"] if artifact else [])
    battle = native.LightspeedBattle()
    battle.reset(0, "CULTIST", 20, relics=relics, replace_relics=True)
    battle.set_player_health(41, 80)
    battle.set_card_piles(["Bloodletting"], [], [], [])
    battle.set_potions(["BLOOD_POTION"])
    battle.step("play", card_index=1)
    state = battle.snapshot()
    player = state["game_state"]["combat_state"]["player"]
    assert player["current_hp"] == 38
    assert {p["id"]: p["amount"] for p in player["powers"]}["STRENGTH"] == 3
    restored = native.LightspeedBattle()
    restored.load_checkpoint({"game_state": state["game_state"], "rng": state["_rng"]})
    for current in (battle, restored):
        current.step("potion", potion_index=0)
        player = current.snapshot()["game_state"]["combat_state"]["player"]
        powers = {p["id"]: p["amount"] for p in player["powers"]}
        assert player["current_hp"] == 54
        assert powers.get("STRENGTH", 0) == 0
        assert powers.get("ARTIFACT", 0) == int(artifact)
    assert restored.snapshot() == battle.snapshot()
