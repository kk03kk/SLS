"""Original final room resources/RNG after actual two-boss actions."""

import json
from pathlib import Path

import pytest

from sls.backends.simulator import native

CASES = json.loads((Path(__file__).parents[1] /
                   'fixtures/regressions/double-boss-terminal-stock.json').read_text())['cases']


@pytest.mark.parametrize('case', CASES, ids=lambda c: str(c['seed']))
def test_two_boss_final_room_matches_original(case):
    run = native.LightspeedRunState()
    run.load_state(case['initial'])
    for bits in (0, 0, 2147483648, 65536):
        run.step(bits)
    state = run.snapshot()
    actual = {'floor': state['run_state']['floor'],
              'hp': state['player_state']['current_hp'], 'max_hp': state['player_state']['max_hp'],
              'gold': state['player_state']['gold'], 'rng': state['rng'],
              'deck_ids_and_upgrades': [{'id': c['id'], 'upgrades': c['upgrades']}
                                       for c in state['public_inventory']['deck']]}
    assert actual == case['expected_final']
    assert state['progress_state']['outcome'] == 2
    restored = native.LightspeedRunState()
    restored.load_state(state)
    assert restored.snapshot() == state
    assert not restored.legal_actions()
