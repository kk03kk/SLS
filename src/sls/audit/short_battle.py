"""History-conditioned CPU diagnostics, separate from production PPO rewards.

An environment checkpoint cannot initialize a recurrent policy. Rebuild its
memory with the *current* model over the recorded public history and actual
semantic actions. Cached parent memory is only an equivalence witness.
"""
from __future__ import annotations

import gzip
import json
from dataclasses import dataclass

import torch

from sls.backends.simulator import SimulatorBackend
from sls.contracts import Action
from sls.curriculum import IRONCLAD_A20_ACT2
from sls.model import PolicyBatch
from sls.model.encoding import ACTION_TYPE_IDS
from sls.rl.episode_limit import EpisodeLimitState
from sls.rl.training_contract import native_source_digest, sha256_file


def canonical(value):
    return json.loads(json.dumps(value, sort_keys=True, allow_nan=False))


@dataclass
class PolicyContext:
    memory: torch.Tensor
    previous_action: int = 0
    previous_reward: float = 0.
    episode_start: bool = True

    def to_dict(self):
        return {"memory": self.memory.detach().cpu().tolist(),
                "previous_action": self.previous_action, "previous_reward": self.previous_reward,
                "episode_start": self.episode_start}


@torch.no_grad()
def predict(model, decision, context):
    device = next(model.parameters()).device
    batch = PolicyBatch.from_decisions([decision], model.config).to(device)
    return model(*batch.model_inputs(), memory=context.memory.to(device),
                 previous_action_types=torch.tensor([context.previous_action], device=device),
                 previous_rewards=torch.tensor([context.previous_reward], device=device),
                 episode_start_mask=torch.tensor([context.episode_start], device=device))


def following_context(output, action, transition):
    return PolicyContext(output.next_memory.detach().cpu(), ACTION_TYPE_IDS[action.kind.value] + 1,
                         float(transition.reward), False)


def rebuild_context(model, sample):
    """Forced legal prefix, no teacher logits, gradients or hidden policy input."""
    if model.training:
        raise ValueError("history prefill must use the interaction/eval mode")
    if sample["native_source_sha256"] != native_source_digest():
        raise ValueError("short-battle source changed; replay and create a new dataset")
    backend = SimulatorBackend(IRONCLAD_A20_ACT2)
    decision = backend.reset(sample["source_seed"])
    context = PolicyContext(model.initial_memory(1).cpu())
    for serialized in sample["prefix"]:
        action = Action.from_dict(serialized)
        if decision.terminal or action not in decision.actions:
            raise ValueError("short-battle prefix contains an illegal semantic action")
        output = predict(model, decision, context)
        transition = backend.step(action)
        context = following_context(output, action, transition)
        decision = transition.decision
    if canonical(backend.checkpoint()) != canonical(sample["checkpoint"]):
        raise ValueError("short-battle history does not reproduce hidden environment state")
    return backend, decision, context


def compare_next(backend, decision, action):
    """Check full hidden state, public decision, reward and terminal flags."""
    restored = SimulatorBackend(backend.profile)
    restored_decision = restored.load_checkpoint(canonical(backend.checkpoint()))
    if decision != restored_decision:
        raise AssertionError("restored public decision or legal action set differs")
    expected = backend.step(action)
    actual = restored.step(action)
    if expected != actual or canonical(backend.checkpoint()) != canonical(restored.checkpoint()):
        raise AssertionError("same-action restore behavior differs")
    return expected


def run_battle(model, sample, *, greedy, max_steps=512):
    """New research task: current battle only, ±1 outcome, not production reward."""
    if next(model.parameters()).device.type != 'cpu':
        raise ValueError('pilot memory cache is CPU-only; rebuild in the target runtime')
    backend = SimulatorBackend(IRONCLAD_A20_ACT2)
    decision = backend.load_checkpoint(canonical(sample["checkpoint"]))
    stored = sample["parent_context"]
    context = PolicyContext(torch.tensor(stored["memory"]), stored["previous_action"],
                            stored["previous_reward"], stored["episode_start"])
    # The caller must verify this cache against the parent model before use.
    from sls.rl.value_adaptation import state_digest
    if state_digest(model.state_dict()) != sample["parent_model_sha256"]:
        raise ValueError("parent memory cache cannot initialize a different policy; rebuild history")
    generator = torch.Generator().set_state(torch.tensor(sample["action_rng"], dtype=torch.uint8))
    limiter = EpisodeLimitState.initial(decision)
    for step in range(max_steps):
        if decision.observation.screen.value != "COMBAT":
            raise ValueError("short-battle sample is not a combat decision")
        output = predict(model, decision, context)
        index = int(output.logits.argmax(-1)) if greedy else int(torch.multinomial(
            output.logits.softmax(-1).cpu(), 1, generator=generator))
        action = decision.actions[index]
        transition = backend.step(action)
        context = following_context(output, action, transition)
        decision = transition.decision
        if transition.truncated:
            raise RuntimeError("short-battle backend truncated")
        if transition.terminated or decision.observation.screen.value != "COMBAT":
            won = decision.observation.player.current_hp > 0 and not (
                transition.terminated and not transition.info.get("success"))
            return {"success": won, "reason": "battle_win" if won else "death", "steps": step + 1,
                    "hp": decision.observation.player.current_hp,
                    "potions": len(decision.observation.potions), "complete_return": 1. if won else -1.,
                    "final_checkpoint": backend.checkpoint()}
        reason = limiter.observe(decision, max_steps=max_steps, max_boundary_visits=4)
        if reason:
            return {"success": False, "reason": reason, "steps": step + 1, "complete_return": -1.,
                    "final_checkpoint": backend.checkpoint()}
    raise AssertionError("short-battle limiter failed")


