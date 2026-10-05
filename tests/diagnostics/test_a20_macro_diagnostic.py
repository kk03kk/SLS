from __future__ import annotations

from sls.contracts import (
    Action,
    ActionKind,
    MapNode,
    Observation,
    Player,
    RunContext,
    ScreenType,
)
from tools.diagnose_a20_act1_macro import describe_action, summarize


def test_map_action_resolves_visible_room_type() -> None:
    observation = Observation(
        Player("IRONCLAD", 50, 80, 0, 0, 3),
        RunContext(20, 1, 2, 99, False, False, False, "HEXAGHOST"),
        ScreenType.MAP,
        map_nodes=(MapNode("map:1:3", 1, 3, "ELITE", True),),
    )
    result = describe_action(
        observation, Action(ActionKind.CHOOSE_MAP_NODE, node_id="map:1:3"),
    )
    assert result["resolved"] == {
        "source": "map", "content_id": "ELITE", "x": 1, "y": 3,
        "reachable": True,
    }


def _action(kind: str, content: str | None = None, subject: str | None = None) -> dict:
    return {
        "kind": kind, "subject_id": subject,
        "target_id": None, "option_id": None, "node_id": None,
        "reward_id": None, "metadata": {},
        "resolved": (
            {"source": "reward", "content_id": content, "properties": {}}
            if content is not None else None
        ),
    }


def _decision(floor: int, selected: dict, alternatives: list[dict]) -> dict:
    return {
        "state": {"screen": "COMBAT_REWARD", "floor": floor},
        "selected": selected,
        "alternatives": [
            {"action": action, "rank": rank, "probability": 1.0 / len(alternatives)}
            for rank, action in enumerate(alternatives, start=1)
        ],
        "confidence": 0.8,
        "normalized_entropy": 0.4,
    }


def test_summary_deduplicates_card_offers_and_classifies_outcomes() -> None:
    anger = _action("CHOOSE_CARD_REWARD", "ANGER", "reward-card:0:0")
    flex = _action("CHOOSE_CARD_REWARD", "FLEX", "reward-card:0:1")
    take_gold = _action("TAKE_REWARD", "GOLD")
    skip = _action("SKIP_REWARD")
    bowl = _action("TAKE_SINGING_BOWL")
    episode = {
        "success": True,
        "final": {"floor": 16, "boss": "HEXAGHOST"},
        "combats": [{"floor": 16}],
        "anomalies": [],
        "inventory_changes": [],
        "macro_decisions": [
            _decision(1, take_gold, [anger, flex, take_gold, skip]),
            _decision(1, anger, [anger, flex, skip]),
            _decision(2, skip, [anger, flex, skip]),
            _decision(3, bowl, [anger, flex, bowl, skip]),
        ],
    }
    result = summarize([episode], [])
    assert result["success_rate"] == 1.0
    assert result["bosses"]["HEXAGHOST"] == {"attempts": 1, "wins": 1, "entries": 1}
    assert result["card_reward_opportunities"] == 3
    assert result["card_reward_outcomes"] == {
        "picked": 1, "skipped": 1, "singing_bowl": 1,
    }
    assert result["card_reward_cards"]["ANGER"] == {
        "offered": 3, "chosen": 1, "chosen_when_offered": 1 / 3,
    }
    assert result["card_reward_cards"]["FLEX"]["offered"] == 3
