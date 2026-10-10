"""Actual interventions advance memory/context and never expose native state."""
import json
from types import SimpleNamespace

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
from sls.model.encoding import ACTION_TYPE_IDS
from tools.probe_frozen_three_key_routes import collect_probe, intervened_action


def observation(screen,floor,ruby=False):
    return Observation(Player('IRONCLAD',70,80,0,3,3),RunContext(20,1,floor,99,ruby,False,False),screen,
                       map_nodes=(MapNode('end',0,8,'MONSTER',True),))


def test_actual_recall_and_base_reward_feed_next_memory_input_without_private_state(tmp_path):
    neow = Action(ActionKind.CHOOSE_NEOW_OPTION,option_id='neow')
    rest = Action(ActionKind.REST)
    recall = Action(ActionKind.RECALL)
    move = Action(ActionKind.CHOOSE_MAP_NODE,node_id='end')
    decisions = [Decision(observation(ScreenType.NEOW,0),(neow,)),
                 Decision(observation(ScreenType.REST,6),(rest,recall)),
                 Decision(observation(ScreenType.MAP,6,True),(move,))]
    executed = []

    class Backend:
        def reset(self,seed):
            return decisions[0]

        def checkpoint(self):
            return dict(seed='PRIVATE_SENTINEL',rng='PRIVATE_SENTINEL')

        def step(self,action):
            executed.append(action)
            return Transition(decisions[len(executed)],0.25 if len(executed)==1 else 0.375,False,
                              info=dict(reason=None,success=False))

    calls = []

    def boundary(**kwargs):
        calls.append((int(kwargs['previous_action_types'][0]),float(kwargs['previous_rewards'][0]),
                      float(kwargs['memory'][0,0])))
        decision = kwargs['decision']
        assert 'PRIVATE_SENTINEL' not in json.dumps(decision.observation.to_dict())
        action = decision.actions[0]
        return dict(observation=decision.observation.to_dict(),chosen_action=action.to_dict()),action,kwargs['memory']+1

    artifact = SimpleNamespace(model=SimpleNamespace(initial_memory=lambda n,device:torch.zeros(n,2)))
    row = collect_probe(Backend(),artifact,1,tmp_path,max_actions=2,boundary_record=boundary)
    assert executed == [neow,recall]
    assert calls == [(0,0.0,0.0),(ACTION_TYPE_IDS[neow.kind.value]+1,0.25,1.0),
                     (ACTION_TYPE_IDS[recall.kind.value]+1,0.375,2.0)]
    assert row['status'] == 'DIAGNOSTIC_LIMIT_UNFINISHED'
    assert 'PRIVATE_SENTINEL' not in (tmp_path/'1.public.jsonl').read_text()
    assert 'PRIVATE_SENTINEL' in (tmp_path/'1.native.jsonl').read_text()


def test_emerald_reward_override_is_actual_legal_candidate():
    skip = Action(ActionKind.SKIP_REWARD)
    key = Action(ActionKind.TAKE_REWARD,reward_id='reward-key:emerald')
    decision = SimpleNamespace(actions=(skip,key))
    actual,reason = intervened_action(decision,skip)
    assert actual == key and reason == 'ACTUAL_LEGAL_KEY_ACQUISITION'
