import copy

import pytest

from sls.audit.boss_flow import (
    BOSSES,
    collect_act4_map_entry,
    collect_first_boss_victory,
    collect_second_boss_boundary,
    collect_second_boss_victory,
    collect_victory_room_entry,
    require_initial_boss_witness,
    validate_boss_order,
)


def test_act3_boss_victory_uses_complete_not_reward_overlay():
    p = payload()
    p['available_commands'] = ['proceed']
    p['game_state']['screen_type'] = 'COMPLETE'
    p['_stock_reward_state'] = {'room_phase': 'COMPLETE'}
    assert collect_first_boss_victory(lambda: p, scene()) == p
    p['game_state']['screen_type'] = 'COMBAT_REWARD'
    ticks = iter([0, 0, 21])
    with pytest.raises(TimeoutError):
        collect_first_boss_victory(lambda: p, scene(), clock=lambda: next(ticks), sleep=lambda _: None)


def scene():
    return {'act': 3, 'floor': 50, 'room': 'BOSS', 'ascension': 20,
            'actual_dungeon_required': True, 'encounter': 'TIME_EATER',
            'initial': {'boss_order': list(BOSSES)}}


def payload():
    return {'_oracle_mode': 'validation', 'available_commands': ['play', 'end'],
            'game_state': {'screen_type': 'NONE', 'combat_state': {'monsters': [{'id': 'TimeEater'}]}},
            '_stock_direct': {
                'boss_flow_evidence_schema': 'sls-stock-boss-flow-v1', 'ascension': 20,
                'dungeon_id': 'TheBeyond', 'floor': 50, 'phase': 'WAITING_ON_USER',
                'room_class': 'com.megacrit.cardcrawl.rooms.MonsterRoomBoss',
                'boss_key': 'Time Eater', 'remaining_bosses': ['Awakened One', 'Donu and Deca']}}


@pytest.mark.parametrize('field,value', [('act', 2), ('ascension', 0), ('room', 'ELITE'),
                                       ('actual_dungeon_required', False), ('encounter', 'AWAKENED_ONE')])
def test_reject_wrong_initial_context(field, value):
    s = scene()
    s[field] = value
    with pytest.raises(ValueError):
        validate_boss_order(s)


@pytest.mark.parametrize('order', [[], ['TIME_EATER'] * 3, ['TIME_EATER', 'UNKNOWN', 'AWAKENED_ONE'],
                                 'TIME_EATER', [1, 2, 3], None])
def test_reject_invalid_order(order):
    s = scene()
    s['initial']['boss_order'] = order
    with pytest.raises(ValueError):
        validate_boss_order(s)


def test_initial_witness_and_tampering():
    require_initial_boss_witness(payload(), scene())
    for field, value in [('boss_key', 'Awakened One'), ('remaining_bosses', []),
                         ('floor', 51), ('ascension', 0), ('boss_flow_evidence_schema', None)]:
        p = payload()
        p['_stock_direct'][field] = value
        with pytest.raises(ValueError):
            require_initial_boss_witness(p, scene())


def test_controlled_initial_gold_requires_matching_actual_witness():
    s = scene()
    s['initial']['gold'] = 1000
    validate_boss_order(s)
    p = payload()
    p['game_state']['gold'] = 1000
    require_initial_boss_witness(p,s)
    p['game_state']['gold'] = 99
    with pytest.raises(ValueError,match='gold differs'):
        require_initial_boss_witness(p,s)


@pytest.mark.parametrize('gold',[True,99.0,1000.0,1001,-1])
def test_unreviewed_initial_gold_rejected(gold):
    s = scene()
    s['initial']['gold'] = gold
    with pytest.raises(ValueError,match='gold'):
        validate_boss_order(s)


def test_waits_for_real_second_boundary():
    first = payload()
    second = copy.deepcopy(first)
    second['_stock_direct'].update(floor=51, boss_key='Awakened One',
                                  remaining_bosses=['Donu and Deca'])
    states = iter([first, second])
    assert collect_second_boss_boundary(lambda: next(states), first_floor=50,
                                       second='AWAKENED_ONE', third='DONU_AND_DECA',
                                       sleep=lambda _: None) == second


