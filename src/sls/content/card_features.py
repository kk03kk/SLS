"""Public mutable card features, shared by native and stock adapters.

Only project fields with the same public meaning. Native specialData and stock
misc are deliberately not treated as interchangeable (notably for Rampage).
"""

from collections.abc import Mapping


def public_card_properties(card_id: str, raw: Mapping[str, object]) -> tuple:
    properties: dict[str, int | bool] = {}
    for name in ("free_to_play_once", "retain", "self_retain",
                 "bottled_flame", "bottled_lightning", "bottled_tornado"):
        if name in raw:
            properties[name] = bool(raw[name])
    if card_id in {"RAMPAGE", "RITUAL_DAGGER"}:
        if "base_damage" in raw:
            properties["base_damage"] = int(raw["base_damage"])
        elif card_id == "RITUAL_DAGGER" and "special_data" in raw:
            # Both engines store Ritual Dagger's persistent base damage here.
            properties["base_damage"] = int(raw["special_data"])
        else:
            raise ValueError(
                f"{card_id} requires public base_damage; rebuild the Observation Oracle"
            )
    return tuple(sorted(properties.items()))


def public_card_option_properties(card_id: str, raw: Mapping[str, object]) -> tuple:
    """Keep visible card state on action-referenced choice and shop entities."""
    properties = dict(public_card_properties(card_id, raw))
    properties["upgrades"] = int(raw.get("upgrades", 0))
    if "base_cost" in raw:
        properties["base_cost"] = int(raw["base_cost"])
    for name in ("cost_for_turn", "current_cost", "cost"):
        if name in raw:
            properties["current_cost"] = int(raw[name])
            break
    return tuple(sorted(properties.items()))
