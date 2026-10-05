"""Canonical public energy-per-turn semantics shared by both backends."""

from __future__ import annotations

from collections.abc import Iterable

ENERGY_PER_TURN_RELICS = frozenset({
    "BUSTED_CROWN",
    "COFFEE_DRIPPER",
    "CURSED_KEY",
    "ECTOPLASM",
    "FUSION_HAMMER",
    "MARK_OF_PAIN",
    "PHILOSOPHERS_STONE",
    "RUNIC_DOME",
    "SOZU",
    "VELVET_CHOKER",
})


def canonical_max_energy(
    relic_ids: Iterable[str],
    *,
    combat_value: int | None = None,
    room_type: str = "",
    in_combat: bool = False,
) -> int:
    """Return the policy-visible energy-per-turn value.

    CommunicationMod omits this value while the simulator's run-level state
    retains the pre-relic default.  Permanent energy relics change the public
    character stat at every boundary.  Slaver's Collar is conditional and is
    therefore added only in an eligible active combat.  An explicit combat
    value remains authoritative because temporary engine rules may affect it.
    """

    if combat_value is not None:
        return int(combat_value)
    normalized = frozenset(str(relic_id).upper() for relic_id in relic_ids)
    energy = 3 + len(normalized & ENERGY_PER_TURN_RELICS)
    room = str(room_type).upper()
    if in_combat and "SLAVERS_COLLAR" in normalized and (
        "ELITE" in room or "BOSS" in room
    ):
        energy += 1
    return energy
