"""CPU-only Heart terminal replay against raw stock evidence and fixture binding."""
from __future__ import annotations

import argparse
import hashlib
import json
import os
import sys
from pathlib import Path

os.environ['CUDA_VISIBLE_DEVICES'] = '-1'
ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / 'src'))

from sls.audit.card_parity import structured_differences
from sls.audit.terminal_resources import (
    native_terminal_resources,
    stock_terminal_resources,
)
from sls.rl.training_contract import native_artifact, native_source_digest


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def replay_case(case):
    from sls.backends.simulator import native

    battle = native.LightspeedBattle()
    battle.reset_encounter_probe(case['seed'], case['encounter'], case['initial_rng'],
                                 20, 4, case['floor'], case['scene_id'])
    initial = case['initial']
    battle.set_player_health(initial['hp'], initial['max_hp'])
    payload = battle.snapshot()
    combat = payload['game_state']['combat_state']
    combat['player']['energy'], combat['player']['block'] = initial['energy'], initial['block']
    combat['monsters'][0]['current_hp'] = initial['monster_hp']['CORRUPT_HEART']
    battle.load_checkpoint({'game_state': payload['game_state'], 'rng': payload['_rng']})
    battle.set_card_piles(initial['hand'], initial['draw'], [], [])
    battle.set_potions([initial['potion']] if 'potion' in initial else [])
    payload = battle.snapshot()
    payload['game_state']['combat_state']['_internal']['potion_capacity'] = 2
    battle.load_checkpoint({'game_state': payload['game_state'], 'rng': payload['_rng']})
    for bits in case['actions']:
        action = dict(bits)
        battle.step(action.pop('kind'), **action)
    current = battle.snapshot()
    observed = native_terminal_resources(current)
    if 'combat_state' not in current['game_state']:
        return observed, {'supported':False, 'equal':None,
                          'reason':'ISOLATED_BATTLE_LOADER_REQUIRES_COMBAT_STATE_NOT_FULLRUN_RESTORE'}
    battle.load_checkpoint({'game_state': current['game_state'], 'rng': current['_rng']})
    restored = battle.snapshot()
    return observed, {'supported':True,
                      'equal':current == restored and observed == native_terminal_resources(restored)}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--capture', type=Path, required=True)
    parser.add_argument('--oracle-build', type=Path, required=True)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    if args.output.exists():
        raise FileExistsError('refuse to overwrite terminal evidence')
    fixture_path = ROOT / 'tests/fixtures/heart-lethal-stock.json'
    manifest_path = ROOT / 'native/oracle/resources/spirecomm/parity/fullrun-heart-lethal-r1.json'
    launch_path = args.capture.with_suffix('.launch.json')
    fixture, capture, build, manifest, launch = [json.loads(path.read_text(encoding='utf-8'))
        for path in (fixture_path, args.capture, args.oracle_build, manifest_path, launch_path)]
    member = 'spirecomm/parity/fullrun-heart-lethal-r1.json'
    if (sha(args.capture) != fixture['capture_sha256'] or sha(manifest_path) != fixture['manifest_sha256']
            or build['output_sha256'] != fixture['oracle_sha256']
            or launch['oracle_sha256'] != fixture['oracle_sha256']
            or build['members'][member] != fixture['manifest_sha256']
            or build.get('schema') != 'sls-oracle-build-v1' or build.get('used_existing_oracle') is not False
            or build['dependencies']['game'] != fixture['stock_jar_sha256']
            or manifest['stock_jar_sha256'] != fixture['stock_jar_sha256']
            or capture.get('scene_manifest_sha256') != fixture['manifest_sha256']
            or not capture.get('execution_complete') or capture.get('execution_error')
            or launch['mode'] != 'validation' or launch['recovery_status'] != 'RECOVERED'
            or launch['completion']['exit_code'] != 0 or launch.get('execution_error')):
        raise ValueError('stock capture/fixture/build/manifest/recovery identity mismatch')
    cases = fixture['cases']
    rows = {row['seed']: row for row in capture['runs']}
    if len(rows) != len(capture['runs']) or set(rows) != {case['seed'] for case in cases}:
        raise ValueError('missing/duplicate terminal seeds')
    results = []
    scenes = {scene['id']: scene for scene in manifest['scenes']}
    for case in cases:
        row = rows[case['seed']]
        if (row['scene'] != scenes[case['scene_id']] or row['scene']['initial'] != case['initial']
                or row['actions'] != case['actions'] or row['actions'] != row['scene']['actions']
                or case['seed'] not in row['scene']['seeds'] or row['ascension'] != 20
                or row['act'] != 4 or row['floor'] != case['floor']
                or row['before']['_rng'] != case['initial_rng']):
            raise ValueError('terminal initial state/action/context mismatch')
        expected = stock_terminal_resources(row['boundaries'][-1])
        if expected != case['expected_terminal_resources']:
            raise ValueError('fixture terminal differs from independent raw stock boundary')
        observed, restore = replay_case(case)
        results.append({'seed':case['seed'], 'stock':expected, 'native':observed,
                        'differences':structured_differences(expected, observed),
                        'isolated_terminal_restore':restore})
    report = {'schema':'sls-heart-terminal-archive-replay-v1','native':native_artifact(),
              'native_source_sha256':native_source_digest(), 'cuda_visible_devices':os.environ['CUDA_VISIBLE_DEVICES'],
              'scope':'TERMINAL_HP_OUTCOME_POTIONS_RNG_AND_RESTORE_ONLY',
              'original_game_launched':False, 'full_a20h_qualification':False,
              'inputs_sha256':{str(path):sha(path) for path in
                (fixture_path,args.capture,args.oracle_build,manifest_path,launch_path)},'runs':results}
    args.output.parent.mkdir(parents=True,exist_ok=True)
    with args.output.open('x',encoding='utf-8') as stream:
        json.dump(report,stream,indent=2)
    if any(row['differences'] or row['isolated_terminal_restore']['equal'] is False for row in results):
        raise SystemExit('terminal differences retained in report')
    supported = sum(row['isolated_terminal_restore']['supported'] for row in results)
    print(f'{len(results)} raw-stock terminal resources match; {supported} isolated restores checked; '
          f'{len(results) - supported} isolated restores unsupported')


if __name__ == '__main__':
    main()
