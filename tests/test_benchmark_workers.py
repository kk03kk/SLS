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


def test_fresh_clone_experimental_benchmark_never_reads_missing_gate(
    tmp_path: Path, monkeypatch,
) -> None:
    missing = tmp_path / "runs" / "policy_transfer_v1.json"
    args = benchmark_workers._parser().parse_args([
        "--mode", "experimental", "--transfer-gate", str(missing),
    ])
    monkeypatch.setattr(
        benchmark_workers, "git_state", lambda: {"dirty": False, "commit": "test"},
    )
    safety = benchmark_workers._benchmark_safety(args)
    assert not missing.exists()
    assert safety == {
        "training_mode": benchmark_workers.TrainingMode.EXPERIMENTAL,
        "policy_transfer_verified": False,
        "transfer_gate_sha256": "EXPERIMENTAL_UNVERIFIED",
        "transfer_gate_schema": None,
    }


def test_benchmark_allows_explicit_transfer_gate(
    tmp_path: Path, monkeypatch,
) -> None:
    gate = tmp_path / "policy-transfer.json"
    gate.write_bytes(b"verified production gate")
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
    safety = benchmark_workers._benchmark_safety(args)
    assert call == {
        "path": gate, "profile_id": "IRONCLAD_A0_ACT1",
        "require_canary": True,
    }
    assert safety["training_mode"] is benchmark_workers.TrainingMode.PRODUCTION
    assert safety["policy_transfer_verified"] is True
    assert safety["transfer_gate_schema"] == "sls-policy-transfer-v1"


def test_fresh_clone_production_benchmark_rejects_missing_gate(
    tmp_path: Path, monkeypatch,
) -> None:
    missing = tmp_path / "runs" / "policy_transfer_v1.json"
    args = benchmark_workers._parser().parse_args([
        "--mode", "production", "--transfer-gate", str(missing),
    ])
    monkeypatch.setattr(
        benchmark_workers, "git_state", lambda: {"dirty": False, "commit": "test"},
    )
    with pytest.raises(FileNotFoundError):
        benchmark_workers._benchmark_safety(args)


def test_benchmark_propagates_gate_profile_errors(monkeypatch) -> None:
    args = benchmark_workers._parser().parse_args(["--mode", "production"])
    def reject(*_args, **_kwargs):
        raise ValueError("does not cover IRONCLAD_A0_ACT1")
    monkeypatch.setattr(benchmark_workers, "verify_policy_transfer_gate", reject)
    monkeypatch.setattr(benchmark_workers, "git_state", lambda: {"dirty": False})
    with pytest.raises(ValueError, match="does not cover"):
        benchmark_workers._verify_benchmark_readiness(args)
