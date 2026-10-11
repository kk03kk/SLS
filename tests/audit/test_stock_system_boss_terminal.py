from copy import deepcopy

import pytest

from tools.replay_act2_system_batch import stock_terminal_outcome


def boss_terminal():
    return {'game_state': {'act': 2, 'screen_type': 'COMBAT_REWARD', 'current_hp': 45},
            '_stock_direct': {'room_class': 'com.megacrit.cardcrawl.rooms.MonsterRoomBoss',
                              'player': {'current_hp': 45},
                              'monsters': [{'id': 'Champ', 'current_hp': 0, 'dying': True}]},
            '_stock_reward_state': {'room_phase': 'COMPLETE'}}


def test_legacy_boss_success_requires_independent_stock_objects():
    assert stock_terminal_outcome(boss_terminal()) == {
        'terminal_reason': 'ACT_2_CLEARED', 'success': True}


@pytest.mark.parametrize('mutation', ['alive', 'ordinary', 'incomplete', 'wrong_act', 'dead_player'])
def test_adapter_terminal_is_insufficient_without_engine_boss_clear(mutation):
    raw = deepcopy(boss_terminal())
    if mutation == 'alive':
        raw['_stock_direct']['monsters'][0]['current_hp'] = 1
    elif mutation == 'ordinary':
        raw['_stock_direct']['room_class'] = 'MonsterRoom'
    elif mutation == 'incomplete':
        raw['_stock_reward_state']['room_phase'] = 'COMBAT'
    elif mutation == 'wrong_act':
        raw['game_state']['act'] = 1
    else:
        raw['_stock_direct']['player']['current_hp'] = 0
    assert stock_terminal_outcome(raw) is None
