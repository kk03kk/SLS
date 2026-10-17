"""Create an immutable CPU pilot corpus from unmodified normal-start runs.

No formal configuration, checkpoint or source qualification is rewritten.
Confirmation roots are stored but their battle outcomes are not evaluated.
"""
from __future__ import annotations

import argparse
import gzip
import json
import os
import sys
import time
import tomllib
from collections import Counter
from pathlib import Path

os.environ['CUDA_VISIBLE_DEVICES'] = '-1'
os.environ['SLS_TEST_CUDA'] = '0'
for name in ('OMP_NUM_THREADS', 'MKL_NUM_THREADS', 'OPENBLAS_NUM_THREADS'):
    os.environ[name] = '2'
ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / 'src'))

import torch

from sls.audit.short_battle import (
    PolicyContext,
    canonical,
    compare_next,
    following_context,
    predict,
    rebuild_context,
    run_battle,
)
from sls.backends.simulator import SimulatorBackend
from sls.curriculum import IRONCLAD_A20_ACT2 as PROFILE
from sls.rl.episode_limit import EpisodeLimitState
from sls.rl.model_migration import migrate_v5_role_policy
from sls.rl.reward import curriculum_terminal_reward, shape_curriculum_reward
from sls.rl.training_contract import native_artifact, native_source_digest, sha256_file
from sls.rl.value_adaptation import complete_returns, state_digest


def dump(path, value):
    with path.open('x', encoding='utf-8') as stream:
        json.dump(value, stream, ensure_ascii=False, indent=2, allow_nan=False)
        stream.write('\n')


