from __future__ import annotations

from pathlib import Path

import pytest

from tools import benchmark_workers


def test_experimental_benchmark_does_not_require_policy_transfer(monkeypatch) -> None:
    args = benchmark_workers._parser().parse_args(["--mode", "experimental"])
    assert args.transfer_gate == benchmark_workers.TRANSFER_GATE
    assert not args.allow_dirty
    monkeypatch.setattr(
        benchmark_workers, "git_state", lambda: {"dirty": False, "commit": "test"},
    )
    result = benchmark_workers._verify_benchmark_readiness(args)
    assert result["training_mode"] == "EXPERIMENTAL"
    assert result["policy_transfer_verified"] is False


def test_benchmark_allows_explicit_transfer_gate(
    tmp_path: Path, monkeypatch,
) -> None:
    gate = tmp_path / "policy-transfer.json"
    args = benchmark_workers._parser().parse_args([
        "--mode", "production", "--transfer-gate", str(gate),
    ])
    call = {}

    def verify(path: Path, *, profile_id: str, require_canary: bool):
        call.update({
            "path": path, "profile_id": profile_id,
            "require_canary": require_canary,
        })
        return {"schema": "sls-policy-transfer-v1"}

    monkeypatch.setattr(benchmark_workers, "git_state", lambda: {"dirty": False})
    monkeypatch.setattr(benchmark_workers, "verify_policy_transfer_gate", verify)
    benchmark_workers._verify_benchmark_readiness(args)
    assert call == {
        "path": gate, "profile_id": "IRONCLAD_A0_ACT1",
        "require_canary": True,
    }


def test_benchmark_propagates_gate_profile_errors(monkeypatch) -> None:
    args = benchmark_workers._parser().parse_args(["--mode", "production"])
    def reject(*_args, **_kwargs):
        raise ValueError("does not cover IRONCLAD_A0_ACT1")
    monkeypatch.setattr(benchmark_workers, "verify_policy_transfer_gate", reject)
    monkeypatch.setattr(benchmark_workers, "git_state", lambda: {"dirty": False})
    with pytest.raises(ValueError, match="does not cover"):
        benchmark_workers._verify_benchmark_readiness(args)
