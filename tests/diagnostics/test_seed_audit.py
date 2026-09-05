from __future__ import annotations

from pathlib import Path

import pytest
import torch

from sls.model import ENCODING_SCHEMA
from sls.runtime import load_policy_artifact
from tools.audit_policy_seed import audit_policy_seed


def test_seed_audit_reproduces_signed_run_and_records_counterfactual() -> None:
    artifact = Path("model/ironclad-a0-act1-5m.pt")
    if not artifact.exists():
        pytest.skip("local exported Act1 policy is unavailable")

    metadata = torch.load(artifact, map_location="cpu", weights_only=False)["metadata"]
    if metadata["encoding_schema"] != ENCODING_SCHEMA:
        # Historical action counts belong to that artifact's input semantics.
        # Do not silently rebind it and evaluate a different policy.
        with pytest.raises(ValueError, match="encoding schema is incompatible"):
            load_policy_artifact(artifact)
        pytest.skip("historical policy is correctly rejected by current encoding")

    result = audit_policy_seed(artifact, -1466613676819842358)

    assert result["native_seed_bits"] == 16980130396889709258
    assert result["baseline"]["actions"] == 196
    assert result["baseline"]["success"] is False
    assert result["block_deficit_counterfactual"]["success"] is True
    assert result["block_deficit_counterfactual"]["overrides"]
