"""CPU first-divergence replay of actual stock green-key action histories."""
from __future__ import annotations

import argparse
import copy
import hashlib
import json
import os
import subprocess
from dataclasses import asdict
from pathlib import Path

from sls.audit.act2_differential import direct_projection
from sls.backends.original.adapter import adapt_original
from sls.content.normalize import (
    normalize_card_id,
    normalize_content_id,
    normalize_monster_id,
    normalize_potion_id,
)
from sls.curriculum import IRONCLAD_A20_HEART
from tools.audit_green_key_capture import audit
from tools.replay_rest_key_archive import differences, native_resources, stock_resources
from tools.reproduce_double_boss_entry import enum_ids

ROOT = Path(__file__).resolve().parents[1]


def active_resources(state):
    result = native_resources(state)
    if state.get('public_combat'):
        player = state['public_combat']['player']
        result.update(hp=player['current_hp'], max_hp=player['max_hp'])
    return result


def stock_bottom_order(cards):
    # Stock CardGroup.addToBottom calls ArrayList.add(0, card).
    return list(reversed(cards))


def reward_projection(stock, state, cards, relics, potions):
    rows = stock['_stock_reward_state']['screen_rewards']
    if any(r['done'] or r['ignored'] or r['type'] not in {'GOLD', 'RELIC', 'CARD', 'EMERALD_KEY', 'POTION'} for r in rows):
        raise ValueError('unsupported stock reward boundary in frozen green corpus')
    expected = dict(gold=[r['gold'] + r['bonus_gold'] for r in rows if r['type'] == 'GOLD'],
                    relics=[normalize_content_id(r['relic']) for r in rows if r['type'] == 'RELIC'],
                    card_rewards=[[dict(id=normalize_card_id(c['id']), upgraded=bool(c['upgrades']), misc=c['misc'])
                                   for c in r['cards']] for r in rows if r['type'] == 'CARD'],
                    potions=[normalize_potion_id(r['potion']) for r in rows if r['type'] == 'POTION'],
                    emerald_key=any(r['type'] == 'EMERALD_KEY' for r in rows), sapphire_key=False)
    rewards = copy.deepcopy(state['screen_info']['rewards'])
    rewards['relics'] = [relics[r] for r in rewards['relics']]
    for group in rewards['card_rewards']:
        for card in group:
            card['id'] = cards[card['id']]
    rewards['potions'] = [potions[p] for p in rewards['potions']]
    return differences(expected, rewards)


def identify_encounter(stock):
    ids = {normalize_monster_id(m['id']) for m in stock['_stock_direct']['monsters']}
    if 'GREMLIN_LEADER' in ids:
        return 'GREMLIN_LEADER'
    if ids == {'BOOK_OF_STABBING'}:
        return 'BOOK_OF_STABBING'
    if ids == {'BLUE_SLAVER', 'TASKMASTER', 'RED_SLAVER'}:
        return 'SLAVERS'
    raise ValueError(f'unsupported natural stock elite combination: {sorted(ids)}')


