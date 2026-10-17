"""Prospective exact paired-test sensitivity for the registered large-effect test."""
import argparse
import json
import math
from pathlib import Path

import numpy as np


def power(delta,q,*,n=384,simulations=20000):
    rng=np.random.default_rng(734017)
    discordant=rng.binomial(n,q,simulations)
    favorable=rng.binomial(discordant,(q+delta)/(2*q))
    probabilities={}
    for d in np.unique(discordant):
        cumulative=0.
        for k in range(int(d)//2+1):
            cumulative+=math.comb(int(d),k)/2**int(d)
            probabilities[int(d),k]=min(1.,2*cumulative)
    p=np.array([probabilities[int(d),min(int(b),int(d-b))] for d,b in zip(discordant,favorable,strict=True)])
    gains=(2*favorable-discordant)/n
    return {'origins':n,'true_gain':delta,'discordance':q,'exact_p_and_observed_10pp_power':float(((p<.05)&(gains>=.10)).mean()),
        'positive_exact_test_power':float(((p<.05)&(gains>0)).mean()),
        'limitation':'Does not independently simulate the added bootstrap-CI condition; sensitivity, not guaranteed power.'}


def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--output',type=Path,required=True)
    args=parser.parse_args()
    result={'simulations':20000,'seed':734017,
            'results':[power(delta,q) for delta in (.10,.15) for q in (.2,.5,.8)]}
    with args.output.open('x',encoding='utf-8') as stream:
        json.dump(result,stream,indent=2)
    print(json.dumps(result))


if __name__=='__main__':
    main()
