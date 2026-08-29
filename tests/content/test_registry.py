from sls.content import load_content_registry
from sls.content.normalize import (
    AMOUNTLESS_POWER_IDS,
    normalize_card_id,
    normalize_content_id,
    normalize_event_id,
    normalize_monster_id,
    normalize_potion_id,
    normalize_power_amount,
    normalize_power_id,
)


def test_committed_registry_is_valid() -> None:
    registry = load_content_registry()
    assert registry.categories
    assert "cards" in registry.categories


def test_original_internal_ids_normalize_through_the_registry() -> None:
    assert normalize_card_id("Ghostly") == "APPARITION"
    assert normalize_card_id("Venomology") == "ALCHEMIZE"
    assert normalize_card_id("Strike_R") == "STRIKE_RED"
    assert normalize_potion_id("SteroidPotion") == "FLEX_POTION"
    assert normalize_content_id("Yang") == "DUALITY"
    assert normalize_content_id("WingedGreaves") == "WING_BOOTS"


def test_stock_dex_loss_power_uses_native_canonical_id() -> None:
    assert normalize_power_id("DexLoss") == "LOSE_DEXTERITY"
    assert normalize_power_id("DEX_LOSS") == "LOSE_DEXTERITY"
    assert normalize_power_id("LOSE_DEXTERITY") == "LOSE_DEXTERITY"
    assert normalize_power_id("IntangiblePlayer") == "INTANGIBLE"
    assert normalize_power_id("INTANGIBLE_PLAYER") == "INTANGIBLE"
    assert normalize_power_id("INTANGIBLE") == "INTANGIBLE"
    assert normalize_power_id("NoBlockPower") == "NO_BLOCK"
    assert normalize_power_id("NO_BLOCK_POWER") == "NO_BLOCK"
    assert normalize_power_id("NO_BLOCK") == "NO_BLOCK"
    assert normalize_power_id("TheBomb0") == "THE_BOMB"
    assert normalize_power_id("TheBomb127") == "THE_BOMB"
    assert normalize_power_id("THE_BOMB") == "THE_BOMB"
    assert normalize_power_id("Regeneration") == "REGEN"
    assert normalize_power_id("REGEN") == "REGEN"


def test_stock_duplication_power_uses_native_canonical_id() -> None:
    assert normalize_power_id("DuplicationPower") == "DUPLICATION"
    assert normalize_power_id("DUPLICATION_POWER") == "DUPLICATION"
    assert normalize_power_id("DUPLICATION") == "DUPLICATION"


def test_amountless_stock_powers_use_presence_without_rewriting_numeric_debuffs() -> None:
    for power_id in ("No Draw", "Barricade", "Corruption", "Painful Stabs"):
        assert normalize_power_amount(power_id, -1) == 1
    assert normalize_power_amount("Strength", -1) == -1


def test_amountless_power_set_contains_required_runtime_aliases() -> None:
    assert {"CONFUSED", "MINION", "REACTIVE", "REGROW", "SHIFTING"} <= set(
        AMOUNTLESS_POWER_IDS
    )


def test_stock_sssserpent_class_alias_matches_liars_game() -> None:
    assert normalize_event_id("Sssserpent") == "THE_SSSSSERPENT"
    assert normalize_event_id("Liars Game") == "THE_SSSSSERPENT"


def test_stock_gremlin_class_ids_match_native_monster_ids() -> None:
    assert normalize_content_id("GremlinWarrior") == "MAD_GREMLIN"
    assert normalize_content_id("GremlinTsundere") == "SHIELD_GREMLIN"
    assert normalize_content_id("Healer") == "MYSTIC"
    assert normalize_monster_id("Champ") == "THE_CHAMP"
    assert normalize_monster_id("Maw") == "THE_MAW"
    assert normalize_content_id("BanditBear") == "BEAR"
    assert normalize_content_id("BanditChild") == "POINTY"
    assert normalize_content_id("BanditLeader") == "ROMEO"
    assert normalize_content_id("SlaverBoss") == "TASKMASTER"
    assert normalize_power_id("Generic Strength Up Power") == "GENERIC_STRENGTH_UP"
    assert normalize_power_id("Life Link") == "REGROW"
    assert normalize_power_id("Compulsive") == "REACTIVE"
    assert normalize_event_id("GoopPuddle") == "WORLD_OF_GOOP"
    assert normalize_event_id("World of Goop") == "WORLD_OF_GOOP"