def test_wrong_boundary_times_out_and_missing_evidence_rejected():
    ticks = iter([0, 0, 21])
    with pytest.raises(TimeoutError):
        collect_second_boss_boundary(payload, first_floor=50, second='AWAKENED_ONE',
                                     third='DONU_AND_DECA',
                                     clock=lambda: next(ticks), sleep=lambda _: None)
    with pytest.raises(ValueError):
        collect_second_boss_boundary(lambda: {}, first_floor=50, second='AWAKENED_ONE',
                                     third='DONU_AND_DECA')


@pytest.mark.parametrize('field,value', [('remaining_bosses', ['Time Eater']),
                                       ('phase', 'EXECUTING_ACTIONS'), ('ascension', 0)])
def test_does_not_accept_stale_or_mismatched_second_boundary(field, value):
    p = payload()
    p['_stock_direct'].update(floor=51, boss_key='Awakened One', remaining_bosses=['Donu and Deca'])
    p['_stock_direct'][field] = value
    ticks = iter([0, 0, 21])
    with pytest.raises(TimeoutError):
        collect_second_boss_boundary(lambda: p, first_floor=50, second='AWAKENED_ONE',
                                     third='DONU_AND_DECA', clock=lambda: next(ticks),
                                     sleep=lambda _: None)


def test_second_boundary_waits_for_stock_intent_materialization():
    p = payload()
    p['_stock_direct'].update(floor=51, boss_key='Awakened One', remaining_bosses=['Donu and Deca'])
    p['game_state']['combat_state']['monsters'][0].update(intent='DEBUG', current_hp=100)
    stable = copy.deepcopy(p)
    stable['game_state']['combat_state']['monsters'][0]['intent'] = 'ATTACK'
    states = iter([p, stable])
    assert collect_second_boss_boundary(lambda: next(states), first_floor=50,
                                       second='AWAKENED_ONE', third='DONU_AND_DECA',
                                       sleep=lambda _: None) == stable


@pytest.mark.parametrize('deck', [None, [], ['Searing Blow+30'] * 4,
                                 ['Searing Blow+30'] * 31, ['Anger'] * 10,
                                 ['Searing Blow+31'] * 10])
def test_flow_deck_rejects_unreviewed_initial_conditions(deck):
    s = scene()
    s['initial']['flow_master_deck'] = deck
    with pytest.raises(ValueError):
        validate_boss_order(s)


def test_flow_deck_requires_boss_fixture_and_accepts_reviewed_deck():
    s = scene()
    s['initial']['flow_master_deck'] = ['Searing Blow+30'] * 10
    validate_boss_order(s)
    del s['initial']['boss_order']
    with pytest.raises(ValueError):
        validate_boss_order(s)


def test_second_victory_rejects_first_boss_or_stale_list():
    p = payload()
    p['_stock_direct'].update(floor=51, boss_key='Awakened One', remaining_bosses=['Donu and Deca'])
    p['game_state']['screen_type'] = 'COMPLETE'
    p['_stock_reward_state'] = {'room_phase': 'COMPLETE'}
    p['available_commands'] = ['proceed']
    assert collect_second_boss_victory(lambda: p, scene()) == p
    p['_stock_direct']['remaining_bosses'] = ['Donu and Deca', 'Time Eater']
    with pytest.raises(ValueError):
        collect_second_boss_victory(lambda: p, scene())


def victory_room_payload():
    p = payload()
    p['_stock_direct'].update(floor=52, room_class='com.megacrit.cardcrawl.rooms.VictoryRoom')
    p['game_state']['screen_type'] = 'EVENT'
    p['available_commands'] = ['choose']
    return p


def test_victory_room_waits_for_actual_entry():
    states = iter([payload(), victory_room_payload()])
    assert collect_victory_room_entry(lambda: next(states), scene(), sleep=lambda _: None) == victory_room_payload()


@pytest.mark.parametrize('field,value', [('floor', 51), ('ascension', 0),
                                       ('room_class', 'com.megacrit.cardcrawl.rooms.MonsterRoomBoss')])
