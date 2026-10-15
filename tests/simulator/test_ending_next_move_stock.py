"""Reflected EnemyMoveInfo is checked separately from the displayed nextMove."""

import gzip
import json
from pathlib import Path

import pytest

from sls.audit.act2_differential import (
    comparison_projection,
    production_combat_projection,
)
from sls.backends.simulator import native

MOVE_IDS = {
    "SPIRE_SHIELD_BASH": 1, "SPIRE_SHIELD_FORTIFY": 2, "SPIRE_SHIELD_SMASH": 3,
    "SPIRE_SPEAR_BURN_STRIKE": 1, "SPIRE_SPEAR_PIERCER": 2, "SPIRE_SPEAR_SKEWER": 3,
    "CORRUPT_HEART_BLOOD_SHOTS": 1, "CORRUPT_HEART_ECHO": 2,
    "CORRUPT_HEART_DEBILITATE": 3, "CORRUPT_HEART_BUFF": 4,
}
CASES = json.loads((Path(__file__).parents[1] /
                    "fixtures/regressions/ending-next-move-stock.json").read_text())["cases"]
CASES.extend(json.loads(gzip.decompress((Path(__file__).parents[1] /
                    "fixtures/regressions/heart-late-buffs-stock.json.gz").read_bytes()))["cases"])


@pytest.mark.parametrize("case", CASES, ids=lambda case: str(case["seed"]))
def test_ending_next_move_public_state_rng_and_continuation_match_stock(case):
    battle = native.LightspeedBattle()
    battle.reset_encounter_probe(case["seed"], case["encounter"], case["initial_rng"],
                                 20, 4, case["floor"], case["scene_id"])
    initial = case["initial"]
    battle.set_player_health(initial["hp"], initial["max_hp"])
    payload = battle.snapshot()
    player = payload["game_state"]["combat_state"]["player"]
    player["energy"], player["block"] = initial["energy"], initial["block"]
    battle.load_checkpoint({"game_state": payload["game_state"], "rng": payload["_rng"]})
    battle.set_card_piles(initial["hand"], initial["draw"], [], [])
    battle.set_potions([])
    payload = battle.snapshot()
    payload["game_state"]["combat_state"]["_internal"]["potion_capacity"] = 2
    battle.load_checkpoint({"game_state": payload["game_state"], "rng": payload["_rng"]})
    for index, expected in enumerate(case["expected"]):
        state = battle.snapshot()
        monsters = state["game_state"]["combat_state"]["monsters"]
        assert [{"id": m["monster_id"], "next_move": MOVE_IDS[m["move_id"]]}
                for m in monsters] == expected["moves"], index
        assert comparison_projection(production_combat_projection(battle), stock=False,
                                     extended_direct=True) == expected["projection"], index
        restored = native.LightspeedBattle()
        restored.load_checkpoint({"game_state": state["game_state"], "rng": state["_rng"]})
        assert restored.snapshot() == state
        if index < len(case["actions"]):
            assert case["actions"][index] == {"kind": "end_turn"}
            battle.step("end_turn")
            restored.step("end_turn")
            assert restored.snapshot() == battle.snapshot()
    assert battle.snapshot()["outcome"] == case["expected_outcome"]
    if case["expected_outcome"] == "PLAYER_LOSS":
        assert battle.snapshot()["_legal_actions"] == []
