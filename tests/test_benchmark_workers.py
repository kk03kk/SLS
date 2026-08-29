from __future__ import annotations

from tools import benchmark_workers


def test_benchmark_defaults_are_training_only() -> None:
    args = benchmark_workers._parser().parse_args([])
    assert args.layouts == benchmark_workers.DEFAULT_LAYOUTS
    assert args.rounds == 3


def test_layout_selection_uses_smallest_layout_within_95_percent_of_peak() -> None:
    rows = [
        {"workers": 16, "shards": 8, "decisions_per_second": 90.0},
        {"workers": 24, "shards": 8, "decisions_per_second": 96.0},
        {"workers": 32, "shards": 16, "decisions_per_second": 100.0},
    ]
    assert benchmark_workers.select_layout(rows) == (24, 8)
