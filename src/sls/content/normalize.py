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
        # Stock class/save IDs use implementation names for these two
        # gremlins; native names them by their public combat roles.
        "GREMLIN_WARRIOR": "MAD_GREMLIN",
        "GREMLIN_TSUNDERE": "SHIELD_GREMLIN",
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
    normalized = normalize_content_id(value)
    return {
        # Stock DexLossPower exposes "DexLoss" while the native simulator
        # models the same end-of-turn marker as LOSE_DEXTERITY.
        "DEX_LOSS": "LOSE_DEXTERITY",
        # Gremlin Nob's stock AngerPower is native MonsterStatus::ENRAGE.
        "ANGER": "ENRAGE",
        # Stock FlexPower is the end-of-turn Strength-loss marker represented
        # by PlayerStatus::LOSE_STRENGTH in the native simulator.
        "FLEX": "LOSE_STRENGTH",
        # Stock's WeakPower public ID is "Weakened".
        "WEAKENED": "WEAK",
        # Stock's RegenPower public ID is "Regenerate".
        "REGENERATE": "REGEN",
        # DuplicationPotion applies DuplicationPower, whose stock public ID
        # includes the implementation suffix omitted by the native enum.
        "DUPLICATION_POWER": "DUPLICATION",
        # Swivel's ForcefieldPower exposes the stock ID "Nullify Attack";
        # native names the same one-attack marker FREE_ATTACK_POWER.
        "NULLIFY_ATTACK": "FREE_ATTACK_POWER",
    }.get(normalized, normalized)


AMOUNTLESS_POWER_IDS = frozenset({
    "BACK_ATTACK", "BARRICADE", "CORRUPTION", "END_TURN_DEATH",
    "FREE_ATTACK_POWER", "NO_DRAW", "PAINFUL_STABS", "SPLIT", "STASIS",
    "SURROUNDED", "UNAWAKENED",
})


def normalize_power_amount(power_id: object, amount: object) -> int:
    """Map stock's ``-1`` marker convention to canonical presence ``1``."""

    value = int(amount or 0)
    return 1 if normalize_power_id(power_id) in AMOUNTLESS_POWER_IDS else value


@lru_cache(maxsize=1)
def _event_aliases() -> dict[str, str]:
    aliases: dict[str, str] = {}
    for item in load_content_registry().categories.get("events", ()):
        canonical = str(item["id"])
        for value in (canonical, item.get("game_id")):
            if value:
                aliases[_compact(value)] = canonical
                aliases[_compact(str(value) + "Event")] = canonical
                # Stock event class names commonly omit a leading article
                # from the display/game ID (for example Cleric vs The Cleric).
                words = str(value).split()
                if words and words[0].upper() in {"A", "AN", "THE"}:
                    without_article = " ".join(words[1:])
                    aliases[_compact(without_article)] = canonical
                    aliases[_compact(without_article + "Event")] = canonical
    aliases[_compact("GremlinWheelGame")] = "WHEEL_OF_CHANGE"
    # The stock class name is unrelated to its save/game ID "Liars Game".
    aliases[_compact("Sssserpent")] = "THE_SSSSSERPENT"
    aliases[_compact("GoopPuddle")] = "WORLD_OF_GOOP"
    return aliases


def normalize_event_id(value: object) -> str:
    """Normalize event IDs, including stock Java class-name suffixes."""

    return _event_aliases().get(_compact(value), normalize_content_id(value))
