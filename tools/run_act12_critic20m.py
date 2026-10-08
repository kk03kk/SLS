"""Run one allocation of the same 20M critic-warmup recipe; fail closed on unsafe exit."""
from __future__ import annotations

import argparse
import json
import math
import os
import signal
import subprocess
import sys
import time
from pathlib import Path

from sls.rl.training_contract import ROOT, sha256_file
from tools.act12_critic20m_contract import validate, validate_compute_gate


def chunk_budget(rate: float, seconds: float, rollout: int) -> int:
    if not math.isfinite(rate) or rate <= 0 or not math.isfinite(seconds):
        raise ValueError('invalid allocation throughput/time')
    # Six hours for evaluation/finalization, 1.5x collection+update margin.
    steps = math.floor(max(0, seconds - 6 * 3600) * rate / 1.5 / rollout) * rollout
    if steps < rollout:
        raise ValueError('no safe training budget remains in allocation')
    return steps


def safe_status(manifest: dict, *, signalled: int | None) -> str:
    if signalled == signal.SIGINT:
        raise ValueError('SIGINT never authorizes automatic allocation continuation')
    stage = manifest.get('stages', {}).get('train', {})
    status = manifest.get('status')
    if stage.get('status') != status:
        raise ValueError('manifest stage/status mismatch')
    if status == 'COMPLETE' and manifest['environment_steps'] >= stage['target_environment_steps']:
        return 'COMPLETE'
    if status == 'SOAK_COMPLETE' and signalled is None:
        return 'SAFE_INTERRUPTED'
    if status == 'INTERRUPTED' and signalled == signal.SIGTERM and stage.get('stop_signal') == 'SIGTERM':
        return 'SAFE_INTERRUPTED'
    raise ValueError('child exit is not verified completion or safe allocation interruption')


def allocation_budget(rate, seconds, rollout, remaining):
    normal = chunk_budget(rate, seconds, rollout)
    # Leave 24 hours for three 4096-seed evaluations if this segment can finish.
    final_capacity = math.floor(max(0, seconds - 24 * 3600) * rate / 1.5 / rollout) * rollout
    needed = math.ceil(remaining / rollout) * rollout
    if needed <= final_capacity:
        return needed
    if needed <= normal:
        if needed <= rollout:
            raise ValueError('insufficient time for endpoint confirmation; preserve checkpoint')
        return needed - rollout
    return normal


