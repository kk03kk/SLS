from sls.validation.novelty import coverage_fingerprints, greedy_select


def test_fingerprints_cover_mechanism_dimensions() -> None:
    observation = {
        "screen": "COMBAT", "run": {"visible_boss_id": "SLIME_BOSS"},
        "hand": [{"card_id": "STRIKE_RED"}],
        "enemies": [{"monster_id": "CULTIST", "intent": "BUFF"}],
        "powers": [{"content_id": "LOSE_DEXTERITY"}],
        "relics": [{"content_id": "BURNING_BLOOD"}],
        "potions": [{"content_id": "SPEED_POTION"}],
    }
    actual = coverage_fingerprints(
        observation, cursor={"room": "MonsterRoom"},
        continuation={"event_id": "CLERIC", "event_phase": 1, "continuation_kind": "COMBAT"},
        selected_action={"kind": "USE_POTION"},
    )
    assert {
        "screen:COMBAT", "room:MonsterRoom", "boss:SLIME_BOSS",
        "event:CLERIC", "event_phase:CLERIC:1", "card:STRIKE_RED",
        "enemy:CULTIST", "encounter:CULTIST", "intent:BUFF",
        "power:LOSE_DEXTERITY", "relic:BURNING_BLOOD",
        "potion:SPEED_POTION", "continuation:COMBAT", "action:USE_POTION",
    }.issubset(actual)


def test_greedy_selection_is_deterministic_unique_and_marginal() -> None:
    candidates = [
        {"seed": 2, "variant": 1, "boundary_count": 10, "max_floor": 3, "terminal": False, "fingerprints": ["A", "B"]},
        {"seed": 1, "variant": 0, "boundary_count": 10, "max_floor": 3, "terminal": False, "fingerprints": ["A", "C"]},
        {"seed": 1, "variant": 1, "boundary_count": 10, "max_floor": 3, "terminal": False, "fingerprints": ["D"]},
        {"seed": 3, "variant": 0, "boundary_count": 10, "max_floor": 3, "terminal": True, "fingerprints": ["A", "B", "C"]},
    ]
    first = greedy_select(candidates, {"A"}, count=2)
    second = greedy_select(reversed(candidates), {"A"}, count=2)
    assert first == second
    assert [(item["seed"], item["variant"]) for item in first] == [(3, 0), (1, 1)]
    assert len({item["seed"] for item in first}) == 2
