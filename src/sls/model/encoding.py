"""Versioned, collision-free policy vocabulary and field schema."""

from __future__ import annotations

import hashlib
import json
import re
from functools import lru_cache
from pathlib import Path
from typing import Any

from sls.content import load_content_registry
from sls.contracts import ActionKind, ScreenType

ENCODING_SCHEMA = "sls-policy-input-v3"
ENTITY_TYPES = (
    "PLAYER", "RUN", "CARD", "ENEMY", "POWER", "RELIC", "POTION",
    "MAP_NODE", "CHOICE", "REWARD", "SHOP_ITEM", "EVENT_OPTION",
    "REST_OPTION", "BOSS_RELIC",
)
ENTITY_TYPE_IDS = {name: index for index, name in enumerate(ENTITY_TYPES)}
ACTION_TYPE_IDS = {kind.value: index for index, kind in enumerate(ActionKind)}
NUMERIC_FIELDS = (
    "current_hp", "max_hp", "block", "energy", "max_energy", "ascension",
    "act", "floor", "gold", "ruby_key", "emerald_key", "sapphire_key",
    "upgrades", "base_cost", "current_cost", "playable", "visible_order",
    "intent_damage", "intent_hits", "is_gone", "amount", "counter", "slot",
    "x", "y", "reachable", "price", "sold", "turn", "deck_index", "known",
    "removed", "order_is_visible", "outgoing_count", "option_ordinal",
)
NUMERIC_FIELD_IDS = {name: index for index, name in enumerate(NUMERIC_FIELDS)}
CATEGORICAL_FIELDS = ("screen", "zone", "intent", "item_type", "source", "visible_boss")
CATEGORICAL_FIELD_IDS = {name: index for index, name in enumerate(CATEGORICAL_FIELDS)}
MONSTER_INTENTS = (
    "ATTACK", "ATTACK_BUFF", "ATTACK_DEBUFF", "ATTACK_DEFEND", "BUFF",
    "DEBUFF", "STRONG_DEBUFF", "DEFEND", "DEFEND_BUFF", "DEFEND_DEBUFF",
    "ESCAPE", "MAGIC", "SLEEP", "STUN", "UNKNOWN", "DEBUG",
)
SCREEN_GROUPS = ("COMBAT", "RUN", "CHOICE")
SCREEN_GROUP_IDS = {name: index for index, name in enumerate(SCREEN_GROUPS)}
SCREEN_TO_GROUP = {
    "COMBAT": "COMBAT",
    "MAP": "RUN", "SHOP": "RUN", "REST": "RUN", "TREASURE": "RUN",
    "ACT_TRANSITION": "RUN",
    "NEOW": "CHOICE", "CARD_REWARD": "CHOICE", "COMBAT_REWARD": "CHOICE",
    "EVENT": "CHOICE", "BOSS_REWARD": "CHOICE", "GAME_OVER": "CHOICE",
}

_PLAYER_POWERS = """
DOUBLE_DAMAGE DRAW_REDUCTION FRAIL INTANGIBLE VULNERABLE WEAK BIAS CONFUSED
CONSTRICTED ENTANGLED FASTING HEX LOSE_DEXTERITY LOSE_STRENGTH NO_BLOCK NO_DRAW
WRAITH_FORM BARRICADE BLASPHEMER CORRUPTION ELECTRO SURROUNDED MASTER_REALITY
PEN_NIB WRATH_NEXT_TURN AMPLIFY BLUR BUFFER COLLECT DOUBLE_TAP DUPLICATION
ECHO_FORM FREE_ATTACK_POWER REBOUND MANTRA ACCURACY AFTER_IMAGE BATTLE_HYMN
BRUTALITY BURST COMBUST CREATIVE_AI DARK_EMBRACE DEMON_FORM DEVA DEVOTION
DRAW_CARD_NEXT_TURN ENERGIZED ENVENOM ESTABLISHMENT EVOLVE FEEL_NO_PAIN
FIRE_BREATHING FLAME_BARRIER FOCUS FORESIGHT HELLO_WORLD INFINITE_BLADES
JUGGERNAUT LIKE_WATER LOOP MAGNETISM MAYHEM METALLICIZE NEXT_TURN_BLOCK
NOXIOUS_FUMES OMEGA PANACHE PHANTASMAL PLATED_ARMOR RAGE REGEN RITUAL RUPTURE
SADISTIC STATIC_DISCHARGE THORNS THOUSAND_CUTS TOOLS_OF_THE_TRADE VIGOR
WAVE_OF_THE_HAND EQUILIBRIUM ARTIFACT DEXTERITY STRENGTH THE_BOMB RETAIN_CARDS
""".split()
_MONSTER_POWERS = """
ARTIFACT BLOCK_RETURN CHOKED CORPSE_EXPLOSION LOCK_ON MARK METALLICIZE
PLATED_ARMOR POISON REGEN SHACKLED STRENGTH VULNERABLE WEAK ANGRY BEAT_OF_DEATH
CURIOSITY CURL_UP ENRAGE FADING FLIGHT GENERIC_STRENGTH_UP INTANGIBLE MALLEABLE
MODE_SHIFT RITUAL SLOW SPORE_CLOUD THIEVERY THORNS TIME_WARP INVINCIBLE REACTIVE
SHARP_HIDE BARRICADE MINION MINION_LEADER PAINFUL_STABS REGROW SHIFTING STASIS
""".split()
_SPECIAL_CONTENT = {
    "IRONCLAD", "GOLD", "EMERALD_KEY", "SAPPHIRE_KEY", "RUBY_KEY", "NEOW",
    "REST", "SMITH", "RECALL", "LIFT", "TOKE", "DIG", "PROCEED", "OPTION",
    # Public placeholder used by Match and Keep before a card is revealed.
    "HIDDEN_CARD",
}
_CATEGORY_VALUES = {
    "NONE", *[item.value for item in ScreenType],
    "DECK", "HAND", "DRAW", "DISCARD", "EXHAUST",
    *MONSTER_INTENTS, "CARD", "RELIC", "POTION",
    "EXHAUST_PILE", "DISCARD_PILE", "DRAW_PILE", "GENERATED", "MASTER_DECK",
    "MONSTER", "ELITE", "EVENT", "REST", "SHOP", "TREASURE", "BOSS",
    "BURNING_ELITE", "M", "E", "?", "R", "$", "T", "B",
}
VOCABULARY_PATH = Path(__file__).with_name("policy_vocabulary_v3.json")
_CONSTANT_HEADERS = Path(__file__).resolve().parents[3] / "cpp" / "simulator" / "include" / "constants"
_NATIVE_MODULE = Path(__file__).resolve().parents[3] / "cpp" / "simulator" / "python" / "module.cpp"


