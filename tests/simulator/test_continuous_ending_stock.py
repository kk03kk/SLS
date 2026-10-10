"""Whole public trajectory from original Act4 entry through actual Heart victory."""
import json
from pathlib import Path

import pytest

from sls.backends.simulator import SimulatorBackend
from sls.curriculum import IRONCLAD_A20_HEART
from tools.replay_ending_continuation import action_from_record, effective_rng

FIXTURES = [json.loads(Path('tests/fixtures/regressions/'+name+'.json').read_text()) for name in
            ('ending-continuous-stock-r1','ending-continuous-death-stock-r1')]


@pytest.mark.parametrize('fixture',FIXTURES,ids=lambda f:str(f['seed']))
def test_continuous_original_ending_decisions_and_fullrun_suffix_restoration(fixture):
    assert fixture['training_eligible'] is False and fixture['natural_trajectory'] is False
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
        assert transition.terminated == step['terminated'] and transition.truncated == step['truncated'], index
        assert transition.info['success'] == step['info']['success'], index
        assert transition.info['reason'] == step['info']['reason'], index
        assert transition.reward == step['reward'], index
        states.append(backend.checkpoint())
        decision = transition.decision
    assert len(states) == len(fixture['steps'])+1 and decision.terminal
    assert transition.info['success'] == fixture['steps'][-1]['info']['success']
    for start, state in enumerate(states):
        restored = SimulatorBackend(profile=IRONCLAD_A20_HEART)
        restored.load_checkpoint(state)
        assert restored.checkpoint() == state, start
        for offset, step in enumerate(fixture['steps'][start:],start+1):
            restored.step(action_from_record(step['selected_action']),validation_evidence=step['validation_evidence'])
            assert restored.checkpoint() == states[offset], (start,offset)
