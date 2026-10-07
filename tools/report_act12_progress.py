"""Recompute a unified verified run report; all raw evidence remains read-only."""
from __future__ import annotations

import argparse
import json
import statistics
from pathlib import Path

from tools.analyze_act12_pilot import require_pilot_health, value_diagnostics
from tools.analyze_reward_screen import analyze_run, outcomes


def report(run: Path, *, allow_missing_export: bool = False) -> dict:
    summary, _, selected = analyze_run(run, horizon=2, allow_missing_export=allow_missing_export)
    if summary['config']['run']['profile'] != 'IRONCLAD_A20_ACT2':
        raise ValueError('requires normal-start A20 Act1+Act2 run')
    endpoint = json.loads((run / 'endpoint-evaluation.json').read_text(encoding='utf-8'))
    if endpoint['checkpoint_sha256'] != summary['verified_bundle_files']['final.pt']:
        raise ValueError('fixed endpoint identity mismatch')
    rows = [json.loads(line) for line in (run / 'stages/train/metrics.jsonl').read_text(encoding='utf-8').splitlines()]
    updates = [r for r in rows if 'update_seconds' in r]
    evaluations = [endpoint['result'], selected['result']] + [r['evaluation'] for r in rows if 'evaluation' in r]
    health = require_pilot_health(evaluations, rows)
    keys = sorted({k for r in updates for k, v in r.items() if isinstance(v, (int, float))
                   and any(s in k for s in ('kl', 'value', 'advantage', 'gradient', 'neow', 'seconds', 'throughput', 'entropy'))})
    return {'schema': 'sls-act12-progress-report-v1', 'verified_run': summary, 'health': health,
            'fixed_endpoint_outcomes': outcomes(endpoint['result'], tuple(endpoint['seeds']), horizon=2),
            'periodic_curve': summary['periodic_curve'],
            'metrics': {k: {'count': len(v), 'mean': statistics.mean(v), 'min': min(v), 'max': max(v), 'last': v[-1]}
                        for k in keys if (v := [r[k] for r in updates if k in r])},
            'true_training_joint_successes': sum(r.get('terminations_success', 0) for r in updates),
            'critic_off_policy_anchors': value_diagnostics(endpoint, summary['config']['ppo']),
            'interpretation': {'confirmed': ['Recorded outcomes and healthy execution are recomputable.'],
                               'hypotheses': ['Critic interference, advantage rescaling and exploration require causal evidence.'],
                               'not_claimed': ['Act1 reach decline alone proves forgetting.', 'A periodic peak establishes joint improvement.']},
            'limitations': 'Development/simulator only; reached-Act2 populations differ by policy; reward and off-policy anchors are diagnostics.'}


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--run', type=Path, required=True)
    parser.add_argument('--output', type=Path, required=True)
    parser.add_argument('--allow-missing-export', action='store_true', help='Explicit historical incomplete-download exception; no model is fabricated')
    args = parser.parse_args()
    if args.output.exists():
        raise FileExistsError('refuse to overwrite evidence')
    result = report(args.run, allow_missing_export=args.allow_missing_export)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    with args.output.open('x', encoding='utf-8') as stream:
        json.dump(result, stream, indent=2)


if __name__ == '__main__':
    main()
