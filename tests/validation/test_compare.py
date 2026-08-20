from sls.validation import parity_differences


def test_equal_public_run_has_no_differences() -> None:
    original = {
        "game_state": {
            "act": 1,
            "floor": 0,
            "current_hp": 80,
            "max_hp": 80,
            "gold": 99,
            "act_boss": "INVALID",
            "deck": [],
            "relics": [],
            "potions": [],
            "map": [],
            "_parity_run": {
                "ruby_key": False,
                "emerald_key": False,
                "sapphire_key": False,
            },
        },
        "_rng": {},
    }
    simulator = {
        "public_run": {
            "act": 1,
            "floor": 0,
            "gold": 99,
            "visible_boss_id": "INVALID",
        },
        "player_state": {
            "current_hp": 80,
            "max_hp": 80,
            "red_key": False,
            "green_key": False,
            "blue_key": False,
        },
        "public_inventory": {"deck": [], "relics": [], "potions": []},
        "public_map": [],
        "rng": {},
    }
    assert parity_differences(original, simulator) == {}
