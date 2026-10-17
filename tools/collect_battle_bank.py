"""Collect one predeclared third Act2 battle per unmodified normal-start run."""
from __future__ import annotations

import argparse
import gzip
import json
import os
from collections import Counter
from pathlib import Path

os.environ['CUDA_VISIBLE_DEVICES']='-1'
for name in ('OMP_NUM_THREADS','MKL_NUM_THREADS','OPENBLAS_NUM_THREADS'):
    os.environ[name]='2'

import torch

from sls.audit.short_battle import (
    PolicyContext,
    canonical,
    compare_next,
    following_context,
    predict,
)
from sls.backends.simulator import SimulatorBackend
from sls.content.selection import PROJECTION_REVISION
from sls.curriculum import IRONCLAD_A20_ACT2
from sls.rl.battle_bank import SCHEMA, projection_digest, write_json
from sls.rl.episode_limit import EpisodeLimitState
from sls.rl.model_migration import migrate_v5_role_policy
from sls.rl.training_contract import native_source_digest, sha256_file
from sls.rl.value_adaptation import state_digest

PARENT=Path('local/runs/ironclad-a20-act1-win-90m-continuation/final.pt')
PARENT_SHA='274963f4fe32b75003c5a1b4ccd394b5185304156e6ea22a76aee2764d54f1e0'
STARTS={'fit':1_600_001_000_000,'development':8_000_103_000_000,'confirmation':8_000_104_000_000}


def collect(model,seed,role,model_sha,*,failure_directory=None):
    backend=SimulatorBackend(IRONCLAD_A20_ACT2)
    decision=backend.reset(seed)
    context=PolicyContext(model.initial_memory(1).cpu())
    rng=torch.Generator().manual_seed(seed)
    prefix=[]
    limiter=EpisodeLimitState.initial(decision)
    floors=[]
    for step in range(4096):
        obs=decision.observation
        if obs.run.act==2 and obs.screen.value=='COMBAT' and obs.run.floor not in floors:
            floors.append(obs.run.floor)
            if len(floors)==3:
                checkpoint=canonical(backend.checkpoint())
                restored=SimulatorBackend(IRONCLAD_A20_ACT2)
                assert restored.load_checkpoint(checkpoint)==decision
                assert canonical(restored.checkpoint())==checkpoint
                # Stop BEFORE any target fight action, including confirmation.
                return {'source_seed':seed,'split':role,'reason':'ROOT_CAPTURED','steps':len(prefix),
                    'root':{'sample_id':str(seed),'source_seed':seed,'split':role,'ordinal_in_source':2,
                    'native_source_sha256':native_source_digest(),'parent_model_sha256':model_sha,
                    'projection_sha256':projection_digest(),'prefix':prefix,'checkpoint':checkpoint,
                    'parent_context':context.to_dict(),'action_rng':rng.get_state().tolist(),
                    'floor':obs.run.floor,'enemies':[e.monster_id for e in obs.enemies],
                    'hp':obs.player.current_hp,'max_hp':obs.player.max_hp,'gold':obs.run.gold,
                    'deck':[c.card_id for c in obs.deck],
                    'potions':[p.content_id for p in obs.potions],
                    'relics':[r.content_id for r in obs.relics]}}
        output=predict(model,decision,context)
        index=int(torch.multinomial(output.logits.softmax(-1).cpu(),1,generator=rng))
        action=decision.actions[index]
        before=canonical(backend.checkpoint()) if step%64==0 else None
        try:
            transition=compare_next(backend,decision,action) if step%64==0 else backend.step(action)
        except BaseException as error:
            if failure_directory is not None:
                failure_directory.mkdir(parents=True,exist_ok=True)
                write_json(failure_directory/f'{seed}-{step}.json',{'source_seed':seed,'split':role,
                    'prefix':prefix,'before':before,'action':action.to_dict(),'error':repr(error)})
            raise
        if transition.truncated:
            raise RuntimeError('source backend truncated')
        prefix.append(action.to_dict())
        context=following_context(output,action,transition)
        decision=transition.decision
        reason=('success' if transition.info.get('success') else 'death') if transition.terminated else limiter.observe(
            decision,max_steps=4096,max_boundary_visits=4)
        if reason:
            return {'source_seed':seed,'split':role,'reason':reason,'steps':len(prefix),'root':None}
    return {'source_seed':seed,'split':role,'reason':'step_limit','steps':len(prefix),'root':None}


