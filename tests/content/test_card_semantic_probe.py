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


def test_oracle_card_allowlist_is_exactly_the_scoped_card_closure() -> None:
    module = _load_build_oracle()
    rows = module.scenario_card_allowlist().decode("utf-8").splitlines()
    assert [row.split("\t", 1)[0] for row in rows] == sorted(
        load_ironclad_a0_scope()["cards"]["ids"]
    )


@pytest.mark.parametrize("card_id,expected_relic", [
    ("ANGER", None),
    ("BURN", "MEDICAL_KIT"),
    ("REGRET", "BLUE_CANDLE"),
])
def test_native_card_probe_has_the_stable_audit_baseline(
    card_id: str, expected_relic: str | None,
) -> None:
    battle = native.LightspeedBattle()
    battle.reset_card_probe(123, card_id, False)
    payload = battle.snapshot()
    decision = adapt_original(payload).decision
    observation = decision.observation
    assert observation.player.current_hp == observation.player.max_hp == 80
    assert observation.player.energy == 4
    assert observation.player.block == 0
    assert [(enemy.monster_id, enemy.current_hp, enemy.intent) for enemy in observation.enemies] == [
        ("CULTIST", 999, "ATTACK")
    ]
    assert observation.hand[0].card_id == card_id
    relic_ids = {relic.content_id for relic in observation.relics}
    assert "BURNING_BLOOD" in relic_ids
    if expected_relic is not None:
        assert expected_relic in relic_ids
    assert any(
        action.kind.value == "PLAY_CARD" and action.subject_id == "HAND:0"
        for action in decision.actions
    )
