"""Original permanent-obtain outcomes and exact restore at each player boundary."""

import copy
import json
from collections import Counter
from pathlib import Path

import pytest

from sls.backends.simulator import native

CASES = json.loads((Path(__file__).parents[1] /
                   'fixtures/regressions/writhing-implant-stock-131200198.json').read_text())['cases']


def measured(state):
    player = (state['combat_checkpoint']['game_state']['combat_state']['player']
              if 'combat_checkpoint' in state else state['player_state'])
    counters = {r['content_id']: r['counter'] for r in state['public_inventory']['relics']}
    values = {'hp': player['current_hp'], 'max_hp': player['max_hp'],
              'gold': player.get('_internal', {}).get('gold', state['player_state']['gold']),
              'rng': {**state['rng'], **state.get('combat_checkpoint', {}).get('rng', {})}}
    values.update({key: value for key, value in counters.items() if key in {'OMAMORI', 'DU_VU_DOLL'}})
    return {'deck': dict(Counter(c['id'] for c in state['public_inventory']['deck'])), 'resources': values}


@pytest.mark.parametrize('case', CASES, ids=lambda case: str(case['seed']))
def test_implant_matches_original_and_restores_exactly(case):
    run = native.LightspeedRunState()
    run.load_state(case['initial'])
    for index, expected in enumerate(case['expected']):
        state = run.snapshot()
        assert measured(state) == expected, index
        restored = native.LightspeedRunState()
        restored.load_state(state)
        assert restored.snapshot() == state
        assert restored.legal_actions() == run.legal_actions()
        if index == len(case['expected']) - 1:
            break
        run.step(2147483648)
        restored.step(2147483648)
        assert restored.snapshot() == run.snapshot()


@pytest.mark.parametrize('case', CASES, ids=lambda case: str(case['seed']))
def test_native_exit_guard_does_not_repeat_permanent_obtain(case):
    """Synthetic native-only finish; not claimed as a stock victory comparison."""
    run = native.LightspeedRunState()
    run.load_state(case['initial'])
    run.step(2147483648)
    state = run.snapshot()
    before = measured(state)
    battle = native.LightspeedBattle()
    battle.load_checkpoint(state['combat_checkpoint'])
    battle.set_card_piles(['Anger'], [], [], [])
    finishing = battle.snapshot()
    finishing['game_state']['combat_state']['monsters'][0]['current_hp'] = 1
    altered = copy.deepcopy(state)
    altered['combat_checkpoint'] = {'game_state': finishing['game_state'], 'rng': finishing['_rng']}
    run.load_state(altered)
    play = next(a for a in run.legal_actions() if a['bits'] == 0)
    run.step(play['bits'])
    after = measured(run.snapshot())
    assert after['deck'] == before['deck']
    for name in ('OMAMORI', 'DU_VU_DOLL'):
        if name in before['resources']:
            assert after['resources'][name] == before['resources'][name]
