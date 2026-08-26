from __future__ import annotations

from pathlib import Path

from sls.rl.training_contract import (
    ACT1_PRODUCTION_READINESS_LEVEL, ACT1_PRODUCTION_READINESS_LOCK,
)
from tools import benchmark_workers


def test_benchmark_defaults_to_strict_training_readiness_lock(monkeypatch) -> None:
    args = benchmark_workers._parser().parse_args([])
    assert args.readiness_lock == ACT1_PRODUCTION_READINESS_LOCK
    assert args.readiness_level == ACT1_PRODUCTION_READINESS_LEVEL
    assert not args.allow_dirty

    call = {}

    def verify(path: Path, *, require_clean: bool, expected_level: str):
        call.update({
            "path": path,
            "require_clean": require_clean,
            "expected_level": expected_level,
        })
        return {"valid": True, "level": ACT1_PRODUCTION_READINESS_LEVEL, "lock_sha256": "strict"}

    monkeypatch.setattr(benchmark_workers, "verify_readiness_lock", verify)
    assert benchmark_workers._verify_benchmark_readiness(args)["valid"]
    assert call == {
        "path": ACT1_PRODUCTION_READINESS_LOCK,
        "require_clean": True,
        "expected_level": ACT1_PRODUCTION_READINESS_LEVEL,
    }


def test_benchmark_allows_explicit_lock_and_level_without_implicit_downgrade(
    tmp_path: Path, monkeypatch,
) -> None:
    lock = tmp_path / "explicit-training-lock.json"
    args = benchmark_workers._parser().parse_args([
        "--readiness-lock", str(lock),
        "--readiness-level", ACT1_PRODUCTION_READINESS_LEVEL,
    ])
    call = {}

    def verify(path: Path, *, require_clean: bool, expected_level: str):
        call.update({"path": path, "level": expected_level, "clean": require_clean})
        return {"valid": True, "level": ACT1_PRODUCTION_READINESS_LEVEL, "lock_sha256": "strict"}

    monkeypatch.setattr(benchmark_workers, "verify_readiness_lock", verify)
    benchmark_workers._verify_benchmark_readiness(args)
    assert call == {
        "path": lock, "level": ACT1_PRODUCTION_READINESS_LEVEL, "clean": True,
    }
