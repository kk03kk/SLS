"""Actual stock TheCity reward outcomes, with exact restore at each decision."""

import json
from pathlib import Path

import pytest

from sls.backends.simulator import native

FIXTURE = Path('tests/fixtures/regressions/act12-thief-city-rewards-131200180.json')


@pytest.mark.parametrize('index', range(15))
def test_stock_thief_reward_and_adjacent_branches(index):
    row = json.loads(FIXTURE.read_text())['runs'][index]
    run = native.LightspeedRunState()
    run.load_state(row['initial'])
    for bits in row['action_bits']:
        state = run.snapshot()
        actions = run.legal_actions()
        assert any(a['bits'] == bits for a in actions)
        restored = native.LightspeedRunState()
        restored.load_state(state)
        assert restored.snapshot() == state
        assert restored.legal_actions() == actions
        run.step(bits)
        restored.step(bits)
        assert restored.snapshot() == run.snapshot()
    state = run.snapshot()
    screen = state['public_screen']
    measured = {
        'potion_modifier': state['progress_state']['potion_chance'],
        'potion_rng': state['rng']['potion'],
        'potions': screen['potions'],
        'all_rng': state['rng'],
        'player_hp': state['player_state']['current_hp'],
        'player_gold': state['player_state']['gold'],
        'reward_gold': screen['gold'],
        'reward_relics': screen['relics'],
        'reward_cards': [[{'id': c['id'], 'upgrades': c['upgrades'], 'misc': c['special_data']}
                          for c in cards] for cards in screen['card_rewards']],
    }
    assert measured == row['expected']
