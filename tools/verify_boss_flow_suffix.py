"""CPU full-suffix continuation of sealed controlled two-boss archives."""
from __future__ import annotations

import argparse
import hashlib
import json
import os
import zipfile
from pathlib import Path

from sls.audit.boss_flow import require_initial_boss_witness
from sls.audit.stock_clock import verify_sealed_oracle
from tools.reproduce_double_boss_entry import recorded_action_bits


def replay_suffixes(native, initial, actions, expected_states):
    if len(expected_states) != len(actions) + 1:
        raise ValueError('action/state cardinality differs')
    run = native.LightspeedRunState()
    run.load_state(initial)
    def persisted_equal(actual, expected):
        # JSON arrays decode as lists, while native bottle_indices are tuples.
        # Compare all persisted fields, then use fresh native types for exact
        # in-runtime restoration. No field exclusion or value normalization.
        return json.dumps(actual, sort_keys=True, separators=(',', ':')) == json.dumps(
            expected, sort_keys=True, separators=(',', ':'))
    if not persisted_equal(run.snapshot(), expected_states[0]):
        raise ValueError('initial checkpoint replay differs')
    fresh_states = [run.snapshot()]
    bits = []
    for index, action in enumerate(actions):
        command = None if action['kind'] == 'proceed_to_second_boss' else recorded_action_bits(action)
        if command is not None:
            if command not in {a['bits'] for a in run.legal_actions()}:
                raise ValueError('recorded combat action is unavailable')
            run.step(command)
        bits.append(command)
        if not persisted_equal(run.snapshot(), expected_states[index + 1]):
            raise ValueError(f'fresh trajectory differs at boundary {index + 1}')
        fresh_states.append(run.snapshot())
    results = []
    for start, state in enumerate(fresh_states):
        restored = native.LightspeedRunState()
        restored.load_state(state)
        checkpoint_equal = restored.snapshot() == state
        matched = checkpoint_equal
        for offset, command in enumerate(bits[start:], start + 1):
            if command is not None:
                if command not in {a['bits'] for a in restored.legal_actions()}:
                    matched = False
                    break
                restored.step(command)
            if restored.snapshot() != fresh_states[offset]:
                matched = False
                break
        results.append(dict(boundary=start, checkpoint_equal=checkpoint_equal,
                            remaining_combat_actions=sum(b is not None for b in bits[start:]),
                            full_remaining_trajectory_equal=matched,
                            persisted_all_fields_equal=True,
                            final_equal=restored.snapshot() == fresh_states[-1]))
    return results


