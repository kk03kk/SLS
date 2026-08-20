from __future__ import annotations

from sls.backends.original.adapter import adapt_original
from sls.contracts import ActionKind, ScreenType


def base_game(**changes: object) -> dict:
    game = {
        "class": "IRONCLAD",
        "ascension_level": 0,
        "act": 1,
        "floor": 1,
        "gold": 99,
        "current_hp": 80,
        "max_hp": 80,
        "deck": [],
        "relics": [],
        "potions": [],
        "map": [],
        "_parity_run": {},
    }
    game.update(changes)
    return game


def test_map_choices_become_semantic_actions() -> None:
    payload = {
        "in_game": True,
        "ready_for_command": True,
        "available_commands": ["choose"],
        "game_state": base_game(
            screen_type="MAP",
            screen_state={"next_nodes": [{"x": 2, "y": 3}]},
        ),
    }
    adapted = adapt_original(payload)
    assert adapted.decision.observation.screen is ScreenType.MAP
    assert adapted.decision.actions[0].kind is ActionKind.CHOOSE_MAP_NODE
    assert adapted.decision.actions[0].node_id == "map:2:3"
    assert adapted.commands[adapted.decision.actions[0].candidate_id] == ("choose 0",)


def test_combat_card_targets_use_decision_scoped_ids() -> None:
    payload = {
        "in_game": True,
        "ready_for_command": True,
        "available_commands": ["play", "end"],
        "game_state": base_game(
            screen_type="NONE",
            combat_state={
                "turn": 1,
                "player": {"current_hp": 80, "max_hp": 80, "energy": 3},
                "hand": [{
                    "id": "Strike_R",
                    "upgrades": 0,
                    "cost": 1,
                    "is_playable": True,
                    "has_target": True,
                }],
                "draw_pile": [],
                "discard_pile": [],
                "exhaust_pile": [],
                "monsters": [{
                    "id": "JawWorm",
                    "current_hp": 40,
                    "max_hp": 40,
                    "block": 0,
                    "intent": "ATTACK",
                }],
            },
        ),
    }
    adapted = adapt_original(payload)
    play = next(action for action in adapted.decision.actions if action.kind is ActionKind.PLAY_CARD)
    assert play.subject_id == "HAND:0"
    assert play.target_id == "MONSTER:0"
    assert adapted.commands[play.candidate_id] == ("play 1 0",)


def test_combat_card_uses_authoritative_cost_for_turn() -> None:
    payload = {
        "in_game": True,
        "ready_for_command": True,
        "available_commands": ["play", "end"],
        "game_state": base_game(
            screen_type="NONE",
            combat_state={
                "turn": 1,
                "player": {"current_hp": 80, "max_hp": 80, "energy": 3},
                "hand": [{
                    "id": "Wild Strike", "upgrades": 0, "cost": 0,
                    "base_cost": 1, "cost_for_turn": 0,
                    "is_playable": True, "has_target": True,
                }],
                "draw_pile": [], "discard_pile": [], "exhaust_pile": [],
                "monsters": [{
                    "id": "Sentry", "current_hp": 40, "max_hp": 40,
                    "block": 0, "intent": "ATTACK",
                }],
            },
        ),
    }

    card = adapt_original(payload).decision.observation.hand[0]

    assert card.base_cost == 1
    assert card.current_cost == 0


def test_combat_card_reward_is_folded_into_semantic_candidates() -> None:
    payload = {
        "in_game": True,
        "ready_for_command": True,
        "available_commands": ["choose", "proceed"],
        "game_state": base_game(
            screen_type="COMBAT_REWARD",
            screen_state={
                "rewards": [{
                    "reward_type": "CARD",
                    "cards": [
                        {"id": "Ghostly", "upgrades": 0},
                        {"id": "Venomology", "upgrades": 1},
                    ],
                }],
            },
        ),
    }
    adapted = adapt_original(payload)
    rewards = adapted.decision.observation.reward_options
    assert [(item.instance_id, item.content_id) for item in rewards] == [
        ("reward-card:0:0", "APPARITION"),
        ("reward-card:0:1", "ALCHEMIZE"),
    ]
    kinds = [action.kind for action in adapted.decision.actions]
    assert kinds.count(ActionKind.CHOOSE_CARD_REWARD) == 2
    skip = next(
        action for action in adapted.decision.actions
        if action.kind is ActionKind.SKIP_CARD_REWARD
    )
    assert skip.option_id == "reward-card:0"
    assert adapted.commands[skip.candidate_id] == ("choose 0", "skip")


def test_golden_idol_second_phase_uses_stable_semantic_option_ids() -> None:
    payload = {
        "in_game": True,
        "ready_for_command": True,
        "available_commands": ["choose"],
        "_continuation": {
            "event_id": "com.megacrit.cardcrawl.events.exordium.GoldenIdolEvent",
            "event_phase": "1",
        },
        "game_state": base_game(
            screen_type="EVENT",
            choice_list=["curse", "damage", "max hp"],
            screen_state={"event_id": "Golden Idol"},
        ),
    }

    adapted = adapt_original(payload)

    assert [action.option_id for action in adapted.decision.actions] == [
        "event-option:2", "event-option:3", "event-option:4",
    ]
    assert list(adapted.commands.values()) == [
        ("choose 0",), ("choose 1",), ("choose 2",),
    ]
    assert [item.instance_id for item in adapted.decision.observation.event_options] == [
        "event-option:2", "event-option:3", "event-option:4",
    ]
