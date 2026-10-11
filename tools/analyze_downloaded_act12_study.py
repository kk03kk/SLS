"""Recompute historical Act1-2 study evidence without torch, CUDA or simulation.

Missing deployment exports and failed scientific gates are reported, never
invented or turned into a successful promotion. This is not a resume validator.
"""
from __future__ import annotations

import argparse
import json
import math
import statistics
import sys
import tomllib
from collections import Counter
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))
sys.path.insert(0, str(ROOT / "src"))

from sls.rl.training_contract import source_sha256
from tools.analyze_act12_pilot import (
    late_success_windows,
    paired_act12,
    value_diagnostics,
)
from tools.analyze_reward_screen import digest, outcomes, read
from tools.compare_run_arms import exact_mcnemar


def paired_reach(left: dict, right: dict) -> dict:
    a = {r['seed']: '2' in r['bosses'] for r in left['seed_results']}
    b = {r['seed']: '2' in r['bosses'] for r in right['seed_results']}
    if a.keys() != b.keys():
        raise ValueError('reach seed mismatch')
    lost = sum(a[s] and not b[s] for s in a)
    gained = sum(b[s] and not a[s] for s in a)
    return {'both_reached': sum(a[s] and b[s] for s in a),
            'lost_reach': lost, 'gained_reach': gained,
            'exact_mcnemar_p': exact_mcnemar(lost, gained)}


def entry_summary(rows: list[dict]) -> dict:
    entries = [r['act_entries']['2'] for r in rows if '2' in r.get('act_entries', {})]
    if not entries:
        return {'samples': 0}
    return {'samples': len(entries), **{
        key: statistics.mean(f(e) for e in entries) for key, f in {
            'hp': lambda e: e['hp'], 'hp_fraction': lambda e: e['hp'] / e['max_hp'],
            'gold': lambda e: e['gold'], 'deck_size': lambda e: len(e['deck']),
            'potions': lambda e: len(e['potions']), 'relics': lambda e: len(e['relics']),
        }.items()}}


def failure_summary(rows: list[dict]) -> dict:
    failures = [r for r in rows if not r['success']]
    return {'reasons': dict(Counter(r['reason'] for r in failures)),
            'cycle_limit_floor_counts': dict(Counter(str(r['floor']) for r in failures if r['reason'] == 'cycle_limit')),
            'act2_last_recorded_enemy_contexts': dict(Counter(
                '+'.join(r.get('last_context', {}).get('enemy_ids', [])) or 'NONCOMBAT_OR_UNRECORDED'
                for r in failures if '2' in r['bosses']).most_common()),
            'winning_seeds': [r['seed'] for r in rows if r['success']]}


def extension_decision(comparisons: dict, arms: dict) -> dict:
    primary = [comparisons[k] for k in ('lambda1_vs_control', 'lambda1_vs_parent')]
    ps = sorted(c['exact_mcnemar_p'] for c in primary)
    gain = all(c['net'] / c['paired_seeds'] >= .01 for c in primary)
    significant = ps[0] <= .025 and ps[1] <= .05
    def late(arm):
        return all(w['successes'] > 0 for w in arms[arm]['late_success_windows'])
    if (gain and significant and all(a['strict_registered_health_gate_passed'] for a in arms.values())
            and late('experimental')):
        choice = 'experimental'
    else:
        secondary = comparisons['control_vs_parent_secondary']
        choice = 'control' if (secondary['net'] / secondary['paired_seeds'] >= .01
                              and secondary['exact_mcnemar_p'] <= .05
                              and arms['control']['strict_registered_health_gate_passed']
                              and late('control')) else None
    return {'automatic_20m_extension': choice is not None, 'eligible_parent_arm': choice,
            'primary_gain_passed': gain, 'primary_holm_passed': significant,
            'registered_health_by_arm': {k: a['strict_registered_health_gate_passed'] for k, a in arms.items()},
            'secondary_analysis_identity': 'historical control vs parent; not preregistered lambda superiority'}


def verify_bundle(run: Path) -> dict:
    bundle = read(run / 'training-bundle.json')
    verified = {}
    for name, expected in bundle['files'].items():
        path = (run / name).resolve()
        if not path.is_relative_to(run.resolve()):
            raise ValueError('bundle path escapes run')
        if digest(path) != expected:
            raise ValueError(f'bundle hash mismatch: {name}')
        verified[name] = expected
    return verified


