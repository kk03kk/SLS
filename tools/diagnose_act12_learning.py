"""Frozen-policy local credit/gradient/memory/Neow/throughput diagnostic, not training efficacy."""
from __future__ import annotations

import argparse
import copy
import cProfile
import json
import os
import pstats
from pathlib import Path

os.environ.setdefault('CUBLAS_WORKSPACE_CONFIG', ':4096:8')


def probe(checkpoint: Path, *, workers: int = 4, seed: int = 132000000) -> dict:
    import torch
    from torch.distributions import Categorical

    import sls.rl.ppo as ppo
    from sls.curriculum import IRONCLAD_A20_ACT2
    from sls.model import PolicyBatch
    from sls.rl.checkpoint import policy_from_training_checkpoint
    from sls.rl.rollout import generalized_advantage_estimate
    from sls.rl.training_contract import (
        native_source_digest,
        runtime_contract,
        sha256_file,
        training_implementation_digest,
    )
    from sls.rl.workers import ShardedWorkerPool
    from tools.verify_act12_diagnostics import assert_same

    if workers < 1 or not 132000000 <= seed < 133000000:
        raise ValueError('diagnostic workers/seed outside registered local space')
    torch.set_num_threads(1)
    torch.use_deterministic_algorithms(True)
    torch.backends.cudnn.benchmark = False
    torch.set_float32_matmul_precision('high')
    torch.manual_seed(1729)
    device = 'cuda' if torch.cuda.is_available() else 'cpu'
    source = torch.load(checkpoint, map_location='cpu', weights_only=False)
    model = policy_from_training_checkpoint(source, device=device)
    before = copy.deepcopy(model.state_dict())
    config = ppo.PPOConfig(**source['contract']['ppo'])
    captured = {}
    original = ppo.generalized_advantage_estimate
    def capture(rewards, values, terminated, bootstrap, gamma, lam):
        captured.update(rewards=rewards, values=values, terminated=terminated, bootstrap=bootstrap)
        return original(rewards, values, terminated, bootstrap, gamma, lam)
    ppo.generalized_advantage_estimate = capture
    try:
        with ShardedWorkerPool(IRONCLAD_A20_ACT2, workers, min(2, workers)) as pool:
            terminal_labels = {}
            time_index = 0
            class RecordedPool:
                def __getattr__(self, name):
                    return getattr(pool, name)

                def step(self, actions):
                    nonlocal time_index
                    result = pool.step(actions)
                    for env, transition in enumerate(result):
                        if transition.terminated:
                            terminal_labels[(time_index, env)] = bool(transition.info.get('success'))
                    time_index += 1
                    return result
            trainer = ppo.PPOTrainer(model, RecordedPool(), config, device=device, seed=seed)
            initial = trainer.decisions
            with torch.no_grad():
                batch = PolicyBatch.from_decisions(initial, model.config).to(device)
                probabilities = Categorical(logits=model(*batch.model_inputs()).logits).probs.cpu().tolist()
            neow = [{'seed': seed + i, 'public_observation': d.observation.to_dict(),
                     'option_ids': [a.option_id for a in d.actions], 'probabilities': row[:len(d.actions)]}
                    for i, (d, row) in enumerate(zip(initial, probabilities, strict=True))]
            profile = cProfile.Profile()
            profile.enable()
            rollout = trainer.collect()
            profile.disable()
            stats = pstats.Stats(profile)
            hotspots = sorted([{'file': str(key[0]), 'line': key[1], 'function': key[2],
                                'calls': value[1], 'self_seconds': value[2], 'cumulative_seconds': value[3]}
                               for key, value in stats.stats.items()],
                              key=lambda r: r['self_seconds'], reverse=True)[:25]
            a98, r98 = generalized_advantage_estimate(captured['rewards'], captured['values'], captured['terminated'], captured['bootstrap'], config.gamma, .98)
            a1, r1 = generalized_advantage_estimate(captured['rewards'], captured['values'], captured['terminated'], captured['bootstrap'], config.gamma, 1)
            trajectories = []
            for env in range(workers):
                for start in rollout.episode_starts[:, env].nonzero().flatten().tolist():
                    ends = captured['terminated'][start:, env].nonzero().flatten()
                    if not ends.numel():
                        continue
                    end = start + int(ends[0])
                    direct = float(sum(config.gamma**i * captured['rewards'][start+i, env]
                                       for i in range(end-start+1)))
                    trajectories.append({'env': env, 'start': start, 'end': end,
                                         'success': terminal_labels.get((end, env)),
                                         'actual_shaped_return': direct,
                                         'gae98_return': float(r98[start, env]), 'gae1_return': float(r1[start, env]),
                                         'advantage98': float(a98[start, env]), 'advantage1': float(a1[start, env])})
            chunks = trainer._sequence_chunks(rollout)[:2]
            logp, values, _ = trainer._evaluate_sequences(rollout, chunks)
            normalized = ppo.normalize_advantages_by_domain(rollout.advantages, rollout.encoded_decisions)
            old_logp = trainer._select_sequences(rollout.old_log_probabilities, chunks).to(device)
            adv = trainer._select_sequences(normalized, chunks).to(device)
            actor = ppo.clipped_policy_loss((logp-old_logp).exp(), adv, config.clip_ratio)
            returns = trainer._select_sequences(rollout.returns, chunks).to(device)
            old_values = trainer._select_sequences(rollout.old_values, chunks).to(device)
            clipped = old_values + (values-old_values).clamp(-config.value_clip_ratio, config.value_clip_ratio)
            critic = config.value_coefficient * .5 * torch.maximum((values-returns).square(), (clipped-returns).square()).mean()
            shared = [p for name, p in model.named_parameters() if name.startswith(('backbone.', 'memory.', 'entity_', 'content.', 'category.', 'cls'))]
            ag = torch.autograd.grad(actor, shared, retain_graph=True, allow_unused=True)
            cg = torch.autograd.grad(critic, shared, allow_unused=True)
            ga = torch.cat([(g if g is not None else torch.zeros_like(p)).flatten() for g, p in zip(ag, shared, strict=True)])
            gc = torch.cat([(g if g is not None else torch.zeros_like(p)).flatten() for g, p in zip(cg, shared, strict=True)])
            gradients = {'actor_norm': float(ga.norm()), 'weighted_critic_norm': float(gc.norm()),
                         'cosine': float(torch.dot(ga, gc) / (ga.norm()*gc.norm()).clamp_min(1e-12)),
                         'samples': len(chunks) * config.recurrent_sequence_length,
                         'limitation': 'One frozen on-policy minibatch; interference pathway, not causal attribution.'}
            # A disposable copy receives one diagnostic PPO update. Original weights/Adam remain untouched.
            scratch = copy.deepcopy(model)
            trainer.model = scratch
            trainer.optimizer = torch.optim.Adam(scratch.parameters(), lr=config.learning_rate)
            scratch_metrics = trainer.optimize(rollout)
            memory = rollout.input_memories[0].to(device)
            memory_rows = []
            scratch.eval()
            with torch.no_grad():
                for t in range(config.rollout_steps):
                    b = PolicyBatch.from_encoded(rollout.encoded_decisions[t]).to(device)
                    kwargs = {'episode_start_mask': rollout.episode_starts[t].to(device),
                              'previous_action_types': rollout.previous_action_types[t].to(device),
                              'previous_rewards': rollout.previous_rewards[t].to(device)}
                    rebuilt = scratch(*b.model_inputs(), memory=memory, **kwargs)
                    if t and t % config.recurrent_sequence_length == 0:
                        stored = scratch(*b.model_inputs(), memory=rollout.input_memories[t].to(device), **kwargs)
                        memory_rows.append({'offset': t, 'memory_l2': float((memory-rollout.input_memories[t].to(device)).norm()),
                                            'logits_max_abs': float((rebuilt.logits-stored.logits).abs().max()),
                                            'value_max_abs': float((rebuilt.value-stored.value).abs().max())})
                    memory = rebuilt.next_memory
            assert_same(before, model.state_dict())
            from sls.rl.training_contract import canonical_digest
            from tools.verify_encoder_optimization import rollout_hash, tensor_hash
            return {'schema': 'sls-act12-learning-diagnostic-v1', 'role': 'LOCAL_DIAGNOSTIC_NOT_TRAINING_EFFICACY',
                    'rollout_sha256': rollout_hash(rollout),
                    'scratch_model_sha256': canonical_digest({k: tensor_hash(v) for k, v in scratch.state_dict().items()}),
                    'checkpoint_sha256': sha256_file(checkpoint), 'source_profile': source['contract']['profile'].profile_id,
                    'source_native_sha256': source['contract']['native_source_sha256'],
                    'evaluation_profile': 'IRONCLAD_A20_ACT2', 'native_source_sha256': native_source_digest(),
                    'training_implementation_sha256': training_implementation_digest(), 'runtime': runtime_contract(torch),
                    'seed_start': seed, 'workers': workers, 'rollout_steps': config.rollout_steps,
                    'completed_trajectories': trajectories, 'shared_gradients': gradients,
                    'memory_after_scratch_update': memory_rows, 'scratch_update_metrics': scratch_metrics,
                    'memory_limit': 'Prefix rebuilt from normal starts under disposable updated model; does not estimate win-rate impact.',
                    'neow_content_probabilities': neow, 'collection_profile_seconds': trainer.last_collect_profile,
                    'cprofile_self_time_hotspots': hotspots,
                    'performance_limit': 'Instrumented local sampling; nested cumulative timings cannot be added; not NUS throughput.',
                    'frozen_weights_unchanged': True, 'diagnostic_model_exported': False,
                    'limitations': 'Complete trajectories within one rollout are length-censored; no matched-state combat causality or Neow intervention claim.'}
    finally:
        ppo.generalized_advantage_estimate = original


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--checkpoint', type=Path, required=True)
    parser.add_argument('--output', type=Path, required=True)
    parser.add_argument('--workers', type=int, default=4)
    args = parser.parse_args()
    if args.output.exists():
        raise FileExistsError('refuse to overwrite diagnostic evidence')
    result = probe(args.checkpoint, workers=args.workers)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    with args.output.open('x', encoding='utf-8') as stream:
        json.dump(result, stream, indent=2)
    print(json.dumps({key: result[key] for key in ('shared_gradients', 'memory_after_scratch_update', 'frozen_weights_unchanged')}))


if __name__ == '__main__':
    main()
