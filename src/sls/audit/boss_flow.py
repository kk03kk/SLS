"""Validate initial boss fixtures and wait for stock's second combat boundary."""

import time

from sls.audit.act2_differential import pending_stock_intents

BOSSES = {'TIME_EATER': 'Time Eater', 'AWAKENED_ONE': 'Awakened One',
          'DONU_AND_DECA': 'Donu and Deca'}


def validate_boss_order(scene):
    initial = scene.get('initial', {})
    if 'boss_order' not in initial:
        if 'flow_master_deck' in initial or 'keys' in initial or 'card_rng_counter' in initial or scene.get('collect_act4_entry'):
            raise ValueError('flow deck/keys require reviewed boss order; no game launched')
        return
    order = initial['boss_order']
    if (scene.get('act') != 3 or scene.get('room') != 'BOSS'
            or scene.get('ascension') != 20 or not scene.get('actual_dungeon_required')
            or not isinstance(order, list) or len(order) != 3
            or any(not isinstance(v, str) for v in order)
            or set(order) != set(BOSSES) or order[0] != scene.get('encounter')):
        raise ValueError('invalid A20 consumed-list boss fixture; no game launched')
    if 'card_rng_counter' in initial:
        counter = initial['card_rng_counter']
        if type(counter) is not int or counter not in (0, 250, 500, 750):
            raise ValueError('unreviewed boundary counter; no game launched')
    if 'keys' in initial:
        keys = initial['keys']
        if (not isinstance(keys, list) or any(not isinstance(k, str) for k in keys)
                or len(keys) != len(set(keys)) or set(keys) - {'RUBY', 'EMERALD', 'SAPPHIRE'}
                or initial.get('final_act_available') is not True):
            raise ValueError('unreviewed initial keys or final-act eligibility; no game launched')
    if scene.get('collect_act4_entry') and set(initial.get('keys', [])) != {'RUBY', 'EMERALD', 'SAPPHIRE'}:
        raise ValueError('Act4 entry requires all three witnessed initial keys')
    if 'flow_master_deck' in initial:
        deck = initial['flow_master_deck']
        if (not isinstance(deck, list) or not 5 <= len(deck) <= 30
                or any(spec != 'Searing Blow+30' for spec in deck)):
            raise ValueError('unreviewed controlled flow deck; no game launched')


def require_initial_boss_witness(payload, scene):
    validate_boss_order(scene)
    if 'keys' in scene['initial']:
        expected = scene['initial']['keys']
        if any(payload.get('_parity_run', {}).get(k.lower() + '_key') != (k in expected)
               for k in ('RUBY', 'EMERALD', 'SAPPHIRE')):
            raise ValueError('stock initial key flags differ from manifest')
    if 'card_rng_counter' in scene['initial']:
        if payload.get('_rng', {}).get('card', {}).get('counter') != scene['initial']['card_rng_counter']:
            raise ValueError('stock initial card RNG counter differs from manifest')
    order = scene['initial']['boss_order']
    direct = payload.get('_stock_direct', {})
    if (payload.get('_oracle_mode') != 'validation'
            or direct.get('boss_flow_evidence_schema') != 'sls-stock-boss-flow-v1'
            or direct.get('ascension') != 20 or direct.get('dungeon_id') != 'TheBeyond'
            or direct.get('floor') != scene['floor']
            or direct.get('room_class') != 'com.megacrit.cardcrawl.rooms.MonsterRoomBoss'
            or direct.get('boss_key') != BOSSES[order[0]]
            or direct.get('remaining_bosses') != [BOSSES[b] for b in order[1:]]):
        raise ValueError('stock initial boss context differs from manifest')


def collect_second_boss_boundary(get_state, *, first_floor, second, third, timeout=20,
                                 clock=time.monotonic, sleep=time.sleep):
    if second not in BOSSES or third not in BOSSES or second == third or not 0 < timeout <= 30:
        raise ValueError('invalid boss boundary request')
    deadline = clock() + timeout
    for _ in range(600):
        if clock() >= deadline:
            break
        payload = get_state()
        direct = payload.get('_stock_direct', {})
        if (payload.get('_oracle_mode') != 'validation'
                or direct.get('boss_flow_evidence_schema') != 'sls-stock-boss-flow-v1'):
            raise ValueError('missing independent boss flow evidence')
        if direct.get('floor', first_floor) > first_floor + 1:
            raise ValueError('missed second boss boundary')
        if (direct.get('floor') == first_floor + 1
                and direct.get('dungeon_id') == 'TheBeyond'
                and direct.get('ascension') == 20
                and direct.get('room_class') == 'com.megacrit.cardcrawl.rooms.MonsterRoomBoss'
                and direct.get('boss_key') == BOSSES[second]
                and direct.get('remaining_bosses') == [BOSSES[third]]
                and direct.get('phase') == 'WAITING_ON_USER'
                and payload.get('game_state', {}).get('screen_type') == 'NONE'
                and payload.get('game_state', {}).get('combat_state', {}).get('monsters')
                and not pending_stock_intents(payload)
                and 'play' in payload.get('available_commands', [])):
            return payload
        sleep(0.05)
    raise TimeoutError('second boss did not stabilize; execution failure')


