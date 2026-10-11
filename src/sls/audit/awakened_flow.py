"""Require witnessed Awakened One lifecycle before qualifying a double-boss route."""

from sls.content.normalize import normalize_monster_id


def _require_awakened_lifecycle(row):
    actions, states = row['actions'], row['boundaries']
    lethal = [i for i, a in enumerate(actions)
              if a.get('kind') == 'play'
              and a.get('target', {}).get('monster_id') == 'AWAKENED_ONE']
    if len(lethal) != 2:
        raise ValueError('requires two independently witnessed Awakened phase kills')
    first, final = lethal
    if len(states) != len(actions) + 1 or first + 1 >= len(actions):
        raise ValueError('incomplete lifecycle boundaries')
    if actions[first + 1].get('kind') != 'end_turn' or final != first + 2:
        raise ValueError('requires witnessed rebirth between phase kills')
    def owner(index):
        monsters = states[index]['game_state'].get('combat_state', {}).get('monsters', [])
        matches = [m for m in monsters if normalize_monster_id(m['id']) == 'AWAKENED_ONE']
        if len(matches) != 1:
            raise ValueError('missing unambiguous Awakened lifecycle state')
        return matches[0]
    dead, reborn = owner(first + 1), owner(first + 2)
    if (dead['current_hp'] != 0 or not dead.get('half_dead')
            or states[first + 1]['game_state']['screen_type'] != 'NONE'
            or reborn['current_hp'] != 320 or reborn['max_hp'] != 320
            or reborn.get('half_dead') or reborn.get('is_gone')):
        raise ValueError('phase death or rebirth differs from independent A20 stock obligation')
    won = states[final + 1]
    if (won['game_state']['screen_type'] != 'COMPLETE'
            or 'proceed' not in won['available_commands']
            or won.get('_stock_reward_state', {}).get('room_phase') != 'COMPLETE'):
        raise ValueError('final phase did not produce actual boss victory')
    return {'phase_one_boundary': first + 1, 'rebirth_boundary': first + 2,
            'final_victory_boundary': final + 1, 'rebirth_hp': 320}


def require_awakened_first_lifecycle(row):
    scene = row['scene']
    if scene['encounter'] != 'AWAKENED_ONE' or scene['ascension'] != 20:
        raise ValueError('requires explicit A20 Awakened first-boss scene')
    return _require_awakened_lifecycle(row)


def require_awakened_second_lifecycle(row):
    scene = row['scene']
    order = scene.get('initial', {}).get('boss_order', [])
    if (scene.get('ascension') != 20 or len(order) != 3
            or order[1] != 'AWAKENED_ONE' or scene['encounter'] != order[0]):
        raise ValueError('requires explicit A20 Awakened second-boss order')
    result = _require_awakened_lifecycle(row)
    proceeds = [i for i, a in enumerate(row['actions']) if a.get('kind') == 'proceed_to_second_boss']
    if len(proceeds) != 1 or proceeds[0] >= result['phase_one_boundary'] - 1:
        raise ValueError('Awakened second fight must follow witnessed first-boss proceed')
    for boundary in ('phase_one_boundary', 'rebirth_boundary', 'final_victory_boundary'):
        direct = row['boundaries'][result[boundary]].get('_stock_direct', {})
        if (direct.get('floor') != scene['floor'] + 1 or direct.get('boss_key') != 'Awakened One'
                or direct.get('ascension') != 20 or direct.get('dungeon_id') != 'TheBeyond'
                or direct.get('room_class') != 'com.megacrit.cardcrawl.rooms.MonsterRoomBoss'):
            raise ValueError('Awakened lifecycle is not in the actual second boss room')
    return result
