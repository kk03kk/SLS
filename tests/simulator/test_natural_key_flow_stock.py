"""Original normal-start public key history, with full natural-state suffixes."""
import json
from pathlib import Path

import pytest

from sls.backends.simulator import SimulatorBackend
from sls.contracts import Action
from sls.curriculum import IRONCLAD_A20_HEART
from tools.replay_ending_continuation import effective_rng

CASES = [case for name in ['natural-key-flow-stock-r1','natural-emerald-flow-stock-r1']
         for case in json.loads(Path('tests/fixtures/regressions/'+name+'.json').read_text())['cases']]


@pytest.mark.parametrize('case',CASES,
                         ids=lambda c:str(c['seed']))
def test_normal_start_stock_public_history_and_every_full_suffix(case):
    backend = SimulatorBackend(profile=IRONCLAD_A20_HEART)
    decision = backend.reset(case['seed'])
    states = []
    for step in case['steps']:
        assert decision.observation.to_dict() == step['observation'], step['boundary']
        assert sorted(a.candidate_id for a in decision.actions) == sorted(
            Action.from_dict(a).candidate_id for a in step['legal_actions']), step['boundary']
        assert effective_rng(backend.raw_state) == step['before_rng'], step['boundary']
        states.append(backend.checkpoint())
        if 'selected_action' in step:
            transition = backend.step(Action.from_dict(step['selected_action']),validation_evidence=step['validation_evidence'])
            assert transition.reward == step['reward']
            assert transition.terminated == step['terminated'] and transition.truncated == step['truncated']
            assert transition.info['reason'] == step['info']['reason']
            assert transition.info['success'] == step['info']['success']
            assert transition.decision.observation.to_dict() == step['next_observation']
            decision = transition.decision
    assert decision.terminal == (case['status'] == 'NATURAL_TERMINAL')
    if case['status'] == 'DIAGNOSTIC_LIMIT_UNFINISHED':
        assert len(states) == case['max_actions']+1
    for start, state in enumerate(states):
        restored = SimulatorBackend(profile=IRONCLAD_A20_HEART)
        restored.load_checkpoint(state)
        assert restored.checkpoint() == state, start
        for offset, step in enumerate(case['steps'][start:-1],start+1):
            restored.step(Action.from_dict(step['selected_action']),validation_evidence=step['validation_evidence'])
            assert restored.checkpoint() == states[offset], (start,offset)
