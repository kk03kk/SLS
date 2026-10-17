"""Natural legal prefix: equality now must imply equality after the next action."""
import copy
import json
from pathlib import Path

import pytest

from sls.backends.simulator import SimulatorBackend, native
from sls.curriculum import IRONCLAD_A20_ACT2

FIXTURE = json.loads(Path('tests/fixtures/regressions/nested-shop-callback-prefix.json').read_text())


def canonical(value):
    return json.loads(json.dumps(value, sort_keys=True))


def prefix(length=None):
    run = native.LightspeedRunState()
    run.reset(FIXTURE['seed'], FIXTURE['ascension'], FIXTURE['math_seed'])
    for action in FIXTURE['actions'][:length]:
        run.step(action)
    return run


@pytest.mark.parametrize('legacy_label', [False, True])
def test_nested_reward_restores_callback_and_hidden_state_for_every_action(legacy_label):
    state = prefix().snapshot()
    assert state['replay_required'] and not state['screen_info']['complete']
    if legacy_label:
        state = copy.deepcopy(state)
        state['replay_required'] = False
        state['screen_info']['complete'] = True
        state['progress_state']['screen_continuation_serialized'] = True
    for candidate in prefix().legal_actions():
        live, restored = prefix(), native.LightspeedRunState()
        restored.load_state(canonical(state))
        assert canonical(live.snapshot()) == canonical(restored.snapshot())
        assert canonical(live.step(candidate['bits'])) == canonical(restored.step(candidate['bits']))
    live, restored = prefix(), native.LightspeedRunState()
    restored.load_state(canonical(state))
    for run in (live, restored):
        run.step(805306368)  # Leave reward -> original shop, then shop -> map.
        assert run.snapshot()['public_run']['screen_state'] == 8
        run.step(805306368)
        assert run.snapshot()['public_run']['screen_state'] == 5
    assert canonical(live.snapshot()) == canonical(restored.snapshot())


def test_backend_transition_reward_terminal_and_candidates_equal_after_restore():
    live = SimulatorBackend(IRONCLAD_A20_ACT2)
    live._native = prefix()
    decision = live._adapt(live._native.snapshot())
    restored = SimulatorBackend(IRONCLAD_A20_ACT2)
    restored.load_checkpoint(canonical(live.checkpoint()))
    for _ in range(2):
        action = next(a for a in decision.actions if live._candidate_bits[a.candidate_id] == 805306368)
        expected, actual = live.step(action), restored.step(action)
        assert expected == actual
        assert canonical(live.checkpoint()) == canonical(restored.checkpoint())
        decision = expected.decision


def test_nested_reward_without_history_is_rejected_not_guessed():
    state = prefix().snapshot()
    del state['replay_actions']
    with pytest.raises(ValueError, match='replay history'):
        native.LightspeedRunState().load_state(state)


def test_nested_restore_rejects_resource_tampering():
    state = prefix().snapshot()
    state['player_state']['gold'] += 1
    with pytest.raises(ValueError, match='does not reproduce'):
        native.LightspeedRunState().load_state(state)


def test_value_collector_crosses_nested_reward_with_per_step_resume(tmp_path, monkeypatch):
    """Exercise the actual failing caller, with a small scripted legal policy."""
    from dataclasses import replace

    import torch

    from sls.model import ModelConfig, Policy
    from sls.rl import PPOConfig
    from sls.rl.value_adaptation import ValueAdaptationConfig, ValueAdapter

    class HistoricalSeedBackend(SimulatorBackend):
        def reset(self, seed):
            super().reset(seed)
            self._native.reset(seed,20,FIXTURE['math_seed'])
            return self._adapt(self._native.snapshot())

    policy = Policy(ModelConfig(embedding_dim=32, transformer_layers=1, attention_heads=4,
        feedforward_dim=64, recurrent_hidden_dim=32, architecture='sls-recurrent-relational-policy-v6'))
    adapter = ValueAdapter(policy, IRONCLAD_A20_ACT2,
        PPOConfig(max_episode_steps=24), ValueAdaptationConfig(episodes=2,fit_episodes=1,
        seed_start=FIXTURE['seed'],max_decisions=48),tmp_path,
        backend=HistoricalSeedBackend(IRONCLAD_A20_ACT2))
    scripted = [*FIXTURE['actions'],805306368,805306368]
    forward = adapter.model.forward

    def force_legal_action(*args, **kwargs):
        result = forward(*args,**kwargs)
        bits = scripted[adapter.state['decisions']]
        # Candidate order is the policy order, not the native integer encoding.
        decision = adapter.backend._adapt(adapter.backend._native.snapshot())
        index = next(i for i,action in enumerate(decision.actions)
                     if adapter.backend._candidate_bits[action.candidate_id]==bits)
        logits = torch.full_like(result.logits,-torch.inf)
        logits[0,index] = 0
        return replace(result,logits=logits)

    monkeypatch.setattr(adapter.model,'forward',force_legal_action)
    adapter.advance(max_decisions=22)
    assert adapter.state['active']['backend']['screen_info']['complete'] is False
    # A new collector loads the actual journal and reconstructs the callback.
    resumed = ValueAdapter(policy,IRONCLAD_A20_ACT2,PPOConfig(max_episode_steps=24),
                           adapter.config,tmp_path,backend=HistoricalSeedBackend(IRONCLAD_A20_ACT2))
    adapter = resumed
    forward = adapter.model.forward
    monkeypatch.setattr(adapter.model,'forward',force_legal_action)
    adapter.advance(max_decisions=2)
    assert adapter.state['decisions']==24
    assert len(adapter.state['episodes'])==1
    assert adapter.backend._native.snapshot()['public_run']['screen_state']==5