def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--output',type=Path,required=True)
    parser.add_argument('--fit',type=int,default=64)
    parser.add_argument('--development',type=int,default=64)
    parser.add_argument('--confirmation',type=int,default=384)
    args=parser.parse_args()
    quotas={k:getattr(args,k) for k in STARTS}
    if any(not 1<=n<=1024 for n in quotas.values()):
        raise ValueError('bank quota must be 1..1024')
    torch.set_num_threads(2)
    torch.set_num_interop_threads(2)
    if sha256_file(PARENT)!=PARENT_SHA:
        raise ValueError('parent changed')
    torch.manual_seed(39171)
    model,_=migrate_v5_role_policy(torch.load(PARENT,map_location='cpu',weights_only=False))
    model.eval()
    model_sha=state_digest(model.state_dict())
    args.output.mkdir(parents=True,exist_ok=True)
    contract={'schema':SCHEMA,'native_source_sha256':native_source_digest(),
        'projection_sha256':projection_digest(),'projection_revision':PROJECTION_REVISION,
        'parent_sha256':PARENT_SHA,'parent_model_sha256':model_sha,'starts':STARTS,'quotas':quotas,
        'selection':'THIRD_ACT2_COMBAT_STOP_BEFORE_TARGET_ACTION_NO_OUTCOME_FILTER',
        'max_sources_per_role':4096,'max_source_decisions':2_000_000}
    contract_file=args.output/'contract.json'
    if contract_file.exists():
        if json.loads(contract_file.read_text())!=contract:
            raise ValueError('collection resume contract mismatch')
    else:
        write_json(contract_file,contract)
    roots={}
    ledger=[]
    total=0
    for role,quota in quotas.items():
        rows=[]
        for ordinal in range(4096):
            seed=STARTS[role]+ordinal
            path=args.output/'origins'/f'{role}-{seed}.json'
            if path.exists():
                record=json.loads(path.read_text(encoding='utf-8'))
                if record['source_seed']!=seed or record['split']!=role:
                    raise ValueError('origin identity mismatch')
            else:
                record=collect(model,seed,role,model_sha,failure_directory=args.output/'failures')
                path.parent.mkdir(parents=True,exist_ok=True)
                write_json(path,record)
            total+=record['steps']
            ledger.append({k:v for k,v in record.items() if k!='root'})
            if record['root'] is not None:
                rows.append(record['root'])
            print(json.dumps({'split':role,'sources':ordinal+1,'roots':len(rows),'quota':quota,
                              'decisions':total}),flush=True)
            if len(rows)==quota:
                break
            if total>contract['max_source_decisions']:
                raise RuntimeError('collection source decision budget exhausted')
        if len(rows)!=quota:
            raise RuntimeError('finite source reservation exhausted without required roots')
        roots[role]=rows
        blob=gzip.compress(json.dumps(rows,allow_nan=False).encode(),mtime=0)
        target=args.output/f'{role}.json.gz'
        if target.exists() and target.read_bytes()!=blob:
            raise ValueError('completed bank cannot be replaced')
        if not target.exists():
            target.write_bytes(blob)
    write_json(args.output/'source-ledger.json',ledger)
    manifest={**contract,'files':{f'{role}.json.gz':sha256_file(args.output/f'{role}.json.gz') for role in STARTS},
        'root_sources':{k:[r['source_seed'] for r in v] for k,v in roots.items()},
        'attempted_sources':len(ledger),'source_decisions':total,
        'source_reasons':dict(Counter(r['reason'] for r in ledger)),
        'confirmation_target_actions_executed':0,'weights_unchanged':state_digest(model.state_dict())==model_sha}
    write_json(args.output/'manifest.json',manifest)
    print(json.dumps({'complete':True,'sources':len(ledger),'decisions':total}))


if __name__=='__main__':
    main()
