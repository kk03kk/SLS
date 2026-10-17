"""Uniform legal-action negative control; no optimization or card heuristics."""
from __future__ import annotations

import argparse
import json
import random
from pathlib import Path

from sls.audit.short_battle import canonical, load_corpus
from sls.backends.simulator import SimulatorBackend
from sls.curriculum import IRONCLAD_A20_ACT2
from sls.rl.episode_limit import EpisodeLimitState
from sls.rl.training_contract import sha256_file


def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--corpus',type=Path,required=True)
    parser.add_argument('--output',type=Path,required=True)
    args=parser.parse_args()
    if args.output.exists():
        raise FileExistsError('retain original probe')
    _,samples=load_corpus(args.corpus)
    results=[]
    for sample in samples:
        if sample['split']=='confirmation' or sample['ordinal_in_source']!=2:
            continue
        for replicate in range(4):
            rng=random.Random(sample['source_seed']*7+replicate)
            backend=SimulatorBackend(IRONCLAD_A20_ACT2)
            decision=backend.load_checkpoint(canonical(sample['checkpoint']))
            limiter=EpisodeLimitState.initial(decision)
            for step in range(512):
                transition=backend.step(rng.choice(decision.actions))
                decision=transition.decision
                if transition.truncated:
                    raise RuntimeError('random control truncated')
                finished=transition.terminated or decision.observation.screen.value!='COMBAT'
                reason=None if finished else limiter.observe(
                    decision,max_steps=512,max_boundary_visits=4)
                if transition.terminated or decision.observation.screen.value!='COMBAT' or reason:
                    win=not reason and decision.observation.player.current_hp>0 and not (
                        transition.terminated and not transition.info.get('success'))
                    results.append({'sample_id':sample['sample_id'],'replicate':replicate,
                        'success':bool(win),'steps':step+1,'limit_reason':reason})
                    break
    report={'corpus_summary_sha256':sha256_file(args.corpus/'summary.json'),
        'policy':'UNIFORM_LEGAL_ACTIONS','ordinal':2,'independent_origins':len(results)//4,
        'replicates_per_origin':4,'wins':sum(r['success'] for r in results),
        'episodes':len(results),'decisions':sum(r['steps'] for r in results),
        'warning':'Replicates from the same origin are not independent origins.',
        'results':results}
    with args.output.open('x',encoding='utf-8') as stream:
        json.dump(report,stream,indent=2)
    print(json.dumps({k:v for k,v in report.items() if k!='results'}))


if __name__=='__main__':
    main()
