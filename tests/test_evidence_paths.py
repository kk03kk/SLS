"""Relocation keeps pinned historical records readable and contained."""

from pathlib import Path

import pytest

from sls.evidence_paths import repository_evidence_path
from sls.rl.act1_transfer import validate_curriculum_simulator_transition
from sls.rl.training_contract import sha256_file


@pytest.mark.parametrize("old,new", [
    ("docs/results/run/data.json", "tests/fixtures/evidence/results/run/data.json"),
    ("docs/audits/run/data.json", "tests/fixtures/evidence/audits/run/data.json"),
    ("docs/training-runtime-contracts.md", "tests/fixtures/evidence/training-runtime-contracts.md"),
    ("runs/archives/model.pt", "local/archives/legacy/model.pt"),
])
def test_legacy_path_resolves_relative_and_absolute(tmp_path, old, new):
    expected = tmp_path / new
    assert repository_evidence_path(tmp_path, old) == expected
    assert repository_evidence_path(tmp_path, tmp_path / old) == expected


def test_unrelated_and_escape_paths(tmp_path):
    assert repository_evidence_path(tmp_path, "local/runs/output") == tmp_path / "local/runs/output"
    with pytest.raises(ValueError, match="within the repository"):
        repository_evidence_path(tmp_path, "../outside.json")
    with pytest.raises(ValueError, match="within the repository"):
        repository_evidence_path(tmp_path, tmp_path.parent / "outside.json")


def test_legacy_transition_still_verifies_hashes():
    import json

    root = Path(__file__).resolve().parents[1]
    legacy = "docs/results/act12-qualification-20261004/simulator-transition.json"
    relocated = repository_evidence_path(root, legacy)
    evidence = json.loads(relocated.read_text(encoding="utf-8"))
    spec = {"transfer_kind": "curriculum-stage", "simulator_transition": {
        "evidence": legacy, "evidence_sha256": sha256_file(relocated),
    }}
    source = evidence["source_native_source_sha256"]
    target = evidence["target_native_source_sha256"]
    assert validate_curriculum_simulator_transition(spec, source, target, root=root) == evidence
    spec["simulator_transition"]["evidence_sha256"] = "0" * 64
    with pytest.raises(ValueError, match="hash/path"):
        validate_curriculum_simulator_transition(spec, source, target, root=root)
