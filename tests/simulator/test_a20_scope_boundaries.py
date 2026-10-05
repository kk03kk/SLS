"""Direct checks for two deliberate Ironclad A20 policy boundaries."""

import pytest

from sls.backends.simulator import SimulatorBackend
from sls.content.registry import load_content_registry
from sls.contracts import ActionKind
from sls.curriculum import IRONCLAD_A0_ACT1, IRONCLAD_A20_ACT1


def _ordinal(category: str, content_id: str) -> int:
    return next(
        int(item["ordinal"])
        for item in load_content_registry().categories[category]
        if item["id"] == content_id
    )


def test_note_for_yourself_is_absent_from_a20_generation_pool() -> None:
    note = _ordinal("events", "NOTE_FOR_YOURSELF")
    a0 = SimulatorBackend(IRONCLAD_A0_ACT1)
    a20 = SimulatorBackend(IRONCLAD_A20_ACT1)
    a0.reset(0)
    a20.reset(0)

    assert note in a0.raw_state["ordered_pools"]["special_one_time_events"]
    assert note not in a20.raw_state["ordered_pools"]["special_one_time_events"]


def test_prismatic_shard_remains_in_a20_shop_rng_pool() -> None:
    shard = _ordinal("relics", "PRISMATIC_SHARD")
    backend = SimulatorBackend(IRONCLAD_A20_ACT1)
    backend.reset(0)

    assert shard in backend.raw_state["ordered_pools"]["shop_relics"]


def test_ineligible_event_fallback_does_not_advance_global_event_rng() -> None:
    """Stock EventRoom passes an event RNG duplicate to generateEvent."""

    backend = SimulatorBackend(IRONCLAD_A20_ACT1)
    backend.reset(0)
    checkpoint = backend.checkpoint()
    checkpoint["ordered_pools"]["events"] = [_ordinal("events", "DEAD_ADVENTURER")]
    checkpoint["ordered_pools"]["shrines"] = [_ordinal("events", "GOLDEN_SHRINE")]
    checkpoint["ordered_pools"]["special_one_time_events"] = []
    decision = backend.load_checkpoint(checkpoint)
    backend._native._set_skip_battles_for_testing(True)

    for kind, node_id in (
        (ActionKind.CHOOSE_NEOW_OPTION, None),
        (ActionKind.SELECT_CARD, None),
        (ActionKind.CHOOSE_MAP_NODE, "map:4:0"),
        (ActionKind.SKIP_REWARD, None),
        (ActionKind.CHOOSE_MAP_NODE, "map:4:1"),
    ):
        action = next(
            item for item in decision.actions
            if item.kind is kind and (node_id is None or item.node_id == node_id)
        )
        decision = backend.step(action).decision

    assert backend.raw_state["public_run"]["current_event_id"] == "Golden Shrine"
    # Only the event-room outcome roll uses the shared stream. The shrine
    # selection uses the duplicate, even when the normal event list has no
    # eligible entry and falls back to a shrine.
    assert backend.raw_state["rng"]["event"]["counter"] == 1


@pytest.mark.parametrize("only_event,gold,special,expected", [
    ("DEAD_ADVENTURER", 99, False, "Golden Shrine"),
    ("HYPNOTIZING_COLORED_MUSHROOMS", 99, False, "Golden Shrine"),
    ("THE_CLERIC", 0, False, "Golden Shrine"),
    ("THE_CLERIC", 34, False, "Golden Shrine"),
    ("THE_CLERIC", 35, False, "The Cleric"),
    ("THE_CLERIC", 99, False, "The Cleric"),
    ("THE_DIVINE_FOUNTAIN", 99, True, "Golden Shrine"),
    ("THE_WOMAN_IN_BLUE", 0, True, "Golden Shrine"),
    ("THE_WOMAN_IN_BLUE", 49, True, "Golden Shrine"),
    ("THE_WOMAN_IN_BLUE", 50, True, "The Woman in Blue"),
])
def test_early_act1_event_eligibility_falls_back_to_shrine(
    only_event: str, gold: int, special: bool, expected: str,
) -> None:
    """Stock eligibility gates floor, gold and removable curses."""

    backend = SimulatorBackend(IRONCLAD_A20_ACT1)
    decision = backend.reset(0)
    backend._native._set_skip_battles_for_testing(True)
    for kind, node_id in (
        (ActionKind.CHOOSE_NEOW_OPTION, None),
        (ActionKind.SELECT_CARD, None),
        (ActionKind.CHOOSE_MAP_NODE, "map:4:0"),
        (ActionKind.SKIP_REWARD, None),
    ):
        action = next(
            item for item in decision.actions
            if item.kind is kind and (node_id is None or item.node_id == node_id)
        )
        decision = backend.step(action).decision

    checkpoint = backend.checkpoint()
    checkpoint["ordered_pools"]["events"] = [
        _ordinal("events", "DEAD_ADVENTURER" if special else only_event),
    ]
    # Place the always-eligible sentinel after the tested one-time event so
    # this seed selects it only when the tested entry is filtered out. This
    # forced pool arrangement tests eligibility, not natural pool membership.
    checkpoint["ordered_pools"]["shrines"] = (
        [] if special else [_ordinal("events", "GOLDEN_SHRINE")]
    )
    checkpoint["ordered_pools"]["special_one_time_events"] = (
        [_ordinal("events", only_event), _ordinal("events", "GOLDEN_SHRINE")]
        if special else []
    )
    checkpoint["player_state"]["gold"] = gold
    checkpoint["public_run"]["gold"] = gold
    decision = backend.load_checkpoint(checkpoint)
    enter = next(item for item in decision.actions
                 if item.kind is ActionKind.CHOOSE_MAP_NODE and item.node_id == "map:4:1")
    backend.step(enter)
    assert backend.raw_state["public_run"]["current_event_id"] == expected
