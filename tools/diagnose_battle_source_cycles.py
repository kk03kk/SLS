"""Replay every recorded pre-root cycle without touching confirmation battles."""
from __future__ import annotations

import argparse
import gzip
import hashlib
import json
import os
from collections import Counter, deque
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
from sls.curriculum import IRONCLAD_A20_ACT2
from sls.rl.battle_bank import write_json
from sls.rl.episode_limit import EpisodeLimitState, policy_boundary_fingerprint
from sls.rl.model_migration import migrate_v5_role_policy
from sls.rl.training_contract import native_source_digest, sha256_file
from sls.rl.value_adaptation import state_digest
from tools.collect_battle_bank import PARENT, PARENT_SHA


def without_action_history(checkpoint):
    value=canonical(checkpoint)
    value.pop('replay_actions',None)
    return value


def classify_tail(tail):
    repeated=[r for r in tail if r['previous_same_boundary_step'] is not None]
    reversible=(bool(repeated) and all(r['same_hidden_except_action_history'] and r['same_rng'] for r in repeated))
    # REMOVE/TRANSFORM/UPGRADE are public semantic names for grid selection
    # clicks too. Actual committed changes cannot satisfy the full-state test.
    selection=(len(tail)>=8 and all(r['action']['kind'] in {
        'SELECT_CARD','REMOVE_CARD','TRANSFORM_CARD','UPGRADE_CARD'} and r['public_changed'] for r in tail[-8:]))
    return 'LEGAL_REVERSIBLE_SELECTION_CYCLE' if reversible and selection else 'UNCLASSIFIED_PRESERVE_TRACE'


def run(model,row):
    backend=SimulatorBackend(IRONCLAD_A20_ACT2)
    decision=backend.reset(row['source_seed'])
    context=PolicyContext(model.initial_memory(1).cpu())
    generator=torch.Generator().manual_seed(row['source_seed'])
    limiter=EpisodeLimitState.initial(decision)
    seen={policy_boundary_fingerprint(decision):(canonical(backend.checkpoint()),0)}
    tail=deque(maxlen=16)
    prefix=[]
    floors=set()
    restores=0
    reason=None
    for step in range(row['steps']):
        obs=decision.observation
        if obs.run.act==2 and obs.screen.value=='COMBAT':
            floors.add(obs.run.floor)
        if len(floors)>=3:
            raise ValueError('unexpected target arrival; confirmation battle remains sealed')
        output=predict(model,decision,context)
        index=int(torch.multinomial(output.logits.softmax(-1).cpu(),1,generator=generator))
        action=decision.actions[index]
        item=compare_next(backend,decision,action) if step>=row['steps']-16 else backend.step(action)
        restores+=int(step>=row['steps']-16)
        if item.truncated:
            raise ValueError('backend truncated during source replay')
        prefix.append(action.to_dict())
        after=item.decision.observation
        key=policy_boundary_fingerprint(item.decision)
        checkpoint=canonical(backend.checkpoint())
        first=seen.get(key)
        tail.append({'step':step+1,'floor':after.run.floor,'act':after.run.act,
            'screen':after.screen.value,'action':action.to_dict(),
            'public_changed':decision!=item.decision,'selected_cards':len(after.selected_cards),
            'fingerprint':key,'previous_same_boundary_step':first[1] if first else None,
            'same_hidden_except_action_history':without_action_history(checkpoint)==without_action_history(first[0]) if first else None,
            'same_rng':checkpoint['rng']==first[0]['rng'] if first else None})
        context=following_context(output,action,item)
        decision=item.decision
        reason=('success' if item.info.get('success') else 'death') if item.terminated else limiter.observe(
            decision,max_steps=4096,max_boundary_visits=4)
        seen.setdefault(key,(checkpoint,step+1))
        if reason:
            break
    if reason!='cycle_limit' or len(prefix)!=row['steps']:
        raise ValueError('recorded source outcome/length did not reproduce')
    classification=classify_tail(list(tail))
    return {'source_seed':row['source_seed'],'split':row['split'],'steps':len(prefix),
        'reason':reason,'floor':decision.observation.run.floor,'act':decision.observation.run.act,
        'screen':decision.observation.screen.value,'classification':classification,
        'restore_checks':restores,'tail':list(tail),'prefix':prefix,
        'checkpoint':canonical(backend.checkpoint()),'target_actions_executed':0}


