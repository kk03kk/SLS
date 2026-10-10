"""Joint key-room routing uses actual public edges and held-key context."""
from types import SimpleNamespace

from sls.audit.natural_three_key_policy import choose_natural_three_key_action
from sls.contracts import Action, ActionKind, MapNode


def test_joint_route_does_not_choose_nearest_rest_on_incomplete_branch():
    nodes = (MapNode('short',0,0,'REST',True),
             MapNode('joint',1,0,'MONSTER',True,('rest',)),
             MapNode('rest',1,1,'REST',False,('treasure',)),
             MapNode('treasure',1,2,'TREASURE',False,('burning',)),
             MapNode('burning',1,3,'BURNING_ELITE',False))
    actions = tuple(Action(ActionKind.CHOOSE_MAP_NODE,node_id=n) for n in ['short','joint'])
    observation = SimpleNamespace(map_nodes=nodes,run=SimpleNamespace(has_ruby_key=False,has_sapphire_key=False,has_emerald_key=False))
    assert choose_natural_three_key_action(SimpleNamespace(observation=observation,actions=actions)) == actions[1]


def test_held_ruby_changes_required_future_rooms():
    nodes = (MapNode('a',0,0,'TREASURE',True,('burning',)),MapNode('burning',0,1,'BURNING_ELITE',False),
             MapNode('b',1,0,'REST',True,('far',)),MapNode('far',1,1,'TREASURE',False))
    actions = tuple(Action(ActionKind.CHOOSE_MAP_NODE,node_id=n) for n in ['a','b'])
    observation = SimpleNamespace(map_nodes=nodes,run=SimpleNamespace(has_ruby_key=True,has_sapphire_key=False,has_emerald_key=False))
    assert choose_natural_three_key_action(SimpleNamespace(observation=observation,actions=actions)) == actions[0]


def test_equal_key_coverage_avoids_additional_regular_elite():
    nodes = (MapNode('a',0,0,'ELITE',True,('rest',)),MapNode('b',1,0,'MONSTER',True,('rest',)),
             MapNode('rest',1,1,'REST',False))
    actions = tuple(Action(ActionKind.CHOOSE_MAP_NODE,node_id=n) for n in ['a','b'])
    observation = SimpleNamespace(map_nodes=nodes,run=SimpleNamespace(has_ruby_key=False,has_sapphire_key=True,has_emerald_key=True))
    assert choose_natural_three_key_action(SimpleNamespace(observation=observation,actions=actions)) == actions[1]
