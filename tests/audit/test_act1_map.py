from __future__ import annotations

from pathlib import Path

import pytest

from sls.audit.act1_map import (
    check_act1_map_structure,
    compare_stock_act1_map_constants,
)
from sls.backends.simulator import SimulatorBackend
from sls.curriculum import IRONCLAD_A20_ACT1

ROOT = Path(__file__).resolve().parents[2]
STOCK_EXORDIUM = (
    ROOT / "local/audits/stock-decompilation-tree/desktop-1.0/source"
    / "com/megacrit/cardcrawl/dungeons/Exordium.java"
)
NATIVE_MAP = ROOT / "native/simulator/src/game/Map.cpp"


@pytest.mark.local_evidence
def test_act1_map_chances_match_stock_projection(tmp_path: Path) -> None:
    if not STOCK_EXORDIUM.exists():
        pytest.skip("stock Java projection is unavailable")
    assert compare_stock_act1_map_constants(STOCK_EXORDIUM, NATIVE_MAP) == {
        "shop": 0.05, "rest": 0.12, "treasure": 0.0,
        "event": 0.22, "elite": 0.08,
    }
    changed = tmp_path / "Map.cpp"
    changed.write_text(
        NATIVE_MAP.read_text(encoding="utf-8").replace(
            "SHOP_ROOM_CHANCE = 0.05F", "SHOP_ROOM_CHANCE = 0.06F", 1,
        ),
        encoding="utf-8",
    )
    with pytest.raises(ValueError, match="map chance differs: shop"):
        compare_stock_act1_map_constants(STOCK_EXORDIUM, changed)


def test_act1_maps_respect_stock_room_boundaries() -> None:
    backend = SimulatorBackend(IRONCLAD_A20_ACT1)
    for seed in range(32):
        backend.reset(seed)
        counts = check_act1_map_structure(backend.raw_state["public_map"])
        assert counts["ELITE"] >= 1
        assert counts["TREASURE"] >= 1