def verify_run(run: Path, plan: dict, study: dict) -> dict:
    verified = verify_bundle(run)
    manifest = read(run / 'run-manifest.json')
    config = tomllib.loads((run / 'training-config.toml').read_text(encoding='utf-8'))
    if (manifest['status'] != 'COMPLETE' or manifest['stages']['train']['status'] != 'COMPLETE'
            or digest(run / 'training-config.toml') != manifest['config_sha256']
            or source_sha256(run / 'training-config.toml') != plan['config_sha256']):
        raise ValueError('incomplete run or invalid config identity')
    if (manifest['native_source_sha256'] != plan['parent']['target_native_source_sha256']
            or manifest['training_implementation_sha256'] != plan['target_training_implementation_sha256']
            or config['ppo'] != manifest['ppo']
            or any(manifest['model'].get(k) != v for k, v in config['model'].items())
            or manifest['model']['encoding_schema'] != manifest['encoding_schema']
            or manifest['model']['vocabulary_hash'] != manifest['vocabulary_sha256']
            or config['warm_start']['checkpoint_sha256'] != plan['parent']['sha256']
            or manifest['environment_steps'] < config['stages']['train']['target_environment_steps']):
        raise ValueError('registered source, configuration or budget mismatch')
    records = [json.loads(line) for line in (run / 'stages/train/metrics.jsonl').read_text().splitlines()]
    updates = [r for r in records if 'update_seconds' in r]
    if len(updates) != manifest['updates'] or [r['update'] for r in updates] != list(range(1, len(updates)+1)):
        raise ValueError('missing or duplicate updates')
    if any(isinstance(v, float) and not math.isfinite(v) for r in updates for v in r.values()):
        raise ValueError('nonfinite update metric')
    periodic, confirm = study['development_periodic_seeds'], study['development_confirmation_seeds']
    if (manifest['periodic_evaluation_seeds'] != periodic or manifest['final_evaluation_seeds'] != confirm
            or config['run']['seed'] != study['training_seed']
            or not config['run']['training_seed_limit'] <= min(periodic[0], confirm[0])
            or periodic[1] > confirm[0] or confirm[1] > study['reserved_final_seeds'][0]):
        raise ValueError('seed identity or namespace mismatch')
    endpoint, reference, selected = (read(run / f'{name}-evaluation.json')
                                     for name in ('endpoint', 'reference', 'final'))
    best = read(run / 'stages/train/selection/best_progress.json')
    if (endpoint['checkpoint_sha256'] != verified['final.pt']
            or endpoint['checkpoint_environment_steps'] != manifest['environment_steps']
            or reference['checkpoint_sha256'] != plan['parent']['sha256']
            or selected['checkpoint_sha256'] != best['checkpoint_sha256']
            or best['checkpoint_sha256'] != verified['stages/train/selection/best_progress.pt']):
        raise ValueError('checkpoint evidence identity mismatch')
    if endpoint['simulator']['native_source_sha256'] != manifest['native_source_sha256']:
        raise ValueError('evaluation source identity mismatch')
    paired_act12(reference, endpoint)
    paired_act12(reference, selected)
    curve = [{'steps': r['environment_steps'],
              'best_checkpoint_updated': r.get('best_checkpoint_updated'),
              **outcomes(r['evaluation'], tuple(periodic), horizon=2)}
             for r in records if 'evaluation' in r]
    confirmations = {name: outcomes(e['result'], tuple(confirm), horizon=2)
                     for name, e in (('parent', reference), ('endpoint', endpoint), ('selected', selected))}
    metric_keys = ('approx_kl_final', 'clip_fraction', 'gradient_norm', 'gradient_clip_fraction',
                   'value_explained_variance', 'value', 'entropy', 'neow_mean_normalized_entropy',
                   'advantage_std_combat', 'advantage_std_run', 'advantage_std_choice',
                   'advantage_scale_combat', 'advantage_scale_run', 'advantage_scale_choice')
    health_keys = ('backend_errors', 'backend_truncations', 'step_limits', 'cycle_limits', 'timeouts')
    training_limits = {k: sum(r[k] for r in updates) for k in
                       ('terminations_backend_truncated', 'terminations_step_limit', 'terminations_cycle_limit')}
    evaluation_limits = {name: {k: e['result'][k] for k in health_keys}
                         for name, e in (('parent', reference), ('endpoint', endpoint), ('selected', selected))}
    strict_gate = (not any(training_limits.values()) and not any(
        v for group in evaluation_limits.values() for v in group.values())
        and not any(v for c in curve for v in c['health'].values()))
    elapsed = sum(r['update_seconds'] for r in updates)
    new_steps = manifest['environment_steps'] - plan['parent']['environment_steps']
    if new_steps < study['additional_decisions_per_arm']:
        raise ValueError('incomplete registered additional budget')
    return {'run': run.name, 'manifest': manifest, 'verified_bundle_files': verified,
            'checkpoint_payload_validation': 'NOT_RUN: hashes/provenance verified; no torch loading, tensor or resume equality claim',
            'missing_deployment_export': not (run / f'{run.name}.pt').exists(),
            'checkpoint_inventory': {'available': sorted(verified),
                                    'periodic_checkpoint_files_in_download': sorted(p.name for p in run.glob('checkpoint-steps-*.pt'))},
            'selected': best, 'curve': curve, 'confirmation': confirmations,
            'late_success_windows': late_success_windows(records, plan['parent']['environment_steps']),
            'strict_registered_health_gate_passed': strict_gate,
            'training_limits': training_limits, 'evaluation_limits': evaluation_limits,
            'new_decisions': new_steps, 'update_hours': elapsed / 3600,
            'update_throughput': new_steps / elapsed,
            'wall_hours': (manifest['stages']['train']['finished_unix'] - manifest['created_unix']) / 3600,
            'timing_seconds': {k: sum(r[k] for r in updates) for k in (
                'collect_seconds', 'optimize_seconds', 'collect_encode_seconds',
                'collect_transition_seconds', 'collect_policy_seconds', 'collect_worker_step_seconds')},
            'metric_summary': {key: {'mean': statistics.mean(r[key] for r in updates),
                                    'first10': statistics.mean(r[key] for r in updates[:10]),
                                    'last10': statistics.mean(r[key] for r in updates[-10:])}
                               for key in metric_keys},
            'neow_counts': {str(i): sum(r[f'neow_option_{i}_count'] for r in updates) for i in range(4)},
            'value_anchors': value_diagnostics(endpoint, config['ppo']),
            'failures': failure_summary(endpoint['result']['seed_results']),
            'entry_resources': entry_summary(endpoint['result']['seed_results'])}


