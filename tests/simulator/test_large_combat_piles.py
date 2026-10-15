"""Dynamic production piles and selection helpers must not inherit a 64-slot cap."""

import pytest

from sls.backends.simulator import native


def _combat(battle):
    return battle.snapshot()["game_state"]["combat_state"]


def _count(battle):
    combat = _combat(battle)
    return sum(len(combat[zone]) for zone in ("hand", "draw_pile", "discard_pile", "exhaust_pile")) + sum(
        card is not None for card in combat["_internal"]["stasis_cards"])


def _restore(battle):
    state = battle.snapshot()
    restored = native.LightspeedBattle()
    restored.load_checkpoint({"game_state": state["game_state"], "rng": state["_rng"]})
    assert restored.snapshot() == state
    return restored


def test_violence_selects_from_over_64_attacks_without_losing_or_duplicating_cards():
    battle = native.LightspeedBattle()
    battle.reset(0, "CULTIST", ascension=20)
    battle.set_card_piles(["Violence"], ["Anger"] * 120, [], [])
    restored = _restore(battle)
    for current in (battle, restored):
        current.step("play", card_index=1)
        assert len(_combat(current)["hand"]) == 3
        assert len(_combat(current)["draw_pile"]) == 117
        assert _count(current) == 121
    assert battle.snapshot() == restored.snapshot()


def test_stasis_large_same_rarity_pool_preserves_cards_and_restored_continuation():
    battle = native.LightspeedBattle()
    battle.reset(0, "AUTOMATON", ascension=20)
    battle.set_player_health(20000, 20000)
    battle.set_card_piles(["Defend_R"], ["Feed"] * 120, [], [])
    for _ in range(4):
        restored = _restore(battle)
        battle.step("end_turn")
        restored.step("end_turn")
        assert restored.snapshot() == battle.snapshot()
        assert _count(battle) == 121
    assert sum(card is not None for card in _combat(battle)["_internal"]["stasis_cards"]) == 2


def test_probe_rejects_unrepresentable_card_identity_before_replacing_piles():
    battle = native.LightspeedBattle()
    battle.reset(0, "CULTIST", ascension=20)
    before = battle.snapshot()
    with pytest.raises(ValueError, match="identity range"):
        battle.set_card_piles([], ["Anger"] * 32768, [], [])
    assert battle.snapshot() == before
