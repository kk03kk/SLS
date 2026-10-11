"""One-survivor stock traces, including declared unnormalized corpse residue."""
import json
from pathlib import Path

import pytest

from sls.audit.act2_differential import direct_projection, production_combat_projection
from sls.audit.card_parity import structured_differences
from sls.backends.simulator import native


@pytest.mark.parametrize('case', json.loads((Path(__file__).parents[1] /
                         'fixtures/ending-death-stock.json').read_text())['cases'],
                         ids=lambda case: str(case['seed']))
def test_survivor_rules_match_stock_with_explicit_dead_shield_residue(case):
    battle = native.LightspeedBattle()
    battle.reset_encounter_probe(case['seed'], case['encounter'], case['initial_rng'],
                                 20, 4, case['floor'], case['scene_id'])
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
        current = production_combat_projection(battle)
        if expected['declared_residue']:
            corpse = current['game_state']['combat_state']['monsters'][0]
            assert corpse['current_hp'] == 0 and corpse['is_gone'] and not corpse['half_dead']
        assert structured_differences(expected['direct'], direct_projection(
            current, stock=False, extended=True)) == expected['declared_residue'], index
        assert current['_rng'] == expected['rng'], index
