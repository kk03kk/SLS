from __future__ import annotations

import importlib.util
from pathlib import Path

import pytest

from sls.content.scope import load_ironclad_a0_scope
from sls.content.semantic_audit import FIRST_TURN_RELIC_EVIDENCE
from sls.content.normalize import normalize_content_id


ROOT = Path(__file__).resolve().parents[2]
native = pytest.importorskip("sls.backends.simulator.native", exc_type=ImportError)


def _load_build_oracle():
    spec = importlib.util.spec_from_file_location("build_oracle", ROOT / "tools" / "build_oracle.py")
    assert spec and spec.loader
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def test_relic_oracle_allowlist_is_the_exact_scoped_set() -> None:
    rows = _load_build_oracle().scenario_relic_allowlist().decode("utf-8").splitlines()
    assert len(rows) == 151
    assert {row.split("\t", 1)[0] for row in rows} == set(
        load_ironclad_a0_scope()["relics"]["ids"]
    )


@pytest.mark.parametrize("relic_id", FIRST_TURN_RELIC_EVIDENCE)
def test_first_turn_relic_probe_exposes_a_legal_boundary(relic_id: str) -> None:
    battle = native.LightspeedBattle()
    battle.reset_relic_probe(0, relic_id)
    snapshot = battle.snapshot()
    assert snapshot["available_commands"]
    relics = snapshot["game_state"]["relics"]
    assert any(normalize_content_id(item["id"]) == relic_id for item in relics)


def test_first_turn_relic_probe_applies_representative_stock_effects() -> None:
    akabeko = native.LightspeedBattle()
    akabeko.reset_relic_probe(0, "AKABEKO")
    assert any(
        power["id"] == "VIGOR" and power["amount"] == 8
        for power in akabeko.snapshot()["game_state"]["combat_state"]["player"]["powers"]
    )

    marbles = native.LightspeedBattle()
    marbles.reset_relic_probe(0, "BAG_OF_MARBLES")
    assert any(
        normalize_content_id(power["id"]) == "VULNERABLE" and power["amount"] == 1
        for power in marbles.snapshot()["game_state"]["combat_state"]["monsters"][0]["powers"]
    )
