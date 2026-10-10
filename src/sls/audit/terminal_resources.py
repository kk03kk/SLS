"""Bounded terminal resources: not a combat-state or full-run certificate."""
from __future__ import annotations

from sls.content.normalize import normalize_potion_id


def stock_terminal_resources(payload):
    game, direct = payload['game_state'], payload['_stock_direct']
    hp = direct['player']['current_hp']
    if (game.get('screen_type') == 'GAME_OVER' and hp == 0
            and game.get('screen_state', {}).get('victory') is False):
        outcome = 'PLAYER_LOSS'
    elif (game.get('screen_type') == 'COMPLETE' and game.get('room_phase') == 'COMPLETE'
          and hp > 0 and game.get('act') == 4
          and game.get('act_boss') == 'The Heart'):
        outcome = 'PLAYER_VICTORY'
    else:
        raise ValueError('not an independently witnessed stable Heart terminal boundary')
    if hp != game['current_hp'] or direct['player']['max_hp'] != game['max_hp']:
        raise ValueError('stock direct and public terminal HP disagree')
    return {'outcome': outcome, 'hp': hp, 'max_hp': direct['player']['max_hp'],
            'potions': [normalize_potion_id(row['id']) for row in direct['potions']],
            'rng': payload['_rng']}


def native_terminal_resources(payload):
    if payload['_legal_actions'] or payload['outcome'] not in {'PLAYER_LOSS', 'PLAYER_VICTORY'}:
        raise ValueError('native terminal must have an outcome and no legal actions')
    game = payload['game_state']
    return {'outcome': payload['outcome'], 'hp': game['current_hp'], 'max_hp': game['max_hp'],
            'potions': [normalize_potion_id(row['id']) for row in game['potions']],
            'rng': payload['_rng']}
