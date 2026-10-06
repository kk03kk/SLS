"""Stock SlaverBlue.getMove A17 split, exercised by stock seed 131100034."""

import pytest

from sls.backends.simulator import native


@pytest.mark.parametrize("ascension,next_move", [(0, "RAKE"), (16, "RAKE"), (17, "STAB"), (20, "STAB")])
def test_rake_cannot_repeat_at_a17_plus(ascension, next_move):
    battle = native.LightspeedBattle()
    battle.reset(131100034, "BLUE_SLAVER", ascension)
    battle.set_player_health(5000, 5000)
    snapshot = battle.snapshot()
    snapshot["game_state"]["combat_state"]["monsters"][0]["move_id"] = "BLUE_SLAVER_RAKE"
    snapshot["_rng"]["ai"] = {
        "counter": 7, "seed0": 2021488468779482057, "seed1": 6835147604065452395,
    }
    battle.load_checkpoint({"game_state": snapshot["game_state"], "rng": snapshot["_rng"]})
    battle.step("end_turn")
    assert battle.snapshot()["game_state"]["combat_state"]["monsters"][0]["move_id"] == "BLUE_SLAVER_" + next_move