def training_arguments(target, before, steps):
    arguments = ['--stage', 'train']
    if before < target:
        if steps <= 0:
            raise ValueError('nonpositive production training budget')
        arguments.extend(['--stop-after-additional-steps', str(steps)])
    return arguments


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--plan', type=Path, required=True)
    parser.add_argument('--allocation', type=int, required=True)
    args = parser.parse_args()
    if not os.environ.get('SLURM_JOB_ID'):
        raise RuntimeError('requires allocated GPU node')
    started = time.monotonic()
    plan, path, config = validate(args.plan)
    if not 0 <= args.allocation < plan['allocations']:
        raise ValueError('allocation index outside registered chain')
    import fcntl
    benchmark = ROOT / config['run']['benchmark']
    benchmark.parent.mkdir(parents=True, exist_ok=True)
    lock = open(benchmark.with_name('long-run.lock'), 'a')
    fcntl.flock(lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
    output = ROOT / config['run']['output']
    ledger_dir = benchmark.parent / 'allocations'
    ledger_dir.mkdir(exist_ok=True)
    ledger_path = ledger_dir / f'{args.allocation:02d}.json'
    if ledger_path.exists():
        raise FileExistsError('allocation already attempted; inspect, do not blindly retry')
    if args.allocation:
        previous = json.loads((ledger_dir / f'{args.allocation - 1:02d}.json').read_text(encoding='utf-8'))
        if previous.get('plan_sha256') != sha256_file(args.plan):
            raise ValueError('previous allocation belongs to another plan')
        if previous.get('status') == 'COMPLETE':
            if previous.get('checkpoint_sha256') != sha256_file(output / 'latest.pt'):
                raise ValueError('completed chain checkpoint changed')
            ledger_path.write_text(json.dumps({**previous, 'allocation': args.allocation,
                                              'skipped_after_completion': True}) + '\n', encoding='utf-8')
            return 0
        if previous.get('status') != 'SAFE_INTERRUPTED' or previous.get('checkpoint_sha256') != sha256_file(output / 'latest.pt'):
            raise ValueError('previous allocation is not a verified safe checkpoint')
    ledger = {'status': 'IN_PROGRESS', 'job_id': os.environ['SLURM_JOB_ID'],
              'allocation': args.allocation, 'plan_sha256': sha256_file(args.plan)}
    def save():
        temporary = ledger_path.with_suffix('.tmp')
        temporary.write_text(json.dumps(ledger, indent=2) + '\n', encoding='utf-8')
        os.replace(temporary, ledger_path)
    save()
    child = None
    signalled = None
    def stop(number, _frame):
        nonlocal signalled
        signalled = number
        if child is not None and child.poll() is None:
            child.send_signal(number)
    signal.signal(signal.SIGTERM, stop)
    signal.signal(signal.SIGINT, stop)
    def run(name, *arguments):
        nonlocal child
        if signalled is not None:
            raise InterruptedError('allocation stopped during preparation')
        command = [sys.executable, str(ROOT / 'tools' / name), *map(str, arguments)]
        print(json.dumps({'command': command}), flush=True)
        child = subprocess.Popen(command, cwd=ROOT)
        code = child.wait()
        child = None
        if code:
            raise RuntimeError(f'{name} exited {code}')
    try:
        import torch

        from sls.rl.preparation import preparation_contract
        torch.use_deterministic_algorithms(True)
        torch.backends.cudnn.benchmark = False
        torch.set_float32_matmul_precision('high')
        if not torch.cuda.is_available():
            raise RuntimeError('CUDA unavailable')
        run('build_native.py', '--jobs', '16')
        if args.allocation == 0:
            run('check_training_configs.py')
            run('benchmark_workers.py', '--layouts', '64:16', '--rounds', '1', '--config', path, '--output', benchmark)
        if args.allocation == 0:
            run('verify_act12_critic_warmup.py', '--plan', args.plan)
        from tools.train_full_run import _training_identity
        gate_path = ROOT / config['run']['compute_gate_report']
        gate = json.loads(gate_path.read_text(encoding='utf-8'))
        validate_compute_gate(gate, _training_identity(config, workers=64, shards=16))
        gate_sha = sha256_file(gate_path)
        if args.allocation and previous.get('compute_gate_sha256') != gate_sha:
            raise ValueError('acceptance report changed between allocations')
        ledger['compute_gate_sha256'] = gate_sha
        save()
        latest = output / 'latest.pt'
        preflight_checkpoint = benchmark.parent / 'initial.pt' if args.allocation == 0 else latest
        run('preflight_training.py', '--skip-build', '--config', path, '--benchmark', benchmark,
            '--checkpoint', preflight_checkpoint, '--output', benchmark.parent / f'preflight-{args.allocation:02d}.json')
        layout = json.loads(benchmark.read_text(encoding='utf-8'))
        measured = [r['decisions_per_second'] for r in layout['results']
                    if (r['workers'], r['shards']) == (64, 16)]
        if len(measured) != 1 or not math.isfinite(measured[0]) or measured[0] <= 0:
            raise ValueError('invalid pinned-layout allocation benchmark')
        rate = min(plan['benchmark_rate'], measured[0])
        ready = {'ok': True, 'contract': preparation_contract(config, torch),
                 'layout': [layout['selected_workers'], layout['selected_shards']],
                 'gpu': torch.cuda.get_device_name(0)}
        benchmark.with_name('ready.json').write_text(json.dumps(ready) + '\n', encoding='utf-8')
        before = int(torch.load(preflight_checkpoint, map_location='cpu', weights_only=False)['trainer']['environment_steps'])
        target = config['stages']['train']['target_environment_steps']
        steps = (allocation_budget(rate, 48 * 3600 - (time.monotonic() - started),
                                   config['ppo']['rollout_steps'] * config['run']['worker_layout'][0],
                                   target - before) if before < target else 0)
        run('train_full_run.py', '--config', path, *training_arguments(target, before, steps))
        manifest = json.loads((output / 'run-manifest.json').read_text(encoding='utf-8'))
        status = safe_status(manifest, signalled=signalled)
        payload = torch.load(latest, map_location='cpu', weights_only=False)
        if (payload['contract']['training_config_sha256'] != _training_identity(config, workers=config['run']['worker_layout'][0], shards=config['run']['worker_layout'][1])
                or payload['contract']['native_source_sha256'] != plan['native_source_sha256']
                or payload['contract']['profile'].profile_id != 'IRONCLAD_A20_ACT2'):
            raise ValueError('saved checkpoint contract differs from registered continuation')
        if payload['trainer']['environment_steps'] != manifest['environment_steps'] or payload['trainer']['update'] != manifest['updates']:
            raise ValueError('safe checkpoint and manifest disagree')
        if manifest['environment_steps'] < before:
            raise ValueError('checkpoint progress regressed')
        # The preflight on the next allocation validates restoring this exact SHA.
        # Time-limit signals leave only five minutes; do not start another update here.
        if status == 'COMPLETE':
            run('analyze_act12_critic20m.py', '--plan', args.plan, '--output', benchmark.parent / 'analysis.json')
        ledger.update(status=status, checkpoint_sha256=sha256_file(latest),
                      environment_steps=manifest['environment_steps'], update=manifest['updates'],
                      resume_validation='required actual-checkpoint preflight before next allocation')
        save()
        if status != 'COMPLETE' and args.allocation == plan['allocations'] - 1:
            print('Allocation estimate exhausted; checkpoint preserved, inspect before extending allocation chain.', flush=True)
            return 3
        return 0
    except Exception as error:
        ledger.update(status='FAILED_INSPECT', error=str(error), error_type=type(error).__name__)
        save()
        raise


if __name__ == '__main__':
    raise SystemExit(main())
