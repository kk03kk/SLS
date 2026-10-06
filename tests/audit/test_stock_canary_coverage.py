from tools.check_act2_production_coverage import satisfies, witnessed_roles


def boundary(index, *, room=None, enemies=(), terminal=False, act=2):
    return {'step_index': index, 'act': act, 'floor': index + 17,
            'screen': 'MAP' if room else ('GAME_OVER' if terminal else 'COMBAT'),
            'terminal': terminal, 'terminal_reason': 'DEATH' if terminal else None,
            'chosen_action': {'kind': 'CHOOSE_MAP_NODE', 'node_id': 'N'} if room else None,
            'observation': {'map_nodes': [{'node_id': 'N', 'visible_room_type': room}] if room else [],
                            'enemies': [{'monster_id': e} for e in enemies]}}


def test_map_elite_entry_counts_a_survived_fight_but_not_event_taskmaster():
    rows = [boundary(0, room='ELITE'), boundary(1, enemies=['BOOK_OF_STABBING']),
            boundary(2, room='MONSTER'), boundary(3, enemies=['SNECKO']), boundary(4, terminal=True)]
    evidence = witnessed_roles(rows)
    assert satisfies('ACT2_ELITE_ENTRY', evidence)
    assert satisfies('ACT2_ORDINARY_FAILURE', evidence)
    event = witnessed_roles([boundary(0, room='EVENT'), boundary(1, enemies=['TASKMASTER']),
                             boundary(2, terminal=True)])
    assert not satisfies('ACT2_ELITE_ENTRY', event)
    assert not satisfies('ACT2_ORDINARY_FAILURE', event)


def test_act1_death_and_boss_death_do_not_count_as_act2_ordinary_failure():
    assert not witnessed_roles([boundary(0, room='MONSTER', act=1),
                               boundary(1, terminal=True, act=1)])['ordinary_failure']
    evidence = witnessed_roles([boundary(0, room='BOSS'), boundary(1, enemies=['THE_CHAMP']),
                               boundary(2, terminal=True)])
    assert satisfies('ACT2_BOSS_ENTRY:CHAMP', evidence)
    assert not satisfies('ACT2_BOSS_ENTRY:COLLECTOR', evidence)
    assert not satisfies('ACT2_ORDINARY_FAILURE', evidence)
