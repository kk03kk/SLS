"""Bind a 20M continuation only after verified completed lambda-study outcomes."""
from __future__ import annotations

import argparse
import copy
import gzip
import json
import math
from pathlib import Path

from sls.rl.preparation import read_config
from sls.rl.training_contract import (
    ROOT,
    local_source_digest,
    native_source_digest,
    sha256_file,
    training_implementation_digest,
)
from tools.analyze_act12_lambda_study import analyze
from tools.analyze_act12_pilot import paired_act12

PERIODIC = (8000012000000, 8000012000512)
CONFIRMATION = (8000013000000, 8000013004096)
OPERATOR_PATHS = ('tools/prepare_act12_long_run.py', 'tools/initialize_act12_continuation.py',
                  'tools/initialize_act1_continuation.py', 'tools/run_act12_long_run.py',
                  'tools/submit_act12_long_run.py', 'tools/verify_act12_diagnostics.py')


def select_arm(report: dict, control_pair: dict, control_windows: list) -> str | None:
    if report['research_decision'] == 'SUPPORTS_SECOND_TRAINING_SEED_REPLICATION':
        return 'experimental'
    if (control_pair['net'] / control_pair['paired_seeds'] >= .01
            and control_pair['exact_mcnemar_p'] <= .05
            and all(w['successes'] > 0 for w in control_windows)):
        return 'control'
    return None


def toml_text(payload: dict) -> str:
    lines = []
    def table(data, prefix):
        if prefix:
            lines.append('[' + '.'.join(prefix) + ']')
        for key, value in data.items():
            if not isinstance(value, dict):
                lines.append(f'{key} = ' + json.dumps(value, ensure_ascii=False))
        lines.append('')
        for key, value in data.items():
            if isinstance(value, dict):
                table(value, (*prefix, key))
    table(payload, ())
    return '\n'.join(lines)


def reject_used_seeds(paths: list[Path]) -> None:
    """Inspect actual evaluation artifacts/metrics, not unexecuted configs."""
    for directory in paths:
        for path in directory.rglob('*.json*'):
            name = path.name.removesuffix('.gz')
            if not name.endswith(('.json', '.jsonl')):
                continue
            if not ('evaluation' in name or name == 'metrics.jsonl'):
                continue
            text = gzip.decompress(path.read_bytes()).decode('utf-8') if path.suffix == '.gz' else path.read_text(encoding='utf-8')
            records = [json.loads(line) for line in text.splitlines()] if name.endswith('.jsonl') else [json.loads(text)]
            def inspect(value):
                if isinstance(value, dict):
                    seeds = value.get('seeds')
                    if isinstance(seeds, list) and len(seeds) == 2 and all(isinstance(v, int) for v in seeds):
                        if any(seeds[0] < end and start < seeds[1] for start, end in (PERIODIC, CONFIRMATION)):
                            raise ValueError('development interval was already evaluated: ' + str(path))
                    seed = value.get('seed')
                    if isinstance(seed, int) and any(start <= seed < end for start, end in (PERIODIC, CONFIRMATION)):
                        raise ValueError('development seed was already evaluated: ' + str(path))
                    for child in value.values():
                        inspect(child)
                elif isinstance(value, list):
                    for child in value:
                        inspect(child)
            for record in records:
                inspect(record)


