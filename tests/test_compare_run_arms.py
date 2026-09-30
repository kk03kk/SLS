"""Tests for the paired run-arm comparison used to decide reward experiments.

Comparing two runs' success rates independently wastes most of the available
power, so the paired path is the one that decides an experiment. These tests pin
the pairing, the block breakdown and the verdict thresholds.
"""

from __future__ import annotations

import sys
from pathlib import Path

import pytest

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "tools"))

from compare_run_arms import ArmEvaluation, exact_mcnemar, report, wilson  # noqa: E402


def _arm(label: str, outcomes: list[bool], *, offset: int = 0) -> ArmEvaluation:
    seeds = {offset + index: value for index, value in enumerate(outcomes)}
    return ArmEvaluation(label, f"synthetic:{label}", seeds, dict.fromkeys(seeds, 100), None)


def test_mcnemar_and_wilson_match_closed_forms() -> None:
    assert exact_mcnemar(0, 0) == 1.0
    assert exact_mcnemar(1, 9) == 22 / 1024
    low, high = wilson(50, 100)
    assert low < 0.5 < high
    assert abs((low + high) / 2 - 0.5) < 0.01


def test_identical_arms_report_no_difference() -> None:
    outcomes = [True] * 70 + [False] * 30
    record = report(_arm("A", outcomes), _arm("B", outcomes), 512)
    assert record["net"] == 0
    assert record["exact_mcnemar_p"] == 1.0
    assert "no significant difference" in record["verdict"]


def test_a_large_paired_effect_is_detected() -> None:
    left = [True] * 300 + [False] * 212
    # 40 seeds flip from loss to win, 4 from win to loss: a real improvement.
    right = left[:]
    for index in range(300, 340):
        right[index] = True
    for index in range(4):
        right[index] = False
    record = report(_arm("A", left), _arm("B", right), 512)
    assert record["net"] == 36
    assert record["exact_mcnemar_p"] < 0.001
    assert "B is better" in record["verdict"]
    assert record["results"]["B"]["wins"] == record["results"]["A"]["wins"] + 36


def test_block_breakdown_exposes_seed_block_differences() -> None:
    # A policy evaluated on two blocks that differ in difficulty: the pooled
    # number hides it, the block table does not.
    outcomes = [True] * 400 + [False] * 112
    record = report(_arm("A", outcomes), _arm("B", outcomes), 256)
    assert len(record["blocks"]) == 2
    assert record["blocks"][0]["left_wins"] != record["blocks"][1]["left_wins"]
    assert sum(row["left_wins"] for row in record["blocks"]) == 400


def test_pairing_rejects_partial_seed_overlap_and_invalid_block_size() -> None:
    with pytest.raises(ValueError, match="identical complete"):
        report(_arm("A", [True, False]), _arm("B", [True, False], offset=1), 512)
    with pytest.raises(ValueError, match="positive"):
        report(_arm("A", [True]), _arm("B", [True]), 0)


def test_seed_loader_rejects_duplicates_and_non_boolean_outcomes() -> None:
    from compare_run_arms import _from_seed_results

    row = {"seed": 1, "success": True, "steps": 100}
    with pytest.raises(ValueError, match="duplicate"):
        _from_seed_results("A", "synthetic", [row, row])
    with pytest.raises(ValueError, match="boolean"):
        _from_seed_results("A", "synthetic", [{**row, "success": "false"}])


def test_small_trailing_block_is_preserved() -> None:
    record = report(_arm("A", [True] * 513), _arm("B", [True] * 513), 512)
    assert sum(row["n"] for row in record["blocks"]) == 513
