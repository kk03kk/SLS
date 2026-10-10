"""Stable terminal classification preserves loss/victory and resource evidence."""
import copy
from types import SimpleNamespace

import pytest

from sls.audit.act2_differential import production_combat_projection
from sls.audit.terminal_resources import (
    native_terminal_resources,
    stock_terminal_resources,
)


def payload():
    return {'game_state':{'screen_type':'COMPLETE','room_phase':'COMPLETE','act':4,
                         'act_boss':'The Heart','current_hp':5,'max_hp':80},
            '_stock_direct':{'player':{'current_hp':5,'max_hp':80},
                             'potions':[{'id':'Potion Slot'}]}, '_rng':{'ai':{'counter':7}}}


def test_terminal_hp_rng_and_victory_are_independent_raw_stock_fields():
    stock = payload()
    expected = stock_terminal_resources(stock)
    native = {'outcome':'PLAYER_VICTORY', '_legal_actions':[], '_rng':stock['_rng'],
              'game_state':{'current_hp':5,'max_hp':80,'potions':[{'id':'Potion Slot'}]}}
    assert native_terminal_resources(native) == expected
    changed = copy.deepcopy(native)
    changed['_rng']['ai']['counter'] += 1
    assert native_terminal_resources(changed) != expected
    stock['game_state'].update(screen_type='GAME_OVER',current_hp=0,screen_state={'victory':False})
    stock['_stock_direct']['player']['current_hp'] = 0
    assert stock_terminal_resources(stock)['outcome'] == 'PLAYER_LOSS'


@pytest.mark.parametrize('key,value', [('act',3),('room_phase','COMBAT'),('current_hp',0)])
def test_incomplete_or_inconsistent_stock_boundary_is_rejected(key, value):
    raw = payload()
    raw['game_state'][key] = value
    with pytest.raises(ValueError):
        stock_terminal_resources(raw)


def test_terminal_actions_and_hp_disagreement_are_not_normalized_away():
    raw = payload()
    raw['game_state']['max_hp'] = 81
    with pytest.raises(ValueError,match='disagree'):
        stock_terminal_resources(raw)
    with pytest.raises(ValueError,match='no legal actions'):
        native_terminal_resources({'outcome':'PLAYER_VICTORY','_legal_actions':[{'kind':'end_turn'}]})


def test_combat_projection_reports_unsupported_terminal_without_inventing_combat():
    battle = SimpleNamespace(snapshot=lambda: {'game_state':{'outcome':'PLAYER_VICTORY'}})
    with pytest.raises(ValueError,match='separate terminal or reward'):
        production_combat_projection(battle)