def load_corpus(directory):
    """Validate immutable bytes and source/run separation before diagnostics."""
    summary = json.loads((directory/'summary.json').read_text())
    if summary['native_source_sha256'] != native_source_digest():
        raise ValueError('short-battle corpus source changed')
    for name, expected in summary['files'].items():
        if (directory/name).parent != directory or sha256_file(directory/name) != expected:
            raise ValueError('short-battle corpus file identity mismatch')
    samples = json.loads(gzip.decompress((directory/'samples.json.gz').read_bytes()))['samples']
    ids, origins = set(), {}
    for row in samples:
        if row['sample_id'] in ids:
            raise ValueError('duplicate short-battle sample')
        ids.add(row['sample_id'])
        source = row['source_seed']
        split = row['split']
        if source in origins and origins[source] != split:
            raise ValueError('source run leaked across splits')
        origins[source] = split
        if row['native_source_sha256'] != summary['native_source_sha256']:
            raise ValueError('mixed simulator sources')
        if (split == 'fit' and not 0 <= source < 2_000_000_000_000) or (
                split in ('development','confirmation') and not 8_000_000_000_000 <= source < 9_000_000_000_000):
            raise ValueError('short-battle source namespace mismatch')
        if split not in ('fit','development','confirmation'):
            raise ValueError('unknown corpus split')
    baseline = json.loads((directory/'baseline.json').read_text())
    allowed = {s['sample_id'] for s in samples if s['split'] != 'confirmation'}
    if any(row['sample_id'] not in allowed for row in baseline):
        raise ValueError('confirmation outcomes entered development baseline')
    baseline_ids = [row['sample_id'] for row in baseline]
    if len(set(baseline_ids)) != len(baseline_ids) or set(baseline_ids) != allowed:
        raise ValueError('missing or duplicate development baseline')
    if len(samples) != summary['roots'] or len(origins) != summary['independent_sources_with_roots']:
        raise ValueError('corpus source/sample counts disagree')
    return summary, samples


def verify_battle_restore(model, sample, *, greedy, max_steps=512):
    """Continuous legal-prefix instance versus restored instance, whole battle."""
    live, decision, context = rebuild_context(model, sample)
    restored = SimulatorBackend(IRONCLAD_A20_ACT2)
    assert restored.load_checkpoint(canonical(sample['checkpoint'])) == decision
    cached = sample['parent_context']
    torch.testing.assert_close(context.memory, torch.tensor(cached['memory']), atol=1e-6, rtol=1e-6)
    generator = torch.Generator().set_state(torch.tensor(sample['action_rng'],dtype=torch.uint8))
    limiter = EpisodeLimitState.initial(decision)
    for step in range(max_steps):
        output = predict(model, decision, context)
        index = int(output.logits.argmax(-1)) if greedy else int(torch.multinomial(
            output.logits.softmax(-1).cpu(),1,generator=generator))
        action = decision.actions[index]
        expected = live.step(action)
        actual = restored.step(action)
        if expected != actual or canonical(live.checkpoint()) != canonical(restored.checkpoint()):
            raise AssertionError('battle continuation differs after restore')
        context = following_context(output,action,expected)
        decision = expected.decision
        if expected.terminated or decision.observation.screen.value != 'COMBAT':
            return {'steps':step+1,'complete':True,'success':decision.observation.player.current_hp>0}
        if limiter.observe(decision,max_steps=max_steps,max_boundary_visits=4):
            return {'steps':step+1,'complete':False,'success':False}
    raise AssertionError('battle verification cap failed')
