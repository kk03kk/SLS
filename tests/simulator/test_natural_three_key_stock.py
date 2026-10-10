"""One actual original three-key prefix matches native without clock conditioning."""
import json
from pathlib import Path

from sls.backends.simulator import SimulatorBackend
from sls.contracts import Action, ActionKind
from sls.curriculum import IRONCLAD_A20_HEART
from tools.replay_ending_continuation import effective_rng

CASE = json.loads(Path('tests/fixtures/regressions/natural-three-key-stock-r1.json').read_text())


def test_normal_start_three_keys_default_clock_and_every_complete_suffix():
    backend = SimulatorBackend(profile=IRONCLAD_A20_HEART)
    decision = backend.reset(CASE['seed'])
    assert decision.observation.screen.value == 'NEOW' and decision.observation.run.floor == 0
    assert not CASE['training_eligible']
    assert not any(CASE['steps'][0]['observation']['run'][k] for k in ['has_ruby_key','has_sapphire_key','has_emerald_key'])
    states,transitions,clocks = [],[],[]
    for step in CASE['steps']:
        assert decision.observation.to_dict() == step['observation'],step['boundary']
        assert sorted(a.candidate_id for a in decision.actions) == sorted(
            Action.from_dict(a).candidate_id for a in step['legal_actions']),step['boundary']
        assert effective_rng(backend.raw_state) == step['before_rng'],step['boundary']
        states.append(backend.checkpoint())
        if 'selected_action' in step:
            if step['validation_evidence']:
                clocks.append((step['boundary'],step['validation_evidence']))
            action = Action.from_dict(step['selected_action'])
            transition = backend.step(action)  # Default production clock, no validation evidence.
            assert transition.reward == step['reward']
            assert transition.terminated == step['terminated'] and transition.truncated == step['truncated']
            assert transition.info['reason'] == step['info']['reason']
            assert transition.info['success'] == step['info']['success']
            assert transition.decision.observation.to_dict() == step['next_observation']
            if action.kind in {ActionKind.TAKE_BLUE_KEY,ActionKind.RECALL} or action.reward_id == 'reward-key:emerald':
                transitions.append((step['boundary'],action.kind))
            decision = transition.decision
    assert clocks == [(58,{'discovery_retrieval_updates':14})]
    assert transitions == [(47,ActionKind.TAKE_BLUE_KEY),(66,ActionKind.TAKE_REWARD),(73,ActionKind.RECALL)]
    assert decision.observation.run.has_ruby_key and decision.observation.run.has_sapphire_key and decision.observation.run.has_emerald_key
    assert not decision.terminal and decision.observation.run.act == 1 and decision.observation.run.floor == 7
    for start,state in enumerate(states):
        restored = SimulatorBackend(profile=IRONCLAD_A20_HEART)
        restored.load_checkpoint(state)
        assert restored.checkpoint() == state,start
        for offset,step in enumerate(CASE['steps'][start:-1],start+1):
            restored.step(Action.from_dict(step['selected_action']))
            assert restored.checkpoint() == states[offset],(start,offset)
