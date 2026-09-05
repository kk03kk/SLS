import json
from pathlib import Path

import pytest

from sls.rl.training_contract import (
    training_validation_digest,
    validate_training_sources,
)
from tools.check_training_sources import diagnose

# Exact metadata supplied from the NUS manifest after job 821880 failed.
# This deliberately differs from the local CPU verification's source digest.
NUS_REPORT = {
    "source_tree_sha256": "8be318932c332ad8a7fba2eb7408b7ed80623176ace76636aea1ac0ec530c1e2",
    "git": {"branch": "main", "commit": "8a34674fe185cbb77c228d129df8993386675305", "dirty": False},
}


def test_real_nus_validation_report_is_reusable():
    assert validate_training_sources(NUS_REPORT).startswith("reviewed-transition:")


@pytest.mark.parametrize("git_change", [{"dirty": True}, {"commit": "unknown"}])
def test_server_transition_is_bound_to_reported_clean_source_revision(git_change):
    with pytest.raises(ValueError, match="does not match"):
        validate_training_sources({**NUS_REPORT, "git": {**NUS_REPORT["git"], **git_change}})


def test_unknown_evidence_has_actionable_error():
    with pytest.raises(ValueError) as captured:
        validate_training_sources({"source_tree_sha256": "not-reviewed"})
    assert "not-reviewed" in str(captured.value)
    assert training_validation_digest() in str(captured.value)
    assert "check_training_sources.py" in str(captured.value)


def test_source_diagnosis_does_not_modify_manifest(tmp_path: Path):
    manifest = tmp_path / "run-manifest.json"
    manifest.write_text(json.dumps({"initialization": NUS_REPORT}), encoding="utf-8")
    original = manifest.read_bytes()
    result = diagnose(manifest)
    assert result["ok"] is True
    assert result["old_source_tree_sha256"] == NUS_REPORT["source_tree_sha256"]
    assert manifest.read_bytes() == original
