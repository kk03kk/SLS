"""Natural key probe decisions depend only on legal public candidates."""
from types import SimpleNamespace

from sls.audit.natural_key_policy import choose_natural_key_action
from sls.contracts import Action, ActionKind, MapNode


def test_recall_and_blue_key_prefer_real_legal_candidate():
    recall = Action(ActionKind.RECALL)
    blue = Action(ActionKind.TAKE_BLUE_KEY, reward_id='blue')
    assert choose_natural_key_action(SimpleNamespace(observation=None, actions=(blue, recall))) == recall


def test_path_follows_edges_to_key_room_not_unreachable_visible_node():
    nodes = (MapNode('left',0,0,'MONSTER',True,('rest',)),
             MapNode('right',1,0,'MONSTER',True,('treasure',)),
             MapNode('rest',0,1,'REST',False), MapNode('treasure',1,1,'TREASURE',False),
             MapNode('unreachable',6,0,'REST',False))
    actions = (Action(ActionKind.CHOOSE_MAP_NODE,node_id='left'),
               Action(ActionKind.CHOOSE_MAP_NODE,node_id='right'))
    run = SimpleNamespace(has_ruby_key=False,has_sapphire_key=False)
    observation = SimpleNamespace(map_nodes=nodes,run=run)
    decision = SimpleNamespace(observation=observation,actions=actions)
    assert choose_natural_key_action(decision) == actions[0]
    run.has_ruby_key = True
    assert choose_natural_key_action(decision) == actions[1]


def test_cyclic_public_map_does_not_loop():
    nodes = (MapNode('a',0,0,'MONSTER',True,('b',)),MapNode('b',0,1,'MONSTER',False,('a',)))
    action = Action(ActionKind.CHOOSE_MAP_NODE,node_id='a')
    observation = SimpleNamespace(map_nodes=nodes,run=SimpleNamespace(has_ruby_key=False,has_sapphire_key=False))
    assert choose_natural_key_action(SimpleNamespace(observation=observation,actions=(action,))) == action