def _cpp_string_array(filename: str, array_name: str) -> set[str]:
    source = (_CONSTANT_HEADERS / filename).read_text(encoding="utf-8")
    match = re.search(
        rf"\b{re.escape(array_name)}\s*\[\s*\]\s*\{{(.*?)\}};",
        source,
        re.DOTALL,
    )
    if match is None:
        raise RuntimeError(f"cannot locate policy vocabulary source {array_name}")
    return set(re.findall(r'"([A-Z][A-Z0-9_]*)"', match.group(1))) - {"INVALID"}


def _native_public_power_tokens() -> set[str]:
    """Return synthetic public powers that are not backed by a status enum."""

    source = _NATIVE_MODULE.read_text(encoding="utf-8")
    return set(re.findall(r'\bpower\("([A-Z][A-Z0-9_]*)"', source))


def build_policy_vocabulary() -> dict[str, Any]:
    registry = load_content_registry()
    content = set(_SPECIAL_CONTENT) | set(_PLAYER_POWERS) | set(_MONSTER_POWERS)
    content.update(_cpp_string_array("PlayerStatusEffects.h", "playerStatusEnumStrings"))
    content.update(_cpp_string_array("MonsterStatusEffects.h", "monsterStatusEnumStrings"))
    content.update(_native_public_power_tokens())
    for items in registry.categories.values():
        content.update(str(item["id"]) for item in items)
    # Some categorical slots (notably the visible boss) legitimately carry a
    # registered content ID.  They share the exact vocabulary, not a hash.
    categorical = {str(value).upper() for value in _CATEGORY_VALUES | content}
    categorical.update(f"OPTION:{index}" for index in range(32))
    payload: dict[str, Any] = {
        "schema": ENCODING_SCHEMA,
        "content": ["NONE", *sorted(content - {"NONE"})],
        "categorical": ["NONE", *sorted(categorical - {"NONE"})],
        "numeric_fields": list(NUMERIC_FIELDS),
        "categorical_fields": list(CATEGORICAL_FIELDS),
        "entity_types": list(ENTITY_TYPES),
        "action_types": list(ACTION_TYPE_IDS),
        "reference_roles": ["subject", "target", "option", "node", "reward"],
        "screen_groups": list(SCREEN_GROUPS),
    }
    encoded = json.dumps(payload, sort_keys=True, separators=(",", ":")).encode("utf-8")
    payload["sha256"] = hashlib.sha256(encoded).hexdigest()
    return payload


@lru_cache(maxsize=1)
def policy_vocabulary() -> dict[str, Any]:
    if not VOCABULARY_PATH.exists():
        raise RuntimeError(
            f"missing committed policy vocabulary: {VOCABULARY_PATH}; "
            "run tools/generate_policy_vocabulary.py"
        )
    payload = json.loads(VOCABULARY_PATH.read_text(encoding="utf-8"))
    unsigned = dict(payload)
    claimed = str(unsigned.pop("sha256", ""))
    encoded = json.dumps(unsigned, sort_keys=True, separators=(",", ":")).encode("utf-8")
    if hashlib.sha256(encoded).hexdigest() != claimed:
        raise RuntimeError("committed policy vocabulary digest is invalid")
    return payload


def vocabulary_hash() -> str:
    return str(policy_vocabulary()["sha256"])


@lru_cache(maxsize=1)
def _vocabulary_indices() -> tuple[dict[str, int], dict[str, int]]:
    vocabulary = policy_vocabulary()
    return (
        {str(value): index for index, value in enumerate(vocabulary["content"])},
        {str(value): index for index, value in enumerate(vocabulary["categorical"])},
    )


def content_token(value: str | None) -> tuple[int, int]:
    if value is None:
        return 0, 0
    raw, variant = str(value), "NONE"
    if ":OPTION:" in raw:
        raw, ordinal = raw.rsplit(":OPTION:", 1)
        variant = f"OPTION:{int(ordinal)}"
    try:
        content = _vocabulary_indices()[0][raw]
    except KeyError as error:
        raise ValueError(f"unknown policy content ID: {value}") from error
    try:
        variant_id = _vocabulary_indices()[1][variant]
    except KeyError as error:
        raise ValueError(f"unknown policy content variant: {variant}") from error
    return content, variant_id


def categorical_token(value: str | None, *, path: str) -> int:
    raw = "NONE" if value is None else str(value).upper()
    try:
        return _vocabulary_indices()[1][raw]
    except KeyError as error:
        raise ValueError(f"unknown policy categorical value at {path}: {value}") from error
