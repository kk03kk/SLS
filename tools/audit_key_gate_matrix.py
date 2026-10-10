"""Compare actual stock key-gate endings, resources, RNG and Heart horizon."""
from __future__ import annotations

import argparse
import hashlib
import json
import os
from dataclasses import asdict
from pathlib import Path

from sls.backends.original.adapter import adapt_original
from sls.curriculum import IRONCLAD_A20_HEART, completed_act_between, evaluate_horizon
from tools.replay_rest_key_archive import differences, native_resources, stock_resources
from tools.verify_boss_flow_suffix import verify


def audit(capture, build, manifest, entry, flow):
    from sls.backends.simulator import SimulatorBackend
    source = json.loads(capture.read_text())
    scenes = json.loads(manifest.read_text())
    trajectory = json.loads(flow.read_text())
    proof = verify(capture, build, entry, flow)
    if (scenes.get('seed_namespace') != [131200420, 131200428]
            or scenes.get('natural_trajectory') is not False or scenes.get('training_eligible') is not False
            or len(source['runs']) != 8 or len(scenes['scenes']) != 8
            or hashlib.sha256(manifest.read_bytes()).hexdigest() != source['scene_manifest_sha256']):
        raise ValueError('key matrix namespace, provenance or coverage differs')
    rows = []
    for mask, (row, native_row, checked) in enumerate(zip(source['runs'], trajectory['runs'], proof['runs'], strict=True)):
        expected_keys = [key for bit,key in enumerate(['RUBY', 'EMERALD', 'SAPPHIRE']) if mask & (1 << bit)]
        scene = row['scene']
        if (scene != scenes['scenes'][mask] or row['seed'] != 131200420 + mask
                or native_row['seed'] != row['seed'] or checked['seed'] != row['seed']
                or scene['initial']['keys'] != expected_keys or scene['initial']['final_act_available'] is not True):
            raise ValueError('matrix case identity differs')
        outcome = row['key_gate_outcome']
        stock = outcome['boundary']
        if (not outcome['history'] or outcome['history'][-1] != stock
                or not 1 <= len(outcome['commands']) <= 8
                or any(command != 'choose 0' for command in outcome['commands'])):
            raise ValueError('incomplete actual dialogue history')
        for boundary in outcome['history']:
            flags = boundary['_parity_run']
            if any(flags[field] is not (key in expected_keys) for key, field in
                   [('RUBY', 'ruby_key'), ('EMERALD', 'emerald_key'), ('SAPPHIRE', 'sapphire_key')]):
                raise ValueError('key changed during actual gate')
        expected_status = 'ACT4_MAP_ENTRY' if mask == 7 else 'ACT3_STOCK_ENDING'
        if outcome['status'] != expected_status or stock['_stock_direct']['act'] != (4 if mask == 7 else 3):
            raise ValueError('stock gate path differs from key requirement')
        if mask != 7 and (stock['game_state']['screen_type'] != 'GAME_OVER'
                          or stock['game_state']['screen_state'].get('victory') is not True):
            raise ValueError('stock Act3 ending lacks positive game-victory witness')
        previous = adapt_original(row['victory_room_entry']).decision.observation
        observed = adapt_original(stock).decision.observation
        original_horizon = evaluate_horizon(IRONCLAD_A20_HEART, observed,
                                            act_completed=completed_act_between(previous, observed))
        native_horizon = checked['native_heart_horizon']
        terminal_equal = (original_horizon.terminated == native_horizon['terminated']
                          and original_horizon.success == native_horizon['info']['success']
                          and original_horizon.reason == native_horizon['info']['reason'])
        original_resources = stock_resources(stock, canonical_counters=False)
        simulated_resources = native_resources(native_row['native_final'], canonical_counters=False)
        raw_resource_differences = differences(original_resources, simulated_resources)
        public_native = SimulatorBackend(profile=IRONCLAD_A20_HEART)._adapt(native_row['native_final']).observation
        comparisons = dict(non_deck_resources=differences(
                               {k:v for k,v in original_resources.items() if k not in {'deck', 'relics'}},
                               {k:v for k,v in simulated_resources.items() if k not in {'deck', 'relics'}}),
                           public_deck=differences([asdict(c) for c in observed.deck], [asdict(c) for c in public_native.deck]),
                           public_relics=differences([asdict(r) for r in observed.relics], [asdict(r) for r in public_native.relics]),
                           rng=differences(stock['_rng'], native_row['native_final']['rng']),
                           act=differences(stock['_stock_direct']['act'], native_row['native_final']['run_state']['act']),
                           floor=differences(stock['_stock_direct']['floor'], native_row['native_final']['run_state']['floor']))
        rows.append(dict(seed=row['seed'], mask=mask, keys=expected_keys, actual_stock_status=outcome['status'],
                         dialogue_commands=outcome['commands'], comparisons=comparisons,
                         raw_resource_differences=raw_resource_differences,
                         raw_resources_equal=not raw_resource_differences,
                         stock_horizon=dict(terminated=original_horizon.terminated, success=original_horizon.success,
                                            reason=original_horizon.reason), native_horizon=native_horizon,
                         horizon_equal=terminal_equal,
                         full_suffix_checkpoints=len(checked['suffixes']),
                         full_suffix_equal=all(s['checkpoint_equal'] and s['full_remaining_trajectory_equal'] and s['final_equal']
                                               for s in checked['suffixes'])))
    passed = all(not any(r['comparisons'].values()) and r['horizon_equal'] and r['full_suffix_equal'] for r in rows)
    raw_equal = all(r['raw_resources_equal'] for r in rows)
    status = ('ENUMERATED_FIELDS_MATCH_RAW_RESOURCE_DIFFERENCES_RETAINED' if passed and not raw_equal
              else 'ENUMERATED_FIELDS_MATCH' if passed else 'DIVERGED')
    return dict(schema='sls-stock-key-gate-matrix-comparison-v2', status=status,
                enumerated_fields_equal=passed, raw_resources_equal=raw_equal,
                capture_sha256=proof['capture_sha256'], native_source_sha256=proof['native_source_sha256'],
                native_binary_sha256=proof['native_binary_sha256'], oracle_sha256=proof['oracle_sha256'],
                manifest_sha256=source['scene_manifest_sha256'], source_sha256=hashlib.sha256(Path(__file__).read_bytes()).hexdigest(),
                scope='EIGHT_CONTROLLED_KEY_SUBSETS_REAL_BOSS_AND_STOCK_DIALOGUE_OUTCOME_NOT_KEY_ACQUISITION',
                runs=rows, training_gate='NOT_QUALIFIED', training_eligible=False, natural_trajectory=False,
                limitations=['controlled powerful deck, boss order and initial keys',
                             'final-act unlocked in all cases', 'only Time Eater then Donu/Deca',
                             'raw Deca corpse powers and folded victory UI remain unequal',
                             'Act4 rest/shop/Shield/Spear/Heart not included'])


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    for name in ('capture', 'oracle-build', 'manifest', 'entry-report', 'flow-report', 'output'):
        parser.add_argument('--' + name, required=True, type=Path)
    args = parser.parse_args()
    if args.output.exists():
        raise FileExistsError('refuse to overwrite matrix evidence')
    os.environ['CUDA_VISIBLE_DEVICES'] = '-1'
    report = audit(args.capture, args.oracle_build, args.manifest, args.entry_report, args.flow_report)
    with args.output.open('x', encoding='utf-8') as stream:
        json.dump(report, stream, indent=2)
        stream.write('\n')
    print(json.dumps(dict(status=report['status'], cases=[dict(seed=r['seed'], keys=r['keys'],
                                                             actual_stock_status=r['actual_stock_status'],
                                                             differences=r['comparisons'], horizon_equal=r['horizon_equal'])
                                                       for r in report['runs']])))
    return 0 if report['enumerated_fields_equal'] else 2


if __name__ == '__main__':
    raise SystemExit(main())
