"""Emerald-first probes select only visible legal routes and resources."""
from types import SimpleNamespace

from sls.audit.natural_emerald_policy import choose_natural_emerald_action
from sls.contracts import Action, ActionKind, MapNode


def test_burning_elite_is_selected_through_real_public_edges():
    left = Action(ActionKind.CHOOSE_MAP_NODE,node_id='left')
    right = Action(ActionKind.CHOOSE_MAP_NODE,node_id='right')
    nodes = (MapNode('left',0,0,'MONSTER',True,('rest',)),
             MapNode('right',1,0,'MONSTER',True,('burning',)),
             MapNode('rest',0,1,'REST',False),MapNode('burning',1,1,'BURNING_ELITE',False),
             MapNode('disconnected',6,0,'BURNING_ELITE',False))
    decision = SimpleNamespace(observation=SimpleNamespace(map_nodes=nodes),actions=(left,right))
    assert choose_natural_emerald_action(decision) == right


def test_emerald_probe_rests_instead_of_spending_rest_on_ruby():
    recall = Action(ActionKind.RECALL)
    rest = Action(ActionKind.REST)
    decision = SimpleNamespace(observation=SimpleNamespace(),actions=(recall,rest))
    assert choose_natural_emerald_action(decision) == rest


def test_only_legal_potion_is_used_without_inspecting_private_inventory():
    potion = Action(ActionKind.USE_POTION,subject_id='POTION:1',target_id='MONSTER:0')
    end = Action(ActionKind.END_TURN)
    decision = SimpleNamespace(observation=SimpleNamespace(enemies=(object(),)),actions=(end,potion))
    assert choose_natural_emerald_action(decision) == potion
