from types import SimpleNamespace

import pytest

from sls.audit.ending_continuation import (
    choose_probe_action,
    collect_ending_continuation,
)
from sls.contracts import Action, ActionKind, Card, Enemy, ShopItem


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


def test_shop_probe_uses_cheapest_legal_public_item():
    items = (ShopItem('shop-card:0','STRIKE','CARD',30),ShopItem('shop-card:1','DEFEND','CARD',20),
             ShopItem('shop-card:2','ANGER','CARD',5,sold=True))
    actions = tuple(Action(ActionKind.BUY_CARD,subject_id=item.instance_id) for item in items[:2])
    decision = SimpleNamespace(observation=SimpleNamespace(screen='SHOP',shop_items=items),actions=actions)
    assert choose_probe_action(decision,shop_action='BUY_CARD') == actions[1]


def test_shop_removal_uses_real_confirmation_and_selection_actions():
    confirm = Action(ActionKind.CONFIRM,option_id='shop-remove')
    decision = SimpleNamespace(observation=SimpleNamespace(screen='SHOP'),actions=(confirm,))
    assert choose_probe_action(decision,shop_action='REMOVE_CARD') == confirm
    removal = Action(ActionKind.REMOVE_CARD,subject_id='select-card:0')
    decision = SimpleNamespace(observation=SimpleNamespace(screen='CARD_REWARD'),actions=(removal,))
    assert choose_probe_action(decision,shop_action='REMOVE_CARD') == removal


def test_repeated_shop_plan_rejected_before_session_access():
    with pytest.raises(ValueError,match='shop probe plan'):
        collect_ending_continuation(None,{},lambda:None,shop_plan=['BUY_CARD','BUY_CARD'])
