from types import SimpleNamespace

import pytest

from sls.audit.ending_continuation import (
    choose_probe_action,
    collect_ending_continuation,
)
from sls.contracts import Action, ActionKind, Card


def test_probe_selects_actual_public_card_id_and_first_duplicate_instance():
    cards = tuple(Card(instance_id='HAND:'+str(i), card_id='SEARING_BLOW', zone='HAND',
                       upgrades=30, base_cost=2, current_cost=2, playable=True) for i in range(2))
    actions = tuple(Action(ActionKind.PLAY_CARD, subject_id=c.instance_id, target_id='ENEMY:0') for c in cards)
    decision = SimpleNamespace(observation=SimpleNamespace(hand=cards),
                               actions=(Action(ActionKind.END_TURN), *actions))
    assert choose_probe_action(decision) == actions[0]


def test_probe_unsupported_public_choice_fails_instead_of_injecting_action():
    decision = SimpleNamespace(observation=SimpleNamespace(hand=()),
                               actions=(Action(ActionKind.CHOOSE_EVENT_OPTION, option_id='0'),))
    with pytest.raises(ValueError, match='no reviewed probe action'):
        choose_probe_action(decision)


def test_probe_invalid_budget_is_rejected_before_session_access():
    with pytest.raises(ValueError, match='budget'):
        collect_ending_continuation(None, {}, lambda:None, max_decisions=0)
