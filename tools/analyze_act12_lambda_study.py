"""Verify completed matched lambda arms and compare fixed endpoints on development data."""
from __future__ import annotations

import argparse
import json
import tomllib
from pathlib import Path

from sls.rl.training_contract import source_sha256
from tools.analyze_act12_pilot import (
    late_success_windows,
    paired_act12,
    require_pilot_health,
)
from tools.analyze_reward_screen import analyze_run, read
from tools.submit_act12_lambda_study import ROOT, validate_study


def decision(comparisons: list[dict], windows: list[dict]) -> str:
    gains = [r['net'] / r['paired_seeds'] for r in comparisons]
    ps = sorted(r['exact_mcnemar_p'] for r in comparisons)
    significant = ps[0] <= 0.025 and ps[1] <= 0.05
    if min(gains) >= 0.01 and significant and all(w['successes'] > 0 for w in windows):
        return 'SUPPORTS_SECOND_TRAINING_SEED_REPLICATION'
    return 'INCONCLUSIVE' if min(gains) > 0 else 'NO_CONFIRMED_JOINT_GAIN'


def analyze(study_path: Path) -> dict:
    study = read(study_path)
    paths = validate_study(study, deep=True)
    summaries, endpoints, references, windows = [], [], [], []
    for path in paths:
        plan = read(path)
        config = tomllib.loads((ROOT / plan['config']).read_text(encoding='utf-8'))
        run = ROOT / config['run']['output']
        summary, _, _ = analyze_run(run, horizon=2)
        if summary['new_decisions'] < plan['recipe']['additional_decisions']:
            raise ValueError('study budget incomplete')
        endpoint = read(run / 'endpoint-evaluation.json')
        reference = read(run / 'reference-evaluation.json')
        if (endpoint['checkpoint_sha256'] != summary['verified_bundle_files']['final.pt']
                or reference['checkpoint_sha256'] != plan['parent']['sha256']):
            raise ValueError('study endpoint or parent identity changed')
        metrics = [json.loads(line) for line in (run / 'stages/train/metrics.jsonl').read_text(
            encoding='utf-8').splitlines()]
        require_pilot_health([endpoint['result'], reference['result']], metrics)
        summaries.append(summary)
        endpoints.append(endpoint)
        references.append(reference)
        windows.append(late_success_windows(metrics, plan['parent']['environment_steps']))
    # A differing runtime is an evaluation incompatibility, not evidence of gain.
    paired_act12(references[0], references[1], labels=('parent-control-runtime', 'parent-experimental-runtime'))
    if references[0]['result']['seed_results'] != references[1]['result']['seed_results']:
        raise ValueError('same frozen reference differs across arm runtimes')
    comparisons = [paired_act12(endpoints[0], endpoints[1], labels=('lambda-.98', 'lambda-1')),
                   paired_act12(references[1], endpoints[1], labels=('frozen90', 'lambda-1'))]
    return {'schema': 'sls-act12-lambda-study-analysis-v1',
            'study_sha256': source_sha256(study_path), 'arms': summaries,
            'primary_fixed_endpoint_comparisons': comparisons,
            'experimental_late_success_windows': windows[1],
            'research_decision': decision(comparisons, windows[1]),
            'multiple_testing': 'Two superiority comparisons; exact two-sided McNemar, Holm family alpha .05.',
            'metric': 'normal-Neow-start Act1+Act2 joint clear',
            'limitations': 'One shared training seed, development only; no final holdout or stock win-rate claim.'}


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--study', type=Path, required=True)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    if args.output.exists():
        raise FileExistsError('refuse to overwrite study analysis')
    result = analyze(args.study)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(result, indent=2) + '\n', encoding='utf-8')
    print(result['research_decision'])
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
