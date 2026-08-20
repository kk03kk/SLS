"""Deterministic exploration policies for parity corpora."""

from __future__ import annotations

from collections.abc import Sequence

from sls.contracts import Action, ActionKind, Decision


PRIORITY = {
    ActionKind.CHOOSE_NEOW_OPTION: 10,
    ActionKind.USE_POTION: 20,
    ActionKind.PLAY_CARD: 30,
    ActionKind.SELECT_CARD: 40,
    ActionKind.CONFIRM: 50,
    ActionKind.CHOOSE_CARD_REWARD: 60,
    ActionKind.TAKE_REWARD: 70,
    ActionKind.TAKE_BLUE_KEY: 71,
    ActionKind.CHOOSE_BOSS_RELIC: 80,
    ActionKind.BUY_CARD: 90,
    ActionKind.BUY_RELIC: 91,
    ActionKind.BUY_POTION: 92,
    ActionKind.REMOVE_CARD: 93,
    ActionKind.LEAVE_SHOP: 100,
    ActionKind.CHOOSE_EVENT_OPTION: 110,
    ActionKind.REST: 120,
    ActionKind.UPGRADE_CARD: 121,
    ActionKind.RECALL: 122,
    ActionKind.LIFT: 123,
    ActionKind.DIG: 124,
    ActionKind.OPEN_CHEST: 130,
    ActionKind.CHOOSE_MAP_NODE: 140,
    ActionKind.SKIP_CARD_REWARD: 150,
    ActionKind.SKIP_REWARD: 151,
    ActionKind.PROCEED: 160,
    ActionKind.END_TURN: 200,
    ActionKind.DISCARD_POTION: 210,
    ActionKind.CANCEL: 220,
}


def deterministic_action(
    original: Decision,
    simulator: Decision,
    *,
    variant: int = 0,
) -> Action:
    original_by_id = {action.candidate_id: action for action in original.actions}
    common = [action for action in simulator.actions if action.candidate_id in original_by_id]
    if not common:
        raise RuntimeError("Original and Simulator expose no common canonical action")
    ordered = sorted(common, key=lambda action: (PRIORITY.get(action.kind, 1000), action.candidate_id))
    return ordered[variant % len(ordered)]


def action_ids(actions: Sequence[Action]) -> tuple[str, ...]:
    return tuple(sorted(action.candidate_id for action in actions))
