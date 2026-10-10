"""Complete combat prefix plus explicit ending subset; no reward-flow certification."""
import json
from pathlib import Path

import pytest

from sls.audit.act2_differential import (
    comparison_projection,
    production_combat_projection,
)
from sls.audit.semantic_actions import resolve_target
from sls.backends.simulator import native
from sls.content.normalize import normalize_monster_id

CASES = json.loads((Path(__file__).parents[1] / 'fixtures/late-encounter-terminal-stock.json').read_text())['cases']


@pytest.mark.parametrize('case', CASES, ids=lambda case: str(case['seed']))
def test_original_late_encounter_prefix_and_ending_subset(case):
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
        if monster['monster_id'] in initial.get('monster_hp', {}):
            monster['current_hp'] = initial['monster_hp'][monster['monster_id']]
    battle.load_checkpoint({'game_state': payload['game_state'], 'rng': payload['_rng']})
    battle.set_card_piles(initial['hand'], initial['draw'], [], [])
    battle.set_potions([])
    payload = battle.snapshot()
    payload['game_state']['combat_state']['_internal']['potion_capacity'] = 2
    battle.load_checkpoint({'game_state': payload['game_state'], 'rng': payload['_rng']})
    for index, action in enumerate(case['actions']):
        prior = production_combat_projection(battle)
        projection = comparison_projection(prior, stock=False, extended_direct=True)
        assert {key: projection[key] for key in case['expected_prefix'][index]} == case['expected_prefix'][index], index
        action = resolve_target(action, prior['game_state']['combat_state']['monsters'])
        if 'target_index' in action:
            action['target_index'] = int(prior['game_state']['combat_state']['monsters'][
                action['target_index']]['instance_id'].split(':')[1])
        battle.step(action.pop('kind'), **action)
    current = battle.snapshot()
    retained = battle.public_combat_probe_snapshot()
    observed = {'encounter_won': current['outcome'] == 'PLAYER_VICTORY',
                'hp': current['game_state']['current_hp'],
                'max_hp': current['game_state']['max_hp'], 'rng': current['_rng'],
                'monsters': [{'id': normalize_monster_id(m['content_id']), 'hp': m['current_hp'],
                              'max_hp': m['max_hp']} for m in retained['monsters']]}
    assert not current['_legal_actions']
    assert observed == case['expected_terminal_subset']
