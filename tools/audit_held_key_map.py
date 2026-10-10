"""Seal stock map capture and compare independent native map/RNG phases."""
from __future__ import annotations

import argparse
import hashlib
import json
import os
import subprocess
from pathlib import Path

from sls.audit.stock_clock import verify_sealed_oracle
from tools.capture_held_key_map import validate
from tools.run_key_room_batch import root_path

ROOT = Path(__file__).resolve().parents[1]


def nodes(rows, ordered=False):
    result = []
    for row in rows:
        children = [dict(x=c['x'], y=c['y']) for c in row['children']]
        if not ordered:
            children.sort(key=lambda c: (c['y'], c['x']))
        result.append(dict(x=row['x'], y=row['y'], symbol=row['symbol'], children=children))
    result.sort(key=lambda r: (r['y'], r['x']))
    if len({(r['x'], r['y']) for r in result}) != len(result):
        raise ValueError('duplicate map coordinates')
    return result


def audit(capture, build_path, manifest_path, executable):
    data, build, manifest, launch = [json.loads(p.read_text()) for p in
                                     (capture, build_path, manifest_path, capture.with_suffix('.launch.json'))]
    validate(manifest)
    if not build_path.name.endswith('.build.json'):
        raise ValueError('expected sealed Oracle build path')
    verify_sealed_oracle(build_path.with_name(build_path.name.removesuffix('.build.json') + '.jar'), build)
    digest = hashlib.sha256(manifest_path.read_bytes()).hexdigest()
    if (data.get('schema') != 'sls-held-key-map-capture-v1' or not data.get('execution_complete')
            or data.get('execution_error') or data['manifest_sha256'] != digest
            or build['members']['spirecomm/parity/' + manifest_path.name] != digest
            or build['dependencies']['game'] != manifest['stock_jar_sha256']
            or launch['oracle_sha256'] != build['output_sha256']
            or launch['mode'] != 'validation' or launch.get('execution_error')
            or launch['completion']['exit_code'] != 0 or launch['recovery_status'] != 'RECOVERED'):
        raise ValueError('stock capture provenance or completion mismatch')
    journal = Path(launch['recovery_journal'])
    if json.loads(journal.read_text())['status'] != 'RECOVERED':
        raise ValueError('runtime recovery journal incomplete')
    expected = {r['id']:r for r in manifest['scenes']}
    if [r['scene']['id'] for r in data['runs']] != [r['id'] for r in manifest['scenes']]:
        raise ValueError('missing, repeated or reordered stock cases')
    produced = json.loads(subprocess.check_output([str(executable.resolve())], timeout=30))
    if (len(produced) != 4 or {(r['act'], r['assign_burning']) for r in produced} !=
            {(act, burning) for act in (2, 3) for burning in (False, True)}
            or any(r['seed'] != 131200410 or not r['production_constructor_equal'] for r in produced)):
        raise ValueError('native phase probe differs from production constructor')
    rows = []
    for row in data['runs']:
        scene = expected[row['scene']['id']]
        stock = row['boundary']
        evidence, direct, run = stock['_parity_scenario'], stock['_stock_direct'], stock['_parity_run']
        if (row['scene'] != scene or row['status'] != 'STOCK_CONSTRUCTOR_MAP_CAPTURED'
                or stock['_oracle_mode'] != 'validation' or evidence['scenario_id'] != scene['id']
                or evidence['corpus'] != manifest_path.stem or evidence['act'] != scene['act']
                or evidence['emerald_key'] is not scene['emerald_key']
                or evidence['final_act_available'] is not scene['final_act_available']
                or direct['act'] != scene['act'] or direct['ascension'] != 20
                or run['emerald_key'] is not scene['emerald_key']
                or row['root_path'] != root_path(stock)
                or direct['floor'] != run['current_map_y'] + (18 if scene['act'] == 2 else 35)):
            raise ValueError('unexpected stock map boundary')
        assign = scene['final_act_available'] and not scene['emerald_key']
        native = next(r for r in produced if r['act'] == scene['act'] and r['assign_burning'] == assign)
        # Stock Gson omits null-valued coordinates when no burning node exists.
        coords = [run.get('burning_elite_x'), run.get('burning_elite_y')]
        expected_coords = [native['burning_x'], native['burning_y']] if assign else [None, None]
        rows.append(dict(id=scene['id'], act=scene['act'], green_key=scene['emerald_key'],
                         final_act_available=scene['final_act_available'],
                         public_nodes=len(stock['game_state']['map']),
                         stock_map_rng=evidence['map_rng_after_constructor'],
                         native_phase_rng=native['stock_constructor_phase_rng'],
                         structural_nodes_equal=nodes(stock['game_state']['map']) == nodes(native['nodes']),
                         ordered_children_equal=nodes(stock['game_state']['map'], True) == nodes(native['nodes'], True),
                         burning_coordinates_equal=coords == expected_coords,
                         map_rng_equal=evidence['map_rng_after_constructor'] == native['stock_constructor_phase_rng'],
                         rest_entry_map_rng_unchanged=direct['map_rng'] == evidence['map_rng_after_constructor']))
    pairs = []
    for act in (2, 3):
        matching = {r['scene']['id']:r['boundary'] for r in data['runs'] if r['scene']['act'] == act}
        unheld, held, locked = [matching[f'act{act}-' + label] for label in
                                ('unheld-unlocked', 'held-unlocked', 'unheld-locked')]
        held_rng, locked_rng, unheld_rng = [r['_parity_scenario']['map_rng_after_constructor'] for r in (held, locked, unheld)]
        pairs.append(dict(act=act, held_locked_rng_equal=held_rng == locked_rng,
                          unheld_extra_selection_draws=unheld_rng['counter'] - held_rng['counter'],
                          three_map_topologies_equal=nodes(unheld['game_state']['map']) == nodes(held['game_state']['map']) == nodes(locked['game_state']['map'])))
    keys = ['structural_nodes_equal', 'burning_coordinates_equal', 'map_rng_equal', 'rest_entry_map_rng_unchanged']
    passed = all(all(r[k] for k in keys) for r in rows) and all(
        r['held_locked_rng_equal'] and r['three_map_topologies_equal'] and r['unheld_extra_selection_draws'] == 1 for r in pairs)
    inputs = ['tools/probe_stock_map_rng.cpp', 'tools/audit_held_key_map.py',
              'native/simulator/src/game/Map.cpp', 'native/simulator/include/game/Map.h',
              'native/simulator/include/game/Random.h', 'native/simulator/include/sts_common.h',
              'native/simulator/include/constants/Rooms.h']
    return dict(schema='sls-held-key-stock-map-comparison-v1', status='MATCH' if passed else 'DIVERGED',
                scope='CONTROLLED_FLAGS_ACTUAL_STOCK_CONSTRUCTORS_AND_NATIVE_MAP_PHASE_NOT_NATURAL_TRANSITION',
                capture_sha256=hashlib.sha256(capture.read_bytes()).hexdigest(),
                oracle_sha256=build['output_sha256'], stock_jar_sha256=manifest['stock_jar_sha256'],
                executable_sha256=hashlib.sha256(executable.read_bytes()).hexdigest(),
                input_sha256={p:hashlib.sha256((ROOT / p).read_bytes()).hexdigest() for p in inputs},
                recovery_journal_sha256=hashlib.sha256(journal.read_bytes()).hexdigest(),
                cases=rows, pairs=pairs, native_phase_outputs=produced,
                training_eligible=False, natural_trajectory=False, training_gate='NOT_QUALIFIED',
                gaps=['native final-act eligibility flag', 'natural key acquisition followed by actual Act transition',
                      'encounter lists, reward pools and boss distribution at the new Act'])


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    for name in ('capture', 'oracle-build', 'manifest', 'map-executable', 'output'):
        parser.add_argument('--' + name, required=True, type=Path)
    args = parser.parse_args()
    if args.output.exists():
        raise FileExistsError('refuse to replace comparison evidence')
    os.environ['CUDA_VISIBLE_DEVICES'] = '-1'
    report = audit(args.capture, args.oracle_build, args.manifest, args.map_executable)
    with args.output.open('x', encoding='utf-8') as stream:
        json.dump(report, stream, indent=2)
        stream.write('\n')
    print(json.dumps(dict(status=report['status'], cases=report['cases'], pairs=report['pairs'])))
    return 0 if report['status'] == 'MATCH' else 2


if __name__ == '__main__':
    raise SystemExit(main())
