from __future__ import annotations

from tools import benchmark_workers


def test_benchmark_defaults_are_training_only() -> None:
    args = benchmark_workers._parser().parse_args([])
    assert args.workers == (8, 16, 24, 32)
    assert args.rounds == 3
