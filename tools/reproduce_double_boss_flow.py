"""Bounded same-action two-combat replay; retain unaligned endpoint evidence."""

import argparse
import hashlib
import json
from pathlib import Path

from sls.audit.act2_differential import (
    comparison_projection,
    production_combat_projection,
)
from sls.audit.card_parity import structured_differences
from sls.audit.corpse_cleanup import (
    classify_cultist_ritual_cleanup,
    classify_deca_artifact_cleanup,
)
from sls.backends.simulator import native
from sls.content.normalize import normalize_card_id
from sls.rl.training_contract import native_source_digest


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--capture', type=Path, required=True)
    parser.add_argument('--entry-report', type=Path, required=True)
    parser.add_argument('--output', type=Path, required=True)
    parser.add_argument('--seeds', type=int, nargs='+', help='Must match explicit entry-report selection')
    args = parser.parse_args()
    if args.output.exists():
        raise FileExistsError('refuse to overwrite evidence')
    stock = json.loads(args.capture.read_text(encoding='utf-8'))
    entry = json.loads(args.entry_report.read_text(encoding='utf-8'))
    source_runs = stock['runs']
    if args.seeds is not None and (len(set(args.seeds)) != len(args.seeds)
            or not set(args.seeds) <= {r['seed'] for r in source_runs}):
        raise ValueError('duplicate or unknown explicit seed selection')
    selected_runs = [r for r in source_runs if args.seeds is None or r['seed'] in args.seeds]
    if [r['seed'] for r in selected_runs] != [r['seed'] for r in entry['runs']]:
        raise ValueError('flow selection differs from entry report')
    if (native.NATIVE_SOURCE_SHA256 != native_source_digest()
            or entry['native_source_sha256'] != native.NATIVE_SOURCE_SHA256
            or entry['capture_sha256'] != hashlib.sha256(args.capture.read_bytes()).hexdigest()
            or not stock.get('execution_complete')):
        raise ValueError('stale source or capture identity')
    results = []
    for row, first in zip(selected_runs, entry['runs'], strict=True):
        if (row['seed'] != first['seed'] or row['actions'] != row['scene']['actions']
                or any(not v['equal'] for v in first['initial_comparisons'].values())
                or any(not v['equal'] for v in first['comparisons'].values())):
            raise ValueError('initial/entry equivalence or declared script unproved')
        run = native.LightspeedRunState()
        run.load_state(first['initial'])
        boundaries = []
        for index, action in enumerate(row['resolved_actions']):
            if action['kind'] == 'proceed_to_second_boss':
                # Stock has a free victory button; native directly enters the
                # second fight. Compare at its recorded stable entry boundary.
                pass
            else:
                bits = (2147483648 if action['kind'] == 'end_turn'
                        else action['card_index'] - 1 | (action['target_index'] << 16))
                if not any(a['bits'] == bits for a in run.legal_actions()):
                    raise ValueError('same semantic action unavailable')
                run.step(bits)
            state = run.snapshot()
            restored = native.LightspeedRunState()
            restored.load_state(state)
            restored_state = restored.snapshot()
            restore_equal = restored_state == state and restored.legal_actions() == run.legal_actions()
            payload = row['boundaries'][index + 1]
            result = {'boundary': index + 1, 'native_state': state, 'restore_equal': restore_equal}
            if not restore_equal:
                result['restored_state'] = restored_state
                result['restore_differences'] = structured_differences(state, restored_state)
            if (payload['game_state']['screen_type'] == 'NONE'
                    and 'combat_checkpoint' in state):
                battle = native.LightspeedBattle()
                battle.load_checkpoint(state['combat_checkpoint'])
                original = comparison_projection(payload, stock=True)
                current = comparison_projection(production_combat_projection(battle),
                                                stock=False, extended_direct=True)
                result.update(stock_projection=original, native_projection=current,
                              equal=original == current)
                if original != current:
                    result['difference_classification'] = (classify_deca_artifact_cleanup(original, current)
                                                           or classify_cultist_ritual_cleanup(original, current))
            else:
                result['qualification'] = 'VICTORY_UI_AND_NATIVE_DIRECT_TRANSITION_UNALIGNED'
            boundaries.append(result)
        final = run.snapshot()
        victory = row['second_boss_victory']
        final_comparisons = {}
        if 'victory_room_entry' in row:
            endpoint = row.get('act4_entry', row['victory_room_entry'])
            direct = endpoint['_stock_direct']
            pairs = {
                'floor': (direct['floor'], final['run_state']['floor']),
                'hp': (direct['player']['current_hp'], final['player_state']['current_hp']),
                'max_hp': (direct['player']['max_hp'], final['player_state']['max_hp']),
                'gold': (endpoint['game_state']['gold'], final['player_state']['gold']),
                'rng': (endpoint['_rng'], final['rng']),
                'deck_ids_and_upgrades': (
                    [{'id': normalize_card_id(c['id']), 'upgrades': c['upgrades']} for c in direct['master_deck']],
                    [{'id': c['id'], 'upgrades': c['upgrades']} for c in final['public_inventory']['deck']]),
            }
            if 'act4_entry' in row:
                pairs['act'] = (direct['act'], final['run_state']['act'])
                pairs['keys'] = (row['act4_entry']['_parity_run']['ruby_key'], final['player_state']['red_key'])
                pairs['emerald_key'] = (row['act4_entry']['_parity_run']['emerald_key'], final['player_state']['green_key'])
                pairs['sapphire_key'] = (row['act4_entry']['_parity_run']['sapphire_key'], final['player_state']['blue_key'])
            final_comparisons = {k: {'stock': v[0], 'native': v[1], 'equal': v[0] == v[1]}
                                 for k, v in pairs.items()}
        results.append({'seed': row['seed'], 'boundaries': boundaries,
                        'stock_second_victory': victory['_stock_direct'],
                        'stock_reward_state': victory['_stock_reward_state'],
                        'native_final': final,
                        'final_comparisons': final_comparisons,
                        'endpoint_status': ('ENUMERATED_ENDPOINT_FIELDS_COMPARED' if final_comparisons
                                            else 'REQUIRES_STOCK_VICTORY_ROOM_ENTRY_FOR_FINAL_RNG_AND_RESOURCE_ALIGNMENT')})
    report = {'schema': 'sls-double-boss-flow-replay-v1', 'runs': results,
              'native_source_sha256': native.NATIVE_SOURCE_SHA256,
              'capture_sha256': entry['capture_sha256'],
              'entry_report_sha256': hashlib.sha256(args.entry_report.read_bytes()).hexdigest(),
              'source_runs': len(source_runs), 'selected_seeds': [r['seed'] for r in selected_runs],
              'excluded_seeds': [r['seed'] for r in source_runs if r not in selected_runs],
              'source_file_sha256': hashlib.sha256(Path(__file__).read_bytes()).hexdigest()}
    args.output.write_text(json.dumps(report, indent=2) + '\n', encoding='utf-8')
    print(json.dumps([{'seed': x['seed'], 'boundary_matches': [b.get('equal') for b in x['boundaries']],
                      'endpoint_status': x['endpoint_status']} for x in results]))


if __name__ == '__main__':
    main()
