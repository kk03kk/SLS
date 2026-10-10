"""Counterfactual diagnostics preserve actual history and episode semantics."""
import json
import subprocess
import sys
from types import SimpleNamespace

import pytest
import torch

from sls.contracts import (
    Action,
    ActionKind,
    Decision,
    MapNode,
    Observation,
    Player,
    RunContext,
    ScreenType,
    Transition,
)
from sls.curriculum import IRONCLAD_A20_HEART
from sls.model.encoding import ACTION_TYPE_IDS
from tools import continue_stock_key_prefix as diagnostic


def decision(floor):
    observation = Observation(Player('IRONCLAD', 70, 80, 0, 3, 3), RunContext(20, 1, floor, 99, False, False, False),
                              ScreenType.MAP, map_nodes=(MapNode('end', 0, 8, 'MONSTER', True),))
    return Decision(observation, (Action(ActionKind.CHOOSE_MAP_NODE, node_id='end'),))


def ppo(**overrides):
    return dict(max_episode_steps=4096, max_boundary_visits=4, gamma=1.0, potential_shaping=False,
                potential_scale=0.2, failure_progress_scale=0.0, limit_failure_reward=-1.0) | overrides


def boundary(**kwargs):
    current = kwargs['decision']
    return dict(screen=current.observation.screen.value, observation=current.observation.to_dict(),
                value=0.25, policy_input_sha256='public-only'), current.actions[0], kwargs['memory'] + 1


def test_teacher_forcing_uses_executed_action_and_base_reward(monkeypatch):
    first, second = decision(1), decision(2)
    executed = Action(ActionKind.RECALL)
    first = Decision(first.observation, first.actions + (executed,))
    seen = []

    class Backend:
        def reset(self, seed):
            return first

        def step(self, action):
            assert action == executed
            return Transition(second, 0.375, False, info=dict(success=False))

    def record(**kwargs):
        seen.append((int(kwargs['previous_action_types'][0]), float(kwargs['previous_rewards'][0])))
        return boundary(**kwargs)

    monkeypatch.setattr(diagnostic, '_boundary_record', record)
    artifact = SimpleNamespace(model=SimpleNamespace(initial_memory=lambda *args: torch.zeros(1, 2)))
    public = dict(seed=99, boundaries=[
        dict(step_index=0, observation=first.observation.to_dict(),
             ordered_candidate_actions=[a.to_dict() for a in first.actions], executed_action=executed.to_dict()),
        dict(step_index=1, observation=second.observation.to_dict(),
             ordered_candidate_actions=[a.to_dict() for a in second.actions], reward_from_previous=0.375)])
    current, memory, previous_actions, previous_rewards, limits, records = diagnostic.rebuild_prefix(
        Backend(), artifact, public, ppo())
    assert current == second and memory.tolist() == [[1.0, 1.0]]
    assert seen == [(0, 0.0)]
    assert int(previous_actions[0]) == ACTION_TYPE_IDS[executed.kind.value] + 1
    assert float(previous_rewards[0]) == 0.375 and limits.steps == 1
    assert records[0]['executed_action'] == executed.to_dict()
    public['boundaries'][-1]['ordered_candidate_actions'] = []
    with pytest.raises(ValueError, match='initial public state/order mismatch'):
        diagnostic.rebuild_prefix(Backend(), artifact, public, ppo())


@pytest.mark.parametrize('cycle,expected,complete', [
    (False, 'DIAGNOSTIC_LIMIT_UNFINISHED', False), (True, 'POLICY_CYCLE_LIMIT', True)])
def test_budget_is_unfinished_but_policy_cycle_is_learning_episode(monkeypatch, tmp_path, cycle, expected, complete):
    current = decision(1)

    class Backend:
        def checkpoint(self):
            return dict(private='PRIVATE_SENTINEL')

        def step(self, action):
            return Transition(current if cycle else decision(2), 0.125, False,
                              info=dict(reason=None, success=False))

    limits = diagnostic.EpisodeLimitState.initial(current)
    monkeypatch.setattr(diagnostic, 'SimulatorBackend', lambda **kwargs: Backend())
    monkeypatch.setattr(diagnostic, 'rebuild_prefix', lambda *args: (
        current, torch.zeros(1, 2), torch.zeros(1, dtype=torch.long), torch.zeros(1), limits, []))
    monkeypatch.setattr(diagnostic, '_boundary_record', boundary)
    monkeypatch.setattr(diagnostic, 'initial_distribution', lambda *args: [])
    spec = dict(artifact=object(), ppo=ppo(max_boundary_visits=1), trained_profile=IRONCLAD_A20_HEART)
    result = diagnostic.continue_model(spec, {}, dict(private='PRIVATE_SENTINEL'), tmp_path/'run',
                                       IRONCLAD_A20_HEART, 1)
    assert result['status'] == expected and result['training_objective_episode_complete'] is complete
    assert result['realized_complete_shaped_return'] == (-1.0 if cycle else None)
    assert not result['actual_backend_terminal'] and not result['diagnostic_truncation_is_game_failure']
    public = (tmp_path/'run/continuation.public.jsonl').read_text()
    assert 'PRIVATE_SENTINEL' not in public
    assert 'PRIVATE_SENTINEL' in (tmp_path/'run/continuation.native.jsonl').read_text()
    assert json.loads(public.splitlines()[0])['shaped_reward_float32'] == (-1.0 if cycle else 0.125)
    if cycle:
        assert 'selected_cards' in result['cycle_tail'][0]


def test_terminal_override_shaping_and_float32_match_learning_target(monkeypatch):
    current = decision(1)
    transition = Transition(Decision(current.observation, (), terminal=True), 7.0, True, info=dict(success=True))
    calls = []
    monkeypatch.setattr(diagnostic, 'curriculum_terminal_reward', lambda *args, **kwargs: 0.9)

    def shape(reward, before, after, profile, **kwargs):
        calls.append((reward, kwargs))
        return reward + 0.123456789

    monkeypatch.setattr(diagnostic, 'shape_curriculum_reward', shape)
    result = diagnostic.learning_reward(current, transition, IRONCLAD_A20_HEART,
                                        ppo(potential_shaping=True), 'cycle_limit')
    assert result == float(torch.tensor(1.023456789, dtype=torch.float32))
    assert calls == [(0.9, dict(gamma=1.0, scale=0.2, terminal=True))]


def test_cli_rejects_gpu():
    result = subprocess.run([sys.executable, str(diagnostic.ROOT/'tools/continue_stock_key_prefix.py'),
                             '--models', 'unused', '--capture', 'unused', '--reference-report', 'unused',
                             '--oracle', 'unused', '--output-dir', 'unused', '--device', 'cuda'],
                            capture_output=True, text=True)
    assert result.returncode == 2 and 'invalid choice' in result.stderr
