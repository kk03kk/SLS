"""Normalize backend-specific base-game IDs to canonical C++ enum IDs."""

from __future__ import annotations

from functools import lru_cache
import re

from sls.content.registry import load_content_registry


def _compact(value: object) -> str:
    return "".join(
        character for character in str(value or "").upper()
        if character.isalnum()
    )


@lru_cache(maxsize=1)
def _aliases() -> dict[str, str]:
    candidates: dict[str, set[str]] = {}
    registry = load_content_registry()
    for items in registry.categories.values():
        for item in items:
            canonical = str(item["id"])
            for value in (canonical, item.get("game_id")):
                if value:
                    candidates.setdefault(_compact(value), set()).add(canonical)
    # A compact spelling shared by categories is ambiguous without context.
    return {
        alias: next(iter(canonical_ids))
        for alias, canonical_ids in candidates.items()
        if len(canonical_ids) == 1
    }


def normalize_content_id(value: object) -> str:
    canonical = _aliases().get(_compact(value))
    if canonical is not None:
        return canonical
    text = re.sub(
        r"(?<=[a-z0-9])(?=[A-Z])", "_", str(value or "UNKNOWN")
    )
    normalized = "_".join(
        text.upper().replace("_", " ").replace("-", " ").split()
    )
    return {
        "FUZZY_LOUSE_NORMAL": "RED_LOUSE",
        "FUZZY_LOUSE_DEFENSIVE": "GREEN_LOUSE",
        "SLAVER_BLUE": "BLUE_SLAVER",
        "SLAVER_RED": "RED_SLAVER",
        "TOXIC_EGG_2": "TOXIC_EGG",
        "FROZEN_EGG_2": "FROZEN_EGG",
        "GREMLIN_FAT": "FAT_GREMLIN",
        "GREMLIN_THIEF": "SNEAKY_GREMLIN",
        "SERPENT": "SPIRE_GROWTH",
    }.get(normalized, normalized)


def normalize_card_id(value: object) -> str:
    compact = _compact(value)
    aliases = {
        "STRIKE": "STRIKE_RED",
        "DEFEND": "DEFEND_RED",
        "STRIKER": "STRIKE_RED",
        "STRIKERED": "STRIKE_RED",
        "DEFENDR": "DEFEND_RED",
        "DEFENDRED": "DEFEND_RED",
    }
    return aliases.get(compact, normalize_content_id(value))


def normalize_potion_id(value: object) -> str:
    compact = _compact(value)
    if compact in {"POTIONSLOT", "EMPTYPOTIONSLOT", "EMPTYPOTIONID"}:
        return "EMPTY_POTION_SLOT"
    return normalize_content_id(value)


def normalize_power_id(value: object) -> str:
    return normalize_content_id(value)