def verify(capture, build_path, entry_path, flow_path):
    from sls.backends.simulator import SimulatorBackend, native
    from sls.curriculum import IRONCLAD_A20_HEART
    from sls.rl.training_contract import native_source_digest
    stock, build, entry, flow, launch = [json.loads(path.read_text()) for path in
                                        (capture, build_path, entry_path, flow_path, capture.with_suffix('.launch.json'))]
    jar = build_path.with_name(build_path.name.removesuffix('.build.json') + '.jar')
    verify_sealed_oracle(jar, build)
    members = [name for name, value in build['members'].items()
               if name.startswith('spirecomm/parity/fullrun-') and name.endswith('.json')
               and value == stock['scene_manifest_sha256']]
    if len(members) != 1:
        raise ValueError('ambiguous sealed scene manifest')
    with zipfile.ZipFile(jar) as archive:
        manifest = json.loads(archive.read(members[0]))
    digest = hashlib.sha256(capture.read_bytes()).hexdigest()
    if (native.NATIVE_SOURCE_SHA256 != native_source_digest()
            or entry['native_source_sha256'] != native.NATIVE_SOURCE_SHA256
            or flow['native_source_sha256'] != native.NATIVE_SOURCE_SHA256
            or entry['capture_sha256'] != digest or flow['capture_sha256'] != digest
            or flow['entry_report_sha256'] != hashlib.sha256(entry_path.read_bytes()).hexdigest()
            or not stock.get('execution_complete') or stock.get('execution_error')
            or launch['oracle_sha256'] != build['output_sha256']
            or launch['mode'] != 'validation' or launch.get('execution_error')
            or launch['recovery_status'] != 'RECOVERED' or launch['completion']['exit_code'] != 0
            or stock['stock_jar_sha256'] != build['dependencies']['game']):
        raise ValueError('source, capture or historical Oracle identity differs')
    by_seed = {r['seed']:r for r in stock['runs']}
    if len(by_seed) != len(stock['runs']):
        raise ValueError('ambiguous source seed')
    if [r['seed'] for r in entry['runs']] != [r['seed'] for r in flow['runs']]:
        raise ValueError('entry/flow selected seed order differs')
    rows = []
    for first, trajectory in zip(entry['runs'], flow['runs'], strict=True):
        row = by_seed[first['seed']]
        if row['scene'] not in manifest['scenes'] or row['seed'] not in row['scene']['seeds']:
            raise ValueError('undeclared source scene or seed')
        require_initial_boss_witness(row['boundaries'][0], row['scene'])
        if (row['actions'] != row['scene']['actions']
                or any(not v['equal'] for v in first['initial_comparisons'].values())
                or not first['comparisons'] or any(not v['equal'] for v in first['comparisons'].values())):
            raise ValueError('initial or second boss boundary not proved')
        states = [first['initial']] + [b['native_state'] for b in trajectory['boundaries']]
        if states[-1] != trajectory['native_final']:
            raise ValueError('last recorded state differs from final')
        suffixes = replay_suffixes(native, first['initial'], row['resolved_actions'], states)
        backend = SimulatorBackend(profile=IRONCLAD_A20_HEART)
        final_transition = backend._transition_from_raw(backend._adapt(states[-2]).observation, states[-1])
        aligned = []
        for boundary in trajectory['boundaries']:
            item = dict(boundary=boundary['boundary'], raw_exact=boundary.get('equal'),
                        alignment=boundary.get('qualification', 'ALIGNED_STABLE_COMBAT'))
            if 'equal' in boundary:
                original, current = boundary['stock_projection'], boundary['native_projection']
                item.update(combat_adapter_projection_equal=original['adapted'] == current['adapted'],
                            legal_semantic_actions_equal=original['actual_actions'] == current['actual_actions'],
                            rng_equal=original['rng'] == current['rng'],
                            raw_difference_classification=boundary.get('difference_classification'))
            aligned.append(item)
        rows.append(dict(seed=row['seed'], scene_id=row['scene']['id'],
                         boss_order=row['scene']['initial']['boss_order'],
                         suffixes=suffixes, boundaries=aligned,
                         endpoint_fields=trajectory['final_comparisons'],
                         stock_endpoint_available=bool(trajectory['final_comparisons']),
                         native_heart_horizon=dict(terminated=final_transition.terminated,
                                                  truncated=final_transition.truncated,
                                                  info=final_transition.info),
                         recorded_native_actions=[None if a['kind'] == 'proceed_to_second_boss'
                                                  else recorded_action_bits(a) for a in row['resolved_actions']]))
    return dict(schema='sls-controlled-boss-full-suffix-v1',
                scope='SEALED_STOCK_ENUMERATED_FIELDS_AND_CONDITIONED_NATIVE_FULL_SUFFIX_NOT_FULL_A20H',
                native_source_sha256=native.NATIVE_SOURCE_SHA256,
                native_binary_sha256=hashlib.sha256(Path(native.__file__).read_bytes()).hexdigest(),
                capture_sha256=digest, oracle_sha256=build['output_sha256'],
                entry_sha256=hashlib.sha256(entry_path.read_bytes()).hexdigest(),
                flow_sha256=hashlib.sha256(flow_path.read_bytes()).hexdigest(),
                source_sha256=hashlib.sha256(Path(__file__).read_bytes()).hexdigest(),
                source_cases=len(stock['runs']), selected_seeds=[r['seed'] for r in rows],
                excluded_seeds=[r['seed'] for r in stock['runs'] if r['seed'] not in {s['seed'] for s in rows}],
                runs=rows, training_gate='NOT_QUALIFIED', training_eligible=False,
                limitations=['conditioned initial RNG/inventory/hand/monster HP and boss order',
                             'stock victory UI is folded into native automatic transition',
                             'raw corpse powers remain unequal',
                             'Act4 entry does not include Shield/Spear or Heart'])


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    for name in ('capture', 'oracle-build', 'entry-report', 'flow-report', 'output'):
        parser.add_argument('--' + name, required=True, type=Path)
    args = parser.parse_args()
    if args.output.exists():
        raise FileExistsError('refuse to replace evidence')
    os.environ['CUDA_VISIBLE_DEVICES'] = '-1'
    report = verify(args.capture, args.oracle_build, args.entry_report, args.flow_report)
    with args.output.open('x', encoding='utf-8') as stream:
        json.dump(report, stream, indent=2)
        stream.write('\n')
    matched = all(all(s['checkpoint_equal'] and s['full_remaining_trajectory_equal'] and s['final_equal']
                      for s in r['suffixes']) for r in report['runs'])
    print(json.dumps(dict(cases=len(report['runs']), suffix_boundaries=sum(len(r['suffixes']) for r in report['runs']),
                         native_full_suffix_equal=matched)))
    return 0 if matched else 2


if __name__ == '__main__':
    raise SystemExit(main())
