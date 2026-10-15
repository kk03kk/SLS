"""Witnessed dead-power classification cannot erase a live or RNG difference."""

import copy
import json

import pytest

from sls.audit.corpse_cleanup import classify_controlled_corpse_cleanup


def _shield():
    stock = {'direct': {'monsters': [{'id': 'SPIRE_SHIELD', 'current_hp': 0,
             'powers': [{'id': 'ARTIFACT', 'amount': 2}, {'id': 'BACK_ATTACK', 'amount': 1}]}]},
             'actual_actions': [], 'rng': {'ai': 123}}
    native = copy.deepcopy(stock)
    native['direct']['monsters'][0]['powers'] = [{'id': 'ARTIFACT', 'amount': 2}]
    raw = {'_stock_direct': {'monsters': [{'id': 'SpireShield', 'current_hp': 0, 'dying': True}]}}
    return stock, native, raw


def test_classification_keeps_the_original_raw_difference():
    stock, native, raw = _shield()
    before = copy.deepcopy(stock)
    assert classify_controlled_corpse_cleanup(stock, native, raw)
    assert stock == before and stock != native


@pytest.mark.parametrize('mutation', ['live', 'not_dying', 'rng', 'targetable', 'unknown_power'])
def test_classification_rejects_material_or_unwitnessed_changes(mutation):
    stock, native, raw = _shield()
    if mutation == 'live':
        raw['_stock_direct']['monsters'][0]['current_hp'] = 1
    elif mutation == 'not_dying':
        raw['_stock_direct']['monsters'][0]['dying'] = False
    elif mutation == 'rng':
        native['rng']['ai'] = 124
    elif mutation == 'targetable':
        native['actual_actions'] = [json.dumps({'kind': 'play', 'target_index': 0})]
    else:
        stock['direct']['monsters'][0]['powers'].append({'id': 'STRENGTH', 'amount': 1})
    assert classify_controlled_corpse_cleanup(stock, native, raw) is None


@pytest.mark.parametrize('size,leader,slots', [(5, 2, [1, 4]), (5, 2, [0, 3]), (7, 3, [0, 4])])
def test_reused_dagger_corpse_shapes_require_independent_death(size, leader, slots):
    stock = {'direct': {'monsters': [
        {'id': 'DAGGER', 'current_hp': 24, 'powers': []} for _ in range(size)]},
        'actual_actions': [], 'rng': {'ai': 123}}
    raw = {'_stock_direct': {'monsters': [
        {'id': 'Dagger', 'current_hp': 24, 'dying': False} for _ in range(size)]}}
    raw['_stock_direct']['monsters'][leader]['id'] = 'Reptomancer'
    for slot in slots:
        stock['direct']['monsters'][slot]['current_hp'] = 0
        raw['_stock_direct']['monsters'][slot].update(current_hp=0, dying=True)
    native = copy.deepcopy(stock)
    native['direct']['monsters'][slots[0]]['powers'] = [{'id': 'MINION', 'amount': 1}]
    assert classify_controlled_corpse_cleanup(stock, native, raw)
    raw['_stock_direct']['monsters'][slots[0]]['dying'] = False
    assert classify_controlled_corpse_cleanup(stock, native, raw) is None


@pytest.mark.parametrize('size', [3, 4])
@pytest.mark.parametrize('mutation', [None, 'live', 'not_dying', 'rng', 'targetable', 'unknown_power'])
def test_exploder_corpse_requires_death_and_no_other_difference(size, mutation):
    stock = {'direct': {'monsters': [
        {'id': 'REPULSOR', 'current_hp': 20, 'powers': []} for _ in range(size)]},
        'actual_actions': [], 'rng': {'ai': 123}}
    stock['direct']['monsters'][-1].update(
        id='EXPLODER', current_hp=0, powers=[{'id': 'EXPLOSIVE', 'amount': 1}])
    native = copy.deepcopy(stock)
    native['direct']['monsters'][-1]['powers'] = []
    raw = {'_stock_direct': {'monsters': [
        {'id': 'Repulsor', 'current_hp': 20, 'dying': False} for _ in range(size)]}}
    raw['_stock_direct']['monsters'][-1].update(id='Exploder', current_hp=0, dying=True)
    before = copy.deepcopy(stock)
    if mutation == 'live':
        raw['_stock_direct']['monsters'][-1]['current_hp'] = 1
    elif mutation == 'not_dying':
        raw['_stock_direct']['monsters'][-1]['dying'] = False
    elif mutation == 'rng':
        native['rng']['ai'] = 124
    elif mutation == 'targetable':
        native['actual_actions'] = [json.dumps({'kind': 'play', 'target_index': size - 1})]
    elif mutation == 'unknown_power':
        native['direct']['monsters'][-1]['powers'] = [{'id': 'STRENGTH', 'amount': 1}]
    result = classify_controlled_corpse_cleanup(stock, native, raw)
    assert bool(result) == (mutation is None)
    assert stock == before and stock != native
