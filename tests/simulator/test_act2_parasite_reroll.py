"""Stock ShelledParasite.getMove bytecode and captured seed 131100022."""

from sls.backends.simulator import native


def test_repeated_fell_reroll_uses_new_roll_for_suck_branch():
    # Independent stock capture: initial Fell, roll<20 would repeat Fell;
    # stock recursively rolls [20,99], whose result selects Suck, not Double.
    battle = native.LightspeedBattle()
    battle.reset(131100022, "SHELL_PARASITE", 20)
    battle.set_player_health(5000, 5000)
    rng = battle.snapshot()["_rng"]
    rng["ai"] = {
        "counter": 3, "seed0": 5037054903538613436, "seed1": 5400470220566599705,
    }
    battle.set_rng_state(rng)
    battle.step("end_turn")
    snapshot = battle.snapshot()
    assert snapshot["game_state"]["combat_state"]["monsters"][0]["move_id"] == "SHELLED_PARASITE_SUCK"
    assert snapshot["_rng"]["ai"] == {
        "counter": 5, "seed0": 5814903577181171517, "seed1": 11312729512505110711,
    }
