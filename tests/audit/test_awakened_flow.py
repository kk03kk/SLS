import copy

import pytest

from sls.audit.awakened_flow import (
    require_awakened_first_lifecycle,
    require_awakened_second_lifecycle,
)


def row():
    def state(hp, half_dead):
        return {'game_state': {'screen_type': 'NONE', 'combat_state': {'monsters': [
            {'id': 'AwakenedOne', 'current_hp': hp, 'max_hp': 320,
             'half_dead': half_dead, 'is_gone': half_dead}]}}, 'available_commands': ['play', 'end']}
    attack = {'kind': 'play', 'target': {'monster_id': 'AWAKENED_ONE', 'alive_ordinal': 0}}
    return {'scene': {'encounter': 'AWAKENED_ONE', 'ascension': 20},
            'actions': [attack, {'kind': 'end_turn'}, copy.deepcopy(attack)],
            'boundaries': [state(320, False), state(0, True), state(320, False),
                           {'game_state': {'screen_type': 'COMPLETE'}, 'available_commands': ['proceed'],
                            '_stock_reward_state': {'room_phase': 'COMPLETE'}}]}


def test_requires_two_phases_and_rebirth():
    assert require_awakened_first_lifecycle(row()) == {
        'phase_one_boundary': 1, 'rebirth_boundary': 2, 'final_victory_boundary': 3, 'rebirth_hp': 320}


@pytest.mark.parametrize('field,value', [('current_hp', 1), ('half_dead', False)])
def test_reject_false_phase_death(field, value):
    r = row()
    r['boundaries'][1]['game_state']['combat_state']['monsters'][0][field] = value
    with pytest.raises(ValueError):
        require_awakened_first_lifecycle(r)


@pytest.mark.parametrize('field,value', [('current_hp', 300), ('max_hp', 300),
                                       ('half_dead', True), ('is_gone', True)])
def test_reject_incomplete_or_wrong_ascension_rebirth(field, value):
    r = row()
    r['boundaries'][2]['game_state']['combat_state']['monsters'][0][field] = value
    with pytest.raises(ValueError):
        require_awakened_first_lifecycle(r)


def test_reject_first_phase_as_final_victory():
    r = row()
    r['boundaries'][3] = copy.deepcopy(r['boundaries'][1])
    with pytest.raises(ValueError):
        require_awakened_first_lifecycle(r)


def test_reject_missing_boundaries_or_only_one_phase():
    r = row()
    r['boundaries'].pop()
    with pytest.raises(ValueError):
        require_awakened_first_lifecycle(r)
    r = row()
    r['actions'][2]['target']['monster_id'] = 'CULTIST'
    with pytest.raises(ValueError):
        require_awakened_first_lifecycle(r)


def second_row():
    r = row()
    r['scene'].update(encounter='TIME_EATER', floor=50,
                      initial={'boss_order': ['TIME_EATER', 'AWAKENED_ONE', 'DONU_AND_DECA']})
    for p in r['boundaries']:
        p['_stock_direct'] = {'floor': 51, 'boss_key': 'Awakened One', 'ascension': 20,
                              'dungeon_id': 'TheBeyond',
                              'room_class': 'com.megacrit.cardcrawl.rooms.MonsterRoomBoss'}
    r['actions'] = [{'kind': 'play', 'target': {'monster_id': 'TIME_EATER'}},
                    {'kind': 'proceed_to_second_boss'}] + r['actions']
    r['boundaries'] = [{}, {}] + r['boundaries']
    return r


def test_second_lifecycle_requires_actual_second_room():
    assert require_awakened_second_lifecycle(second_row()) == {
        'phase_one_boundary': 3, 'rebirth_boundary': 4, 'final_victory_boundary': 5, 'rebirth_hp': 320}


@pytest.mark.parametrize('field,value', [('floor', 50), ('boss_key', 'Time Eater'),
                                       ('ascension', 0), ('dungeon_id', 'Exordium'),
                                       ('room_class', 'com.megacrit.cardcrawl.rooms.MonsterRoom')])
def test_second_lifecycle_rejects_wrong_actual_context(field, value):
    r = second_row()
    r['boundaries'][4]['_stock_direct'][field] = value
    with pytest.raises(ValueError):
        require_awakened_second_lifecycle(r)


def test_second_lifecycle_rejects_missing_proceed_and_wrong_order():
    r = second_row()
    r['actions'][1]['kind'] = 'end_turn'
    with pytest.raises(ValueError):
        require_awakened_second_lifecycle(r)
    r = second_row()
    r['scene']['initial']['boss_order'][1] = 'DONU_AND_DECA'
    with pytest.raises(ValueError):
        require_awakened_second_lifecycle(r)
