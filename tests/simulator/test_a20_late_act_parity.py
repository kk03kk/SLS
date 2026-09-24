"""Regression expectations derived from the stock monster Java implementations."""

import random

import pytest

from sls.backends.simulator import native


def test_reptomancer_reused_daggers_checkpoint_round_trip():
    battle = native.LightspeedBattle()
    battle.reset(92622, "REPTOMANCER", 20)
    battle.set_player_health(5000, 5000)
    chooser = random.Random(0)
    for _ in range(40):
        snapshot = battle.snapshot()
        replay = native.LightspeedBattle()
        replay.load_checkpoint({"game_state": snapshot["game_state"], "rng": snapshot["_rng"]})
        assert replay.snapshot()["game_state"] == snapshot["game_state"]
        assert replay.snapshot()["_rng"] == snapshot["_rng"]
        action = chooser.choice(snapshot["_legal_actions"])
        arguments = {k: v for k, v in action.items() if k not in ("kind", "command") and v is not None}
        battle.step(action["kind"], **arguments)
        replay.step(action["kind"], **arguments)
        assert replay.snapshot() == battle.snapshot()


def _forced_battle(encounter, moves, *, ascension=20, relics=(), hp=999):
    battle = native.LightspeedBattle()
    battle.reset(0, encounter, ascension, relics=list(relics), replace_relics=True)
    battle.set_player_health(hp, hp)
    snapshot = battle.snapshot()
    for monster in snapshot["game_state"]["combat_state"]["monsters"]:
        if monster["monster_id"] in moves:
            monster["move_id"] = moves[monster["monster_id"]]
    battle.load_checkpoint({"game_state": snapshot["game_state"], "rng": snapshot["_rng"]})
    return battle


@pytest.mark.parametrize("ascension,amount", [(0, 3), (18, 3), (19, 5), (20, 5)])
def test_collector_mega_debuff_uses_stock_ascension_duration(ascension, amount):
    # TheCollector constructor: megaDebuffAmt is 5 at A19+, otherwise 3.
    battle = _forced_battle("COLLECTOR", {"THE_COLLECTOR": "THE_COLLECTOR_MEGA_DEBUFF"},
                            ascension=ascension)
    battle.step("end_turn")
    powers = {p["id"]: p["amount"] for p in battle.snapshot()["game_state"]["combat_state"]["player"]["powers"]}
    assert {name: powers[name] for name in ("WEAK", "VULNERABLE", "FRAIL")} == dict.fromkeys(
        ("WEAK", "VULNERABLE", "FRAIL"), amount,
    )


@pytest.mark.parametrize("ascension", [18, 20])
def test_time_eater_ripple_artifact_blocks_vulnerable_before_weak(ascension):
    # TimeEater.takeTurn case 3: Vulnerable, Weak, then A19 Frail.
    battle = _forced_battle("TIME_EATER", {"TIME_EATER": "TIME_EATER_RIPPLE"},
                            ascension=ascension, relics=("CLOCKWORK_SOUVENIR",))
    battle.step("end_turn")
    powers = {p["id"]: p["amount"] for p in battle.snapshot()["game_state"]["combat_state"]["player"]["powers"]}
    assert "VULNERABLE" not in powers
    assert powers["WEAK"] == 1
    assert powers.get("FRAIL", 0) == int(ascension >= 19)


@pytest.mark.parametrize("encounter,moves,prior_rolls", [
    ("AUTOMATON", {"BRONZE_AUTOMATON": "BRONZE_AUTOMATON_FLAIL"}, 0),
    ("AUTOMATON", {"BRONZE_AUTOMATON": "BRONZE_AUTOMATON_HYPER_BEAM"}, 0),
    ("DONU_AND_DECA", {"DECA": "DECA_BEAM"}, 0),
    ("DONU_AND_DECA", {"DECA": "DECA_SQUARE_OF_PROTECTION", "DONU": "DONU_BEAM"}, 1),
    ("SHIELD_AND_SPEAR", {"SPIRE_SHIELD": "SPIRE_SHIELD_BASH"}, 0),
    ("SHIELD_AND_SPEAR", {"SPIRE_SHIELD": "SPIRE_SHIELD_FORTIFY", "SPIRE_SPEAR": "SPIRE_SPEAR_BURN_STRIKE"}, 1),
])
def test_lethal_late_act_attack_does_not_execute_queued_ai_roll(encounter, moves, prior_rolls):
    # Stock takeTurn queues RollMoveAction after damage; death stops that queue.
    battle = _forced_battle(encounter, moves, hp=1)
    before = battle.snapshot()["_rng"]["ai"]["counter"]
    battle.step("end_turn")
    after = battle.snapshot()
    assert after["game_state"]["outcome"] == "PLAYER_LOSS"
    assert after["_rng"]["ai"]["counter"] - before == prior_rolls


@pytest.mark.parametrize("ascension,cap,beat", [(18, 300, 1), (19, 200, 2), (20, 200, 2)])
def test_heart_damage_cap_beat_of_death_and_next_turn_reset(ascension, cap, beat):
    # CorruptHeart.usePreBattleAction and InvinciblePower.atStartOfTurn.
    battle = _forced_battle("THE_HEART", {}, ascension=ascension)
    battle.set_card_piles(["BLUDGEON"] * 8, [], [], [])
    snapshot = battle.snapshot()
    snapshot["game_state"]["combat_state"]["player"]["energy"] = 30
    battle.load_checkpoint({"game_state": snapshot["game_state"], "rng": snapshot["_rng"]})
    for _ in range(8):
        battle.step("play", card_index=1, target_index=0)
    combat = battle.snapshot()["game_state"]["combat_state"]
    heart = combat["monsters"][0]
    assert heart["current_hp"] == 800 - min(cap, 8 * 32)
    assert combat["player"]["current_hp"] == 999 - 8 * beat
    assert next(p["amount"] for p in heart["powers"] if p["id"] == "Invincible") == max(0, cap - 8 * 32)
    battle.step("end_turn")
    combat = battle.snapshot()["game_state"]["combat_state"]
    assert next(p["amount"] for p in combat["monsters"][0]["powers"] if p["id"] == "Invincible") == cap
