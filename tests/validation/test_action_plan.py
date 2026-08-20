from __future__ import annotations

import json
from types import SimpleNamespace

import pytest

from sls.contracts import Action, ActionKind
from tools.replay_original_segment import _load_action_plan, _planned_action


def test_semantic_action_plan_is_strict_and_versioned(tmp_path) -> None:
    action = Action(ActionKind.END_TURN)
    path = tmp_path / "plan.json"
    path.write_text(json.dumps({
        "schema": "sls-semantic-action-plan-v1",
        "source": {"kind": "UNIT_TEST"},
        "actions": [action.to_dict()],
    }), encoding="utf-8")

    loaded, document = _load_action_plan(path)

    assert loaded == [action]
    assert document["source"] == {"kind": "UNIT_TEST"}
    decision = SimpleNamespace(actions=(action,))
    assert _planned_action(action, decision, decision, offset=0) == action


def test_semantic_action_plan_refuses_candidate_drift() -> None:
    planned = Action(ActionKind.PLAY_CARD, subject_id="HAND:0")
    different = Action(ActionKind.END_TURN)

    with pytest.raises(RuntimeError, match="not legal in Original/Simulator"):
        _planned_action(
            planned,
            SimpleNamespace(actions=(different,)),
            SimpleNamespace(actions=(different,)),
            offset=3,
        )
