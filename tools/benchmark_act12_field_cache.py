"""Bounded ABBA prototype: cache static dataclass metadata, never dynamic state.

No production files or trained models are changed. A prototype result does not
authorize integrating it into the ongoing experiment or long-run recipe.
"""
from __future__ import annotations

import argparse
import functools
import json
import statistics
import time
from pathlib import Path

from tools.diagnose_act12_learning import probe


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--checkpoint', type=Path, required=True)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    if args.output.exists():
        raise FileExistsError('refuse to overwrite prototype evidence')
    from sls.contracts import observation
    original = observation.fields
    cache = functools.lru_cache(maxsize=128)(original)
    def cached(value):
        return cache(value if isinstance(value, type) else type(value))
    rows = []
    baseline = None
    try:
        for mode in ('reference', 'prototype', 'prototype', 'reference'):
            observation.fields = original if mode == 'reference' else cached
            started = time.perf_counter()
            result = probe(args.checkpoint)
            elapsed = time.perf_counter() - started
            comparable = {k: result[k] for k in ('rollout_sha256', 'scratch_model_sha256',
                          'completed_trajectories', 'shared_gradients', 'memory_after_scratch_update',
                          'scratch_update_metrics', 'neow_content_probabilities')}
            if baseline is None:
                baseline = comparable
            if comparable != baseline:
                raise ValueError('prototype changes rollout, policy/critic, memory or diagnostic PPO update')
            rows.append({'mode': mode, 'seconds': elapsed, 'collection_profile_seconds': result['collection_profile_seconds'],
                         'rollout_sha256': result['rollout_sha256'], 'scratch_model_sha256': result['scratch_model_sha256']})
            print(json.dumps(rows[-1]), flush=True)
    finally:
        observation.fields = original
    medians = {mode: statistics.median(r['seconds'] for r in rows if r['mode'] == mode)
               for mode in ('reference', 'prototype')}
    result = {'schema': 'sls-act12-static-metadata-prototype-v1', 'rows': rows, 'median_seconds': medians,
              'numerical_equivalence': True, 'production_integrated': False,
              'decision': 'DEFER_INTEGRATION_KEEP_PARENT_IMPLEMENTATION',
              'limitations': 'Two runs per path; local instrumented collection plus disposable PPO update and startup. No NUS throughput claim.'}
    args.output.parent.mkdir(parents=True, exist_ok=True)
    with args.output.open('x', encoding='utf-8') as stream:
        json.dump(result, stream, indent=2)


if __name__ == '__main__':
    main()
