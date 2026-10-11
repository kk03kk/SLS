import copy

import pytest

from sls.audit.corpse_cleanup import classify_deca_artifact_cleanup


def pair():
    stock = {'direct': {'monsters': [{'id': 'DECA', 'current_hp': 0, 'powers': []}]},
             'actual_actions': ['{"kind":"end_turn"}'], 'rng': {'counter': 1}}
    native = copy.deepcopy(stock)
    native['direct']['monsters'][0]['powers'] = [{'id': 'ARTIFACT', 'amount': 3}]
    return stock, native


def test_classification_preserves_raw_difference():
    stock, native = pair()
    before = copy.deepcopy(native)
    assert classify_deca_artifact_cleanup(stock, native)
    assert stock != native and native == before


@pytest.mark.parametrize('field,value', [('id', 'AWAKENED_ONE'), ('current_hp', 1),
                                       ('powers', [{'id': 'STRENGTH', 'amount': 3}])])
def test_reject_other_enemy_live_power_or_callback(field, value):
    stock, native = pair()
    native['direct']['monsters'][0][field] = value
    assert classify_deca_artifact_cleanup(stock, native) is None


def test_reject_rng_difference_or_dead_target_action():
    stock, native = pair()
    native['rng']['counter'] = 2
    assert classify_deca_artifact_cleanup(stock, native) is None
    stock, native = pair()
    native['actual_actions'] = ['{"kind":"play","target_index":0}']
    assert classify_deca_artifact_cleanup(stock, native) is None
