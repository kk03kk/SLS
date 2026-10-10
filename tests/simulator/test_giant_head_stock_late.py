"""Actual original A20 late damage schedule, including its plateau."""

import json
from pathlib import Path

import pytest

from sls.backends.simulator import native

CASES = json.loads((Path(__file__).parents[1] / 'fixtures/giant-head-late-stock.json').read_text())['cases']


@pytest.mark.parametrize('case', CASES, ids=lambda case: str(case['seed']))
def test_late_attack_matches_stock_and_checkpoint_restore(case):
    battle = native.LightspeedBattle()
    battle.reset_encounter_probe(case['seed'], 'GIANT_HEAD', case['rng'], 20, 3, 40, case['scene_id'])
    initial = case['initial']
    battle.set_player_health(initial['hp'], initial['max_hp'])
    battle.set_card_piles(initial['hand'], initial['draw'], [], [])
    battle.set_potions([])
    state = battle.snapshot()
    state['game_state']['combat_state']['_internal']['potion_capacity'] = 2
    battle.load_checkpoint({'game_state': state['game_state'], 'rng': state['_rng']})
    for index, expected in enumerate(case['expected']):
        state = battle.snapshot()
        combat = state['game_state']['combat_state']
        monster = combat['monsters'][0]
        assert {'hp': combat['player']['current_hp'], 'block': combat['player']['block'],
                # Non-attack diagnostic sentinels differ (native0/stock-1).
                # Attack damage itself remains an exact original-value check.
                'damage': monster['move_base_damage'] if 'ATTACK' in monster['intent'] else -1,
                'intent': monster['intent'],
                'rng': state['_rng']} == expected, index
        if index == len(case['expected']) - 1:
            break
        restored = native.LightspeedBattle()
        restored.load_checkpoint({'game_state': state['game_state'], 'rng': state['_rng']})
        assert restored.snapshot() == state
        restored.step('end_turn')
        battle.step('end_turn')
        assert restored.snapshot() == battle.snapshot()
