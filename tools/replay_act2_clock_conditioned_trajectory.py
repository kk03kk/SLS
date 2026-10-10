"""Separate conditional diagnosis using passive stock clock logs, never guessed counts."""

from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path

from sls.audit.stock_clock import stock_clock_rows, verify_sealed_oracle


def main() -> int:
    from sls.audit.trajectory_reader import read_trajectory
    from tools.replay_act2_production_trajectory import replay

    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('trajectory', type=Path)
    parser.add_argument('--oracle-build', type=Path, required=True)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    if args.output.exists():
        raise FileExistsError('refuse to overwrite conditional evidence')
    metadata, _ = read_trajectory(args.trajectory)
    suffix = '-' + str(metadata['seed'])
    if not args.trajectory.stem.endswith(suffix):
        raise ValueError('trajectory does not identify its batch and seed')
    batch_path = args.trajectory.with_name(args.trajectory.stem[:-len(suffix)] + '.json')
    batch = json.loads(batch_path.read_text(encoding='utf-8'))
    launch_path = batch_path.with_suffix('.launch.json')
    launch = json.loads(launch_path.read_text(encoding='utf-8'))
    build = json.loads(args.oracle_build.read_text(encoding='utf-8'))
    oracle = args.oracle_build.with_name(args.oracle_build.name.removesuffix('.build.json') + '.jar')
    verify_sealed_oracle(oracle, build)
    row = [r for r in batch['runs'] if r['seed'] == metadata['seed']]
    digest = hashlib.sha256(args.trajectory.read_bytes()).hexdigest()
    if (not batch.get('execution_complete') or batch.get('execution_error')
            or len(row) != 1 or row[0]['sha256'] != digest
            or launch['mode'] != 'production' or launch['recovery_status'] != 'RECOVERED'
            or launch['completion']['exit_code'] != 0
            or launch['oracle_sha256'] != build['output_sha256']
            or build.get('schema') != 'sls-oracle-build-v1'
            or build.get('used_existing_oracle') is not False):
        raise ValueError('stock clock execution/source/recovery identity failure')
    recovery = Path(launch['recovery_journal'])
    journal = json.loads(recovery.read_text(encoding='utf-8'))
    if journal['status'] != 'RECOVERED':
        raise ValueError('clock log was not collected in a recovered owned process')
    stdout = recovery.with_name('stdout.log')
    clocks = stock_clock_rows(stdout.read_text(encoding='utf-8', errors='replace'), metadata['seed'])
    if not clocks:
        raise ValueError('no independent stock clock witness; do not infer from outcomes')
    result = replay(args.trajectory, clock_witnesses=clocks)
    result.update(stock_clock_log_sha256=hashlib.sha256(stdout.read_bytes()).hexdigest(),
                  oracle_build_sha256=build['output_sha256'],
                  oracle_build_manifest_sha256=hashlib.sha256(args.oracle_build.read_bytes()).hexdigest(),
                  launch_evidence_sha256=hashlib.sha256(launch_path.read_bytes()).hexdigest(),
                  batch_sha256=hashlib.sha256(batch_path.read_bytes()).hexdigest())
    args.output.write_text(json.dumps(result, indent=2) + '\n', encoding='utf-8')
    print(json.dumps({k: result[k] for k in ('seed', 'status', 'boundaries_checked', 'clock_inputs')}))
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
