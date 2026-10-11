"""Stock Implant -> actual lethal -> stable rewards, with exact checkpoint replay."""

import json
from pathlib import Path

import pytest

from sls.backends.simulator import native
from tests.simulator.test_writhing_implant_stock import measured

CASES = json.loads((Path(__file__).parents[1] /
                   'fixtures/regressions/writhing-implant-victory-stock.json').read_text())['cases']


@pytest.mark.parametrize('case', CASES, ids=lambda case: str(case['seed']))
def test_implant_victory_matches_stock_and_restores(case):
    run = native.LightspeedRunState()
    run.load_state(case['initial'])
    for index, expected in enumerate(case['expected']):
        state = run.snapshot()
        actual = measured(state)
        if index == len(case['expected']) - 1:
            screen = state['public_screen']
            actual['resources'].update({
                'reward_gold': screen.get('gold'),
                'reward_potions': screen.get('potions'),
                'reward_relics': screen.get('relics'),
                'reward_cards': [[{'id': c['id'], 'upgrades': c['upgrades'],
                                  'misc': c['special_data']} for c in cards]
                                 for cards in screen.get('card_rewards', [])],
                'potion_modifier': state['progress_state']['potion_chance'],
                'card_rarity_factor': state['progress_state']['card_rarity_factor'],
            })
        assert actual == expected, index
        restored = native.LightspeedRunState()
        restored.load_state(state)
        assert restored.snapshot() == state
        assert restored.legal_actions() == run.legal_actions()
        if index < len(case['actions']):
            bits = case['actions'][index]
            assert any(a['bits'] == bits for a in run.legal_actions())
            run.step(bits)
            restored.step(bits)
            assert restored.snapshot() == run.snapshot()
