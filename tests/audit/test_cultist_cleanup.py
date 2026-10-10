import copy
import json

import pytest

from sls.audit.corpse_cleanup import classify_cultist_ritual_cleanup


def pair():
    stock = {'direct': {'monsters': [
        {'id': 'CULTIST', 'current_hp': 0, 'powers': []},
        {'id': 'CULTIST', 'current_hp': 0, 'powers': []},
        {'id': 'AWAKENED_ONE', 'current_hp': 320, 'powers': []}]},
        'actual_actions': [json.dumps({'kind': 'play', 'target_index': 2})], 'rng': {'counter': 4}}
    own = copy.deepcopy(stock)
    own['direct']['monsters'][1]['powers'] = [{'id': 'RITUAL', 'amount': 5}]
    return stock, own


def test_narrow_classification_preserves_raw_difference():
    stock, own = pair()
    before = copy.deepcopy(own)
    assert classify_cultist_ritual_cleanup(stock, own).startswith('WITNESSED_DEAD_CULTIST')
    assert stock != own and own == before


@pytest.mark.parametrize('change', ['live', 'other_power', 'other_enemy', 'rng', 'dead_target'])
def test_reject_unwitnessed_or_material_difference(change):
    stock, own = pair()
    if change == 'live':
        own['direct']['monsters'][1]['current_hp'] = 1
    elif change == 'other_power':
        own['direct']['monsters'][1]['powers'][0]['amount'] = 4
    elif change == 'other_enemy':
        own['direct']['monsters'][1]['id'] = 'DARKLING'
    elif change == 'rng':
        own['rng']['counter'] = 5
    else:
        own['actual_actions'].append(json.dumps({'kind': 'play', 'target_index': 1}))
    assert classify_cultist_ritual_cleanup(stock, own) is None
