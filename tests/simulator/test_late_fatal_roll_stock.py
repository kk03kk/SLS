"""Independent stock prefixes, fatal action order and lossless restoration."""

import json
from pathlib import Path

import pytest

from sls.audit.act2_differential import (
    comparison_projection,
    production_combat_projection,
)
from sls.backends.simulator import native

CASES = json.loads((Path(__file__).parents[1] /
                    "fixtures/regressions/late-fatal-roll-stock.json").read_text(encoding="utf-8"))["cases"]


@pytest.mark.parametrize("case", CASES, ids=lambda case: str(case["seed"]))
def test_late_fatal_attack_matches_independent_stock_and_restore(case):
    battle = native.LightspeedBattle()
    battle.reset_encounter_probe(case["seed"], case["encounter"], case["initial_rng"],
                                 20, case["act"], case["floor"], case["scene_id"])
    initial = case["initial"]
    battle.set_player_health(initial["hp"], initial["max_hp"])
    payload = battle.snapshot()
    combat = payload["game_state"]["combat_state"]
    combat["player"]["energy"] = initial["energy"]
    combat["player"]["block"] = initial["block"]
    for monster in combat["monsters"]:
        if monster["monster_id"] in initial.get("moves", {}):
            monster["move_id"] = initial["moves"][monster["monster_id"]]["native"]
    battle.load_checkpoint({"game_state": payload["game_state"], "rng": payload["_rng"]})
    battle.set_card_piles(initial["hand"], initial["draw"], [], [])
    battle.set_potions([])
    payload = battle.snapshot()
    payload["game_state"]["combat_state"]["_internal"]["potion_capacity"] = 2
    battle.load_checkpoint({"game_state": payload["game_state"], "rng": payload["_rng"]})
    for index, expected in enumerate(case["expected"]):
        current = production_combat_projection(battle)
        assert comparison_projection(current, stock=False, extended_direct=True) == expected, index
        state = battle.snapshot()
        restored = native.LightspeedBattle()
        restored.load_checkpoint({"game_state": state["game_state"], "rng": state["_rng"]})
        assert restored.snapshot() == state
        if index < len(case["actions"]):
            action = case["actions"][index]
            assert action == {"kind": "end_turn"}
            battle.step("end_turn")
            restored.step("end_turn")
            assert restored.snapshot() == battle.snapshot()
    assert battle.snapshot()["outcome"] == "PLAYER_LOSS"
    assert battle.snapshot()["_legal_actions"] == []
