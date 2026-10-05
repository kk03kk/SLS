"""Reviewed rule corrections permit fresh weight transfer, never exact resume."""

import json
from pathlib import Path

import pytest

from sls.rl.act1_transfer import validate_curriculum_simulator_transition
from sls.rl.training_contract import sha256_file


@pytest.fixture
def reviewed(tmp_path):
    before, after = tmp_path / "before.json", tmp_path / "after.json"
    before.write_text('{"fixture": "before"}')
    after.write_text('{"fixture": "after"}')
    evidence = {"schema": "sls-curriculum-simulator-transition-v1", "status": "VERIFIED_RULE_CORRECTION",
                "source_native_source_sha256": "a" * 64, "target_native_source_sha256": "b" * 64,
                "transfer_mode": "weights-only", "exact_resume_allowed": False,
                "semantics_revision": "synthetic-unit-fixture-v1",
                "evidence": {p.name: sha256_file(p) for p in (before, after)}}
    path = tmp_path / "transition.json"
    path.write_text(json.dumps(evidence))
    spec = {"transfer_kind": "curriculum-stage",
            "simulator_transition": {"evidence": path.name, "evidence_sha256": sha256_file(path)}}
    return spec, path, evidence


def test_unchanged_simulator_needs_no_migration(tmp_path):
    assert validate_curriculum_simulator_transition({}, "a" * 64, "a" * 64, root=tmp_path) is None


def test_changed_rules_require_explicit_review(tmp_path):
    with pytest.raises(ValueError, match="explicit"):
        validate_curriculum_simulator_transition({}, "a" * 64, "b" * 64, root=tmp_path)


def test_reviewed_weights_only_transition(reviewed, tmp_path):
    spec, _, expected = reviewed
    assert validate_curriculum_simulator_transition(spec, "a" * 64, "b" * 64, root=tmp_path) == expected


@pytest.mark.parametrize("change", ["hash", "source", "target", "exact", "proof", "kind"])
def test_unreviewed_changes_rejected(reviewed, tmp_path, change):
    spec, path, evidence = reviewed
    if change == "hash":
        spec["simulator_transition"]["evidence_sha256"] = "0" * 64
    elif change == "proof":
        (tmp_path / "after.json").write_text('{"tampered": true}')
    elif change == "kind":
        spec["transfer_kind"] = "optimization-experiment"
    else:
        key = {"source": "source_native_source_sha256", "target": "target_native_source_sha256",
               "exact": "exact_resume_allowed"}[change]
        evidence[key] = True if change == "exact" else "c" * 64
        path.write_text(json.dumps(evidence))
        spec["simulator_transition"]["evidence_sha256"] = sha256_file(path)
    with pytest.raises(ValueError):
        validate_curriculum_simulator_transition(spec, "a" * 64, "b" * 64, root=tmp_path)


def test_real_snecko_transition_evidence_is_self_consistent():
    root = Path(__file__).resolve().parents[2]
    path = root / "docs/results/act12-qualification-20261004/simulator-transition.json"
    evidence = json.loads(path.read_text())
    spec = {"transfer_kind": "curriculum-stage", "simulator_transition": {
        "evidence": path.relative_to(root).as_posix(), "evidence_sha256": sha256_file(path),
    }}
    result = validate_curriculum_simulator_transition(spec, evidence["source_native_source_sha256"],
                                                     evidence["target_native_source_sha256"], root=root)
    assert result["semantics_revision"] == "sls-snecko-tail-whip-order-v1"
