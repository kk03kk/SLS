import json
from pathlib import Path

import pytest

from sls.backends.simulator import native
from sls.content.normalize import normalize_monster_id

FIXTURE = json.loads((Path(__file__).parents[1] / 'fixtures/reptomancer-stock-public.json').read_text())


@pytest.mark.parametrize('case', FIXTURE['cases'], ids=lambda case: str(case['seed']))
def test_production_projection_has_stock_retained_corpses_without_mutating_checkpoint(case):
    battle = native.LightspeedBattle()
    battle.reset_encounter_probe(case['seed'], 'REPTOMANCER', case['initial_rng'], 20, 3, 40, 'public-repto')
    state = case['initial']
    battle.set_player_health(state['hp'], state['max_hp'])
    battle.set_card_piles(state['hand'], state['draw'], [], [])
    for _ in range(4):
        battle.step('end_turn')
    before = battle.snapshot()
    public = battle.public_combat_probe_snapshot()
    assert battle.snapshot() == before
    assert len(before['game_state']['combat_state']['monsters']) == 5
    assert len(public['monsters']) == len(case['expected']) == 7
    for actual, expected in zip(public['monsters'], case['expected'], strict=True):
        assert normalize_monster_id(actual['content_id']) == normalize_monster_id(expected['id'])
        assert (actual['current_hp'], actual['max_hp'], actual['block']) == (
            expected['current_hp'], expected['max_hp'], expected['block'])
