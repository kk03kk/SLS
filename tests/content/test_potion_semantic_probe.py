from __future__ import annotations

import importlib.util
from pathlib import Path

import pytest

from sls.backends.original.adapter import adapt_original
from sls.content.scope import load_ironclad_a0_scope


ROOT = Path(__file__).resolve().parents[2]
native = pytest.importorskip("sls.backends.simulator.native", exc_type=ImportError)


def _load_build_oracle():
    spec = importlib.util.spec_from_file_location("build_oracle", ROOT / "tools" / "build_oracle.py")
    assert spec is not None and spec.loader is not None
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def test_oracle_potion_allowlist_is_exactly_the_scoped_set() -> None:
    rows = _load_build_oracle().scenario_potion_allowlist().decode("utf-8").splitlines()
    assert [row.split("\t", 1)[0] for row in rows] == sorted(
        load_ironclad_a0_scope()["potions"]["ids"]
    )


@pytest.mark.parametrize("potion_id,sacred_bark,hp", [
    ("BLOCK_POTION", False, 40),
    ("BLOCK_POTION", True, 40),
    ("FAIRY_POTION", False, 1),
])
def test_native_potion_probe_has_the_stable_audit_baseline(
    potion_id: str, sacred_bark: bool, hp: int,
) -> None:
    battle = native.LightspeedBattle()
    battle.reset_potion_probe(123, potion_id, sacred_bark)
    observation = adapt_original(battle.snapshot()).decision.observation
    assert observation.player.current_hp == hp
    assert observation.player.max_hp == 80
    assert observation.player.energy == 2
    assert observation.potions[0].content_id == potion_id
    relics = {relic.content_id for relic in observation.relics}
    assert "BURNING_BLOOD" in relics
    assert ("SACRED_BARK" in relics) is sacred_bark


def test_native_potion_probe_uses_sacred_bark_potency() -> None:
    normal = native.LightspeedBattle()
    normal.reset_potion_probe(123, "BLOCK_POTION", False)
    normal.step("potion", potion_index=0, target_index=0)
    bark = native.LightspeedBattle()
    bark.reset_potion_probe(123, "BLOCK_POTION", True)
    bark.step("potion", potion_index=0, target_index=0)
    assert adapt_original(normal.snapshot()).decision.observation.player.block == 12
    assert adapt_original(bark.snapshot()).decision.observation.player.block == 24


def test_blessing_of_the_forge_does_not_upgrade_unupgradable_statuses() -> None:
    battle = native.LightspeedBattle()
    battle.reset_potion_probe(123, "BLESSING_OF_THE_FORGE", False)
    battle.step("potion", potion_index=0, target_index=0)
    hand = adapt_original(battle.snapshot()).decision.observation.hand
    assert [(card.card_id, card.upgrades) for card in hand] == [
        ("STRIKE_RED", 1), ("DEFEND_RED", 1), ("DAZED", 0),
    ]
