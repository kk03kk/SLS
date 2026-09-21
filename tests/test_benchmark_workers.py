from __future__ import annotations

from sls.rl.preparation import fixed_worker_layout
from tools import benchmark_workers
from tools.prepare_and_train import PRODUCTION_LAYOUTS


def test_benchmark_defaults_are_training_only() -> None:
    args = benchmark_workers._parser().parse_args([])
    assert args.layouts == benchmark_workers.DEFAULT_LAYOUTS
    assert args.rounds == 3


def test_production_layouts_exercise_all_allocated_cpu_shards() -> None:
    assert "64:8" in PRODUCTION_LAYOUTS
    assert "64:16" in PRODUCTION_LAYOUTS
    assert "128:16" in PRODUCTION_LAYOUTS


def test_layout_selection_uses_smallest_layout_within_95_percent_of_peak() -> None:
    rows = [
        {"workers": 16, "shards": 8, "decisions_per_second": 90.0},
        {"workers": 24, "shards": 8, "decisions_per_second": 96.0},
        {"workers": 32, "shards": 16, "decisions_per_second": 100.0},
    ]
    assert benchmark_workers.select_layout(rows) == (24, 8)


def test_fixed_worker_layout_is_explicit_and_validated() -> None:
    config = {"run": {"worker_layout": [64, 16]}}
    assert fixed_worker_layout(config) == (64, 16)
    assert fixed_worker_layout({"run": {}}) is None
    for invalid in ([64], [16, 64], [0, 0], "64:16"):
        config["run"]["worker_layout"] = invalid
        try:
            fixed_worker_layout(config)
        except ValueError:
            pass
        else:
            raise AssertionError(f"invalid worker layout accepted: {invalid!r}")
