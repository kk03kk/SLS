"""Fatal stock countdown: owner dies before player, later monsters stay alive."""

import json
from pathlib import Path

import pytest

from sls.backends.simulator import native

CASES = json.loads((Path(__file__).parents[1] /
                    "fixtures/regressions/exploder-fatal-stock.json").read_text())["cases"]


@pytest.mark.parametrize("case", CASES, ids=lambda case: str(case["seed"]))
def test_fatal_countdown_matches_independent_stock(case):
    battle = native.LightspeedBattle()
    battle.reset_encounter_probe(case["seed"], "FOUR_SHAPES", case["initial_rng"],
                                 20, 3, 40, case["scene_id"])
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
    for action in case["actions"]:
        battle.step(action["kind"])
    final = battle.snapshot()
    actual = {
        "hp": final["game_state"]["current_hp"],
        "monsters": [{"id": m["content_id"], "hp": m["current_hp"]}
                     for m in battle.public_combat_probe_snapshot()["monsters"]],
        "rng": final["_rng"], "outcome": final["outcome"],
        "legal_actions": final["_legal_actions"],
    }
    assert actual == case["expected_terminal"]
    restored = native.LightspeedBattle()
    restored.load_checkpoint({"game_state": final["game_state"], "rng": final["_rng"]})
    assert restored.snapshot() == final
