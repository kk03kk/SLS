from types import SimpleNamespace

import pytest

from sls.audit.ending_continuation import (
    choose_probe_action,
    collect_ending_continuation,
)
from sls.contracts import Action, ActionKind, Card, Enemy


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


def test_heart_end_turn_probe_uses_public_enemy_identity():
    enemy = Enemy('MONSTER:0','CORRUPT_HEART',800,800,0,'STRONG_DEBUFF',0,0)
    card = Card('HAND:0','SEARING_BLOW','HAND',30,2,2,True)
    attack = Action(ActionKind.PLAY_CARD,subject_id=card.instance_id,target_id=enemy.instance_id)
    end = Action(ActionKind.END_TURN)
    decision = SimpleNamespace(observation=SimpleNamespace(hand=(card,),enemies=(enemy,)),actions=(attack,end))
    assert choose_probe_action(decision,policy='HEART_END_TURN') == end
    assert choose_probe_action(decision) == attack


def test_unreviewed_probe_policy_is_rejected_before_session_access():
    with pytest.raises(ValueError,match='policy'):
        collect_ending_continuation(None, {}, lambda:None,policy='FORCE_DEATH')
