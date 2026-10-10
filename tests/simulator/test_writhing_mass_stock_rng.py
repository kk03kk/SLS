import json
from pathlib import Path

import pytest

from sls.backends.simulator import native

FIXTURE = json.loads((Path(__file__).parents[1] / 'fixtures/writhing-fourturn-stock.json').read_text())


@pytest.mark.parametrize('case', FIXTURE['cases'], ids=lambda case: str(case['seed']))
def test_writhing_four_turn_damage_intent_and_ai_stream_match_stock(case):
    battle = native.LightspeedBattle()
    battle.reset_encounter_probe(case['seed'], 'WRITHING_MASS', case['initial_rng'],
                                 ascension=20, act=3, floor=40, scenario_id='stock-flail-repeat')
    initial = case['initial']
    battle.set_player_health(initial['hp'], initial['max_hp'])
    battle.set_card_piles(initial['hand'], initial['draw'], [], [])
    for index, expected in enumerate(case['expected']):
        if index:
            battle.step('end_turn')
        snapshot = battle.snapshot()
        combat = snapshot['game_state']['combat_state']
        monster = combat['monsters'][0]
        assert combat['player']['current_hp'] == expected['player_hp'], index
        assert monster['block'] == expected['block'], index
        # Stock uses -1 for the non-attacking Implant intent; native uses 0.
        # Both denote no attack, rather than negative damage.
        if expected['intent_damage'] >= 0:
            assert monster['move_adjusted_damage'] == expected['intent_damage'], index
            assert monster['move_hits'] == expected['intent_hits'], index
        else:
            assert monster['move_adjusted_damage'] <= 0, index
        assert snapshot['_rng']['ai'] == expected['ai'], index