def construct(row, map_row, native):
    stock, scene = row['boundaries'][0], row['scene']
    setup = scene['initial']
    include = ROOT / 'native/simulator/include/constants'
    cards = enum_ids(include / 'Cards.h', 'enum class CardId : std::uint16_t {')
    relics = enum_ids(include / 'Relics.h', 'enum class RelicId : std::uint8_t {')
    encounters = enum_ids(include / 'MonsterEncounters.h', 'enum class MonsterEncounter : std::int8_t {')
    run = native.LightspeedRunState()
    run.reset(row['seed'], 20)
    state = run.snapshot()
    floor = stock['_parity_run']['current_map_y'] + 18
    state['run_state'].update(act=2, floor=floor, burning_elite_x=map_row['x'],
                             burning_elite_y=map_row['y'], burning_elite_buff=map_row['buff'])
    state['derived_rng']['map'].update(act=2, derived_seed=row['seed'] + 200)
    encounter = identify_encounter(stock)
    state['progress_state'].update(screen_state=9, current_room=3, current_event=0,
                                  current_map_x=map_row['x'], current_map_y=map_row['y'],
                                  boss=encounters[adapt_original(stock).decision.observation.run.visible_boss_id],
                                  potion_chance=stock['_stock_reward_state']['potion_modifier'],
                                  card_rarity_factor=stock['_stock_reward_state']['card_rarity_factor'])
    state['screen_info'] = dict(screen_state=9, complete=True, encounter=encounters[encounter])
    # Condition persistent streams/pools, never copy generated monsters,
    # hand, draw order, powers, energy, outcome, or post-combat rewards.
    state['rng'] = copy.deepcopy(stock['_rng'])
    room_initial = native.rng_probe(row['seed'] + floor)['initial']
    for name in ('ai', 'shuffle', 'card_random', 'misc', 'monster_hp'):
        state['rng'][name] = dict(room_initial)
    state['player_state'].update(current_hp=setup['hp'], max_hp=setup['max_hp'], gold=setup['gold'],
                                 red_key=False, blue_key=False, green_key=False,
                                 deck=[dict(id=cards[normalize_card_id(c)], upgraded=False, misc=0)
                                       for c in stock_bottom_order(setup['deck'])],
                                 relics=[dict(id=relics[normalize_content_id(r)], data=0) for r in setup['relics']])
    for name, values in stock['_stock_direct']['ordered_relic_pools'].items():
        state['ordered_pools'][name] = [relics[normalize_content_id(r)] for r in values]
    run.load_state(state)
    return run, state, encounter


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--capture', type=Path, required=True)
    parser.add_argument('--oracle-build', type=Path, required=True)
    parser.add_argument('--manifest', type=Path, required=True)
    parser.add_argument('--map-probe', type=Path, required=True)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    if args.output.exists():
        raise FileExistsError('refuse to overwrite replay evidence')
    os.environ['CUDA_VISIBLE_DEVICES'] = '-1'
    source = audit(args.capture, args.oracle_build, args.manifest)
    maps = json.loads(args.map_probe.read_text())
    if maps['equal'] is not True or maps['native'] != maps['stock']:
        raise ValueError('independent native map constructor did not match stock')
    for name, digest in maps['inputs'].items():
        if hashlib.sha256((ROOT / name).read_bytes()).hexdigest() != digest:
            raise ValueError('map constructor source differs from measured probe')
    expanded = maps.get('producer') == 'sls-burning-map-range-v1'
    executable = ROOT / ('local/build/probe-burning-map-range-r1.exe' if expanded
                         else 'local/build/probe-green-map-r1.exe')
    command = [str(executable), '131200370', '32'] if expanded else [str(executable)]
    produced = json.loads(subprocess.check_output(command, timeout=30))
    expected_produced = maps['scan'] if expanded else maps['native']
    if (hashlib.sha256(executable.read_bytes()).hexdigest() != maps['executable_sha256']
            or produced != expected_produced):
        raise ValueError('map constructor executable identity or output mismatch')
    if expanded:
        by_seed = {r['seed']:r for r in produced}
        selected = [{k:by_seed[r['seed']][k] for k in ('seed','x','y','buff')} for r in maps['native']]
        if selected != maps['native']:
            raise ValueError('map constructor selection differs from measured range')
    from sls.backends.simulator import SimulatorBackend, native
    from sls.rl.training_contract import native_source_digest
    if native.NATIVE_SOURCE_SHA256 != native_source_digest():
        raise ValueError('stale native binary')
    capture = json.loads(args.capture.read_text())
    map_rows = {r['seed']: r for r in maps['native']}
    results = []
    include = ROOT / 'native/simulator/include/constants'
    cards = {v:k for k,v in enum_ids(include / 'Cards.h', 'enum class CardId : std::uint16_t {').items()}
    relics = {v:k for k,v in enum_ids(include / 'Relics.h', 'enum class RelicId : std::uint8_t {').items()}
    potions = {v:k for k,v in enum_ids(include / 'Potions.h', 'enum class Potion : std::uint8_t {').items()}
    for row in capture['runs']:
        run, initial, encounter = construct(row, map_rows[row['seed']], native)
        backend = SimulatorBackend(profile=IRONCLAD_A20_HEART)
        result = dict(seed=row['seed'], encounter=encounter, initial=initial, boundaries=[],
                      first_divergence=None, status='IN_PROGRESS')
        results.append(result)
        if map_rows[row['seed']]['buff'] == 1:
            # Independent native no-buff constructor, with identical RNG and
            # scene inputs. Never derive baseHP by undoing observed stockHP.
            unbuffed, _, _ = construct(row, dict(map_rows[row['seed']], buff=-1), native)
            base = unbuffed.snapshot()['public_combat']['monsters']
            stock_monsters = row['boundaries'][0]['_stock_direct']['monsters']
            expected_hp = [m['max_hp'] + (m['max_hp'] + 2) // 4 for m in base]
            actual_hp = [m['max_hp'] for m in stock_monsters]
            result['hp_buff_constructor'] = dict(native_base_hp=[m['max_hp'] for m in base],
                                                 independently_expected_hp=expected_hp,
                                                 stock_hp=actual_hp, equal=expected_hp == actual_hp)
        previous_observation = None
        for index, stock in enumerate(row['boundaries']):
            current = run.snapshot()
            decision = backend._adapt(current)
            original = adapt_original(stock).decision
            rng = {**current['rng'], **current.get('combat_checkpoint', {}).get('rng', {})}
            checks = dict(observation=differences(asdict(original.observation), asdict(decision.observation)),
                          legal_actions=differences([asdict(a) for a in original.actions],
                                                    [asdict(a) for a in decision.actions]),
                          resources=differences(stock_resources(stock), active_resources(current)),
                          rng=differences(stock['_rng'], rng))
            checks['terminal'] = differences(original.terminal, decision.terminal)
            if index == 0 and 'hp_buff_constructor' in result:
                hp = result['hp_buff_constructor']
                checks['hp_constructor'] = differences(hp['independently_expected_hp'], hp['stock_hp'])
            if previous_observation is not None:
                transition = backend._transition_from_raw(previous_observation, current)
                checks['transition_info'] = differences(row['actions'][index - 1]['info'],
                                                        {k:transition.info[k] for k in ('reason', 'success')})
            checks['ordered_pools'] = differences(
                {name:[normalize_content_id(v) for v in values] for name, values in
                 stock['_stock_direct']['ordered_relic_pools'].items()},
                {name:[relics[v] for v in current['ordered_pools'][name]] for name in
                 stock['_stock_direct']['ordered_relic_pools']})
            if current['progress_state']['screen_state'] == 2 and not decision.terminal:
                checks['rewards'] = reward_projection(stock, current, cards, relics, potions)
            restored = native.LightspeedRunState()
            restored.load_state(current)
            boundary = dict(index=index, differences=checks, checkpoint_restored_equal=restored.snapshot() == current,
                            native=current)
            result['boundaries'].append(boundary)
            if 'combat_checkpoint' in current:
                battle = native.LightspeedBattle()
                battle.load_checkpoint(current['combat_checkpoint'])
                boundary['raw_direct_differences'] = differences(
                    direct_projection(stock, stock=True),
                    direct_projection(battle.snapshot(), stock=False, extended=True))
            else:
                boundary['raw_direct_comparison'] = 'NATIVE_BATTLE_SERIALIZER_ABSENT_AT_REWARD_BOUNDARY'
            if any(checks.values()) or not boundary['checkpoint_restored_equal']:
                result.update(first_divergence=dict(boundary=index, differences=checks), status='DIVERGED')
                break
            if index == len(row['actions']):
                result['status'] = 'ALL_CAPTURED_BOUNDARIES_MATCH'
                break
            actual = row['actions'][index]['actual']
            matches = [a for a in decision.actions if json.dumps(asdict(a), sort_keys=True) ==
                       json.dumps(actual, sort_keys=True)]
            if len(matches) != 1:
                raise ValueError('stock action does not map uniquely to native public action')
            bits = backend._candidate_bits[matches[0].candidate_id]
            boundary['executed_bits'] = bits
            previous_observation = decision.observation
            run.step(bits)
            restored.step(bits)
            boundary['restored_suffix_equal'] = restored.snapshot() == run.snapshot()
            if not boundary['restored_suffix_equal']:
                result.update(status='RESTORED_SUFFIX_DIVERGED', first_divergence=dict(boundary=index))
                break
        if result['status'] == 'ALL_CAPTURED_BOUNDARIES_MATCH':
            final = result['boundaries'][-1]['native']
            for start, boundary in enumerate(result['boundaries']):
                clone = native.LightspeedRunState()
                clone.load_state(boundary['native'])
                for following in result['boundaries'][start:-1]:
                    clone.step(following['executed_bits'])
                boundary['full_suffix_restored_equal'] = clone.snapshot() == final
                if not boundary['full_suffix_restored_equal']:
                    result.update(status='FULL_SUFFIX_RESTORE_DIVERGED', first_divergence=dict(boundary=start))
                    break
    report = dict(schema='sls-green-first-divergence-v1', stock_integrity=source,
                  scope='CONDITIONED_ENCOUNTER_AND_PERSISTENT_STREAMS_POOLS_INDEPENDENT_ROOM_CONSTRUCTOR',
                  unqualified_initial_fields=['encounter_distribution', 'persistent_rng_generation', 'ordered_pool_generation'],
                  native_source_sha256=native.NATIVE_SOURCE_SHA256,
                  capture_sha256=hashlib.sha256(args.capture.read_bytes()).hexdigest(),
                  map_probe_sha256=hashlib.sha256(args.map_probe.read_bytes()).hexdigest(),
                  replay_source_sha256=hashlib.sha256(Path(__file__).read_bytes()).hexdigest(),
                  runs=results, training_gate='NOT_QUALIFIED')
    with args.output.open('x', encoding='utf-8') as stream:
        json.dump(report, stream, indent=2)
        stream.write('\n')
    print(json.dumps([dict(seed=r['seed'], status=r['status'],
                           first_boundary=r['first_divergence']['boundary'] if r['first_divergence'] else None)
                      for r in results]))


if __name__ == '__main__':
    main()
