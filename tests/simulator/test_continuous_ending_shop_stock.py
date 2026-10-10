"""Stock transaction parity and explicit conditioned-history restore limits."""
import json
from pathlib import Path

import pytest

from sls.backends.simulator import SimulatorBackend
from sls.contracts import Action
from sls.curriculum import IRONCLAD_A20_HEART
from tools.replay_ending_continuation import action_from_record, effective_rng


def test_stock_continuous_shop_public_trajectory_and_eligible_suffixes():
    fixture = json.loads(Path('tests/fixtures/regressions/ending-continuous-shop-stock-r1.json').read_text())
    assert not fixture['training_eligible'] and not fixture['natural_trajectory']
    backend = SimulatorBackend(profile=IRONCLAD_A20_HEART)
    decision = backend.load_checkpoint(fixture['native_initial'])
    states = [backend.checkpoint()]
    for index, step in enumerate(fixture['steps']):
        assert decision.observation.to_dict() == step['observation'], index
        assert sorted(a.candidate_id for a in decision.actions) == sorted(
            action_from_record(a).candidate_id for a in step['legal_actions']), index
        assert effective_rng(backend.raw_state) == step['before_rng'], index
        transition = backend.step(action_from_record(step['selected_action']),
                                  validation_evidence=step['validation_evidence'])
        assert transition.decision.observation.to_dict() == step['next_observation'], index
        assert effective_rng(backend.raw_state) == step['after_rng'], index
        assert transition.reward == step['reward'], index
        assert transition.terminated == step['terminated'] and transition.truncated == step['truncated'], index
        assert transition.info['success'] == step['info']['success'], index
        assert transition.info['reason'] == step['info']['reason'], index
        states.append(backend.checkpoint())
        decision = transition.decision
    assert decision.terminal and transition.info['success']
    eligible = 0
    rejected = 0
    for start, state in enumerate(states):
        restored = SimulatorBackend(profile=IRONCLAD_A20_HEART)
        if not state['screen_info']['complete'] or state['replay_required']:
            # This fixture began at an artificial Act3 origin. Native callback
            # recovery requires actual reset-to-boundary history; do not rewrite it.
            with pytest.raises(ValueError, match='Run action is not legal'):
                restored.load_checkpoint(state)
            rejected += 1
            continue
        restored.load_checkpoint(state)
        assert restored.checkpoint() == state
        eligible += 1
        for offset, step in enumerate(fixture['steps'][start:],start+1):
            restored.step(action_from_record(step['selected_action']),validation_evidence=step['validation_evidence'])
            assert restored.checkpoint() == states[offset], (start,offset)
    assert (eligible, rejected) == (24, 2)


def test_natural_origin_purge_checkpoint_replays_real_history_and_removal():
    fixture = json.loads(Path('tests/fixtures/regressions/natural-shop-purge-native-r1.json').read_text())
    backend = SimulatorBackend(profile=IRONCLAD_A20_HEART)
    decision = backend.reset(fixture['seed'])
    for step in fixture['history']:
        assert decision.observation.to_dict() == step['observation']
        decision = backend.step(Action.from_dict(step['action'])).decision
    assert json.loads(json.dumps(backend.checkpoint())) == fixture['before']
    backend.step(Action.from_dict(fixture['purge']))
    state = backend.checkpoint()
    assert json.loads(json.dumps(state)) == fixture['checkpoint'] and not state['screen_info']['complete']
    restored = SimulatorBackend(profile=IRONCLAD_A20_HEART)
    restored.load_checkpoint(state)
    assert restored.checkpoint() == state
    remove = Action.from_dict(fixture['remove'])
    backend.step(remove)
    restored.step(remove)
    assert backend.checkpoint() == restored.checkpoint()
    assert json.loads(json.dumps(backend.checkpoint())) == fixture['after']
