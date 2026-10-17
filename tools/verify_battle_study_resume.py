"""CPU-only fresh-process continuation of an already completed study smoke."""
from __future__ import annotations

import argparse
import json
import os
from pathlib import Path

os.environ['CUDA_VISIBLE_DEVICES']='-1'
for key in ('OMP_NUM_THREADS','MKL_NUM_THREADS','OPENBLAS_NUM_THREADS'):
    os.environ[key]='2'

import torch

from sls.curriculum import IRONCLAD_A20_ACT2
from sls.rl import load_checkpoint, save_checkpoint
from sls.rl.battle_bank import write_json
from sls.rl.training_contract import ROOT, sha256_file
from sls.rl.workers import VectorWorkerPool
from tools.run_battle_study import identity, load_config, make_trainer
from tools.verify_act12_diagnostics import assert_same


def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--smoke',type=Path,required=True)
    parser.add_argument('--output',type=Path,required=True)
    parser.add_argument('--config',type=Path,default=Path('configs/train/ironclad_a20_battle_study_r1.toml'))
    args=parser.parse_args()
    os.chdir(ROOT)
    actual=args.output.with_suffix('.pt')
    if args.output.exists() or actual.exists() or not args.output.resolve().is_relative_to(ROOT/'local/reports'):
        raise ValueError('fresh diagnostic output inside local/reports required')
    torch.set_num_threads(2)
    torch.set_num_interop_threads(2)
    torch.use_deterministic_algorithms(True)
    config=load_config(args.config)
    config['_path']=str(args.config)
    proof=json.loads((args.smoke/'result.json').read_text())
    if proof['device']!='cpu' or proof['status']!='PASS' or proof['identity']!=identity(config,args.config):
        raise ValueError('current CPU smoke required')
    expected=torch.load(args.smoke/'final.pt',map_location='cpu',weights_only=False)
    with VectorWorkerPool(IRONCLAD_A20_ACT2,size=1) as workers:
        trainer=make_trainer(config,workers,device='cpu',smoke=True,tiny=proof['tiny'])
        load_checkpoint(args.smoke/'safe.pt',trainer)
        trainer.optimize(trainer.collect())
        save_checkpoint(actual,trainer)
    resumed=torch.load(actual,map_location='cpu',weights_only=False)
    for field in ('schema','contract','model','optimizer','trainer','environments','python_rng','torch_rng','cuda_rng'):
        assert_same(expected[field],resumed[field])
    write_json(args.output,{'status':'PASS','device':'cpu','fresh_process':True,'tiny':proof['tiny'],
        'identity':proof['identity'],'model_optimizer_environment_and_rng_exact':True,
        'reference_sha256':sha256_file(args.smoke/'final.pt'),'resumed_sha256':sha256_file(actual),
        'tool_sha256':sha256_file(Path(__file__)),'formal_gpu_or_learning_claim':False})
    print('fresh-process CPU continuation: PASS')


if __name__=='__main__':
    main()
