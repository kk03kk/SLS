"""Validate and submit two matched Act1-2 arms serially in one GPU allocation.

Login-node path uses file hashes/config parsing only; torch and CUDA are loaded
by each arm's existing compute-node preparation, never by this submitter.
"""
from __future__ import annotations

import argparse
import json
import re
import shlex
import subprocess
import sys
import tomllib
from pathlib import Path

from sls.rl.training_contract import source_sha256
from tools.operator_paths import repository_path
from tools.submit_act12_pilot import validate_plan
from tools.submit_slurm import _parser, build_sbatch_command

ROOT = Path(__file__).resolve().parents[1]


def validate_study(study: dict, *, root: Path = ROOT, deep: bool = False) -> list[Path]:
    if study.get('schema') != 'sls-act12-lambda-study-v1' or study.get('status') != 'READY':
        raise ValueError('requires a ready hash-bound lambda study')
    if set(study.get('arms', {})) != {'control', 'experimental'}:
        raise ValueError('requires exactly the two registered arms')
    configs, plans, paths = [], [], []
    for arm in ('control', 'experimental'):
        binding = study['arms'][arm]
        path = repository_path(root, binding['plan'])
        if source_sha256(path) != binding['plan_sha256']:
            raise ValueError('study arm plan changed')
        plan = json.loads(path.read_text(encoding='utf-8'))
        config = tomllib.loads(validate_plan(plan, root=root, deep=deep).read_text(encoding='utf-8'))
        configs.append(config)
        plans.append(plan)
        paths.append(path)
    if plans[0]['parent'] != plans[1]['parent']:
        raise ValueError('study parents differ')
    for section in ('model', 'stages', 'warm_start'):
        if configs[0][section] != configs[1][section]:
            raise ValueError(f'study {section} differs')
    if (configs[0]['ppo']['gae_lambda'], configs[1]['ppo']['gae_lambda']) != (0.98, 1.0):
        raise ValueError('registered lambda values differ')
    if ({k: v for k, v in configs[0]['ppo'].items() if k != 'gae_lambda'} !=
            {k: v for k, v in configs[1]['ppo'].items() if k != 'gae_lambda'}):
        raise ValueError('study changes another PPO variable')
    allowed = {'output', 'benchmark'}
    if ({k: v for k, v in configs[0]['run'].items() if k not in allowed} !=
            {k: v for k, v in configs[1]['run'].items() if k not in allowed}):
        raise ValueError('study changes another run variable')
    if configs[0]['run']['output'] == configs[1]['run']['output']:
        raise ValueError('study outputs collide')
    return paths


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--study', type=Path, required=True)
    parser.add_argument('--dry-run', action='store_true')
    args = parser.parse_args()
    path = repository_path(ROOT, args.study)
    study = json.loads(path.read_text(encoding='utf-8'))
    plans = validate_study(study)
    hours = study.get('wall_limit_hours')
    if not isinstance(hours, int) or not 1 <= hours <= 72:
        raise ValueError('study wall limit must be an integer in 1..72 hours')
    control = json.loads(plans[0].read_text(encoding='utf-8'))
    command = build_sbatch_command(_parser().parse_args([
        'train', '--config', str(ROOT / control['config']), '--prepare',
        '--bound-plan', str(plans[0]), '--python', sys.executable,
        '--constraint', 'xgpg', '--cpus', '16', '--memory', '64G',
        '--time', f'{hours // 24}-{hours % 24:02d}:00:00',
    ]))
    command[command.index('--wrap') + 1] = 'exec ' + shlex.join([
        sys.executable, str(ROOT / 'tools/run_act12_lambda_study.py'), '--study', str(path),
    ])
    command[command.index('--job-name=sls-train')] = '--job-name=sls-act12-lambda-r1'
    print(shlex.join(command), flush=True)
    if args.dry_run:
        print('One node, control then experimental, paired analysis; no job submitted.')
        return 0
    dirty = subprocess.check_output(['git', 'status', '--porcelain'], cwd=ROOT, text=True)
    if dirty.strip():
        raise ValueError('submit only committed sources')
    for plan_path in plans:
        plan = json.loads(plan_path.read_text(encoding='utf-8'))
        config = tomllib.loads((ROOT / plan['config']).read_text(encoding='utf-8'))
        if (ROOT / config['run']['output']).exists():
            raise FileExistsError('arm output exists; inspect before repeating submission')
    if (ROOT / 'local/runs/act12-lambda-study-r1').exists():
        raise FileExistsError('study output exists; inspect before repeating submission')
    receipt = ROOT / 'local/operator/act12-lambda-study-r1-submission.json'
    receipt.parent.mkdir(parents=True, exist_ok=True)
    with receipt.open('x', encoding='utf-8') as stream:
        json.dump({'status': 'SUBMITTING', 'study_sha256': source_sha256(path)}, stream)
    (ROOT / 'local/runs/slurm-logs').mkdir(parents=True, exist_ok=True)
    result = subprocess.run(command, cwd=ROOT, text=True, capture_output=True)
    if result.returncode:
        receipt.write_text(json.dumps({'status': 'FAILED_SUBMISSION', 'error': result.stderr}) + '\n', encoding='utf-8')
        raise RuntimeError('study submission failed; inspect receipt before retrying')
    job = result.stdout.strip().split(';', 1)[0]
    if not re.fullmatch(r'[1-9][0-9]*', job):
        raise RuntimeError('unexpected Slurm receipt; inspect before retrying')
    receipt.write_text(json.dumps({'status': 'SUBMITTED', 'study_sha256': source_sha256(path),
                                  'job': job}, indent=2) + '\n', encoding='utf-8')
    print(json.dumps({'job': job, 'same_node_serial_arms': True, 'training_results': 'NOT_YET_RUN'}))
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
