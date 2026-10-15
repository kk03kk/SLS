"""Stock growable permanent decks and high-ordinal GRID selections."""

import json
from functools import cache
from pathlib import Path

import pytest

from sls.backends.simulator import native
from sls.backends.simulator.environment import SimulatorBackend
from sls.curriculum import ironclad_fullrun_profile


@cache
def _strike():
    run = native.LightspeedRunState()
    run.reset(0, 20)
    state = run.snapshot()
    return next(card for card, public in zip(state["player_state"]["deck"],
                                            state["public_inventory"]["deck"], strict=True)
                if public["id"] == "STRIKE_RED")


def _restore(run):
    state = run.snapshot()
    restored = native.LightspeedRunState()
    restored.load_state(state)
    assert restored.snapshot() == state
    assert restored.legal_actions() == run.legal_actions()
    return restored


def _large_smith_grid(size=300):
    case = json.loads((Path(__file__).parents[1] /
                       "fixtures/regressions/key-act4-entry-stock.json").read_text())["cases"][0]
    run = native.LightspeedRunState()
    run.load_state(case["initial"])
    for bits in case["bits"]:
        run.step(bits)
    run.step(3)  # actual Act4 map's rest node, reached through recorded boss flow
    state = run.snapshot()
    assert state["screen_info"]["screen_state"] == 7
    state["player_state"]["deck"] = [dict(_strike()) for _ in range(size)]
    state["player_state"]["bottle_indices"] = [-1, -1, -1]
    run.load_state(state)
    run.step(1)
    assert len(run.legal_actions()) == size
    return run


@pytest.mark.parametrize("size", [96, 300])
def test_legal_card_reward_grows_large_deck_and_restores_exactly(size):
    run = native.LightspeedRunState()
    run.reset(0, 20)
    run._set_skip_battles_for_testing(True)
    for bits in (0, 0, 4):
        run.step(bits)
    state = run.snapshot()
    state["player_state"]["deck"] = [dict(_strike()) for _ in range(size)]
    run.load_state(state)
    assert any(a["bits"] == 0 and a["reward_type"] == 0 for a in run.legal_actions())
    restored = _restore(run)
    run.step(0)
    restored.step(0)
    assert restored.snapshot() == run.snapshot()
    assert len(run.snapshot()["player_state"]["deck"]) == size + 1
    _restore(run)


def test_large_grid_indexes_are_unique_and_upgrade_the_exact_high_ordinal_card():
    run = _large_smith_grid()
    actions = run.legal_actions()
    assert [a["bits"] for a in actions] == list(range(300))
    assert [a["idx1"] for a in actions] == list(range(300))
    assert all(a["idx2"] == 0 for a in actions)
    restored = _restore(run)
    run.step(299)
    restored.step(299)
    assert restored.snapshot() == run.snapshot()
    deck = run.snapshot()["player_state"]["deck"]
    assert [i for i, card in enumerate(deck) if card["upgraded"]] == [299]
    _restore(run)


@pytest.mark.parametrize("bits", [300, 32768, 65536])
def test_grid_rejects_out_of_range_and_aliased_indices_without_mutation(bits):
    run = _large_smith_grid()
    before = run.snapshot()
    with pytest.raises(ValueError, match="not legal"):
        run.step(bits)
    assert run.snapshot() == before


def test_large_grid_policy_actions_map_to_the_exact_native_option():
    run = _large_smith_grid()
    backend = SimulatorBackend(ironclad_fullrun_profile(20, require_heart=True))
    backend._native = run
    decision = backend._adapt(run.snapshot())
    assert len(decision.actions) == 300
    assert len({action.candidate_id for action in decision.actions}) == 300
    assert len(decision.observation.deck) == 300
    action = next(a for a in decision.actions if a.subject_id == "select-card:299")
    backend.step(action)
    assert [i for i, card in enumerate(run.snapshot()["player_state"]["deck"])
            if card["upgraded"]] == [299]


@pytest.mark.parametrize("select_type", [1, 2, 4, 5])
def test_high_ordinal_transform_remove_and_duplicate_preserve_identity_and_restore(select_type):
    run = _large_smith_grid()
    state = run.snapshot()
    state["screen_info"]["select_type"] = select_type
    if select_type != 4:
        # Controlled algorithm check with an explicit return-to-map callback;
        # generic transform/duplicate GRID still uses canonical replay in runs.
        state["screen_info"].pop("rest_grid_contract")
    state["player_state"]["bottle_indices"] = [298, -1, -1]
    run.load_state(state)
    restored = native.LightspeedRunState()
    restored.load_state(state)
    run.step(299)
    restored.step(299)
    assert restored.snapshot() == run.snapshot()
    final = run.snapshot()
    assert final["player_state"]["bottle_indices"][0] == 298
    assert len(final["player_state"]["deck"]) == {1: 300, 2: 300, 4: 299, 5: 301}[select_type]
    assert all(card["id"] == _strike()["id"] for card in final["player_state"]["deck"][:299])
    if select_type in (1, 2):
        assert final["player_state"]["deck"][-1]["id"] != _strike()["id"]
        assert final["player_state"]["deck"][-1]["upgraded"] is (select_type == 2)
    _restore(run)


def test_large_deck_initializes_real_combat_and_restores_continuous_actions():
    run = _large_smith_grid()
    run.step(299)
    run.step(3)  # Act4 shop
    run.step(805306368)  # leave shop
    run.step(3)  # Shield and Spear
    state = run.snapshot()
    assert "combat_checkpoint" in state
    assert len(state["player_state"]["deck"]) == 300
    restored = _restore(run)
    action = run.legal_actions()[0]["bits"]
    run.step(action)
    restored.step(action)
    assert restored.snapshot() == run.snapshot()
