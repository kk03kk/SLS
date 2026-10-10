"""Audit stock green-key capture integrity; does not qualify native equivalence."""
from __future__ import annotations

import argparse
import hashlib
import json
from collections import Counter
from dataclasses import asdict
from pathlib import Path

from sls.audit.stock_clock import verify_sealed_oracle
from sls.backends.original.adapter import adapt_original
from tools.run_green_key_batch import validate
from tools.run_key_room_batch import root_path


def next_buff(state):
    """Stock RandomXS128 nextInt(4), preserving unsigned 64-bit arithmetic."""
    mask = (1 << 64) - 1
    a, b = state['seed0'], state['seed1']
    a ^= (a << 23) & mask
    c = (a ^ b ^ (a >> 17) ^ (b >> 26)) & mask
    value = (((c + b) & mask) >> 1) % 4
    return value, dict(counter=state['counter'] + 1, seed0=b, seed1=c)


def canonical(value):
    return json.dumps(value, sort_keys=True)


def audit(capture, build_path, manifest_path):
    data, build, manifest, launch = [json.loads(path.read_text()) for path in (
        capture, build_path, manifest_path, capture.with_suffix('.launch.json'))]
    validate(manifest)
    if not build_path.name.endswith('.build.json'):
        raise ValueError('expected sealed .build.json artifact')
    verify_sealed_oracle(build_path.with_name(build_path.name.removesuffix('.build.json') + '.jar'), build)
    manifest_sha = hashlib.sha256(manifest_path.read_bytes()).hexdigest()
    if (not data.get('execution_complete') or data.get('execution_error')
            or data.get('schema') != 'sls-green-key-capture-v1'
            or data['manifest_sha256'] != manifest_sha
            or build['members']['spirecomm/parity/' + manifest_path.name] != manifest_sha
            or build['dependencies']['game'] != manifest['stock_jar_sha256']
            or launch['oracle_sha256'] != build['output_sha256']
            or launch['mode'] != 'validation' or launch['recovery_status'] != 'RECOVERED'
            or launch.get('execution_error') or launch['completion']['exit_code'] != 0):
        raise ValueError('capture/manifest/build/launch identity or completion mismatch')
    scenes = {s['id']: s for s in manifest['scenes']}
    actual_keys, rows = [], []
    for row in data['runs']:
        scene = scenes.get(row['scene']['id'])
        if row['scene'] != scene or row['seed'] not in scene['seeds']:
            raise ValueError('unexpected scene or seed')
        actual_keys.append((scene['id'], row['seed']))
        boundaries, actions = row['boundaries'], row['actions']
        if len(boundaries) != len(actions) + 1 or not boundaries:
            raise ValueError('incomplete action history')
        first, last = boundaries[0], boundaries[-1]
        direct = first['_stock_direct']
        if (row['initial_root_path'] != root_path(first) or first['_oracle_mode'] != 'validation'
                or first['_parity_scenario']['corpus'] != manifest_path.stem
                or direct['floor'] != first['_parity_run']['current_map_y'] + 18
                or (direct['act'], direct['ascension'], direct['turn']) != (2, 20, 1)
                or not direct['current_node_has_emerald_key'] or first['_parity_run']['emerald_key']
                or direct['player']['current_hp'] != scene['initial']['hp']
                or direct['player']['max_hp'] != scene['initial']['max_hp']
                or Counter(c['id'] for c in direct['master_deck']) != Counter(scene['initial']['deck'])
                or any(c['upgrades'] or c['misc'] for c in direct['master_deck'])
                or [r['id'] for r in direct['relics']] != scene['initial']['relics']
                or first['game_state']['gold'] != scene['initial']['gold']
                or not first['game_state']['combat_state']['hand']
                or any(m['next_move'] < 0 for m in direct['monsters'])):
            raise ValueError('incomplete or inconsistent initial combat')
        buff, after = next_buff(first['_parity_scenario']['map_rng_before_entry'])
        if direct['map_rng'] != after:
            raise ValueError('stock elite entry does not consume exactly one declared map draw')
        power, amount = {0: ('Strength', 3), 2: ('Metallicize', 6), 3: ('Regenerate', 5)}.get(buff, (None, None))
        # HP buff needs an independent base-HP constructor comparison; do not
        # derive its expected value from the resulting stock HP.
        buff_check = 'HP_CONSTRUCTOR_COMPARISON_PENDING'
        if power is not None:
            if any(not any(p['id'] == power and p['amount'] == amount for p in m['powers'])
                   for m in direct['monsters']):
                raise ValueError('stock buff powers differ from drawn branch')
            buff_check = 'ALL_STOCK_MONSTERS_HAVE_EXPECTED_POWER'
        for index, action in enumerate(actions):
            adapted = adapt_original(boundaries[index])
            matches = [a for a in adapted.decision.actions if canonical(asdict(a)) == canonical(action['actual'])]
            if len(matches) != 1:
                raise ValueError('recorded action was not uniquely public-legal')
            expected = list(adapted.commands[matches[0].candidate_id])
            if action['commands'][:len(expected)] != expected:
                raise ValueError('wire action differs from recorded public action')
        outcome = adapt_original(last).decision
        if row['status'] == 'GREEN_KEY_ACQUIRED':
            if not last['_parity_run']['emerald_key'] or not any(
                    a['actual']['reward_id'] == 'reward-key:emerald' for a in actions):
                raise ValueError('green flag without actual reward action')
        elif row['status'] == 'GAME_TERMINAL':
            if not outcome.terminal or last['_stock_direct']['player']['current_hp'] > 0:
                raise ValueError('terminal was not actual player death')
        elif row['status'] != 'DIAGNOSTIC_LIMIT_UNFINISHED':
            raise ValueError('unfinished capture status')
        rows.append(dict(seed=row['seed'], scene=scene['id'], status=row['status'], actions=len(actions),
                         drawn_buff=buff, buff_check=buff_check,
                         inventory_scope='DECK_MULTIPLICITY_AND_MODIFIERS_RELIC_ORDER_GOLD_HP',
                         actual_master_deck_order=[c['id'] for c in direct['master_deck']],
                         map_rng_before=first['_parity_scenario']['map_rng_before_entry'], map_rng_after=after))
    expected_keys = [(s['id'], seed) for s in manifest['scenes'] for seed in s['seeds']]
    if actual_keys != expected_keys:
        raise ValueError('missing/duplicated/reordered stock case')
    return dict(schema='sls-green-stock-audit-v1', simulator_comparison='NOT_YET_PERFORMED',
                capture_sha256=hashlib.sha256(capture.read_bytes()).hexdigest(), runs=rows)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--capture', type=Path, required=True)
    parser.add_argument('--oracle-build', type=Path, required=True)
    parser.add_argument('--manifest', type=Path, required=True)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    report = audit(args.capture, args.oracle_build, args.manifest)
    with args.output.open('x') as stream:
        json.dump(report, stream, indent=2)
        stream.write('\n')
    print(json.dumps(report, indent=2))


if __name__ == '__main__':
    main()
