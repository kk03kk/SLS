import json
from pathlib import Path

import pytest

from sls.backends.simulator import native

CASES = []
for filename in ('key-act4-entry-stock.json', 'key-act4-boundaries-stock.json'):
    CASES.extend(json.loads((Path(__file__).parents[1] /
                           'fixtures/regressions' / filename).read_text())['cases'])


@pytest.mark.parametrize('case', CASES, ids=lambda c: str(c['seed']))
def test_three_keys_actual_act4_entry_matches_stock(case):
    run = native.LightspeedRunState()
    run.load_state(case['initial'])
    for bits in case['bits']:
        assert any(a['bits'] == bits for a in run.legal_actions())
        run.step(bits)
    state = run.snapshot()
    actual = {'floor': state['run_state']['floor'], 'act': state['run_state']['act'],
              'hp': state['player_state']['current_hp'], 'max_hp': state['player_state']['max_hp'],
              'gold': state['player_state']['gold'], 'rng': state['rng'],
              'keys': state['player_state']['red_key'], 'emerald_key': state['player_state']['green_key'],
              'sapphire_key': state['player_state']['blue_key'],
              'deck_ids_and_upgrades': [{'id': c['id'], 'upgrades': c['upgrades']}
                                       for c in state['public_inventory']['deck']]}
    assert actual == case['expected_final']
    assert state['run_state']['act'] == 4 and state['progress_state']['outcome'] == 1
    restored = native.LightspeedRunState()
    restored.load_state(state)
    assert restored.snapshot() == state
    assert restored.legal_actions() == run.legal_actions()
    assert restored.legal_actions()
