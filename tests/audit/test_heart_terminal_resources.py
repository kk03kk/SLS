import copy
import json
from pathlib import Path

import pytest

from sls.audit.act2_differential import heart_terminal_resources

CASES = json.loads((Path(__file__).parents[1] /
                   "fixtures/regressions/heart-terminal-public-stock.json").read_text(encoding="utf-8"))["cases"]


@pytest.mark.parametrize("case", CASES, ids=lambda case: str(case["seed"]))
def test_independent_original_heart_terminal_proof(case):
    result = heart_terminal_resources(case["stock_terminal"], stock=True)
    assert result["outcome"] == ("PLAYER_LOSS" if result["hp"] == 0 else "PLAYER_VICTORY")
    assert result["policy_actions"] == []
    assert result["gold"] == 99
    assert result["rng"] == case["stock_terminal"]["_rng"]


@pytest.mark.parametrize("mutation", ["room", "difficulty", "hp", "boss", "phase"])
def test_terminal_claim_rejects_inconsistent_original_proof(mutation):
    payload = copy.deepcopy(CASES[1]["stock_terminal"])
    if mutation == "room":
        payload["_stock_direct"]["room_class"] = "com.megacrit.cardcrawl.rooms.MonsterRoom"
    elif mutation == "difficulty":
        payload["_stock_direct"]["ascension"] = 0
    elif mutation == "hp":
        payload["_stock_direct"]["player"]["current_hp"] += 1
    elif mutation == "boss":
        payload["_stock_direct"]["monsters"][0]["id"] = "SpireShield"
    else:
        payload["game_state"]["room_phase"] = "COMBAT"
    with pytest.raises(ValueError):
        heart_terminal_resources(payload, stock=True)
