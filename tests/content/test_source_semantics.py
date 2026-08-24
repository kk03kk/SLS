from __future__ import annotations

import pytest

from sls.content.scope import load_ironclad_a0_scope
from sls.content.source_audit import (
    java_card_metadata,
    java_potion_metadata,
    java_relic_metadata,
    java_relic_callbacks,
    java_sources,
    registry_game_ids,
)


native = pytest.importorskip("sls.backends.simulator.native", exc_type=ImportError)


def _scoped_sources(category: str) -> dict[str, object]:
    scope = load_ironclad_a0_scope()
    ids = scope[category]["ids"]
    game_ids = registry_game_ids(category, ids)
    indexed = java_sources(category)
    assert set(game_ids) == set(ids)
    missing = {canonical: game for canonical, game in game_ids.items() if game not in indexed}
    assert not missing
    return {canonical: indexed[game] for canonical, game in game_ids.items()}


def test_all_scoped_card_static_metadata_matches_stock_java() -> None:
    expected = {
        card_id: java_card_metadata(source)
        for card_id, source in _scoped_sources("cards").items()
    }
    actual = {
        str(item["enum_id"]): dict(item) for item in native.card_metadata_probe()
    }
    mismatches = {
        card_id: {
            key: (value, actual[card_id].get(key))
            for key, value in metadata.items()
            if actual[card_id].get(key) != value
        }
        for card_id, metadata in expected.items()
    }
    assert not {key: value for key, value in mismatches.items() if value}


def test_all_scoped_potion_metadata_matches_stock_java() -> None:
    rarity_ids = {"COMMON": 0, "UNCOMMON": 1, "RARE": 2}
    expected = {
        potion_id: java_potion_metadata(source)
        for potion_id, source in _scoped_sources("potions").items()
    }
    actual = {
        str(item["enum_id"]): dict(item) for item in native.potion_metadata_probe()
    }
    mismatches = {}
    for potion_id, metadata in expected.items():
        normalized = dict(metadata)
        normalized["rarity"] = rarity_ids[str(normalized["rarity"])]
        difference = {
            key: (value, actual[potion_id].get(key))
            for key, value in normalized.items()
            if actual[potion_id].get(key) != value
        }
        if difference:
            mismatches[potion_id] = difference
    assert not mismatches


def test_all_scoped_relic_tiers_match_stock_java() -> None:
    expected = {
        relic_id: java_relic_metadata(source)
        for relic_id, source in _scoped_sources("relics").items()
    }
    actual = {
        str(item["enum_id"]): dict(item) for item in native.relic_metadata_probe()
    }
    mismatches = {
        relic_id: (metadata["tier"], str(actual[relic_id].get("tier")).upper())
        for relic_id, metadata in expected.items()
        if str(actual[relic_id].get("tier")).upper() != metadata["tier"]
    }
    assert not mismatches


def test_all_scoped_relic_callbacks_are_extracted_deterministically() -> None:
    callbacks = {
        relic_id: java_relic_callbacks(source)
        for relic_id, source in _scoped_sources("relics").items()
    }
    assert len(callbacks) == 151
    assert callbacks["AKABEKO"] == ["atBattleStart"]
    assert callbacks["BLOODY_IDOL"] == ["onGainGold"]
    assert callbacks["TUNGSTEN_ROD"] == ["onLoseHpLast"]
    # White Beast has no callback of its own; the stock reward system checks
    # for the marker relic globally. Empty is therefore meaningful evidence.
    assert callbacks["WHITE_BEAST_STATUE"] == []
    assert all(values == list(dict.fromkeys(values)) for values in callbacks.values())
