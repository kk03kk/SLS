"""Bounded late-cycle regressions; stock takeTurn/getMove define expected rules.

These are native regression tests, NOT new production stock captures.
Source identities: act2-scenes.json TheCollector/BronzeAutomaton entries.
"""
import pytest

from sls.backends.simulator import native


@pytest.mark.parametrize('artifact,expected', [(2, {'FRAIL': 5}), (3, {})])
def test_collector_mega_debuff_consumes_artifact_in_stock_order(artifact, expected):
    battle = native.LightspeedBattle()
    battle.reset(131100051, 'COLLECTOR', 20, relics=['CLOCKWORK_SOUVENIR'], replace_relics=True)
    battle.set_player_health(5000, 5000)
    snapshot = battle.snapshot()
    state = snapshot['game_state']['combat_state']
    next(m for m in state['monsters'] if m['monster_id'] == 'THE_COLLECTOR')['move_id'] = 'THE_COLLECTOR_MEGA_DEBUFF'
    next(p for p in state['player']['powers'] if p['id'] == 'ARTIFACT')['amount'] = artifact
    battle.load_checkpoint({'game_state': snapshot['game_state'], 'rng': snapshot['_rng']})
    battle.step('end_turn')
    powers = {p['id']: p['amount'] for p in battle.snapshot()['game_state']['combat_state']['player']['powers']}
    assert {k: v for k, v in powers.items() if k in {'WEAK', 'VULNERABLE', 'FRAIL'}} == expected


def test_automaton_checkpoint_at_second_cycle_keeps_stock_a20_boost():
    battle = native.LightspeedBattle()
    battle.reset(131100057, 'AUTOMATON', 20)
    battle.set_player_health(5000, 5000)
    for turn in range(13):
        snapshot = battle.snapshot()
        boss = next(m for m in snapshot['game_state']['combat_state']['monsters'] if m['monster_id'] == 'BRONZE_AUTOMATON')
        if turn in (5, 11):
            assert boss['move_id'] == 'BRONZE_AUTOMATON_HYPER_BEAM'
        if turn in (6, 12):
            assert boss['move_id'] == 'BRONZE_AUTOMATON_BOOST'
        restored = native.LightspeedBattle()
        restored.load_checkpoint({'game_state': snapshot['game_state'], 'rng': snapshot['_rng']})
        battle.step('end_turn')
        restored.step('end_turn')
        assert battle.snapshot() == restored.snapshot()
