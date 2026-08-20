from __future__ import annotations

import pytest

from sls.contracts import Action, ActionKind


def test_candidate_identity_is_semantic_and_stable() -> None:
    left = Action(ActionKind.PLAY_CARD, subject_id="card:7", target_id="monster:0")
    right = Action.from_dict(left.to_dict())
    assert left == right
    assert left.candidate_id == right.candidate_id


def test_backend_metadata_cannot_enter_an_action() -> None:
    with pytest.raises(ValueError, match="private action metadata"):
        Action(ActionKind.END_TURN, metadata=(("bits", 7),))
