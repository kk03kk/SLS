import json
from pathlib import Path

import pytest

from sls.backends.simulator import native

FIXTURE = json.loads((Path(__file__).parents[1] / 'fixtures/writhing-flail-stock.json').read_text())


@pytest.mark.parametrize('case', FIXTURE['cases'], ids=lambda case: str(case['seed']))
def test_a20_flail_block_matches_independent_stock(case):
    battle = native.LightspeedBattle()
    battle.reset_encounter_probe(case['seed'], 'WRITHING_MASS', case['initial_rng'],
                                 ascension=20, act=3, floor=40, scenario_id='stock-flail-block')
    initial = case['initial']
    battle.set_player_health(initial['hp'], initial['max_hp'])
    battle.set_card_piles(initial['hand'], initial['draw'], [], [])
    for _ in range(case['flail_boundary']):
        battle.step('end_turn')
    monster = battle.snapshot()['game_state']['combat_state']['monsters'][0]
    assert monster['block'] == case['expected_block']
