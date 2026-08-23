from sls.content import load_content_registry
from sls.content.normalize import (
    normalize_card_id,
    normalize_content_id,
    normalize_potion_id,
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
