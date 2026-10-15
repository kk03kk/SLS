"""Public route overrides cannot select unavailable keys or disconnected nodes."""

from types import SimpleNamespace

from sls.contracts import Action, ActionKind
from tools.capture_fullrun_key_route import key_route_action


def _decision(actions, nodes=(), emerald=False):
    return SimpleNamespace(actions=tuple(actions), observation=SimpleNamespace(
        map_nodes=tuple(nodes), run=SimpleNamespace(has_emerald_key=emerald)))


def _node(name, children=(), burning=False):
    return SimpleNamespace(node_id=name, outgoing_node_ids=tuple(children),
                           visible_room_type="BURNING_ELITE" if burning else "MONSTER")


def test_keys_override_only_available_candidates():
    recall = Action(ActionKind.RECALL)
    sapphire = Action(ActionKind.TAKE_BLUE_KEY, reward_id="reward-key:sapphire")
    assert key_route_action(_decision([recall, sapphire])) == (sapphire, "PUBLIC_KEY_CHOICE")
    assert key_route_action(_decision([]))[0] is None


def test_route_uses_a_legal_first_edge_and_ignores_disconnected_burning_elite():
    left = Action(ActionKind.CHOOSE_MAP_NODE, node_id="left")
    right = Action(ActionKind.CHOOSE_MAP_NODE, node_id="right")
    nodes = [_node("left"), _node("right", ["middle"]), _node("middle", ["burning"]),
             _node("burning", burning=True), _node("disconnected", burning=True)]
    assert key_route_action(_decision([left, right], nodes))[0] == right
    assert key_route_action(_decision([left], nodes))[0] is None
    assert key_route_action(_decision([left, right], nodes, emerald=True))[0] is None


def test_cyclic_public_map_search_is_bounded():
    action = Action(ActionKind.CHOOSE_MAP_NODE, node_id="a")
    nodes = [_node("a", ["b"]), _node("b", ["a"]), _node("unreachable", burning=True)]
    assert key_route_action(_decision([action], nodes))[0] is None
