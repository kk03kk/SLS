"""Deterministic public-only actions for bounded natural key-flow probes."""
from sls.contracts import ActionKind


def choose_natural_key_action(decision):
    """Seek a reachable key room without inspecting native state or RNG."""
    observation = decision.observation
    actions = decision.actions
    if not actions:
        raise ValueError('terminal decision has no probe action')
    for kind in (ActionKind.RECALL, ActionKind.TAKE_BLUE_KEY):
        candidates = [a for a in actions if a.kind == kind]
        if candidates:
            return min(candidates, key=lambda a: a.candidate_id)
    maps = [a for a in actions if a.kind == ActionKind.CHOOSE_MAP_NODE]
    if maps:
        nodes = {n.node_id: n for n in observation.map_nodes}
        target = ('REST' if not observation.run.has_ruby_key else
                  'TREASURE' if not observation.run.has_sapphire_key else 'BURNING_ELITE')

        def distance(node_id, visited=frozenset()):
            if node_id not in nodes or node_id in visited:
                return 1000
            node = nodes[node_id]
            if node.visible_room_type == target:
                return 0
            return 1 + min((distance(child, visited | {node_id}) for child in node.outgoing_node_ids), default=1000)

        return min(maps, key=lambda a: (distance(a.node_id), a.candidate_id))
    plays = [a for a in actions if a.kind == ActionKind.PLAY_CARD]
    if plays:
        cards = {c.instance_id: c for c in observation.hand}
        enemies = {e.instance_id: e for e in observation.enemies}
        order = {'BASH': 0, 'STRIKE': 1, 'STRIKE_RED': 1, 'DEFEND': 2, 'DEFEND_RED': 2}
        return min(plays, key=lambda a: (order.get(cards[a.subject_id].card_id, 3),
                                        enemies[a.target_id].current_hp if a.target_id in enemies else 10000,
                                        a.candidate_id))
    priority = ['REST', 'TAKE_REWARD', 'SKIP_CARD_REWARD', 'SKIP_REWARD', 'END_TURN',
                'LEAVE_SHOP', 'OPEN_CHEST', 'PROCEED', 'CONFIRM', 'CHOOSE_NEOW_OPTION',
                'CHOOSE_EVENT_OPTION', 'SELECT_CARD', 'CHOOSE_BOSS_RELIC']
    return min(actions, key=lambda a: (priority.index(a.kind.value) if a.kind.value in priority else 100,
                                       a.candidate_id))
