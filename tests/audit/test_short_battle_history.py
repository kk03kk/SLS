"""Memory belongs to policy plus public history, not the simulator checkpoint."""
import copy

import pytest
import torch

from sls.audit.short_battle import (
    PolicyContext,
    canonical,
    compare_next,
    following_context,
    predict,
    rebuild_context,
)
from sls.backends.simulator import SimulatorBackend
from sls.curriculum import IRONCLAD_A20_ACT2
from sls.model import ModelConfig, Policy
from sls.rl.training_contract import native_source_digest
from tools.prepare_act2_short_battles import source_plan


def model():
    torch.manual_seed(417)
    return Policy(ModelConfig(embedding_dim=32, transformer_layers=1, attention_heads=4,
        feedforward_dim=64, recurrent_hidden_dim=32, architecture='sls-recurrent-relational-policy-v6')).eval()


def sample(policy):
    backend = SimulatorBackend(IRONCLAD_A20_ACT2)
    decision = backend.reset(1500000000000)
    context = PolicyContext(policy.initial_memory(1))
    actions = []
    for _ in range(5):
        output = predict(policy, decision, context)
        action = decision.actions[0]
        transition = backend.step(action)
        actions.append(action.to_dict())
        context = following_context(output, action, transition)
        decision = transition.decision
    return {'source_seed':1500000000000, 'native_source_sha256':native_source_digest(),
            'prefix':actions, 'checkpoint':canonical(backend.checkpoint())}, context


def test_current_model_memory_is_rebuilt_from_actual_actions():
    policy = model()
    record, expected = sample(policy)
    rng = torch.get_rng_state().clone()
    _, _, actual = rebuild_context(policy, record)
    assert actual.to_dict() == expected.to_dict()
    assert torch.equal(rng,torch.get_rng_state())
    changed = copy.deepcopy(policy)
    with torch.no_grad():
        changed.memory.bias_ih.add_(.25)
    _, _, new_context = rebuild_context(changed, record)
    assert not torch.equal(actual.memory, new_context.memory)
    assert new_context.previous_action == actual.previous_action
    assert new_context.previous_reward == actual.previous_reward


def test_history_reconstruction_rejects_wrong_source_and_hidden_state():
    policy = model()
    record, _ = sample(policy)
    wrong = copy.deepcopy(record)
    wrong['native_source_sha256'] = 'stale'
    with pytest.raises(ValueError, match='source changed'):
        rebuild_context(policy, wrong)
    wrong = copy.deepcopy(record)
    wrong['checkpoint']['player_state']['gold'] += 1
    with pytest.raises(ValueError, match='hidden environment state'):
        rebuild_context(policy, wrong)


def test_restore_probe_checks_following_reward_and_terminal_flags(monkeypatch):
    backend = SimulatorBackend(IRONCLAD_A20_ACT2)
    decision = backend.reset(1500000000000)
    compare_next(backend, decision, decision.actions[0])
    original = SimulatorBackend.step
    from dataclasses import replace
    def corrupt(self, action):
        result = original(self, action)
        return result if self is backend else replace(result, reward=result.reward + 1)
    monkeypatch.setattr(SimulatorBackend, 'step', corrupt)
    decision = backend._adapt(backend._native.snapshot())
    with pytest.raises(AssertionError, match='same-action'):
        compare_next(backend, decision, decision.actions[0])


def test_sources_are_disjoint_and_roles_assigned_before_outcomes():
    rows = source_plan(32)
    assert len({r['source_seed'] for r in rows}) == 32
    for row in rows:
        assert (row['source_seed'] < 2_000_000_000_000) == (row['split'] == 'fit')
    assert len([r for r in rows if r['split']=='confirmation']) == 8
