"""Public-only Emerald-first diagnostic route, separate from red/blue probes."""
from sls.audit.natural_key_policy import choose_natural_key_action
from sls.contracts import ActionKind


def choose_natural_emerald_action(decision):
    observation = decision.observation
    actions = decision.actions
    maps = [a for a in actions if a.kind == ActionKind.CHOOSE_MAP_NODE]
    if maps:
        nodes = {n.node_id: n for n in observation.map_nodes}

        def distance(node_id, seen=frozenset()):
            if node_id not in nodes or node_id in seen:
                return 1000
            node = nodes[node_id]
            if node.visible_room_type == 'BURNING_ELITE':
                return 0
            return 1 + min((distance(child,seen | {node_id}) for child in node.outgoing_node_ids),default=1000)

        return min(maps,key=lambda a:(distance(a.node_id),a.candidate_id))
    rest = next((a for a in actions if a.kind == ActionKind.REST),None)
    if rest:
        return rest
    potions = [a for a in actions if a.kind == ActionKind.USE_POTION]
    if potions and observation.enemies:
        return min(potions,key=lambda a:a.candidate_id)
    return choose_natural_key_action(decision)
