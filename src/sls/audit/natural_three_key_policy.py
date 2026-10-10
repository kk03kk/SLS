"""Joint public-map routing for three-key diagnostic trajectories."""
from sls.audit.natural_key_policy import choose_natural_key_action
from sls.contracts import ActionKind


def choose_natural_three_key_action(decision):
    observation = decision.observation
    actions = decision.actions
    maps = [a for a in actions if a.kind == ActionKind.CHOOSE_MAP_NODE]
    if maps:
        run = observation.run
        missing = (0 if run.has_ruby_key else 1) | (0 if run.has_sapphire_key else 2) | (0 if run.has_emerald_key else 4)
        nodes = {n.node_id:n for n in observation.map_nodes}
        key_rooms = {'REST':1,'TREASURE':2,'BURNING_ELITE':4}
        memo = {}

        def score(node_id, remaining, seen=frozenset()):
            if node_id not in nodes or node_id in seen:
                return (remaining.bit_count(),0,0,0)
            cache_key = (node_id,remaining,seen)
            if cache_key in memo:
                return memo[cache_key]
            node = nodes[node_id]
            remaining &= ~key_rooms.get(node.visible_room_type,0)
            children = [score(child,remaining,seen | {node_id}) for child in node.outgoing_node_ids]
            tail = min(children) if children else (remaining.bit_count(),0,0,0)
            result = (tail[0],tail[1]+int(node.visible_room_type == 'ELITE'),
                      tail[2]+int(node.visible_room_type in {'MONSTER','BURNING_ELITE'}),tail[3]+1)
            memo[cache_key] = result
            return result

        return min(maps,key=lambda a:(score(a.node_id,missing),a.candidate_id))
    recall = next((a for a in actions if a.kind == ActionKind.RECALL),None)
    if recall:
        return recall
    rest = next((a for a in actions if a.kind == ActionKind.REST),None)
    if rest:
        return rest
    potions = [a for a in actions if a.kind == ActionKind.USE_POTION]
    if potions and observation.enemies:
        return min(potions,key=lambda a:a.candidate_id)
    return choose_natural_key_action(decision)