def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--bank',type=Path,default=Path('local/reports/act2-study-r1/bank-r4'))
    parser.add_argument('--output',type=Path,required=True)
    parser.add_argument('--review',action='store_true',help='Reclassify immutable replay traces; never execute target battles')
    args=parser.parse_args()
    if args.review:
        summary=json.loads((args.output/'summary.json').read_text())
        result=args.output/'classification-review.json'
        if result.exists() or summary['source']!=native_source_digest():
            raise ValueError('existing review or changed native source')
        if sha256_file(args.output/'executed-source.py.txt')!=summary['tool_sha256']:
            raise ValueError('original executed trace collector is unavailable')
        rows=[]
        for row in summary['results']:
            path=args.output/row['trace_file']
            if sha256_file(path)!=row['trace_sha256']:
                raise ValueError('raw source trace changed')
            trace=json.loads(gzip.decompress(path.read_bytes()))
            rows.append({**row,'classification':classify_tail(trace['tail'])})
        write_json(result,{'status':'MEASURED','native_source_sha256':native_source_digest(),
            'original_trace_summary_sha256':sha256_file(args.output/'summary.json'),
            'classifier_source_sha256':sha256_file(Path(__file__)),
            'classification_refinement':'Include public REMOVE/TRANSFORM/UPGRADE grid clicks; full hidden-state, RNG and visible-change requirements unchanged',
            'source_attempts':summary['source_attempts'],'replayed_cycles':len(rows),
            'replayed_decisions':summary['replayed_decisions'],'restore_checks':summary['restore_checks'],
            'classifications':dict(Counter(r['classification'] for r in rows)),
            'floors':dict(Counter(r['floor'] for r in rows)),'target_actions_executed':0,'results':rows})
        print(json.dumps({k:v for k,v in json.loads(result.read_text()).items() if k!='results'}))
        return
    args.output.mkdir(parents=True,exist_ok=False)
    torch.set_num_threads(2)
    torch.set_num_interop_threads(2)
    torch.manual_seed(39171)
    manifest=json.loads((args.bank/'manifest.json').read_text())
    if sha256_file(PARENT)!=PARENT_SHA or manifest['native_source_sha256']!=native_source_digest():
        raise ValueError('source or parent changed')
    model,_=migrate_v5_role_policy(torch.load(PARENT,map_location='cpu',weights_only=False))
    model.eval()
    original=state_digest(model.state_dict())
    if original!=manifest['parent_model_sha256']:
        raise ValueError('parent migration differs from collection')
    ledger=json.loads((args.bank/'source-ledger.json').read_text())
    selected=[r for r in ledger if r['reason']=='cycle_limit']
    results=[]
    for row in selected:
        trace=run(model,row)
        path=args.output/f"{row['split']}-{row['source_seed']}.json.gz"
        path.write_bytes(gzip.compress(json.dumps(trace,ensure_ascii=False).encode(),mtime=0))
        results.append({k:v for k,v in trace.items() if k not in ('prefix','checkpoint','tail')})
        results[-1].update(trace_file=path.name,trace_sha256=sha256_file(path))
        print(f"{len(results)}/{len(selected)}: {trace['classification']} floor {trace['floor']}",flush=True)
    if state_digest(model.state_dict())!=original:
        raise ValueError('frozen parent changed')
    write_json(args.output/'summary.json',{'status':'MEASURED','source':native_source_digest(),
        'bank_sha256':sha256_file(args.bank/'manifest.json'),
        'source_ledger_sha256':sha256_file(args.bank/'source-ledger.json'),
        'tool_sha256':hashlib.sha256(Path(__file__).read_bytes()).hexdigest(),
        'source_attempts':len(ledger),'recorded_cycles':len(selected),'replayed_cycles':len(results),
        'replayed_decisions':sum(r['steps'] for r in results),'restore_checks':sum(r['restore_checks'] for r in results),
        'classifications':dict(Counter(r['classification'] for r in results)),
        'floors':dict(Counter(r['floor'] for r in results)),'target_actions_executed':0,
        'results':results,'limits':['These are existing origin replays, not new independent sources.',
            'A repeated public state does not prove a recurrent stochastic policy is mathematically infinite.',
            'Historical untraced runs remain unclassified; no causal before/after reduction claimed.']})


if __name__=='__main__':
    main()
