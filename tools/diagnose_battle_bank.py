"""Fixed-entry tactical opportunity, potion and GRU probes; no confirmation."""
from __future__ import annotations

import argparse
import json
import os
from collections import Counter
from pathlib import Path

os.environ['CUDA_VISIBLE_DEVICES']='-1'
for key in ('OMP_NUM_THREADS','MKL_NUM_THREADS','OPENBLAS_NUM_THREADS'):
    os.environ[key]='2'

import numpy as np
import torch

from sls.rl.battle_bank import load_bank, write_json
from sls.rl.battle_evaluation import evaluate_roots
from sls.rl.model_migration import migrate_v5_role_policy
from sls.rl.training_contract import sha256_file
from tools.collect_battle_bank import PARENT, PARENT_SHA


def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--bank',type=Path,required=True)
    parser.add_argument('--output',type=Path,required=True)
    args=parser.parse_args()
    if args.output.exists():
        raise FileExistsError('diagnostic is immutable')
    torch.set_num_threads(2)
    torch.set_num_interop_threads(2)
    if sha256_file(PARENT)!=PARENT_SHA:
        raise ValueError('parent changed')
    torch.manual_seed(39171)
    model,_=migrate_v5_role_policy(torch.load(PARENT,map_location='cpu',weights_only=False))
    model.eval()
    rows,manifest=load_bank(args.bank,'development')
    # All development roots form the no-selection baseline. More expensive
    # counterfactuals use the first 32 origin IDs, fixed before any outcomes.
    baseline=evaluate_roots(model,rows)
    subset=rows[:32]
    sampled=evaluate_roots(model,subset,greedy=False,replicates=4)
    fit_rows,_=load_bank(args.bank,'fit')
    fit_sampled=evaluate_roots(model,fit_rows[:32],greedy=False,replicates=4)
    constant=float(np.mean([1. if r['success'] else -1. for r in fit_sampled['results']]))
    targets=np.array([1. if r['success'] else -1. for r in sampled['results']])
    values=np.array([r['root_value'] for r in sampled['results']])
    no_potions=evaluate_roots(model,subset,no_potions=True)
    no_memory=evaluate_roots(model,subset,zero_memory=True)
    by_source={row['sample_id']:[] for row in subset}
    for row in sampled['results']:
        by_source[row['sample_id']].append(int(row['success']))
    histogram=Counter(sum(v) for v in by_source.values())
    ids={row['sample_id'] for row in subset}
    greedy_subset=[r for r in baseline['results'] if r['sample_id'] in ids]
    enemies={r['sample_id']:'+'.join(sorted(r['enemies'])) for r in rows}
    by_enemy={}
    for r in baseline['results']:
        group=by_enemy.setdefault(enemies[r['sample_id']],{'origins':0,'wins':0,'reasons':Counter()})
        group['origins']+=1
        group['wins']+=int(r['success'])
        group['reasons'][r['reason']]+=1
    report={'bank_sha256':sha256_file(args.bank/'manifest.json'),'source':manifest['native_source_sha256'],
        'development_greedy':baseline,'first_32_stochastic_four_repeats':sampled,
        'development_by_enemy_combination':by_enemy,
        'first_32_greedy':{'episodes':len(greedy_subset),'wins':sum(r['success'] for r in greedy_subset),
            'success_rate':sum(r['success'] for r in greedy_subset)/len(greedy_subset),'results':greedy_subset},
        'fit_first_32_stochastic_four_repeats':fit_sampled,
        'battle_return_calibration':{'independent_development_origins':32,'replicates_per_origin':4,
            'old_parent_head_mse':float(np.mean((values-targets)**2)),
            'fit_mean_constant':constant,'fit_mean_constant_mse':float(np.mean((constant-targets)**2)),
            'neutral_zero_mse':float(np.mean(targets**2)),
            'warning':'New short-task targets; inherited Act1 head was not trained for this target. MSE is not policy gain.'},
        'first_32_no_use_potion':no_potions,'first_32_zero_root_memory':no_memory,
        'stochastic_wins_per_origin_histogram':dict(histogram),'confirmation':'NOT_READ_OR_EVALUATED',
        'interpretation_limits':['Same entrance isolates suffix variation, not optimal deck quality.',
            'Four losses do not prove an entrance is unwinnable.',
            'Different actions can consume different RNG streams; only initial RNG state is paired.',
            'Zero memory/no potion are ablations, not proposed production policies.']}
    write_json(args.output,report)
    print(json.dumps({k:v for k,v in report.items() if k in ('stochastic_wins_per_origin_histogram','confirmation')}))


if __name__=='__main__':
    main()
