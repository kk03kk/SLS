"""Small normal-start diagnostic, no optimizer or policy updates."""

# ruff: noqa: E402
import json
import os
import sys
import time
from dataclasses import asdict
from pathlib import Path

os.environ.setdefault('CUBLAS_WORKSPACE_CONFIG', ':4096:8')
ROOT = Path(__file__).resolve().parents[3]
sys.path.insert(0, str(ROOT / 'src'))
import torch

from sls.curriculum import IRONCLAD_A20_ACT2
from sls.rl.checkpoint import policy_from_training_checkpoint
from sls.rl.evaluate import evaluate
from sls.rl.training_contract import evaluation_identity, sha256_file

torch.set_num_threads(2)
torch.use_deterministic_algorithms(True)
torch.backends.cudnn.benchmark = False
torch.set_float32_matmul_precision('high')
source = ROOT / 'local/runs/ironclad-a20-act1-win-90m-continuation/final.pt'
digest = '274963f4fe32b75003c5a1b4ccd394b5185304156e6ea22a76aee2764d54f1e0'
assert sha256_file(source) == digest
payload = torch.load(source, map_location='cpu', weights_only=False)
device = 'cuda' if torch.cuda.is_available() else 'cpu'
policy = policy_from_training_checkpoint(payload).eval().to(device)
seeds = tuple(range(8000008000000, 8000008000032))
output = ROOT / 'local/reports/act12-planning-20261005'
output.mkdir(parents=True, exist_ok=True)
destination = output / '90m-zero-shot-32-with-diagnostics.json'
assert not destination.exists(), 'Do not overwrite diagnostic evidence'
started = time.monotonic()
result = asdict(evaluate(policy, IRONCLAD_A20_ACT2, seeds, device=device,
    max_steps=4096, max_boundary_visits=4, failure_progress_scale=0.,
    environment_shards=4, crash_dump_dir=output/'crashes',
    progress_callback=lambda done,total,steps: print(done,total,steps,flush=True) if done==total else None))
assert sha256_file(source) == digest
record = {'schema':'sls-cross-horizon-development-probe-v1',
    'source_profile':'IRONCLAD_A20_ACT1','target_profile':'IRONCLAD_A20_ACT2',
    'checkpoint_sha256':digest,'checkpoint_environment_steps':90013696,
    'seeds':[seeds[0],seeds[-1]+1],'evaluation_role':'development-diagnostic',
    'battles_skipped':False,'weights_updated':False,
    'elapsed_seconds':time.monotonic()-started,
    **evaluation_identity(device=device,environment_shards=4,ascension=20),'result':result}
destination.write_text(json.dumps(record,indent=2)+'\n',encoding='utf-8')
print(json.dumps({k:result[k] for k in ('episodes','successes','reached_act2','reached_act3',
    'backend_errors','backend_truncations','step_limits','cycle_limits','timeouts','mean_steps',
    'death_floor_distribution','success_rate_ci95')},indent=2),flush=True)
