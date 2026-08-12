"""Stable numeric vocabulary shared by real-game and simulator observations."""

IRONCLAD_CARD_IDS = (
    "ANGER", "CLEAVE", "WARCRY", "FLEX", "IRON_WAVE", "BODY_SLAM",
    "TRUE_GRIT", "SHRUG_IT_OFF", "CLASH", "THUNDERCLAP", "POMMEL_STRIKE",
    "TWIN_STRIKE", "CLOTHESLINE", "ARMAMENTS", "HAVOC", "HEADBUTT",
    "WILD_STRIKE", "HEAVY_BLADE", "PERFECTED_STRIKE", "SWORD_BOOMERANG",
    "EVOLVE", "UPPERCUT", "GHOSTLY_ARMOR", "FIRE_BREATHING", "DROPKICK",
    "CARNAGE", "BLOODLETTING", "RUPTURE", "SECOND_WIND", "SEARING_BLOW",
    "BATTLE_TRANCE", "SENTINEL", "ENTRENCH", "RAGE", "FEEL_NO_PAIN",
    "DISARM", "SEEING_RED", "DARK_EMBRACE", "COMBUST", "WHIRLWIND",
    "SEVER_SOUL", "RAMPAGE", "SHOCKWAVE", "METALLICIZE", "BURNING_PACT",
    "PUMMEL", "FLAME_BARRIER", "BLOOD_FOR_BLOOD", "INTIMIDATE",
    "HEMOKINESIS", "RECKLESS_CHARGE", "INFERNAL_BLADE", "DUAL_WIELD",
    "POWER_THROUGH", "INFLAME", "SPOT_WEAKNESS", "DOUBLE_TAP",
    "DEMON_FORM", "BLUDGEON", "FEED", "LIMIT_BREAK", "CORRUPTION",
    "BARRICADE", "FIEND_FIRE", "BERSERK", "IMPERVIOUS", "JUGGERNAUT",
    "BRUTALITY", "REAPER", "EXHUME", "OFFERING", "IMMOLATE",
    "STRIKE_RED", "DEFEND_RED", "BASH",
)

CARD_IDS = ("UNKNOWN",) + IRONCLAD_CARD_IDS + (
    "WOUND", "BURN", "DAZED", "SLIMED",
)
CARD_ID_TO_INDEX = {card_id: index for index, card_id in enumerate(CARD_IDS)}

POTION_IDS = (
    "UNKNOWN", "EMPTY_POTION_SLOT", "AMBROSIA", "ANCIENT_POTION",
    "ATTACK_POTION", "BLESSING_OF_THE_FORGE", "BLOCK_POTION", "BLOOD_POTION",
    "BOTTLED_MIRACLE", "COLORLESS_POTION", "CULTIST_POTION", "CUNNING_POTION",
    "DEXTERITY_POTION", "DISTILLED_CHAOS", "DUPLICATION_POTION", "ELIXIR_POTION",
    "ENERGY_POTION", "ENTROPIC_BREW", "ESSENCE_OF_DARKNESS", "ESSENCE_OF_STEEL",
    "EXPLOSIVE_POTION", "FAIRY_POTION", "FEAR_POTION", "FIRE_POTION",
    "FLEX_POTION", "FOCUS_POTION", "FRUIT_JUICE", "GAMBLERS_BREW",
    "GHOST_IN_A_JAR", "HEART_OF_IRON", "LIQUID_BRONZE", "LIQUID_MEMORIES",
    "POISON_POTION", "POTION_OF_CAPACITY", "POWER_POTION", "REGEN_POTION",
    "SKILL_POTION", "SMOKE_BOMB", "SNECKO_OIL", "SPEED_POTION",
    "STANCE_POTION", "STRENGTH_POTION", "SWIFT_POTION", "WEAK_POTION",
)
POTION_ID_TO_INDEX = {potion_id: index for index, potion_id in enumerate(POTION_IDS)}

PLAYER_POWER_IDS = (
    "UNKNOWN", "STRENGTH", "DEXTERITY", "ARTIFACT", "VULNERABLE", "WEAK",
    "FRAIL", "ENTANGLED", "NO_DRAW", "LOSE_STRENGTH", "BARRICADE",
    "CORRUPTION", "DOUBLE_TAP", "BRUTALITY", "COMBUST", "DARK_EMBRACE",
    "DEMON_FORM", "EVOLVE", "FEEL_NO_PAIN", "FIRE_BREATHING",
    "FLAME_BARRIER", "JUGGERNAUT", "METALLICIZE", "RAGE", "RUPTURE",
)
ENEMY_POWER_IDS = (
    "UNKNOWN", "ARTIFACT", "BLOCK_RETURN", "METALLICIZE", "PLATED_ARMOR",
    "STRENGTH", "VULNERABLE", "WEAK", "CURL_UP", "ENRAGE", "MODE_SHIFT",
    "RITUAL", "SHARP_HIDE", "SPORE_CLOUD", "THIEVERY", "ASLEEP",
)


def normalize_power_id(value: object) -> str:
    return "_".join(str(value or "UNKNOWN").upper().replace("_", " ").split())


def normalize_card_id(value: object) -> str:
    text = "".join(
        character for character in str(value or "").upper()
        if character.isalnum()
    )
    aliases = {
        "STRIKE": "STRIKE_RED", "DEFEND": "DEFEND_RED",
        "STRIKER": "STRIKE_RED", "STRIKERED": "STRIKE_RED",
        "DEFENDR": "DEFEND_RED", "DEFENDRED": "DEFEND_RED",
    }
    if text in aliases:
        return aliases[text]
    for card_id in CARD_IDS:
        compact = "".join(character for character in card_id if character.isalnum())
        if compact == text:
            return card_id
    return "UNKNOWN"


def normalize_potion_id(value: object) -> str:
    text = "".join(
        character for character in str(value or "").upper() if character.isalnum()
    )
    if text in {"POTIONSLOT", "EMPTYPOTIONSLOT"}:
        return "EMPTY_POTION_SLOT"
    # CommunicationMod calls Flex Potion "Steroid Potion" internally.
    if text == "STEROIDPOTION":
        return "FLEX_POTION"
    for potion_id in POTION_IDS:
        compact = "".join(character for character in potion_id if character.isalnum())
        if compact == text:
            return potion_id
    return "UNKNOWN"


assert len(IRONCLAD_CARD_IDS) == 75
