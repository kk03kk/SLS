"""Explicit ending subsets require independent victory proof, not absent combat."""

import copy

import pytest

from sls.audit.act2_differential import encounter_terminal_subset


def _stock():
    return {'game_state': {'current_hp': 500, 'max_hp': 500, 'room_phase': 'COMPLETE'},
            '_rng': {'ai': {'counter': 1}}, '_stock_direct': {
                'player': {'current_hp': 500, 'max_hp': 500}, 'monsters': [
                    {'id': 'Transient', 'current_hp': 999, 'max_hp': 999, 'dying': False, 'escaped': True}]}}


def test_escape_victory_subset_preserves_positive_enemy_hp_and_rng():
    stock = _stock()
    result = encounter_terminal_subset(stock, stock=True)
    assert result['encounter_won'] and result['monsters'][0]['hp'] == 999
    assert result['rng'] == stock['_rng']


@pytest.mark.parametrize('mutation', ['incomplete', 'alive_enemy', 'hp_disagrees'])
def test_stock_subset_rejects_unproved_victory(mutation):
    stock = copy.deepcopy(_stock())
    if mutation == 'incomplete':
        stock['game_state']['room_phase'] = 'COMBAT'
    elif mutation == 'alive_enemy':
        stock['_stock_direct']['monsters'][0]['escaped'] = False
    else:
        stock['game_state']['current_hp'] = 499
    with pytest.raises(ValueError, match='victory proof'):
        encounter_terminal_subset(stock, stock=True)
