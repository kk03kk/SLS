"""Bounded expectations independently read from the identified stock bytecode."""

import pytest

pytest.importorskip("sls.backends.simulator.native", exc_type=ImportError)

from tools.audit_act2_pilot_rules import scenarios


@pytest.fixture(scope="module")
def cases():
    return scenarios()


@pytest.mark.parametrize("move,damage,wounds", [
    ("BOOK_OF_STABBING_MULTI_STAB", 14, 2),
    ("BOOK_OF_STABBING_SINGLE_STAB", 24, 1),
    ("TASKMASTER_SCOURING_WHIP", 34, 3),  # Slavers: 13 + 7 + 14.
])
def test_stock_a20_damage_and_wounds(cases, move, damage, wounds):
    before, after = [s["combat"] for s in cases[move]]
    assert before["player"]["current_hp"] - after["player"]["current_hp"] == damage
    assert sum(c["id"] == "WOUND" for pile in ("hand", "draw_pile", "discard_pile")
               for c in after[pile]) == wounds
    if move.startswith("TASKMASTER"):
        master = next(m for m in after["monsters"] if m["monster_id"] == "TASKMASTER")
        assert master["_internal"]["strength"] == 1
        assert master["move_adjusted_damage"] == 8


def test_stock_a20_leader_encourages_allies_and_self(cases):
    after = cases["GREMLIN_LEADER_ENCOURAGE"][-1]["combat"]
    assert all(m["_internal"]["strength"] == 5 for m in after["monsters"])
    assert [m["block"] for m in after["monsters"]] == [10, 10, 0]


def test_stock_a20_chosen_opens_with_hex(cases):
    before, after = [s["combat"] for s in cases["CHOSEN_HEX"]]
    assert before["monsters"][0]["move_id"] == "CHOSEN_HEX"
    assert {p["id"]: p["amount"] for p in after["player"]["powers"]}["HEX"] == 1


def test_stock_a20_byrd_requires_four_attack_hits_and_halves_damage(cases):
    trace = cases["BYRD_FOUR_ATTACKS"]
    assert [s["combat"]["monsters"][0]["current_hp"] for s in trace] == [33, 30, 27, 24, 21]
    assert trace[3]["combat"]["monsters"][0]["move_id"] != "BYRD_STUNNED"
    assert trace[4]["combat"]["monsters"][0]["move_id"] == "BYRD_STUNNED"
    assert trace[4]["combat"]["monsters"][1]["powers"][0]["amount"] == 4
