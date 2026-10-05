from __future__ import annotations

from pathlib import Path

import pytest

from sls.audit.act1_encounters import compare_stock_act1_encounters
from sls.audit.act1_targets import (
    build_a20_act1_targets,
    compare_stock_event_pools,
    compare_stock_ironclad_potion_pool,
    target_ids,
)
from sls.backends.simulator import SimulatorBackend
from sls.curriculum import IRONCLAD_A20_ACT1
from sls.rl.training_contract import native_source_digest
from tools.build_semantic_coverage import build_obligations

STOCK_SOURCE = (
    Path(__file__).resolve().parents[2]
    / "local/audits/stock-decompilation-tree/desktop-1.0/source/com/megacrit/cardcrawl"
)


def _targets() -> dict:
    backend = SimulatorBackend(IRONCLAD_A20_ACT1)
    backend.reset(0)
    return build_a20_act1_targets(backend.raw_state["ordered_pools"])


def test_a20_act1_targets_exclude_ineligible_events_and_later_acts() -> None:
    targets = _targets()
    events = target_ids(targets, "events")
    assert "NOTE_FOR_YOURSELF" not in events
    assert "SECRET_PORTAL" not in events
    assert "THE_JOUST" not in events
    assert "BIG_FISH" in events
    assert "SECRET_PORTAL" in targets["outside_act1_events"]
    assert "AUTOMATON" not in target_ids(targets, "encounters")
    assert "SLIME_BOSS" in target_ids(targets, "encounters")
    shard = next(row for row in targets["categories"]["relics"]
                 if row["content_id"] == "PRISMATIC_SHARD")
    assert shard["policy_acquisition"] == "EXCLUDED"


def test_coverage_builder_uses_target_inventory_without_claiming_parity() -> None:
    targets = _targets()
    targets["authority"] = {
        "stock_jar_sha256": "stock-sha",
        "native_source_sha256": native_source_digest(),
    }
    bytecode = {
        "stock_jar_sha256": "stock-sha",
        "categories": {
            category: [{"content_id": item["content_id"], "stock_classes": []}
                       for item in rows]
            for category, rows in targets["categories"].items()
            if category != "encounters"
        },
    }
    obligations = build_obligations(bytecode, targets=targets)
    ids = {(row["category"], row["content_id"])
           for row in obligations["obligations"]}
    assert ("events", "NOTE_FOR_YOURSELF") not in ids
    assert ("events", "SECRET_PORTAL") not in ids
    assert ("encounters", "SLIME_BOSS") in ids
    assert ("encounters", "AUTOMATON") not in ids
    assert all(row["status"] == "UNREVIEWED" for row in obligations["obligations"])

    targets["authority"]["native_source_sha256"] = "stale"
    with pytest.raises(ValueError, match="native source hash is stale"):
        build_obligations(bytecode, targets=targets)


def test_coverage_builder_rejects_a_missing_target_category() -> None:
    targets = _targets()
    targets["authority"] = {
        "stock_jar_sha256": "stock-sha",
        "native_source_sha256": native_source_digest(),
    }
    with pytest.raises(ValueError, match="lacks target categories"):
        build_obligations(
            {"stock_jar_sha256": "stock-sha", "categories": {}},
            targets=targets,
        )


@pytest.mark.local_evidence
def test_stock_act1_event_pool_order_matches_native_when_projection_is_available() -> None:
    if not STOCK_SOURCE.exists():
        pytest.skip("local stock decompilation projection is unavailable")
    backend = SimulatorBackend(IRONCLAD_A20_ACT1)
    backend.reset(0)
    pools = backend.raw_state["ordered_pools"]
    assert compare_stock_event_pools(STOCK_SOURCE, pools) == {
        "events": 11, "shrines": 6, "special_one_time_events": 13,
    }
    changed = {**pools, "events": list(reversed(pools["events"]))}
    with pytest.raises(ValueError, match="order differs"):
        compare_stock_event_pools(STOCK_SOURCE, changed)


@pytest.mark.local_evidence
def test_stock_act1_encounter_tables_match_native_when_projection_is_available(
    tmp_path: Path,
) -> None:
    stock = STOCK_SOURCE / "dungeons/Exordium.java"
    if not stock.exists():
        pytest.skip("local stock decompilation projection is unavailable")
    native = Path(__file__).resolve().parents[2] / "native/simulator/include/constants/MonsterEncounters.h"
    assert compare_stock_act1_encounters(stock, native) == {
        "weak": 4, "strong": 10, "elite": 3,
    }
    changed = tmp_path / "encounters.h"
    changed.write_text(
        native.read_text(encoding="utf-8").replace("ME::GREMLIN_GANG", "ME::CULTIST", 1),
        encoding="utf-8",
    )
    with pytest.raises(ValueError, match="order differs"):
        compare_stock_act1_encounters(stock, changed)


@pytest.mark.local_evidence
def test_stock_ironclad_potion_draw_order_matches_native(
    tmp_path: Path,
) -> None:
    if not STOCK_SOURCE.exists():
        pytest.skip("local stock decompilation projection is unavailable")
    native = Path(__file__).resolve().parents[2] / "native/simulator/include/constants/Potions.h"
    assert compare_stock_ironclad_potion_pool(STOCK_SOURCE, native) == 33
    changed = tmp_path / "Potions.h"
    changed.write_text(
        native.read_text(encoding="utf-8").replace(
            "Potion::BLOOD_POTION, Potion::ELIXIR_POTION",
            "Potion::ELIXIR_POTION, Potion::BLOOD_POTION",
            1,
        ),
        encoding="utf-8",
    )
    with pytest.raises(ValueError, match="order differs"):
        compare_stock_ironclad_potion_pool(STOCK_SOURCE, changed)
