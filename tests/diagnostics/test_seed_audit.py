from __future__ import annotations

from pathlib import Path

import pytest
import torch

from sls.curriculum import IRONCLAD_A0_ACT1, IRONCLAD_A20_ACT1
from sls.model import ENCODING_SCHEMA, ModelConfig, Policy
from sls.runtime import load_policy_artifact
from sls.runtime.artifact import save_policy_artifact
from tools import audit_policy_seed as seed_audit_tool
from tools.audit_policy_seed import audit_policy_seed


@pytest.mark.local_evidence
def test_seed_audit_reproduces_signed_run_and_records_counterfactual() -> None:
    artifact = Path(__file__).resolve().parents[2] / "runs/archives/policies/ironclad-a0-act1-5m.pt"
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


@pytest.mark.parametrize("profile", [IRONCLAD_A0_ACT1, IRONCLAD_A20_ACT1])
def test_current_seed_audit_uses_artifact_environment_and_signed_seed(tmp_path, monkeypatch, profile):
    # Generated test weights verify audit mechanics, never training quality.
    with torch.random.fork_rng():
        torch.manual_seed(17)
        model = Policy(ModelConfig(embedding_dim=16, transformer_layers=1,
                                  attention_heads=4, feedforward_dim=32,
                                  recurrent_hidden_dim=16))
    artifact = save_policy_artifact(
        model, tmp_path / 'policy.pt', goal='ACT1',
        ascension_min=profile.ascension, ascension_max=profile.ascension,
        provenance={'profile': profile, 'git_commit': 'synthetic-test',
                    'native_source_sha256': 'synthetic-test', 'training_config_sha256': 'synthetic-test'},
    )
    observed_profiles = []
    backend_type = seed_audit_tool.SimulatorBackend

    def record_backend(actual_profile):
        observed_profiles.append(actual_profile)
        return backend_type(actual_profile)

    monkeypatch.setattr(seed_audit_tool, 'SimulatorBackend', record_backend)
    signed = audit_policy_seed(artifact, -1466613676819842358)
    unsigned = audit_policy_seed(artifact, 16980130396889709258)
    assert signed['schema'] == 'sls-policy-seed-audit-v2'
    assert signed['environment']['profile_id'] == profile.profile_id
    assert signed['environment']['ascension'] == profile.ascension
    assert signed['native_seed_bits'] == unsigned['native_seed_bits']
    assert signed['baseline'] == unsigned['baseline']
    assert signed['block_deficit_counterfactual'] == unsigned['block_deficit_counterfactual']
    assert signed['baseline']['actions'] > 0
    assert observed_profiles == [profile] * 4
