"""Lethal card/potion order and post-victory RNG against original objects."""
import json
from pathlib import Path

import pytest

from sls.backends.simulator import native
from sls.content.normalize import normalize_potion_id


@pytest.mark.parametrize('case', json.loads((Path(__file__).parents[1] /
                         'fixtures/heart-lethal-stock.json').read_text())['cases'],
                         ids=lambda case: str(case['seed']))
def test_heart_lethal_terminal_resources_match_stock(case):
    battle = native.LightspeedBattle()
    battle.reset_encounter_probe(case['seed'], case['encounter'], case['initial_rng'],
                                 20, 4, case['floor'], case['scene_id'])
    initial = case['initial']
    battle.set_player_health(initial['hp'], initial['max_hp'])
    payload = battle.snapshot()
    combat = payload['game_state']['combat_state']
    combat['player']['energy'] = initial['energy']
    combat['player']['block'] = initial['block']
    combat['monsters'][0]['current_hp'] = initial['monster_hp']['CORRUPT_HEART']
    battle.load_checkpoint({'game_state': payload['game_state'], 'rng': payload['_rng']})
    battle.set_card_piles(initial['hand'], initial['draw'], [], [])
    battle.set_potions([initial['potion']] if 'potion' in initial else [])
    payload = battle.snapshot()
    payload['game_state']['combat_state']['_internal']['potion_capacity'] = 2
    battle.load_checkpoint({'game_state': payload['game_state'], 'rng': payload['_rng']})
    action = dict(case['actions'][0])
    battle.step(action.pop('kind'), **action)
    current = battle.snapshot()
    observed = {'outcome': current['outcome'], 'hp': current['game_state']['current_hp'],
                'max_hp': current['game_state']['max_hp'],
                'potions': [normalize_potion_id(row['id']) for row in current['game_state']['potions']],
                'rng': current['_rng']}
    assert current['_legal_actions'] == []
    assert observed == case['expected_terminal_resources']
