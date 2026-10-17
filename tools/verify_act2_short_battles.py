"""Recheck corpus identity and entire restored battle continuations on CPU."""
from __future__ import annotations

import argparse
import json
import os
from pathlib import Path

os.environ['CUDA_VISIBLE_DEVICES']='-1'
for name in ('OMP_NUM_THREADS','MKL_NUM_THREADS','OPENBLAS_NUM_THREADS'):
    os.environ[name]='2'

import torch

from sls.audit.short_battle import load_corpus, verify_battle_restore
from sls.rl.model_migration import migrate_v5_role_policy
from sls.rl.training_contract import sha256_file


def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--corpus',type=Path,required=True)
    parser.add_argument('--output',type=Path,required=True)
    parser.add_argument('--cases',type=int,default=8)
    args=parser.parse_args()
    if args.output.exists():
        raise FileExistsError('retain original verification')
    torch.set_num_threads(2)
    torch.set_num_interop_threads(2)
    summary,samples=load_corpus(args.corpus)
    payload=torch.load('local/runs/ironclad-a20-act1-win-90m-continuation/final.pt',map_location='cpu',weights_only=False)
    model,_=migrate_v5_role_policy(payload)
    model.eval()
    # Select by source and ordinal only, never by a future victory label.
    cases=[]
    seen=set()
    for sample in sorted(samples,key=lambda s:(-s['ordinal_in_source'],s['source_seed'])):
        if sample['split']=='confirmation' or sample['source_seed'] in seen:
            continue
        seen.add(sample['source_seed'])
        cases.append(sample)
        if len(cases)==args.cases:
            break
    results=[]
    for sample in cases:
        for greedy in (False,True):
            result=verify_battle_restore(model,sample,greedy=greedy)
            results.append({'sample_id':sample['sample_id'],'greedy':greedy,**result})
        print(json.dumps({'verified':sample['sample_id']}),flush=True)
    result={'corpus_summary_sha256':sha256_file(args.corpus/'summary.json'),
            'native_source_sha256':summary['native_source_sha256'],'torch':str(torch.__version__),
            'cpu_threads':2,'cases':results,'same_action_steps':sum(r['steps'] for r in results),
            'confirmation_outcomes':'NOT_EVALUATED','formal_training_ready':False}
    with args.output.open('x',encoding='utf-8') as f:
        json.dump(result,f,indent=2)
    print(json.dumps({'cases':len(results),'steps':result['same_action_steps']}))


if __name__=='__main__':
    raise SystemExit(main())
