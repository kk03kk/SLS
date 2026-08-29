from __future__ import annotations

from pathlib import Path

import pytest

from tools import benchmark_workers


def test_benchmark_defaults_to_policy_transfer_gate(monkeypatch) -> None:
    args = benchmark_workers._parser().parse_args([])
    assert args.transfer_gate == benchmark_workers.TRANSFER_GATE
    assert not args.allow_dirty

    call = {}

    def verify(path: Path, *, profile_id: str):
        call.update({"path": path, "profile_id": profile_id})
        return {"schema": "sls-policy-transfer-v1"}

    monkeypatch.setattr(benchmark_workers, "verify_policy_transfer_gate", verify)
    assert benchmark_workers._verify_benchmark_readiness(args)["schema"]
    assert call == {
        "path": benchmark_workers.TRANSFER_GATE,
        "profile_id": "IRONCLAD_A0_ACT1",
    }


def test_benchmark_allows_explicit_transfer_gate(
    tmp_path: Path, monkeypatch,
) -> None:
    gate = tmp_path / "policy-transfer.json"
    args = benchmark_workers._parser().parse_args(["--transfer-gate", str(gate)])
    call = {}

    def verify(path: Path, *, profile_id: str):
        call.update({"path": path, "profile_id": profile_id})
        return {"schema": "sls-policy-transfer-v1"}

    monkeypatch.setattr(benchmark_workers, "verify_policy_transfer_gate", verify)
    benchmark_workers._verify_benchmark_readiness(args)
    assert call == {"path": gate, "profile_id": "IRONCLAD_A0_ACT1"}


def test_benchmark_propagates_gate_profile_errors(monkeypatch) -> None:
    args = benchmark_workers._parser().parse_args([])
    def reject(*_args, **_kwargs):
        raise ValueError("does not cover IRONCLAD_A0_ACT1")
    monkeypatch.setattr(benchmark_workers, "verify_policy_transfer_gate", reject)
    with pytest.raises(ValueError, match="does not cover"):
        benchmark_workers._verify_benchmark_readiness(args)