def test_victory_room_wrong_context_times_out(field, value):
    p = victory_room_payload()
    p['_stock_direct'][field] = value
    ticks = iter([0, 0, 21])
    with pytest.raises(TimeoutError):
        collect_victory_room_entry(lambda: p, scene(), clock=lambda: next(ticks), sleep=lambda _: None)


def test_victory_room_requires_diagnostics_and_rejects_skipped_boundary():
    with pytest.raises(ValueError):
        collect_victory_room_entry(lambda: {}, scene())
    p = victory_room_payload()
    p['_stock_direct']['floor'] = 53
    with pytest.raises(ValueError):
        collect_victory_room_entry(lambda: p, scene())


@pytest.mark.parametrize('field,value', [('screen_type', 'COMPLETE'), ('available_commands', ['proceed'])])
def test_victory_room_requires_stable_event_action(field, value):
    p = victory_room_payload()
    if field == 'screen_type':
        p['game_state'][field] = value
    else:
        p[field] = value
    ticks = iter([0, 0, 21])
    with pytest.raises(TimeoutError):
        collect_victory_room_entry(lambda: p, scene(), clock=lambda: next(ticks), sleep=lambda _: None)


@pytest.mark.parametrize('keys', [None, 'RUBY', ['RUBY', 'RUBY'], ['UNKNOWN'], [True]])
def test_reject_unreviewed_initial_keys(keys):
    s = scene()
    s['initial'].update(keys=keys, final_act_available=True)
    with pytest.raises(ValueError):
        validate_boss_order(s)


def test_initial_keys_require_context_eligibility_and_witness():
    s = scene()
    s['initial'].update(keys=['RUBY', 'EMERALD', 'SAPPHIRE'], final_act_available=True)
    s['collect_act4_entry'] = True
    p = payload()
    with pytest.raises(ValueError):
        require_initial_boss_witness(p, s)
    p['_parity_run'] = {'ruby_key': True, 'emerald_key': True, 'sapphire_key': True}
    require_initial_boss_witness(p, s)
    s['initial']['final_act_available'] = False
    with pytest.raises(ValueError):
        validate_boss_order(s)
    s['initial']['final_act_available'] = True
    s['initial']['keys'].pop()
    with pytest.raises(ValueError):
        validate_boss_order(s)


def act4_payload():
    p = payload()
    p['_stock_direct'].update(schema='sls-stock-direct-v2', act=4, dungeon_id='TheEnding',
                              dungeon_class='com.megacrit.cardcrawl.dungeons.TheEnding')
    p['game_state']['screen_type'] = 'MAP'
    p['available_commands'] = ['choose']
    return p


def test_actual_act4_map_required():
    p = act4_payload()
    assert collect_act4_map_entry(lambda: p) == p
    with pytest.raises(ValueError):
        collect_act4_map_entry(lambda: {})


@pytest.mark.parametrize('field,value', [('ascension', 0), ('act', 3), ('dungeon_id', 'TheBeyond'),
                                       ('dungeon_class', 'com.megacrit.cardcrawl.dungeons.TheBeyond')])
def test_act4_map_rejects_context_tags_without_actual_dungeon(field, value):
    p = act4_payload()
    p['_stock_direct'][field] = value
    ticks = iter([0, 0, 31])
    with pytest.raises(TimeoutError):
        collect_act4_map_entry(lambda: p, clock=lambda: next(ticks), sleep=lambda _: None)


@pytest.mark.parametrize('counter', [-1, 1, 250.5, True, '250', None])
def test_reject_unreviewed_boundary_rng_initial_condition(counter):
    s = scene()
    s['initial']['card_rng_counter'] = counter
    with pytest.raises(ValueError):
        validate_boss_order(s)


@pytest.mark.parametrize('counter', [0, 250, 500, 750])
def test_requested_initial_counter_requires_independent_witness(counter):
    s = scene()
    s['initial']['card_rng_counter'] = counter
    p = payload()
    with pytest.raises(ValueError, match='card RNG counter'):
        require_initial_boss_witness(p, s)
    p['_rng'] = {'card': {'counter': counter}}
    require_initial_boss_witness(p, s)
    p['_rng']['card']['counter'] = counter + 1
    with pytest.raises(ValueError, match='card RNG counter'):
        require_initial_boss_witness(p, s)
