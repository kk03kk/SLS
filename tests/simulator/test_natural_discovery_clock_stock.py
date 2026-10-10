"""Default clock misses a real stock RNG suffix; observed clock explains it."""
import json
from pathlib import Path

import pytest

from sls.backends.simulator import SimulatorBackend
from sls.contracts import Action
from sls.curriculum import IRONCLAD_A20_HEART
from tools.replay_ending_continuation import effective_rng

CASE = json.loads(Path('tests/fixtures/regressions/natural-discovery-clock-stock-r1.json').read_text())


@pytest.mark.parametrize('conditioned',[False,True],ids=['production-default','observed-stock-clock'])
def test_same_real_prefix_default_and_observed_clock_keep_separate_identities(conditioned):
    backend = SimulatorBackend(profile=IRONCLAD_A20_HEART)
    decision = backend.reset(CASE['seed'])
    assert not CASE['training_eligible']
    rng_mismatches = []
    states = []
    witnessed = []
    for step in CASE['steps']:
        assert decision.observation.to_dict() == step['observation'],step['boundary']
        assert sorted(a.candidate_id for a in decision.actions) == sorted(
            Action.from_dict(a).candidate_id for a in step['legal_actions']),step['boundary']
        if effective_rng(backend.raw_state) != step['before_rng']:
            rng_mismatches.append(step['boundary'])
        states.append(backend.checkpoint())
        if 'selected_action' in step:
            evidence = step['validation_evidence'] if conditioned else None
            if evidence:
                witnessed.append((step['boundary'],evidence))
            transition = backend.step(Action.from_dict(step['selected_action']),validation_evidence=evidence)
            assert transition.reward == step['reward']
            assert transition.terminated == step['terminated'] and transition.truncated == step['truncated']
            assert transition.info['reason'] == step['info']['reason']
            assert transition.info['success'] == step['info']['success']
            assert transition.decision.observation.to_dict() == step['next_observation']
            decision = transition.decision
    assert not decision.terminal  # This real prefix stops at a divergence, not a game loss.
    if conditioned:
        assert rng_mismatches == []
        assert witnessed == [(CASE['clock_action_boundary'],{'discovery_retrieval_updates':CASE['observed_updates']})]
    else:
        assert rng_mismatches == [CASE['first_default_rng_divergence']]
        assert effective_rng(backend.raw_state) == CASE['default_final_rng']
    # Restore states from each path's own actual history, without erasing clock evidence.
    for start,state in enumerate(states):
        restored = SimulatorBackend(profile=IRONCLAD_A20_HEART)
        restored.load_checkpoint(state)
        assert restored.checkpoint() == state
        for offset,step in enumerate(CASE['steps'][start:-1],start+1):
            restored.step(Action.from_dict(step['selected_action']),
                          validation_evidence=step['validation_evidence'] if conditioned else None)
            assert restored.checkpoint() == states[offset],(start,offset)
