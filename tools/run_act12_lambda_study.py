"""Run both preregistered arms on one allocated GPU node, then verify outcomes."""
from __future__ import annotations

import argparse
import json
import os
import signal
import subprocess
import sys
import time
from pathlib import Path

from tools.submit_act12_lambda_study import ROOT, validate_study


def require_completed_arm(plan: dict) -> None:
    import tomllib
    config = tomllib.loads((ROOT / plan['config']).read_text(encoding='utf-8'))
    manifest = json.loads((ROOT / config['run']['output'] / 'run-manifest.json').read_text(encoding='utf-8'))
    stage = manifest.get('stages', {}).get('train', {})
    if (manifest.get('status') != 'COMPLETE' or stage.get('status') != 'COMPLETE'
            or stage.get('completed_environment_steps', 0) < config['stages']['train']['target_environment_steps']
            or manifest.get('native_source_sha256') != plan['parent']['target_native_source_sha256']
            or manifest.get('training_implementation_sha256') != plan['target_training_implementation_sha256']):
        raise ValueError('arm did not complete the registered budget and identity; next arm blocked')


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--study', type=Path, required=True)
    args = parser.parse_args()
    if not os.environ.get('SLURM_JOB_ID'):
        raise RuntimeError('study runner requires an allocated GPU node')
    started = time.monotonic()
    study = json.loads(args.study.read_text(encoding='utf-8'))
    plans = validate_study(study)
    output = ROOT / 'local/runs/act12-lambda-study-r1'
    output.mkdir()  # Existing/interrupted study requires explicit inspection.
    stop_requested = False
    child = None
    def stop(number, _frame):
        nonlocal stop_requested
        stop_requested = True
        if child is not None and child.poll() is None:
            child.send_signal(number)
    signal.signal(signal.SIGTERM, stop)
    signal.signal(signal.SIGINT, stop)
    completed = []
    for arm, plan_path in zip(('control', 'experimental'), plans, strict=True):
        if stop_requested:
            return 143
        remaining = study['wall_limit_hours'] * 3600 - (time.monotonic() - started) - 1800
        if remaining <= 0:
            raise RuntimeError('insufficient remaining wall time; do not start next arm')
        plan = json.loads(plan_path.read_text(encoding='utf-8'))
        environment = {**os.environ, 'SLS_STUDY_AVAILABLE_SECONDS': str(remaining)}
        command = [sys.executable, str(ROOT / 'tools/prepare_and_train.py'),
                   '--config', str(ROOT / plan['config']), '--bound-plan', str(plan_path)]
        print(json.dumps({'arm': arm, 'available_seconds': remaining, 'command': command}), flush=True)
        child = subprocess.Popen(command, cwd=ROOT, env=environment)
        code = child.wait()
        child = None
        if code != 0 or stop_requested:
            (output / 'status.json').write_text(json.dumps({'status': 'INCOMPLETE',
                'completed_arms': completed, 'stopped_arm': arm, 'exit_code': code}) + '\n', encoding='utf-8')
            return code if code else 143
        # Training intentionally exits zero after safely handling a signal.
        # The manifest, rather than exit code alone, decides completion.
        require_completed_arm(plan)
        completed.append(arm)
    if stop_requested:
        return 143
    command = [sys.executable, str(ROOT / 'tools/analyze_act12_lambda_study.py'),
               '--study', str(args.study.resolve()), '--output', str(output / 'analysis.json')]
    child = subprocess.Popen(command, cwd=ROOT)
    code = child.wait()
    child = None
    (output / 'status.json').write_text(json.dumps({'status': 'COMPLETE' if code == 0 and not stop_requested
        else 'ANALYSIS_FAILED', 'completed_arms': completed, 'analysis_exit_code': code}) + '\n', encoding='utf-8')
    return code if not stop_requested else 143


if __name__ == '__main__':
    raise SystemExit(main())
