from __future__ import annotations

from collections import deque

import pytest

from sls.contracts import Action, ActionKind, Decision, ScreenType


def _toward_burning_elite(decision: Decision) -> Action:
    nodes = {node.node_id: node for node in decision.observation.map_nodes}
    target = next((
        node.node_id for node in nodes.values()
        if node.visible_room_type == "BURNING_ELITE"
    ), None)
    candidates = {
        action.node_id: action for action in decision.actions
        if action.kind is ActionKind.CHOOSE_MAP_NODE
    }
    if target is not None:
        queue = deque((node_id, node_id) for node_id in candidates)
        visited: set[str] = set()
        while queue:
            node_id, first = queue.popleft()
            if node_id in visited:
                continue
            visited.add(node_id)
            if node_id == target:
                return candidates[first]
            if node_id in nodes:
                queue.extend((child, first) for child in nodes[node_id].outgoing_node_ids)
    return next(iter(candidates.values()))


def _structural_action(decision: Decision) -> Action:
    for predicate in (
        lambda action: action.kind is ActionKind.TAKE_REWARD
        and action.reward_id == "reward-key:emerald",
        lambda action: action.kind is ActionKind.TAKE_BLUE_KEY,
        lambda action: action.kind is ActionKind.RECALL,
    ):
        selected = next((action for action in decision.actions if predicate(action)), None)
        if selected is not None:
            return selected
    if any(action.kind is ActionKind.CHOOSE_MAP_NODE for action in decision.actions):
        return _toward_burning_elite(decision)
    preferences = (
        ActionKind.SKIP_REWARD, ActionKind.OPEN_CHEST, ActionKind.PROCEED,
        ActionKind.LEAVE_SHOP, ActionKind.CHOOSE_BOSS_RELIC, ActionKind.REST,
        ActionKind.CONFIRM, ActionKind.SKIP_CARD_REWARD,
    ) if decision.observation.screen is ScreenType.COMBAT_REWARD else (
        ActionKind.OPEN_CHEST, ActionKind.SKIP_CARD_REWARD, ActionKind.SKIP_REWARD,
        ActionKind.PROCEED, ActionKind.LEAVE_SHOP, ActionKind.CHOOSE_BOSS_RELIC,
        ActionKind.REST, ActionKind.CONFIRM,
    )
    for kind in preferences:
        selected = next((action for action in decision.actions if action.kind is kind), None)
        if selected is not None:
            return selected
    return decision.actions[0]


def test_act_two_horizon_ends_at_boss_defeat_before_reward_choices() -> None:
    pytest.importorskip("sls.backends.simulator.native", exc_type=ImportError)
    from sls.backends.simulator import SimulatorBackend
    from sls.curriculum import IRONCLAD_A0_ACT2

    backend = SimulatorBackend(IRONCLAD_A0_ACT2)
    decision = backend.reset(0)
    backend._native._set_skip_battles_for_testing(True)
    last_transition = None
    for _ in range(160):
        if decision.terminal:
            break
        last_transition = backend.step(_structural_action(decision))
        decision = last_transition.decision
    else:
        pytest.fail("Act 2 structural route did not terminate")

    assert last_transition is not None
    assert last_transition.info == {
        "reason": "ACT_2_CLEARED",
        "success": True,
        "terminal_outcome": None,
    }
    assert decision.observation.run.act == 2
    assert decision.observation.screen is ScreenType.COMBAT_REWARD
    assert decision.actions == ()


@pytest.mark.parametrize("ascension", (0, 20))
def test_policy_visible_route_structurally_reaches_and_defeats_heart(ascension: int) -> None:
    pytest.importorskip("sls.backends.simulator.native", exc_type=ImportError)
    from sls.backends.simulator import SimulatorBackend
    from sls.curriculum import ironclad_fullrun_profile

    backend = SimulatorBackend(ironclad_fullrun_profile(ascension, require_heart=True))
    decision = backend.reset(0)
    # Skip combat resolution only.  Every route choice below is selected from
    # canonical, policy-visible actions and observations.
    backend._native._set_skip_battles_for_testing(True)
    acts: set[int] = set()
    last_transition = None
    for _ in range(200):
        acts.add(decision.observation.run.act)
        if decision.terminal:
            break
        last_transition = backend.step(_structural_action(decision))
        decision = last_transition.decision
    else:
        pytest.fail("canonical FullRun structural route did not terminate")

    assert last_transition is not None
    assert last_transition.info == {
        "reason": "GAME_VICTORY",
        "success": True,
        "terminal_outcome": "PLAYER_VICTORY",
    }
    assert acts == {1, 2, 3, 4}
    assert decision.observation.run.has_ruby_key
    assert decision.observation.run.has_emerald_key
    assert decision.observation.run.has_sapphire_key
    assert decision.observation.run.visible_boss_id == "THE_HEART"
    second_boss = int(backend.raw_state["run_state"]["second_boss"])
    assert (second_boss != 0) is (ascension == 20)
