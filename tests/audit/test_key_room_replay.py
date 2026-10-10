"""Reject stale/projection-inconsistent evidence and preserve comparison limits."""
import copy

import pytest

from sls.backends.original.adapter import adapt_original
from tools.replay_blue_key_archive import chest_creation, stock_rewards
from tools.replay_rest_key_archive import differences, stock_resources


def payload():
    return {
        "_stock_direct": {"player": {"current_hp": 40, "max_hp": 80}, "master_deck": [],
                          "relics": [{"id": "Burning Blood", "counter": -1}],
                          "potions": [{"id": "Potion Slot", "slot": -1}, {"id": "Potion Slot", "slot": -1}]},
        "game_state": {"current_hp": 40, "max_hp": 80, "gold": 99, "deck": []},
        "_parity_run": {"ruby_key": False, "emerald_key": False, "sapphire_key": False},
    }


def test_existing_counter_abi_is_distinct_from_raw_equality():
    source = payload()
    assert stock_resources(source)["relics"][0]["counter"] == 0
    assert stock_resources(source, canonical_counters=False)["relics"][0]["counter"] == -1
    source["_stock_direct"]["relics"][0]["counter"] = 3
    assert stock_resources(source)["relics"][0]["counter"] == 3
    assert len(stock_resources(source)["potions"]) == 2


def test_resource_projection_disagreement_cannot_pass():
    source = payload()
    source["game_state"]["current_hp"] = 41
    with pytest.raises(ValueError, match="projections disagree"):
        stock_resources(source)


def test_reward_projection_keeps_linked_relic_and_key_and_rejects_stale_done():
    relic = dict(type="RELIC", relic="Letter Opener", done=False, ignored=False)
    key = dict(type="SAPPHIRE_KEY", done=False, ignored=False)
    source = {"_stock_reward_state": {"screen_rewards": [relic, key]}}
    assert stock_rewards(source) == dict(gold=[], relics=["LETTER_OPENER"], sapphire_key=True)
    relic["done"] = True
    with pytest.raises(ValueError, match="unexpected active"):
        stock_rewards(source)


def test_unreachable_controlled_room_does_not_get_an_invented_edge():
    initial = {"public_map": [{"y": 7, "x": 1, "outgoing_node_ids": ["map:1:8"]}]}
    snapshot = copy.deepcopy(initial)
    row = {"boundaries": [{"_parity_run": {"current_map_x": 0, "current_map_y": 8}}]}
    result = chest_creation(row, initial, None)
    assert result["status"] == "UNSUPPORTED_CONTROLLED_NODE_NOT_REACHABLE"
    assert "differences" not in result
    assert initial == snapshot


def test_diff_keeps_order_and_late_disagreement():
    assert differences([1, 2, 3], [1, 2, 4]) == [dict(path="[2]", expected=3, actual=4)]
    assert differences([1, 2], [2, 1])
    assert differences({"key": False}, {"key": True})


def test_new_pool_and_chest_audit_evidence_never_changes_policy_input():
    source = payload()
    source.update(in_game=True, available_commands=["choose", "state"])
    source["game_state"].update(screen_type="REST", screen_state={"rest_options": ["rest", "smith", "recall"]},
                                choice_list=["rest", "smith", "recall"], act=2, floor=25)
    before = adapt_original(source).decision
    source["_stock_direct"].update(ordered_relic_pools={"rare_relics": ["secret future relic"]},
                                   chest={"gold_reward": True, "relic_reward": "RARE_RELIC"})
    source["_rng"] = {"treasure": {"seed0": 999, "seed1": 123, "counter": 777}}
    assert adapt_original(source).decision == before
