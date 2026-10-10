"""Original Donu/Deca one-survivor observations."""
import json
from pathlib import Path

import pytest

from sls.audit.act2_differential import (
    comparison_projection,
    production_combat_projection,
)
from sls.backends.simulator import native


@pytest.mark.parametrize('case', json.loads((Path(__file__).parents[1] /
                         'fixtures/donu-deca-deaths-stock.json').read_text())['cases'],
                         ids=lambda case: str(case['seed']))
def test_donu_deca_survivor_state_masks_and_rng_match_original(case):
    battle = native.LightspeedBattle()
    battle.reset_encounter_probe(case['seed'], case['encounter'], case['initial_rng'],
                                 20, 3, case['floor'], case['scene_id'])
    initial = case['initial']
    battle.set_player_health(initial['hp'], initial['max_hp'])
    payload = battle.snapshot()
    combat = payload['game_state']['combat_state']
    combat['player']['energy'] = initial['energy']
    combat['player']['block'] = initial['block']
    for monster in combat['monsters']:
        if monster['monster_id'] in initial['monster_hp']:
            monster['current_hp'] = initial['monster_hp'][monster['monster_id']]
    battle.load_checkpoint({'game_state': payload['game_state'], 'rng': payload['_rng']})
    battle.set_card_piles(initial['hand'], initial['draw'], [], [])
    battle.set_potions([])
    payload = battle.snapshot()
    payload['game_state']['combat_state']['_internal']['potion_capacity'] = 2
    battle.load_checkpoint({'game_state': payload['game_state'], 'rng': payload['_rng']})
    for index, expected in enumerate(case['expected']):
        if index:
            action = dict(case['actions'][index - 1])
            battle.step(action.pop('kind'), **action)
        projection = comparison_projection(production_combat_projection(battle),
                                           stock=False, extended_direct=True)
        assert {key: projection[key] for key in expected} == expected, index
