from __future__ import annotations

import json
from pathlib import Path
import subprocess

import pytest

from sls.backends.simulator import IRONCLAD_A0_ACT1, SimulatorBackend
from sls.content.scope import ironclad_a0_scope_hash
from sls.model.encoding import ENCODING_SCHEMA, vocabulary_hash
from sls.rl.training_contract import ROOT, canonical_digest, native_source_digest
from sls.validation.transfer import compare_distributions, contract_differences
from sls.validation.transfer_gate import verify_policy_transfer_gate


def test_public_contract_comparison_excludes_rng_and_is_order_stable() -> None:
    first = SimulatorBackend(IRONCLAD_A0_ACT1).reset(7)
    second = SimulatorBackend(IRONCLAD_A0_ACT1).reset(7)
    assert contract_differences(first, second) == {}


def test_stochastic_comparison_uses_distribution_not_trajectory_order() -> None:
    result = compare_distributions("AABB" * 500, "BABA" * 500)
    assert result.accepted
    assert result.total_variation == 0.0
    shifted = compare_distributions("AAAA" * 500, "BBBB" * 500)
    assert not shifted.accepted
    assert shifted.total_variation == 1.0


def test_committed_transfer_gate_is_only_a_template() -> None:
    gate = ROOT / "configs/validation/policy_transfer_v1.json"
    with pytest.raises(ValueError, match="no evidence artifact"):
        verify_policy_transfer_gate(gate, profile_id="IRONCLAD_A0_ACT1")
    with pytest.raises(ValueError, match="does not cover"):
        verify_policy_transfer_gate(gate, profile_id="IRONCLAD_A0_ACT2")


def test_transfer_gate_rejects_a_validly_resigned_but_failed_evidence(
    tmp_path: Path,
) -> None:
    evidence = {
        "schema": "sls-policy-transfer-evidence-v1",
        "profile": "IRONCLAD_A0_ACT1",
        "encoding_schema": ENCODING_SCHEMA,
        "vocabulary_sha256": vocabulary_hash(),
        "content_scope_sha256": ironclad_a0_scope_hash(),
        "native_source_sha256": native_source_digest(),
        "git_commit": subprocess.run(
            ["git", "rev-parse", "HEAD"], cwd=ROOT, check=True,
            capture_output=True, text=True,
        ).stdout.strip(),
        "source_files": {},
        "public_contract": {"accepted": False},
    }
    evidence["evidence_sha256"] = canonical_digest(evidence)
    evidence_path = tmp_path / "evidence.json"
    evidence_path.write_text(json.dumps(evidence), encoding="utf-8")

    gate = json.loads(
        (ROOT / "configs/validation/policy_transfer_v1.json").read_text(
            encoding="utf-8"
        )
    )
    gate["evidence"] = str(evidence_path)
    gate["evidence_sha256"] = evidence["evidence_sha256"]
    gate.pop("gate_sha256")
    gate["gate_sha256"] = canonical_digest(gate)
    gate_path = tmp_path / "gate.json"
    gate_path.write_text(json.dumps(gate), encoding="utf-8")
    with pytest.raises(ValueError, match="not accepted"):
        verify_policy_transfer_gate(
            gate_path, profile_id="IRONCLAD_A0_ACT1",
            require_clean_repository=False,
        )
