from __future__ import annotations

import pytest

from sls.content.scope import load_ironclad_a0_scope


native = pytest.importorskip("sls.backends.simulator.native", exc_type=ImportError)


def _play_first_card(battle: object) -> None:
    action = next(
        item for item in battle.snapshot()["_legal_actions"]
        if item["kind"] == "play" and item["card_index"] == 1
    )
    battle.step("play", card_index=1, target_index=0)


def test_every_scoped_playable_card_enters_the_native_action_pipeline() -> None:
    scope = load_ironclad_a0_scope()
    intentionally_unplayable = set(scope["cards"]["curses"]) | set(scope["cards"]["statuses"])
    tailored = {"BLOOD_FOR_BLOOD", "CLASH", "SECRET_TECHNIQUE"}
    for card_id in sorted(set(scope["cards"]["ids"]) - intentionally_unplayable - tailored):
        battle = native.LightspeedBattle()
        battle.reset(123, "CULTIST", 0)
        battle.set_card_piles(
            [card_id, "Strike_R", "Defend_R"],
            ["Strike_R", "Defend_R"], ["Strike_R"], ["Defend_R"],
        )
        _play_first_card(battle)

    blood = native.LightspeedBattle()
    blood.reset(123, "CULTIST", 0)
    blood.set_card_piles(["Blood for Blood+"], [], [], [])
    _play_first_card(blood)

    clash = native.LightspeedBattle()
    clash.reset(123, "CULTIST", 0)
    clash.set_card_piles(["Clash", "Strike_R"], [], [], [])
    _play_first_card(clash)

    technique = native.LightspeedBattle()
    technique.reset(123, "CULTIST", 0)
    technique.set_card_piles(["Secret Technique"], ["Defend_R"], [], [])
    _play_first_card(technique)


def test_every_scoped_status_and_curse_has_the_stock_playability_path() -> None:
    scope = load_ironclad_a0_scope()
    for card_id in scope["cards"]["statuses"]:
        battle = native.LightspeedBattle()
        battle.reset(123, "CULTIST", 0, relics=["Medical Kit"], replace_relics=True)
        battle.set_card_piles([card_id], [], [], [])
        _play_first_card(battle)
    for card_id in scope["cards"]["curses"]:
        battle = native.LightspeedBattle()
        battle.reset(123, "CULTIST", 0, relics=["Blue Candle"], replace_relics=True)
        battle.set_card_piles([card_id], [], [], [])
        _play_first_card(battle)


def test_every_scoped_potion_has_an_execution_or_automatic_trigger_path() -> None:
    scope = load_ironclad_a0_scope()
    for potion_id in scope["potions"]["ids"]:
        battle = native.LightspeedBattle()
        battle.reset(123, "CULTIST", 0)
        battle.set_card_piles(
            ["Strike_R", "Defend_R"], ["Strike_R", "Defend_R"],
            ["Strike_R"], ["Defend_R"],
        )
        battle.set_potions([potion_id])
        uses = [
            item for item in battle.snapshot()["_legal_actions"]
            if item["kind"] == "potion"
        ]
        if potion_id == "FAIRY_POTION":
            assert not uses
            continue
        assert uses, potion_id
        battle.step("potion", potion_index=0, target_index=0)


def test_every_scoped_relic_can_initialize_and_enter_combat() -> None:
    scope = load_ironclad_a0_scope()
    for relic_id in scope["relics"]["ids"]:
        battle = native.LightspeedBattle()
        battle.reset(123, "CULTIST", 0, relics=[relic_id], replace_relics=True)
        for _ in range(3):
            actions = battle.snapshot()["_legal_actions"]
            action = next((item for item in actions if item["kind"] == "play"), None)
            if action is not None:
                break
            proceed = next((item for item in actions if item["kind"] == "proceed"), None)
            if proceed is not None:
                battle.step("proceed")
            else:
                battle.step("choose", choice_index=0)
        assert action is not None, relic_id
        battle.step(
            "play", card_index=action["card_index"], target_index=0,
        )


def test_every_act1_encounter_initializes_with_a_legal_boundary() -> None:
    scope = load_ironclad_a0_scope()
    for encounter_id in scope["encounters"]["act1"]:
        battle = native.LightspeedBattle()
        battle.reset(123, encounter_id, 0)
        assert battle.snapshot()["_legal_actions"], encounter_id


def test_every_act1_encounter_completes_a_deterministic_turn_lifecycle() -> None:
    scope = load_ironclad_a0_scope()
    expected = {
        "BLUE_SLAVER": (31, "PLAYER_VICTORY"),
        "CULTIST": (25, "PLAYER_VICTORY"),
        "EXORDIUM_THUGS": (37, "PLAYER_VICTORY"),
        "EXORDIUM_WILDLIFE": (34, "PLAYER_VICTORY"),
        "GREMLIN_GANG": (28, "PLAYER_LOSS"),
        "GREMLIN_NOB": (24, "PLAYER_LOSS"),
        "HEXAGHOST": (44, "PLAYER_LOSS"),
        "JAW_WORM": (39, "PLAYER_VICTORY"),
        "LAGAVULIN": (56, "PLAYER_LOSS"),
        "LARGE_SLIME": (58, "PLAYER_VICTORY"),
        "LOOTER": (20, "PLAYER_VICTORY"),
        "LOTS_OF_SLIMES": (39, "PLAYER_VICTORY"),
        "RED_SLAVER": (27, "PLAYER_VICTORY"),
        "SLIME_BOSS": (36, "PLAYER_LOSS"),
        "SMALL_SLIMES": (26, "PLAYER_VICTORY"),
        "THE_GUARDIAN": (36, "PLAYER_LOSS"),
        "THREE_LOUSE": (34, "PLAYER_VICTORY"),
        "THREE_SENTRIES": (59, "PLAYER_VICTORY"),
        "TWO_FUNGI_BEASTS": (31, "PLAYER_VICTORY"),
        "TWO_LOUSE": (21, "PLAYER_VICTORY"),
    }
    assert set(expected) == set(scope["encounters"]["act1"])
    for encounter_id in sorted(expected):
        battle = native.LightspeedBattle()
        battle.reset(
            123, encounter_id, 0,
            deck=["Defend_R"] * 5 + ["Strike_R"] * 5,
        )
        for step in range(120):
            snapshot = battle.snapshot()
            actions = snapshot["_legal_actions"]
            if not actions:
                break
            action = (
                next((item for item in actions if item["kind"] == "play"), None)
                or next((item for item in actions if item["kind"] == "end"), None)
                or actions[0]
            )
            arguments = {
                key: action[key]
                for key in ("card_index", "potion_index", "target_index", "choice_index")
                if action.get(key) is not None
            }
            battle.step(action["kind"], **arguments)
        else:
            pytest.fail(f"Act 1 encounter did not terminate: {encounter_id}")
        assert (step, snapshot["outcome"]) == expected[encounter_id]