def prepare(study: Path, output: Path) -> dict:
    if output.exists():
        raise FileExistsError('refuse to overwrite bound long-run preparation')
    report = analyze(study)
    from tools.analyze_act12_pilot import late_success_windows
    from tools.submit_act12_lambda_study import validate_study
    plans = validate_study(json.loads(study.read_text(encoding='utf-8')), deep=True)
    configs = [read_config(ROOT / json.loads(p.read_text(encoding='utf-8'))['config']) for p in plans]
    runs = [ROOT / c['run']['output'] for c in configs]
    control_endpoint = json.loads((runs[0] / 'endpoint-evaluation.json').read_text(encoding='utf-8'))
    reference = json.loads((runs[0] / 'reference-evaluation.json').read_text(encoding='utf-8'))
    pair = paired_act12(reference, control_endpoint, labels=('frozen90', 'lambda-.98'))
    metrics = [json.loads(line) for line in (runs[0] / 'stages/train/metrics.jsonl').read_text(encoding='utf-8').splitlines()]
    windows = late_success_windows(metrics, 90013696)
    arm = select_arm(report, pair, windows)
    evidence = {'study_analysis': report, 'secondary_control_vs_parent': pair,
                'secondary_control_windows': windows, 'chosen_arm': arm,
                'followup_policy': '20M endpoint continuation; no additional training-seed replication gate',
                'secondary_test_limit': 'Control test is secondary, unadjusted p<=.05; not the preregistered lambda superiority claim.'}
    if arm is None:
        raise ValueError('no qualifying joint gain: do not bind or submit a 20M extension')
    reject_used_seeds([ROOT / 'local/runs', ROOT / 'docs/results'])
    index = int(arm == 'experimental')
    parent = runs[index]
    config = copy.deepcopy(configs[index])
    config.pop('warm_start', None)
    manifest = json.loads((parent / 'run-manifest.json').read_text(encoding='utf-8'))
    if (manifest['training_implementation_sha256'] != training_implementation_digest()
            or manifest['native_source_sha256'] != native_source_digest()):
        raise ValueError('current learning/environment implementation differs from parent')
    run = config['run']
    child = 'local/runs/ironclad-a20-act12-long20m-r1'
    run.update(output=child, benchmark='local/runs/preparation/ironclad-a20-act12-long20m-r1/benchmark.json',
               continuation_from=parent.relative_to(ROOT).as_posix(),
               continuation_checkpoint_sha256=sha256_file(parent / 'latest.pt'),
               continuation_selection_evidence='completed-endpoint',
               development_reference_checkpoint=(parent / 'final.pt').relative_to(ROOT).as_posix(),
               development_reference_sha256=sha256_file(parent / 'final.pt'),
               development_reference_profile='IRONCLAD_A20_ACT2',
               periodic_evaluation_seed_start=PERIODIC[0], periodic_evaluation_seed_count=512,
               final_evaluation_seed_start=CONFIRMATION[0], final_evaluation_seed_count=4096)
    config['stages']['train'].update(target_environment_steps=manifest['environment_steps'] + 20_000_000,
                                   evaluate_every_steps=2_000_000, checkpoint_every_steps=1_000_000,
                                   minimum_evaluation_episodes=512, minimum_final_evaluation_episodes=4096)
    benchmark = json.loads((ROOT / configs[index]['run']['benchmark']).read_text(encoding='utf-8'))
    rows = [r for r in benchmark['results'] if [r['workers'], r['shards']] == run['worker_layout']]
    rate = float(rows[0]['decisions_per_second']) if len(rows) == 1 else 0
    if not math.isfinite(rate) or rate <= 0:
        raise ValueError('no valid measured NUS pinned-layout throughput')
    allocations = math.ceil((20_000_000 / rate * 1.5 + 8 * 3600) / (40 * 3600))
    output.mkdir(parents=True)
    path = output / 'training-config.toml'
    path.write_text(toml_text(config), encoding='utf-8')
    plan = {'schema': 'sls-act12-long-run-v1', 'status': 'BOUND_TO_COMPLETED_STUDY',
            'study': study.resolve().relative_to(ROOT).as_posix(), 'study_sha256': sha256_file(study),
            'config': path.relative_to(ROOT).as_posix(), 'config_sha256': sha256_file(path),
            'parent_checkpoint_sha256': run['continuation_checkpoint_sha256'],
            'native_source_sha256': native_source_digest(),
            'training_implementation_sha256': training_implementation_digest(),
            'operator_sha256': local_source_digest(OPERATOR_PATHS),
            'allocations': allocations, 'allocation_hours': 48, 'additional_decisions': 20_000_000,
            'benchmark_rate': rate, 'decision_evidence': evidence,
            'limitations': 'One training seed; simulator development evidence only. Rate is an estimate, not a completion guarantee.'}
    (output / 'plan.json').write_text(json.dumps(plan, indent=2) + '\n', encoding='utf-8')
    return plan


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--study', type=Path, required=True)
    parser.add_argument('--output', type=Path, default=ROOT / 'local/operator/act12-long20m-r1')
    args = parser.parse_args()
    print(json.dumps(prepare(args.study, args.output), indent=2))


if __name__ == '__main__':
    main()