def source_plan(count):
    rows = []
    ordinals = Counter()
    for i in range(count):
        split = ('fit', 'fit', 'development', 'confirmation')[i % 4]
        start = {'fit': 1_500_000_000_000, 'development': 8_000_102_003_000,
                 'confirmation': 8_000_102_004_000}[split]
        rows.append({'source_seed': start + ordinals[split], 'split': split})
        ordinals[split] += 1
    return rows


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--output', type=Path, required=True)
    parser.add_argument('--sources', type=int, default=32)
    parser.add_argument('--max-decisions', type=int, default=16384)
    args = parser.parse_args()
    if args.sources < 4 or args.sources > 128 or not 0 < args.max_decisions <= 65536:
        raise ValueError('local pilot limits: sources 4..128, decisions 1..65536')
    args.output.mkdir(parents=True, exist_ok=False)
    torch.set_num_threads(2)
    torch.set_num_interop_threads(2)
    parent = ROOT / 'local/runs/ironclad-a20-act1-win-90m-continuation/final.pt'
    expected = '274963f4fe32b75003c5a1b4ccd394b5185304156e6ea22a76aee2764d54f1e0'
    if sha256_file(parent) != expected:
        raise ValueError('parent checkpoint changed')
    parent_payload = torch.load(parent, map_location='cpu', weights_only=False)
    torch.manual_seed(81271)
    model, migration = migrate_v5_role_policy(parent_payload)
    model.eval()
    parent_model = state_digest(model.state_dict())
    source = native_source_digest()
    plan = source_plan(args.sources)
    # Reject overlap with all configured training reservations, not only start seeds.
    scanned = []
    for p in sorted((ROOT/'configs/train').glob('*.toml')):
        config = tomllib.loads(p.read_text())
        first = config.get('run', {}).get('seed', 0)
        budget = max([s.get('target_environment_steps', 0) for s in config.get('stages', {}).values()] or [0])
        for row in plan:
            if first <= row['source_seed'] < first + budget + 128:
                raise ValueError(f'source seed overlaps configured training reservation: {p}')
        scanned.append({'path': str(p.relative_to(ROOT)), 'sha256': sha256_file(p)})
    registration = ROOT/'local/reports/act12-normalstart-release/development-seeds.json'
    allocated = json.loads(registration.read_text())['seed_range']
    assert all(allocated[0] <= r['source_seed'] < allocated[1] for r in plan if r['split'] != 'fit')
    dump(args.output/'allocation.json', {'sources': plan, 'configured_training_scan': scanned,
         'development_parent_allocation': str(registration), 'sha256': sha256_file(registration),
         'suballocation': True, 'future_outcome_selection': False})
    dump(args.output/'identity.json', {'schema': 'sls-act2-short-battle-pilot-v1', 'parent_sha256': expected,
         'parent_model_sha256': parent_model, 'native_source_sha256': source, 'native_artifact': native_artifact(),
         'migration': migration, 'profile': PROFILE.profile_id, 'memory': 'CURRENT_MODEL_FORCED_LEGAL_PREFIX',
         'resource_injection': False, 'max_roots_per_source': 3, 'source_policy': 'STOCHASTIC_FROZEN_PARENT',
        'confirmation_outcomes': 'NOT_EVALUATED', 'formal_training_ready': False,
        'confirmation_limit': 'Pilot source split, not a sealed final test: later roots may contain earlier fight outcomes in their public prefixes.'})
    samples, ledger, checks = [], [], Counter()
    decisions = 0
    started = time.monotonic()
    for ordinal, row in enumerate(plan):
        backend = SimulatorBackend(PROFILE)
        decision = backend.reset(row['source_seed'])
        context = PolicyContext(model.initial_memory(1).cpu())
        generator = torch.Generator().manual_seed(row['source_seed'] % (2**63))
        limiter = EpisodeLimitState.initial(decision)
        prefix, rewards, roots, tail = [], [], [], []
        battle_floor = None
        reason = 'budget_censored'
        while decisions < args.max_decisions:
            obs = decision.observation
            output = predict(model, decision, context)
            if obs.run.act == 2 and obs.screen.value == 'COMBAT' and obs.run.floor != battle_floor:
                battle_floor = obs.run.floor
                if len(roots) < 3:
                    sample = {**row, 'sample_id': f'{row["source_seed"]}-{len(roots)}',
                        'native_source_sha256': source, 'parent_model_sha256': parent_model,
                        'prefix': list(prefix), 'checkpoint': canonical(backend.checkpoint()),
                        'parent_context': context.to_dict(), 'action_rng': generator.get_state().tolist(),
                        'value_shaped': float(output.value[0]), 'decision_index': len(prefix),
                        'ordinal_in_source': len(roots), 'floor': obs.run.floor,
                        'encounter': backend.raw_state['public_combat']['encounter'] if 'encounter' in backend.raw_state['public_combat'] else None,
                        'enemies': [e.monster_id for e in obs.enemies], 'hp': obs.player.current_hp,
                        'max_hp': obs.player.max_hp, 'gold': obs.run.gold, 'potions': [p.content_id for p in obs.potions],
                        'deck': [c.card_id for c in obs.deck], 'relics': [r.content_id for r in obs.relics]}
                    roots.append(sample)
            index = int(torch.multinomial(output.logits.softmax(-1), 1, generator=generator))
            action = decision.actions[index]
            tail.append({'screen': obs.screen.value, 'floor': obs.run.floor, 'action': action.to_dict(),
                         'selected_cards': obs.to_dict()['selected_cards']})
            tail = tail[-12:]
            key = f'{obs.run.act}:{obs.screen.value}'
            if checks[key] == 0 or len(prefix) % 47 == 0:
                before = canonical(backend.checkpoint())
                try:
                    transition = compare_next(backend, decision, action)
                except Exception as error:
                    dump(args.output/'failure.json', {'source': row, 'prefix': prefix,
                        'checkpoint': before, 'action': action.to_dict(), 'error': repr(error)})
                    raise
                checks[key] += 1
            else:
                transition = backend.step(action)
            if transition.truncated:
                raise RuntimeError('source collection backend truncated')
            reason = ('success' if transition.info.get('success') else 'death') if transition.terminated else limiter.observe(
                transition.decision, max_steps=4096, max_boundary_visits=4)
            terminal = reason is not None
            reward = curriculum_terminal_reward(transition.decision.observation, PROFILE,
                success=bool(transition.info.get('success')), failure_progress_scale=0.) if transition.terminated else (-1. if terminal else float(transition.reward))
            rewards.append(shape_curriculum_reward(reward, obs, transition.decision.observation,
                PROFILE, gamma=1., scale=.2, terminal=terminal))
            prefix.append(action.to_dict())
            context = following_context(output, action, transition)
            decision = transition.decision
            decisions += 1
            if terminal:
                break
        completed = reason not in (None, 'budget_censored')
        returns = complete_returns(rewards) if completed else None
        # Write roots only after collecting, never select them by these outcomes.
        for sample in roots:
            if sample['split'] != 'confirmation':
                sample['original_complete_return'] = float(returns[sample['decision_index']]) if returns is not None else None
            samples.append(sample)
        ledger.append({**row, 'reason': reason or 'budget_censored', 'steps': len(prefix), 'roots': len(roots),
                       'tail': tail if reason == 'cycle_limit' else [], 'complete_return_available': completed})
        print(json.dumps({'source': ordinal + 1, 'of': len(plan), 'roots': len(samples), 'decisions': decisions}), flush=True)
        if decisions >= args.max_decisions:
            break
    replay_steps = 0
    for sample in samples:
        backend, decision, rebuilt = rebuild_context(model, sample)
        replay_steps += len(sample['prefix'])
        cached = torch.tensor(sample['parent_context']['memory'])
        torch.testing.assert_close(rebuilt.memory, cached, atol=1e-6, rtol=1e-6)
        assert rebuilt.to_dict() == sample['parent_context']
        action = decision.actions[0]
        compare_next(backend, decision, action)
    # gzip mtime is fixed; immutable manifest binds every byte.
    blob = gzip.compress(json.dumps({'samples': samples}, ensure_ascii=False, allow_nan=False).encode(), mtime=0)
    (args.output/'samples.json.gz').write_bytes(blob)
    dump(args.output/'sources.json', ledger)
    measured = []
    for sample in samples:
        if sample['split'] == 'confirmation':
            continue
        outcomes = {}
        for mode in ('stochastic', 'greedy'):
            result = run_battle(model, sample, greedy=mode == 'greedy')
            result.pop('final_checkpoint')
            outcomes[mode] = result
        measured.append({'sample_id': sample['sample_id'], 'source_seed': sample['source_seed'],
                         'split': sample['split'], 'enemies': sample['enemies'], 'ordinal': sample['ordinal_in_source'],
                         'value_shaped': sample['value_shaped'], 'original_complete_return': sample.get('original_complete_return'),
                         **outcomes})
    dump(args.output/'baseline.json', measured)
    summary = {'schema': 'sls-act2-short-foundation-summary-v1', 'native_source_sha256': source,
        'planned_sources': len(plan), 'attempted_sources': len(ledger), 'source_decisions': decisions,
        'prefix_reconstruction_decisions': replay_steps, 'baseline_decisions': sum(r[m]['steps'] for r in measured for m in ('stochastic','greedy')),
        'independent_sources_with_roots': len({s['source_seed'] for s in samples}), 'roots': len(samples),
        'split_sources': dict(Counter(s['split'] for s in ledger if s['roots'])),
        'split_roots': dict(Counter(s['split'] for s in samples)),
        'enemy_distribution': dict(Counter('+'.join(s['enemies']) for s in samples)),
        'source_terminations': dict(Counter(s['reason'] for s in ledger)),
        'same_action_checks': dict(checks), 'all_root_memory_reconstructions_passed': True,
        'frozen_weights_unchanged': parent_model == state_digest(model.state_dict()),
        'original_parent_bytes_unchanged': sha256_file(parent) == expected,
        'confirmation_outcomes': 'NOT_EVALUATED', 'formal_training_ready': False,
        'elapsed_seconds': time.monotonic()-started,
        'files': {p.name: sha256_file(p) for p in args.output.iterdir() if p.is_file()}}
    dump(args.output/'summary.json', summary)
    print(json.dumps(summary, ensure_ascii=False), flush=True)
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
