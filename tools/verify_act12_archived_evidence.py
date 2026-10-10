"""Recheck archived Act2 evidence identities without loading models or native code.

This checks evidence integrity, not new simulator execution or whole-game parity.
"""

from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path

from sls.audit.stock_clock import stock_clock_rows


def digest(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def read(path: Path) -> dict:
    return json.loads(path.read_text(encoding='utf-8'))


def verify_clock_archive(report_path: Path, batch_path: Path) -> dict:
    report, batch = read(report_path), read(batch_path)
    launch_path = batch_path.with_suffix('.launch.json')
    launch = read(launch_path)
    if (not batch.get('execution_complete') or batch.get('execution_error')
            or launch.get('mode') != 'production'
            or launch.get('recovery_status') != 'RECOVERED'
            or launch.get('completion', {}).get('exit_code') != 0
            or launch.get('execution_error')):
        raise ValueError('invalid production execution/recovery')
    if (report.get('schema') != 'sls-act2-clock-conditioned-replay-v1'
            or report.get('status') != 'CONDITIONAL_PUBLIC_TRAJECTORY_MATCH'
            or report.get('first_divergence') is not None
            or report.get('terminal_reason_verified') is not True
            or report.get('purpose') != 'CONDITIONAL_RULES_DIAGNOSTIC_NOT_UNCONDITIONAL_PRODUCTION_PASS'):
        raise ValueError('invalid conditional report classification')
    for path, field in [(batch_path, 'batch_sha256'), (launch_path, 'launch_evidence_sha256')]:
        if digest(path) != report[field]:
            raise ValueError(f'identity mismatch: {field}')
    journal_path = Path(launch['recovery_journal'])
    if read(journal_path)['status'] != 'RECOVERED':
        raise ValueError('unrecovered journal')
    stdout = journal_path.with_name('stdout.log')
    if digest(stdout) != report['stock_clock_log_sha256']:
        raise ValueError('clock stdout identity mismatch')
    rows = stock_clock_rows(stdout.read_text(encoding='utf-8', errors='strict'), report['seed'])
    recorded = [{k: row[k] for k in ('seed', 'floor', 'serial', 'updates')}
                for row in report['clock_inputs']]
    if not rows or rows != recorded:
        raise ValueError('report clock differs from passive stock witness')
    runs = [row for row in batch['runs'] if row['seed'] == report['seed']]
    if len(runs) != 1:
        raise ValueError('ambiguous trajectory')
    trajectory = Path(runs[0]['output'])
    if digest(trajectory) != runs[0]['sha256'] or runs[0]['sha256'] != report['trajectory_sha256']:
        raise ValueError('trajectory identity mismatch')
    records = [json.loads(line) for line in trajectory.read_text(encoding='utf-8').splitlines()]
    metadata, boundaries = records[0], records[1:]
    if (metadata.get('seed') != report['seed'] or metadata.get('backend') != 'original'
            or metadata.get('environment') != report['evaluation_environment']
            or len(boundaries) != report['boundaries_checked'] or not boundaries[-1]['terminal']
            or (boundaries[0]['screen'], boundaries[0]['act'], boundaries[0]['floor'],
                boundaries[0]['observation']['run']['ascension']) != ('NEOW', 1, 0, 20)):
        raise ValueError('trajectory context mismatch')
    for clock in report['clock_inputs']:
        boundary = boundaries[clock['boundary']]
        if boundary['floor'] != clock['floor'] or boundary.get('chosen_action') is None:
            raise ValueError('clock boundary context mismatch')
    if launch['oracle_sha256'] != report['oracle_build_sha256']:
        raise ValueError('Oracle launch identity mismatch')
    return {'seed': report['seed'], 'boundaries': len(boundaries), 'clock_inputs': recorded,
            'status': 'ARCHIVED_CONDITIONAL_EVIDENCE_INTEGRITY_PASS',
            'unconditional_production_pass': False, 'new_native_execution': False}


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--root', type=Path, default=Path(__file__).resolve().parents[1])
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    if args.output.exists():
        raise FileExistsError('refuse to overwrite evidence')
    audit = read(args.root / 'docs/results/fullrun-parity-20261007/act12-training-audit-r1.json')
    directory = args.root / 'local/audits/fullrun-parity-20261007'
    inputs = []
    for name, expected in audit['inputs_sha256'].items():
        if digest(directory / name) != expected:
            raise ValueError(f'archived input identity mismatch: {name}')
        inputs.append({'name': name, 'sha256': expected})
    clock = verify_clock_archive(directory / 'automaton-clock-conditioned-r1.json',
                                 directory / 'production-bosses-r1.json')
    result = {'schema': 'sls-act12-offline-evidence-review-v1',
              'audit_sha256': digest(args.root / 'docs/results/fullrun-parity-20261007/act12-training-audit-r1.json'),
              'verified_inputs': inputs, 'automaton': clock,
              'scope': 'ARCHIVED_EVIDENCE_INTEGRITY_ONLY',
              'new_model_or_native_execution': False,
              'remaining': ['unconditional Discovery frame-driven RNG parity',
                            'uncovered shared-mechanic/content branches']}
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(result, indent=2) + '\n', encoding='utf-8')
    print(json.dumps({'verified_inputs': len(inputs), 'automaton': clock}))
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
