from __future__ import annotations

import json
from pathlib import Path

import pytest

from sls.backends.simulator import native

FIXTURE = json.loads((Path(__file__).parents[1] / "fixtures/act1-transform-selection-stock.json").read_text())


@pytest.mark.parametrize("case", FIXTURE["cases"], ids=lambda row: f"{row['kind']}-{row['excluded']}")
def test_transform_selection_matches_stock_filtered_list(case) -> None:
    result = native.transform_selection_probe(case["seed"], case["kind"], case["excluded"])
    assert result["selected"] == case["selected"]
    assert result["selected"] != case["excluded"]
    assert result["final"]["counter"] == 1


@pytest.mark.parametrize("alive,blocks,counter", [
    ([True], [11], 0),
    ([True, False, False], [11, 0, 0], 0),
    ([True, True, False], [0, 11, 0], 1),
    ([True, False, True], [0, 0, 11], 1),
])
def test_shield_gremlin_block_targets_match_stock_boundaries(alive, blocks, counter) -> None:
    result = native.act1_gremlin_block_probe(0, alive)
    assert result["blocks"] == blocks
    assert result["final"]["counter"] == counter
