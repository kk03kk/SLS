"""Verify prefixes, memories and fit/development continuations; seal confirmation."""
from __future__ import annotations

import argparse
import os
from collections import Counter
from pathlib import Path

os.environ['CUDA_VISIBLE_DEVICES']='-1'
for key in ('OMP_NUM_THREADS','MKL_NUM_THREADS','OPENBLAS_NUM_THREADS'):
    os.environ[key]='2'

import numpy as np
import torch

from sls.audit.short_battle import canonical, verify_battle_restore
from sls.backends.simulator import SimulatorBackend
from sls.contracts import Action
from sls.curriculum import IRONCLAD_A20_ACT2
from sls.rl.battle_bank import load_bank, write_json
from sls.rl.history_prefill import RootPrefillCache
from sls.rl.model_migration import migrate_v5_role_policy
from sls.rl.training_contract import sha256_file
from tools.collect_battle_bank import PARENT, PARENT_SHA


def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--bank',type=Path,required=True)
    parser.add_argument('--output',type=Path,required=True)
    args=parser.parse_args()
    if args.output.exists():
        raise FileExistsError('immutable bank audit exists')
    torch.set_num_threads(2)
    torch.set_num_interop_threads(2)
    if sha256_file(PARENT)!=PARENT_SHA:
        raise ValueError('parent changed')
    torch.manual_seed(39171)
    model,_=migrate_v5_role_policy(torch.load(PARENT,map_location='cpu',weights_only=False))
    model.eval()
    counts={}
    actions=0
    memory_checks=0
    memory_max_error=0.
    distributions={}
    continuations=[]
    for role in ('fit','development','confirmation'):
        # Confirmation access here is structural verification only. No policy
        # scoring, target action, victory label or resource-based selection.
        rows,_=load_bank(args.bank,role,allow_confirmation=True)
        for row in rows:
            backend=SimulatorBackend(IRONCLAD_A20_ACT2)
            decision=backend.reset(row['source_seed'])
            seen=[]
            for serialized in row['prefix']:
                obs=decision.observation
                if obs.run.act==2 and obs.screen.value=='COMBAT' and obs.run.floor not in seen:
                    seen.append(obs.run.floor)
                action=Action.from_dict(serialized)
                if action not in decision.actions:
                    raise ValueError('illegal serialized source prefix')
                decision=backend.step(action).decision
                actions+=1
            obs=decision.observation
            if (decision.terminal or obs.run.act!=2 or obs.screen.value!='COMBAT'
                    or obs.run.floor in seen or len(seen)!=2):
                raise ValueError('root is not first decision on third distinct combat floor')
            if canonical(backend.checkpoint())!=canonical(row['checkpoint']):
                raise ValueError('serialized prefix differs in hidden state/RNG')
            restored=SimulatorBackend(IRONCLAD_A20_ACT2)
            if restored.load_checkpoint(canonical(row['checkpoint']))!=decision:
                raise ValueError('root public state/candidates differ after restore')
            if canonical(restored.checkpoint())!=canonical(backend.checkpoint()):
                raise ValueError('root hidden state differs after restore')
            del restored,backend
        for start in range(0,len(rows),16):
            group=rows[start:start+16]
            cache=RootPrefillCache(group,lambda count:None)
            contexts=cache.root_contexts(model,range(len(group)))
            for row,context in zip(group,contexts,strict=True):
                witness=row['parent_context']
                expected=torch.tensor(witness['memory'])
                torch.testing.assert_close(context.memory,expected,atol=1e-6,rtol=1e-6,
                    msg=f'parent memory differs for source {row["sample_id"]}')
                memory_max_error=max(memory_max_error,float((context.memory-expected).abs().max()))
                for field in ('previous_action','previous_reward','episode_start'):
                    if getattr(context,field)!=witness[field]:
                        raise ValueError('reconstructed policy context differs from continuous witness')
                memory_checks+=1
            del cache
        counts[role]=len(rows)
        if role!='confirmation':
            for row in rows[:8]:
                for greedy in (False,True):
                    result=verify_battle_restore(model,row,greedy=greedy)
                    alive=result.pop('success')
                    continuations.append({'sample_id':row['sample_id'],'role':role,'greedy':greedy,
                        'alive_at_stop':alive,**result})
        distributions[role]={
            'enemy_combinations':dict(Counter('+'.join(sorted(r['enemies'])) for r in rows)),
            'floors':dict(Counter(r['floor'] for r in rows)),
            'hp_fraction_quantiles':np.quantile([r['hp']/r['max_hp'] for r in rows],[0,.25,.5,.75,1]).tolist(),
            'deck_size_quantiles':np.quantile([len(r['deck']) for r in rows],[0,.25,.5,.75,1]).tolist(),
            'potion_counts':dict(Counter(len(r['potions']) for r in rows)),
            'all_ascension_20':all(r['checkpoint']['run_state']['ascension']==20 for r in rows)}
        print(f'{role}: {len(rows)} legal roots verified',flush=True)
    write_json(args.output,{'status':'PASS','bank_sha256':sha256_file(args.bank/'manifest.json'),
        'roots':counts,'prefix_actions':actions,
        'target_actions_executed':sum(r['steps'] for r in continuations),
        'confirmation_target_actions_executed':0,
        'parent_memory_checks':memory_checks,'parent_memory_max_abs_error':memory_max_error,
        'root_distributions':distributions,
        'battle_continuations':continuations,
        'confirmation_outcomes_exposed':False,'root_definition':'FIRST_LIVE_DECISION_ON_THIRD_DISTINCT_ACT2_COMBAT_FLOOR'})


if __name__=='__main__':
    main()
