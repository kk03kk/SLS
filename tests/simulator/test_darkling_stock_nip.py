import json
from pathlib import Path

import pytest

from sls.backends.simulator import native

FIXTURE = json.loads((Path(__file__).parents[1] / 'fixtures/darkling-nip-stock.json').read_text())


@pytest.mark.parametrize('case', FIXTURE['cases'], ids=lambda case: str(case['seed']))
def test_darkling_intents_and_actual_damage_match_independent_stock_four_turns(case):
    battle = native.LightspeedBattle()
    battle.reset_encounter_probe(case['seed'], 'THREE_DARKLINGS', case['initial_rng'],
                                 ascension=20, act=3, floor=40, scenario_id='stock-nip')
    initial = case['initial']
    battle.set_player_health(initial['hp'], initial['max_hp'])
    battle.set_card_piles(initial['hand'], initial['draw'], [], [])
    for boundary, expected in enumerate(case['expected']):
        if boundary:
            battle.step('end_turn')
        combat = battle.snapshot()['game_state']['combat_state']
        assert combat['player']['current_hp'] == expected['hp'], boundary
        assert [m['move_adjusted_damage'] for m in combat['monsters']] == expected['intent_damage'], boundary
