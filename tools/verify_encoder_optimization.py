"""Compare the original CPU conversion and optimized encoder on fixed evidence.

Uses actual frozen parent weights, exact input/output checks and deterministic
collect+PPO micro-updates. No resulting model is exported as a trained policy.
Run without another game, benchmark or test process.
"""
from __future__ import annotations

import argparse
import copy
import hashlib
import json
import os
import random
import statistics
import time
import tomllib
from pathlib import Path

os.environ.setdefault('CUBLAS_WORKSPACE_CONFIG', ':4096:8')

import numpy as np
import torch

from sls.curriculum import IRONCLAD_A20_ACT2
from sls.model import PolicyBatch, batching
from sls.rl import PPOConfig, PPOTrainer, ShardedWorkerPool
from sls.rl.training_contract import native_source_digest, runtime_contract
from sls.runtime import load_policy_artifact
from tools.profile_act12_batching import load_samples


def tensor_hash(tensor: torch.Tensor) -> str:
    value = tensor.detach().cpu().contiguous()
    return hashlib.sha256(str((str(value.dtype), tuple(value.shape))).encode()
                          + value.numpy().tobytes()).hexdigest()


def rollout_hash(rollout) -> str:
    values = []
    for name in rollout.__dataclass_fields__:
        field = getattr(rollout, name)
        if name == 'encoded_decisions':
            values.extend(tensor_hash(getattr(item, key)) for step in field
                          for item in step for key in item.__dataclass_fields__)
        else:
            values.append(tensor_hash(field))
    return hashlib.sha256(json.dumps(values).encode()).hexdigest()


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--artifact', type=Path, required=True)
    parser.add_argument('--config', type=Path, required=True)
    parser.add_argument('--trajectories', type=Path, nargs='+', required=True)
    parser.add_argument('--output', type=Path, required=True)
    parser.add_argument('--rounds', type=int, default=3)
    args = parser.parse_args()
    if args.output.exists():
        raise FileExistsError('refuse to overwrite evidence')
    if args.rounds < 2:
        raise ValueError('at least two measured updates required')
    torch.set_num_threads(1)
    torch.use_deterministic_algorithms(True)
    torch.backends.cudnn.benchmark = False
    torch.set_float32_matmul_precision('high')
    device = 'cuda' if torch.cuda.is_available() else 'cpu'
    artifact = load_policy_artifact(args.artifact, device=device)
    if artifact.metadata.model_sha256 != 'ed9068343c8d628a13595a3919d8a15c5f9a19840be1042e07e5f12fcc8ca30c':
        raise ValueError('requires frozen90 parent model')
    decisions, selections = load_samples(args.trajectories)
    optimized = batching._cpu_tensor
    def reference(values, dtype):
        return torch.tensor(values, dtype=dtype)
    def synchronize():
        if device == 'cuda':
            torch.cuda.synchronize()
    # Both paths execute exactly the same feature extraction and batch packing.
    batching._cpu_tensor = reference
    old_encoded = [batching.encode_decision(d) for d in decisions]
    batching._cpu_tensor = optimized
    new_encoded = [batching.encode_decision(d) for d in decisions]
    for old, new in zip(old_encoded, new_encoded, strict=True):
        for name in old.__dataclass_fields__:
            left, right = getattr(old, name), getattr(new, name)
            assert left.dtype == right.dtype and torch.equal(left, right), name
    with torch.no_grad():
        for start in range(0, len(decisions), 8):
            old = PolicyBatch.from_encoded(old_encoded[start:start+8])
            new = PolicyBatch.from_encoded(new_encoded[start:start+8])
            for a, b in zip(old.model_inputs(), new.model_inputs(), strict=True):
                assert a.dtype == b.dtype and torch.equal(a, b)
            old, new = old.to(device), new.to(device)
            memory = torch.ones_like(artifact.model.initial_memory(old.screen_types.shape[0], device))
            a = artifact.model(*old.model_inputs(), memory=memory)
            b = artifact.model(*new.model_inputs(), memory=memory)
            for name in ('logits', 'value', 'next_memory'):
                assert torch.equal(getattr(a, name), getattr(b, name)), name
            assert torch.equal(torch.log_softmax(a.logits, -1), torch.log_softmax(b.logits, -1))
    payload = tomllib.loads(args.config.read_text(encoding='utf-8'))
    # Short fixed workload, not a replacement for production-layout benchmarking.
    config = PPOConfig(**{**payload['ppo'], 'rollout_steps': 32,
                         'recurrent_sequence_length': 32, 'minibatch_sequences': 4})
    results = []
    for name, conversion in [('reference', reference), ('optimized', optimized),
                             ('optimized', optimized), ('reference', reference)]:
        batching._cpu_tensor = conversion
        model = copy.deepcopy(artifact.model).train()
        random.seed(918273)
        torch.manual_seed(918273)
        with ShardedWorkerPool(IRONCLAD_A20_ACT2, 4, shard_count=2) as workers:
            trainer = PPOTrainer(model, workers, config, device=device, seed=130000000,
                                 training_seed_limit=2000000000000,
                                 native_contract_digest=native_source_digest(),
                                 training_config_digest='ENCODER_EQUIVALENCE_DIAGNOSTIC')
            trainer.train_update()
            synchronize()
            timings, hashes, metrics = [], [], []
            for _ in range(args.rounds):
                synchronize()
                start = time.perf_counter()
                rollout = trainer.collect()
                measured = trainer.optimize(rollout)
                synchronize()
                timings.append(time.perf_counter() - start)
                hashes.append(rollout_hash(rollout))
                metrics.append(measured)
            parameters = {k: tensor_hash(v) for k, v in model.state_dict().items()}
            results.append({'conversion': name, 'seconds': timings,
                            'rollout_sha256': hashes, 'metrics': metrics,
                            'final_parameters': parameters})
        del trainer, model, rollout
    batching._cpu_tensor = optimized
    for row in results[1:]:
        for key in ('rollout_sha256', 'metrics', 'final_parameters'):
            if row[key] != results[0][key]:
                raise RuntimeError(f'encoder changed deterministic {key}')
    medians = {name: statistics.median([s for r in results if r['conversion'] == name
                                        for s in r['seconds']])
               for name in ('reference', 'optimized')}
    result = {'schema': 'sls-encoder-optimization-verification-v1', 'status': 'PASS',
              'native_source_sha256': native_source_digest(), 'runtime': runtime_contract(torch),
              'numpy_version': np.__version__, 'parent_model_sha256': artifact.metadata.model_sha256,
              'artifact_sha256': hashlib.sha256(args.artifact.read_bytes()).hexdigest(),
              'selections': selections, 'input_and_model_fields_exact': True,
              'rollout_and_ppo_update_exact': True, 'worker_layout': [4, 2],
              'rollout_steps': 32, 'order': ['reference', 'optimized', 'optimized', 'reference'],
              'median_collect_optimize_seconds': medians, 'runs': results,
              'limitation': 'Windows small fixed workload; no NUS throughput or policy-quality claim'}
    args.output.write_text(json.dumps(result, indent=2) + '\n', encoding='utf-8')
    print(json.dumps({'status': result['status'], 'median_seconds': medians}))
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
