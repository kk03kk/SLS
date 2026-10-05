# ruff: noqa: E402
import json
import os
import sys
from dataclasses import asdict
from pathlib import Path

os.environ.setdefault('CUBLAS_WORKSPACE_CONFIG', ':4096:8')
root = Path(__file__).resolve().parents[3]
(root / 'local/reports/act12-audit-20261003').mkdir(parents=True, exist_ok=True)
sys.path.insert(0, str(root / 'src'))
import torch

from sls.curriculum import IRONCLAD_A20_ACT2
from sls.rl import policy_from_training_checkpoint
from sls.rl.evaluate import evaluate
from sls.rl.training_contract import evaluation_identity, sha256_file

torch.set_num_threads(2)
torch.use_deterministic_algorithms(True)
torch.backends.cudnn.benchmark = False
torch.set_float32_matmul_precision('high')
source = root / 'local/runs/ironclad-a20-act1-win-70m-continuation/final.pt'
assert sha256_file(source) == 'cb53fee1ae3f47cc906665c903068dabaf38a328be1e07697e6d17c5b21d7d5c'
payload = torch.load(source, map_location='cpu', weights_only=False)
model = policy_from_training_checkpoint(payload).eval().to('cuda')
seeds = tuple(range(8000005000000,8000005000032))
result = asdict(evaluate(model,IRONCLAD_A20_ACT2,seeds,device='cuda',max_steps=4096,
                        max_boundary_visits=4,failure_progress_scale=0.,environment_shards=4,
                        crash_dump_dir=root/'local/reports/act12-audit-20261003/crashes',
                        progress_callback=lambda done,total,steps: print(done,total,steps,flush=True) if done==total else None))
record = {'schema':'sls-cross-horizon-development-probe-v1','source_profile':'IRONCLAD_A20_ACT1',
          'target_profile':'IRONCLAD_A20_ACT2','checkpoint_sha256':sha256_file(source),
          'seeds':[seeds[0],seeds[-1]+1],'evaluation_role':'development-diagnostic',
          'battles_skipped':False,'weights_updated':False,
          **evaluation_identity(device='cuda',environment_shards=4,ascension=20),'result':result}
(root/'local/reports/act12-audit-20261003/zero-shot-act12.json').write_text(json.dumps(record,indent=2),encoding='utf-8')
print(json.dumps({k:result[k] for k in ('episodes','successes','reached_act2','reached_act3',
 'backend_errors','backend_truncations','step_limits','cycle_limits','timeouts','mean_steps')}))