def analyze(runs: Path, study_path: Path) -> dict:
    study = read(study_path)
    plans, configs, arm_reports, endpoints, references = {}, {}, {}, {}, {}
    for arm in ('control', 'experimental'):
        binding = study['arms'][arm]
        plan_path = ROOT / binding['plan']
        if source_sha256(plan_path) != binding['plan_sha256']:
            raise ValueError('registered plan hash changed')
        plan = plans[arm] = read(plan_path)
        run = runs / Path(tomllib.loads((ROOT / plan['config']).read_text())['run']['output']).name
        configs[arm] = tomllib.loads((run / 'training-config.toml').read_text())
        arm_reports[arm] = verify_run(run, plan, study)
        endpoints[arm] = read(run / 'endpoint-evaluation.json')
        references[arm] = read(run / 'reference-evaluation.json')
    for section in ('model', 'stages', 'warm_start'):
        if configs['control'][section] != configs['experimental'][section]:
            raise ValueError('matched configuration differs')
    for section, excluded in (('ppo', {'gae_lambda'}), ('run', {'output', 'benchmark'})):
        if ({k: v for k, v in configs['control'][section].items() if k not in excluded}
                != {k: v for k, v in configs['experimental'][section].items() if k not in excluded}):
            raise ValueError('unexpected matched variable')
    if tuple(configs[a]['ppo']['gae_lambda'] for a in ('control', 'experimental')) != (.98, 1):
        raise ValueError('lambda identity mismatch')
    paired_act12(references['control'], references['experimental'])
    if references['control']['result']['seed_results'] != references['experimental']['result']['seed_results']:
        raise ValueError('same parent reference differs')
    comparisons = {
        'lambda1_vs_control': paired_act12(endpoints['control'], endpoints['experimental'], labels=('control', 'lambda1')),
        'lambda1_vs_parent': paired_act12(references['experimental'], endpoints['experimental'], labels=('parent', 'lambda1')),
        'control_vs_parent_secondary': paired_act12(references['control'], endpoints['control'], labels=('parent', 'control'))}
    resource_pairs = {}
    for arm in ('control', 'experimental'):
        parent = references[arm]['result']['seed_results']
        candidate = endpoints[arm]['result']['seed_results']
        common = ({r['seed'] for r in parent if '2' in r['bosses']}
                  & {r['seed'] for r in candidate if '2' in r['bosses']})
        resource_pairs[arm] = {'reach_pairs': paired_reach(references[arm]['result'], endpoints[arm]['result']),
                               'parent_common_survivors': entry_summary([r for r in parent if r['seed'] in common]),
                               'endpoint_common_survivors': entry_summary([r for r in candidate if r['seed'] in common])}
    return {'schema': 'sls-downloaded-act12-evidence-v1', 'study_sha256': source_sha256(study_path),
            'arms': arm_reports, 'paired_comparisons': comparisons, 'resource_pairs': resource_pairs,
            **extension_decision(comparisons, arm_reports),
            'limitations': ['One training seed; development only; holdout sealed.',
                            'No checkpoint deserialization, local GPU, simulator, or performance benchmark.',
                            'Reached-act resources and boss populations depend on policy.',
                            'No claim about original-game win rate or causal catastrophic forgetting.']}


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--runs', type=Path, default=ROOT / 'local/runs')
    parser.add_argument('--study', type=Path, required=True)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    if args.output.exists():
        raise FileExistsError('refuse to overwrite evidence')
    result = analyze(args.runs, args.study)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    with args.output.open('x', encoding='utf-8') as stream:
        json.dump(result, stream, indent=2, ensure_ascii=False, allow_nan=False)
        stream.write('\n')
    print(json.dumps({'automatic_20m_extension': result['automatic_20m_extension'],
                      'output': str(args.output)}))
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
