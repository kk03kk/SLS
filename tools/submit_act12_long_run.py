"""Torch-free login submission of a bound, state-preserving 20M allocation chain."""
from __future__ import annotations

import argparse
import json
import shlex
import subprocess
import sys
from pathlib import Path

from sls.rl.preparation import read_config
from sls.rl.training_contract import (
    ROOT,
    local_source_digest,
    native_source_digest,
    sha256_file,
    training_implementation_digest,
)
from tools.prepare_act12_long_run import (
    CONFIRMATION,
    OPERATOR_PATHS,
    PERIODIC,
    reject_used_seeds,
)
from tools.submit_slurm import _parser, build_sbatch_command


def validate(path: Path) -> tuple[dict, Path, dict]:
    plan = json.loads(path.read_text(encoding='utf-8'))
    if plan.get('schema') != 'sls-act12-long-run-v1' or plan.get('status') != 'BOUND_TO_COMPLETED_STUDY':
        raise ValueError('long run is not bound to completed development evidence')
    config_path = (ROOT / plan['config']).resolve()
    if not config_path.is_relative_to(ROOT) or sha256_file(config_path) != plan['config_sha256']:
        raise ValueError('bound long-run configuration changed')
    if (plan['native_source_sha256'] != native_source_digest()
            or plan['training_implementation_sha256'] != training_implementation_digest()
            or plan['operator_sha256'] != local_source_digest(OPERATOR_PATHS)
            or sha256_file(ROOT / plan['study']) != plan['study_sha256']):
        raise ValueError('bound long-run sources or study changed')
    config = read_config(config_path)
    run = config['run']
    original = read_config(ROOT / run['continuation_from'] / 'training-config.toml')
    manifest = json.loads((ROOT / run['continuation_from'] / 'run-manifest.json').read_text(encoding='utf-8'))
    if (manifest['status'] != 'COMPLETE' or run['profile'] != 'IRONCLAD_A20_ACT2'
            or config['ppo'] != original['ppo'] or config['model'] != original['model']
            or run['worker_layout'] != original['run']['worker_layout']
            or sha256_file(ROOT / run['continuation_from'] / 'latest.pt') != plan['parent_checkpoint_sha256']
            or config['stages']['train']['target_environment_steps'] != manifest['environment_steps'] + 20_000_000
            or plan['additional_decisions'] != 20_000_000
            or (run['periodic_evaluation_seed_start'], run['periodic_evaluation_seed_start'] + run['periodic_evaluation_seed_count']) != PERIODIC
            or (run['final_evaluation_seed_start'], run['final_evaluation_seed_start'] + run['final_evaluation_seed_count']) != CONFIRMATION
            or plan['allocation_hours'] != 48
            or isinstance(plan['allocations'], bool) or not isinstance(plan['allocations'], int)
            or not 1 <= plan['allocations'] <= 32):
        raise ValueError('registered continuation contract differs')
    return plan, config_path, config


def commands(path: Path, count: int, *, python: str = sys.executable) -> list[list[str]]:
    result = []
    for index in range(count):
        command = build_sbatch_command(_parser().parse_args([
            'train', '--config', str(path), '--python', python, '--constraint', 'xgpg',
            '--cpus', '16', '--memory', '64G', '--time', '2-00:00:00']))
        command[command.index('--job-name=sls-train')] = '--job-name=sls-act12-long20m-r1'
        command[command.index('--wrap') + 1] = 'exec ' + shlex.join([
            python, str(ROOT / 'tools/run_act12_long_run.py'), '--plan', str(path), '--allocation', str(index)])
        result.append(command)
    return result


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--plan', type=Path, required=True)
    parser.add_argument('--dry-run', action='store_true')
    args = parser.parse_args()
    plan, _, config = validate(args.plan)
    chain = commands(args.plan.resolve(), plan['allocations'])
    if args.dry_run:
        for command in chain:
            print(shlex.join(command))
        print('Later allocations use afterok; no training or job submitted.')
        return
    if subprocess.check_output(['git', 'status', '--porcelain'], cwd=ROOT, text=True).strip():
        raise ValueError('submit only committed source')
    output = ROOT / config['run']['output']
    if output.exists():
        raise FileExistsError('long-run output exists; inspect before submission')
    reject_used_seeds([ROOT / 'local/runs', ROOT / 'docs/results'])
    receipt = args.plan.with_name('submission.json')
    with receipt.open('x', encoding='utf-8') as stream:
        json.dump({'status': 'SUBMITTING', 'plan_sha256': sha256_file(args.plan)}, stream)
    (ROOT / 'local/runs/slurm-logs').mkdir(parents=True, exist_ok=True)
    jobs = []
    try:
        for command in chain:
            if jobs:
                command.insert(1, '--dependency=afterok:' + jobs[-1])
            raw = subprocess.check_output(command, cwd=ROOT, text=True).strip()
            job = raw.split(';')[0]
            if not job.isdecimal():
                raise ValueError('unrecognized sbatch response; inspect receipt before retrying')
            jobs.append(job)
            receipt.write_text(json.dumps({'status': 'SUBMITTING', 'jobs': jobs,
                               'plan_sha256': sha256_file(args.plan)}) + '\n', encoding='utf-8')
    except Exception:
        receipt.write_text(json.dumps({'status': 'PARTIAL_SUBMISSION_INSPECT', 'jobs': jobs}) + '\n', encoding='utf-8')
        raise
    receipt.write_text(json.dumps({'status': 'SUBMITTED', 'jobs': jobs,
                                  'plan_sha256': sha256_file(args.plan)}) + '\n', encoding='utf-8')
    print(json.dumps({'jobs': jobs, 'logical_training_budget': 20_000_000}))


if __name__ == '__main__':
    main()
