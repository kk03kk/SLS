from __future__ import annotations

import pytest

from sls.contracts import Card, Observation, Player, RunContext, ScreenType


def minimal_observation(**changes: object) -> Observation:
    values = {
        "player": Player("IRONCLAD", 80, 80, 0, 3, 3),
        "run": RunContext(0, 1, 0, 99, False, False, False),
        "screen": ScreenType.NEOW,
    }
    values.update(changes)
    return Observation(**values)


def test_observation_contains_no_action_set() -> None:
    observation = minimal_observation()
    assert "legal_actions" not in observation.to_dict()


def test_hidden_draw_order_is_rejected() -> None:
    with pytest.raises(ValueError, match="hidden draw-pile order"):
        Card("card:1", "STRIKE_RED", "DRAW", 0, 1, 1, visible_order=0)