def collect_first_boss_victory(get_state, scene, *, timeout=20,
                               clock=time.monotonic, sleep=time.sleep):
    if not 0 < timeout <= 30:
        raise ValueError('invalid victory boundary timeout')
    deadline = clock() + timeout
    for _ in range(600):
        if clock() >= deadline:
            break
        payload = get_state()
        require_initial_boss_witness(payload, scene)
        if (payload.get('_stock_reward_state', {}).get('room_phase') == 'COMPLETE'
                and payload.get('game_state', {}).get('screen_type') == 'COMPLETE'
                and 'proceed' in payload.get('available_commands', [])):
            return payload
        sleep(0.05)
    raise TimeoutError('first boss victory did not stabilize; execution failure')


def collect_second_boss_victory(get_state, scene, *, timeout=20,
                                clock=time.monotonic, sleep=time.sleep):
    validate_boss_order(scene)
    if not 0 < timeout <= 30:
        raise ValueError('invalid second victory timeout')
    order = scene['initial']['boss_order']
    deadline = clock() + timeout
    for _ in range(600):
        if clock() >= deadline:
            break
        payload = get_state()
        direct = payload.get('_stock_direct', {})
        if (payload.get('_oracle_mode') != 'validation'
                or direct.get('boss_flow_evidence_schema') != 'sls-stock-boss-flow-v1'
                or direct.get('ascension') != 20 or direct.get('dungeon_id') != 'TheBeyond'
                or direct.get('room_class') != 'com.megacrit.cardcrawl.rooms.MonsterRoomBoss'
                or direct.get('floor') != scene['floor'] + 1
                or direct.get('boss_key') != BOSSES[order[1]]
                or direct.get('remaining_bosses') != [BOSSES[order[2]]]):
            raise ValueError('second boss victory context differs from manifest')
        if (payload.get('_stock_reward_state', {}).get('room_phase') == 'COMPLETE'
                and payload.get('game_state', {}).get('screen_type') == 'COMPLETE'
                and 'proceed' in payload.get('available_commands', [])):
            return payload
        sleep(0.05)
    raise TimeoutError('second boss victory did not stabilize; execution failure')


def collect_victory_room_entry(get_state, scene, *, timeout=20,
                               clock=time.monotonic, sleep=time.sleep):
    validate_boss_order(scene)
    if not 0 < timeout <= 30:
        raise ValueError('invalid victory room timeout')
    deadline = clock() + timeout
    for _ in range(600):
        if clock() >= deadline:
            break
        payload = get_state()
        direct = payload.get('_stock_direct', {})
        if (payload.get('_oracle_mode') != 'validation'
                or direct.get('boss_flow_evidence_schema') != 'sls-stock-boss-flow-v1'):
            raise ValueError('missing independent victory room evidence')
        if direct.get('floor', scene['floor']) > scene['floor'] + 2:
            raise ValueError('missed victory room boundary')
        if (direct.get('ascension') == 20 and direct.get('dungeon_id') == 'TheBeyond'
                and direct.get('floor') == scene['floor'] + 2
                and direct.get('room_class') == 'com.megacrit.cardcrawl.rooms.VictoryRoom'
                and payload.get('game_state', {}).get('screen_type') == 'EVENT'
                and 'choose' in payload.get('available_commands', [])):
            return payload
        sleep(0.05)
    raise TimeoutError('victory room entry did not stabilize; execution failure')


def collect_act4_map_entry(get_state, *, timeout=30, clock=time.monotonic, sleep=time.sleep):
    if not 0 < timeout <= 30:
        raise ValueError('invalid Act4 entry timeout')
    deadline = clock() + timeout
    for _ in range(600):
        if clock() >= deadline:
            break
        payload = get_state()
        direct = payload.get('_stock_direct', {})
        if payload.get('_oracle_mode') != 'validation' or direct.get('schema') != 'sls-stock-direct-v2':
            raise ValueError('missing independent Act4 context evidence')
        if (direct.get('ascension') == 20 and direct.get('act') == 4
                and direct.get('dungeon_id') == 'TheEnding'
                and direct.get('dungeon_class') == 'com.megacrit.cardcrawl.dungeons.TheEnding'
                and payload.get('game_state', {}).get('screen_type') == 'MAP'
                and 'choose' in payload.get('available_commands', [])):
            return payload
        sleep(0.05)
    raise TimeoutError('actual Act4 map did not stabilize; execution failure')
