"""Stock upgradeBaseCost semantics through real native card actions."""

import pytest

native = pytest.importorskip("sls.backends.simulator.native", exc_type=ImportError)


@pytest.mark.parametrize("hits", range(5))
def test_blood_for_blood_upgrade_preserves_damage_discount(hits):
    battle = native.LightspeedBattle()
    battle.reset(123, "CULTIST")
    battle.set_card_piles(
        ["Bloodletting"] * hits + ["Armaments+", "Blood for Blood"], [], [], [],
    )
    for _ in range(hits):
        battle.step("play", card_index=1, target_index=0)
    before = battle.snapshot()["game_state"]["combat_state"]["hand"][-1]
    assert before["cost"] == 4 - hits
    battle.step("play", card_index=1, target_index=0)
    after = battle.snapshot()["game_state"]["combat_state"]["hand"][-1]
    assert after["upgrades"] == 1
    assert after["base_cost"] == after["cost"] == max(0, 3 - hits)


@pytest.mark.parametrize("upgrade_card", ("Armaments+", "Apotheosis+"))
def test_upgrade_keeps_a_madness_card_free_for_this_turn(upgrade_card):
    battle = native.LightspeedBattle()
    battle.reset(2, "CULTIST")
    battle.set_card_piles(["Madness", "Entrench", upgrade_card], [], [], [])
    battle.step("play", card_index=1, target_index=0)
    before = battle.snapshot()["game_state"]["combat_state"]["hand"][0]
    assert before["id"] == "ENTRENCH"
    assert before["cost"] == before["base_cost"] == 0
    battle.step("play", card_index=2, target_index=0)
    after = battle.snapshot()["game_state"]["combat_state"]["hand"][0]
    assert after["upgrades"] == 1
    # Stock sets the upgraded base cost to 1, but leaves an already-zero
    # costForTurn unchanged (AbstractCard.upgradeBaseCost).
    assert after["base_cost"] == 1
    assert after["cost"] == 0
