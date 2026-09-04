from __future__ import annotations

from pathlib import Path

import pytest

from tools.audit_policy_seed import audit_policy_seed


def test_seed_audit_reproduces_signed_run_and_records_counterfactual() -> None:
    artifact = Path("model/ironclad-a0-act1-5m.pt")
    if not artifact.exists():
        pytest.skip("local exported Act1 policy is unavailable")

    result = audit_policy_seed(artifact, -1466613676819842358)

    assert result["native_seed_bits"] == 16980130396889709258
    assert result["baseline"]["actions"] == 196
    assert result["baseline"]["success"] is False
    assert result["block_deficit_counterfactual"]["success"] is True
    assert result["block_deficit_counterfactual"]["overrides"]
