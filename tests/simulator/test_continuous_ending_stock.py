"""Whole public trajectory from original Act4 entry through actual Heart victory."""
import json
from pathlib import Path

from sls.backends.simulator import SimulatorBackend
from sls.curriculum import IRONCLAD_A20_HEART
from tools.replay_ending_continuation import action_from_record, effective_rng

FIXTURE = json.loads(Path('tests/fixtures/regressions/ending-continuous-stock-r1.json').read_text())


def test_continuous_original_ending_decisions_and_fullrun_suffix_restoration():
    assert FIXTURE['training_eligible'] is False and FIXTURE['natural_trajectory'] is False
    backend = SimulatorBackend(profile=IRONCLAD_A20_HEART)
    decision = backend.load_checkpoint(FIXTURE['native_initial'])
    states = [backend.checkpoint()]
    for index, step in enumerate(FIXTURE['steps']):
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
        states.append(backend.checkpoint())
        decision = transition.decision
    assert len(states) == 20 and decision.terminal
    assert transition.info['success'] is True
    for start, state in enumerate(states):
        restored = SimulatorBackend(profile=IRONCLAD_A20_HEART)
        restored.load_checkpoint(state)
        assert restored.checkpoint() == state, start
        for offset, step in enumerate(FIXTURE['steps'][start:],start+1):
            restored.step(action_from_record(step['selected_action']),validation_evidence=step['validation_evidence'])
            assert restored.checkpoint() == states[offset], (start,offset)
