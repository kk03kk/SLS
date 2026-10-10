import json
from pathlib import Path

import pytest

from sls.audit.act2_differential import direct_projection, production_combat_projection
from sls.backends.simulator import native

FIXTURE_PATH = Path(__file__).parents[1] / 'fixtures/ending-interaction-stock.json'
LOSS_FIXTURE = Path(__file__).parents[1] / 'fixtures/heart-loss-stock.json'


@pytest.mark.parametrize('case', json.loads(FIXTURE_PATH.read_text())['cases'] +
                         json.loads(LOSS_FIXTURE.read_text())['cases'],
                         ids=lambda case: str(case['seed']))
def test_ending_controlled_mechanisms_match_independent_stock_state_and_rng(case):
    battle = native.LightspeedBattle()
    battle.reset_encounter_probe(case['seed'], case['encounter'], case['initial_rng'],
                                 20, 4, case['floor'], case['scene_id'])
    initial = case['initial']
    battle.set_player_health(initial['hp'], initial['max_hp'])
    payload = battle.snapshot()
    payload['game_state']['combat_state']['player']['energy'] = initial['energy']
    payload['game_state']['combat_state']['player']['block'] = initial['block']
    battle.load_checkpoint({'game_state': payload['game_state'], 'rng': payload['_rng']})
    battle.set_card_piles(initial['hand'], initial['draw'], [], [])
    battle.set_potions([initial['potion']] if 'potion' in initial else [])
    payload = battle.snapshot()
    payload['game_state']['combat_state']['_internal']['potion_capacity'] = 2
    battle.load_checkpoint({'game_state': payload['game_state'], 'rng': payload['_rng']})
    for index, expected in enumerate(case['expected']):
        if index:
            action = dict(case['actions'][index - 1])
            battle.step(action.pop('kind'), **action)
        current = production_combat_projection(battle)
        assert direct_projection(current, stock=False, extended=True) == expected['direct'], index
        assert current['_rng'] == expected['rng'], index
        if 'terminal_loss' in expected:
            assert (current['game_state']['outcome'] == 'PLAYER_LOSS') == expected['terminal_loss']
            if expected['terminal_loss']:
                assert current['_legal_actions'] == []
