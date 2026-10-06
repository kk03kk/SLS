"""Profile CPU encoding and padding on immutable stock trajectory boundaries.

Run without concurrent tests, games or training. This is a CPU microbenchmark,
not an estimate of server training throughput or policy quality.
"""
from __future__ import annotations

import argparse
import cProfile
import hashlib
import json
import statistics
import time
from pathlib import Path

import torch

from sls.backends.original.adapter import adapt_original
from sls.contracts import Decision
from sls.curriculum import IRONCLAD_A20_ACT2
from sls.diagnostics.canary import read_trajectory
from sls.model.batching import PolicyBatch, encode_decision
from sls.rl.training_contract import native_source_digest


def load_samples(paths: list[Path]):
    decisions, selections = [], []
    for path in paths:
        _, boundaries = read_trajectory(path)
        if not boundaries or not boundaries[-1]['terminal']:
            raise ValueError('requires complete stock trajectories')
        eligible = [i for i, b in enumerate(boundaries) if not b['terminal']]
        if len(eligible) < 16:
            raise ValueError('requires at least 16 nonterminal boundaries')
        indices = [eligible[j * (len(eligible) - 1) // 15] for j in range(16)]
        for index in indices:
            row = boundaries[index]
            decision = adapt_original(
                row['diagnostic_state'], allow_key_acquisition=IRONCLAD_A20_ACT2.allows_keys,
            ).decision
            actions = {json.dumps(a.to_dict(), sort_keys=True): a for a in decision.actions}
            recorded = [json.dumps(a, sort_keys=True) for a in row['candidate_actions']]
            if (decision.observation.to_dict() != row['observation'] or
                    len(set(recorded)) != len(recorded) or set(actions) != set(recorded)):
                raise ValueError('recorded stock projection no longer matches adapter')
            # Capture stores canonical policy ordering; adapter wire order differs.
            # Reconstruct exactly that recorded order, without changing any action.
            decision = Decision(decision.observation, tuple(actions[k] for k in recorded))
            decisions.append(decision)
        selections.append({'path': path.as_posix(),
                           'sha256': hashlib.sha256(path.read_bytes()).hexdigest(),
                           'boundaries': indices})
    return decisions, selections


def timing(function, repetitions: int) -> dict:
    values = []
    for _ in range(repetitions):
        start = time.perf_counter()
        function()
        values.append(time.perf_counter() - start)
    return {'median_seconds': statistics.median(values), 'min_seconds': min(values),
            'repetitions': repetitions}


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('trajectories', type=Path, nargs='+')
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    profile_path = args.output.with_suffix('.prof')
    if args.output.exists() or profile_path.exists():
        raise FileExistsError('refuse to overwrite benchmark evidence')
    torch.set_num_threads(1)
    decisions, selections = load_samples(args.trajectories)
    if len(decisions) < 64:
        raise ValueError('requires at least 64 sampled decisions')
    decisions = decisions[:64]
    encoded = [encode_decision(d) for d in decisions]
    for _ in range(10):
        PolicyBatch.from_decisions(decisions)
    result = {'schema': 'sls-act12-batching-profile-v1',
              'native_source_sha256': native_source_digest(),
              'batching_source_sha256': hashlib.sha256(
                  Path('src/sls/model/batching.py').read_bytes()).hexdigest(),
              'torch_version': torch.__version__, 'threads': 1, 'batch_size': 64,
              'selection_rule': '16 equally spaced nonterminal boundaries per capture; first 64',
              'selections': selections,
              'encode': timing(lambda: [encode_decision(d) for d in decisions], 30),
              'padding': timing(lambda: PolicyBatch.from_encoded(encoded), 50),
              'claim': 'CPU microbenchmark only; not end-to-end training throughput'}
    profile = cProfile.Profile()
    profile.enable()
    for _ in range(10):
        PolicyBatch.from_decisions(decisions)
    profile.disable()
    profile.dump_stats(str(profile_path))
    args.output.write_text(json.dumps(result, indent=2) + '\n', encoding='utf-8')
    print(json.dumps({k: result[k] for k in ('encode', 'padding')}))
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
