"""Analyze the CPU pilot without evaluating confirmation battle branches."""
from __future__ import annotations

import argparse
import json
import math
from collections import Counter

import numpy as np

from sls.audit.short_battle import load_corpus
from sls.rl.training_contract import sha256_file


def wilson(wins, total):
    z = 1.959963984540054
    p = wins / total
    denominator = 1 + z*z/total
    center = (p + z*z/(2*total))/denominator
    half = z*math.sqrt(p*(1-p)/total + z*z/(4*total*total))/denominator
    return [center-half, center+half]


def paired_power(n, discordance, delta=.10, simulations=20000):
    """Exact two-sided McNemar rejection, Monte Carlo prospective power."""
    rng = np.random.default_rng(73401+n)
    discordant = rng.binomial(n, discordance, simulations)
    favorable = rng.binomial(discordant, (discordance+delta)/(2*discordance))
    critical = {}
    for d in np.unique(discordant):
        probability = 2.**(-int(d))
        cdf = probability
        threshold = -1
        for k in range(int(d)//2+1):
            if 2*cdf <= .05:
                threshold = k
            probability *= (int(d)-k)/(k+1)
            cdf += probability
        critical[int(d)] = threshold
    reject = np.array([min(int(k), int(d-k)) <= critical[int(d)]
                       for d,k in zip(discordant,favorable,strict=True)])
    return float(reject.mean())


def main():
    from pathlib import Path
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--corpus', type=Path, required=True)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    if args.output.exists():
        raise FileExistsError('retain original analysis')
    summary, samples = load_corpus(args.corpus)
    baseline = json.loads((args.corpus/'baseline.json').read_text())
    fit = [r for r in baseline if r['split']=='fit']
    development = [r for r in baseline if r['split']=='development']
    constant = float(np.mean([r['original_complete_return'] for r in fit]))
    def calibration(rows):
        target = np.array([r['original_complete_return'] for r in rows])
        old = np.array([r['value_shaped'] for r in rows])
        return {'n_roots':len(rows),'old_value_mean':float(old.mean()),
                'complete_return_mean':float(target.mean()),
                'old_head_mse':float(np.mean((old-target)**2)),
                'fit_constant_mse':float(np.mean((constant-target)**2)),
                'limitation':'All originating episodes failed; no win/loss ranking or causal claim.'}
    rates = []
    for ordinal in range(3):
        rows = [r for r in baseline if r['ordinal']==ordinal]
        record = {'ordinal':ordinal,'independent_origins':len({r['source_seed'] for r in rows}),
                  'n':len(rows)}
        for mode in ('stochastic','greedy'):
            wins = sum(r[mode]['success'] for r in rows)
            record[mode] = {'wins':wins,'rate':wins/len(rows),
                            'wilson95':wilson(wins,len(rows))}
        rates.append(record)
    failure = Counter('+'.join(r['enemies']) for r in baseline if not r['stochastic']['success'])
    sources = json.loads((args.corpus/'sources.json').read_text())
    def distribution(values):
        values=np.asarray(values,dtype=float)
        return {'min':float(values.min()),'p25':float(np.quantile(values,.25)),
                'median':float(np.median(values)),'p75':float(np.quantile(values,.75)),
                'max':float(values.max()),'mean':float(values.mean())}
    result = {'corpus_summary_sha256':sha256_file(args.corpus/'summary.json'),
              'native_source_sha256':summary['native_source_sha256'],
              'roots':len(samples),'independent_origins':summary['independent_sources_with_roots'],
              'ordinal_baselines_fit_development_only':rates,
              'stochastic_failure_enemy_counts':dict(failure),
              'paired_greedy_stochastic':dict(Counter(
                  f"stochastic={r['stochastic']['success']},greedy={r['greedy']['success']}" for r in baseline)),
              'old_full_task_value_calibration':{'fit':calibration(fit),'development':calibration(development)},
              'source_cycle_rate':sum(r['reason']=='cycle_limit' for r in sources)/len(sources),
              'static_resources_all_roots':{
                  'hp_fraction':distribution([s['hp']/s['max_hp'] for s in samples]),
                  'deck_size':distribution([len(s['deck']) for s in samples]),
                  'gold':distribution([s['gold'] for s in samples]),
                  'potions':distribution([len(s['potions']) for s in samples]),
                  'relics':distribution([len(s['relics']) for s in samples]),
                  'floor':dict(Counter(s['floor'] for s in samples)),
                  'ordinal':dict(Counter(s['ordinal_in_source'] for s in samples)),
                  'enemy_compositions':dict(Counter('+'.join(s['enemies']) for s in samples)),
                  'encounter_identifier_unavailable':sum(s['encounter'] is None for s in samples)},
              'paired_power_delta_10pp':[{'origins':n,'discordance':q,'power':paired_power(n,q)}
                 for n in (16,32,64,128,256,512,640,768) for q in (.2,.5,.8)],
              'confirmation_limit':'Pilot only: multiple roots/public prefixes from one run reveal earlier outcomes. Use fresh single-root origins for final confirmation.',
              'interpretation':'Ordinal 0 is saturated in pilot; ordinal 2 is a candidate, not a proven population optimum. No formal training performed.'}
    with args.output.open('x',encoding='utf-8') as stream:
        json.dump(result,stream,indent=2,allow_nan=False)
    print(json.dumps(result,indent=2))


if __name__=='__main__':
    main()
