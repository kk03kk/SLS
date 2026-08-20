from __future__ import annotations

import pytest

from sls.contracts import (
    Action,
    ActionKind,
    Decision,
    Observation,
    Player,
    RunContext,
    ScreenType,
    Transition,
)


def observation() -> Observation:
    return Observation(
        Player("IRONCLAD", 80, 80, 0, 3, 3),
        RunContext(0, 1, 0, 99, False, False, False),
        ScreenType.NEOW,
    )


def test_duplicate_candidates_are_rejected() -> None:
    action = Action(ActionKind.CHOOSE_NEOW_OPTION, option_id="neow:0")
    with pytest.raises(ValueError, match="semantically unique"):
        Decision(observation(), (action, action))


def test_transition_and_decision_termination_agree() -> None:
    with pytest.raises(ValueError, match="termination"):
        Transition(Decision(observation(), (), terminal=True), 0.0, terminated=False)
