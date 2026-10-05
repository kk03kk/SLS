from __future__ import annotations

import pytest

from sls.audit.act1_targets import PROFILE_ID
from sls.audit.act1_targets import SCHEMA as TARGET_SCHEMA
from sls.audit.semantic_coverage import (
    COVERAGE_SCHEMA,
    require_semantic_training_gate,
    validate_semantic_coverage,
)
from sls.content.scope import ironclad_scope


def _matched() -> dict[str, object]:
    return {
        "schema": COVERAGE_SCHEMA,
        "obligations": [{
            "obligation_id": "card:ANGER:use:base",
            "category": "cards",
            "content_id": "ANGER",
            "status": "SEMANTIC_MATCH",
            "stock_evidence": {"artifact_sha256": "a" * 64},
            "simulator_evidence": {"source_sha256": "b" * 64},
            "comparisons": {
                "before": True, "actions": True, "after": True, "rng": True,
            },
        }],
    }


def test_complete_independent_obligation_passes_gate() -> None:
    result = validate_semantic_coverage(_matched())
    assert result["ready_for_training"] is True
    require_semantic_training_gate(_matched())


def test_partial_branch_blocks_training() -> None:
    payload = _matched()
    payload["obligations"][0]["status"] = "BRANCH_PARTIAL"  # type: ignore[index]
    with pytest.raises(ValueError, match="semantic parity gate failed"):
        require_semantic_training_gate(payload)


def test_match_cannot_reuse_common_mode_evidence() -> None:
    payload = _matched()
    evidence = {"artifact_sha256": "a" * 64, "source_sha256": "b" * 64}
    payload["obligations"][0]["stock_evidence"] = evidence  # type: ignore[index]
    payload["obligations"][0]["simulator_evidence"] = evidence  # type: ignore[index]
    with pytest.raises(ValueError, match="evidence must be independent"):
        validate_semantic_coverage(payload)


def test_training_migration_rejects_an_incomplete_green_manifest() -> None:
    with pytest.raises(ValueError, match="MISSING"):
        require_semantic_training_gate(_matched(), require_scope_complete=True)


@pytest.mark.parametrize("status", ["SEMANTIC_UI_FOLD", "PRESENTATION_ONLY"])
def test_ui_classification_cannot_bypass_independent_evidence(status: str) -> None:
    payload = _matched()
    row = payload["obligations"][0]
    row["status"] = status
    row["rationale"] = "Only the animation is omitted; decision state is unchanged."
    row.pop("stock_evidence")
    with pytest.raises(ValueError, match="stock evidence is missing"):
        validate_semantic_coverage(payload)


@pytest.mark.parametrize("status", ["SEMANTIC_UI_FOLD", "PRESENTATION_ONLY"])
def test_ui_classification_requires_rationale_and_rng_comparison(status: str) -> None:
    payload = _matched()
    row = payload["obligations"][0]
    row["status"] = status
    with pytest.raises(ValueError, match="needs a rationale"):
        validate_semantic_coverage(payload)
    row["rationale"] = "The mandatory intro has no decision."
    row["comparisons"]["rng"] = False
    with pytest.raises(ValueError, match="did not pass"):
        validate_semantic_coverage(payload)


def test_mixed_semantic_and_presentation_rows_remain_semantic() -> None:
    payload = _matched()
    row = dict(payload["obligations"][0])
    row.update(obligation_id="card:ANGER:render", status="PRESENTATION_ONLY",
               rationale="Rendering has no gameplay effect.")
    payload["obligations"].append(row)
    assert validate_semantic_coverage(payload)["content"][0]["status"] == "SEMANTIC_MATCH"


def test_stale_evidence_and_invalid_hash_cannot_pass() -> None:
    payload = _matched()
    payload["native_source_sha256"] = "c" * 64
    with pytest.raises(ValueError, match="does not match native_source"):
        validate_semantic_coverage(payload)
    payload.pop("native_source_sha256")
    payload["obligations"][0]["stock_evidence"]["artifact_sha256"] = "not-a-hash"
    with pytest.raises(ValueError, match="stock evidence is missing"):
        validate_semantic_coverage(payload)


def test_act1_gate_checks_declared_targets_instead_of_all_acts() -> None:
    from sls.audit.semantic_coverage import REQUIRED_SYSTEM_OBLIGATIONS

    payload = _matched()
    payload.update(scope_id=PROFILE_ID, scope_sha256=ironclad_scope(20)["scope_sha256"],
                   stock_jar_sha256="a" * 64, native_source_sha256="b" * 64)
    categories = ("cards", "potions", "relics", "events", "encounters", "monsters")
    targets = {
        "schema": TARGET_SCHEMA, "profile_id": PROFILE_ID,
        "scope_sha256": payload["scope_sha256"],
        "authority": {"stock_jar_sha256": "a" * 64, "native_source_sha256": "b" * 64},
        "categories": {name: [{"content_id": "TARGET"}] for name in categories},
    }
    base = payload["obligations"][0]
    payload["obligations"] = [
        dict(base, obligation_id=f"{name}:{item}", category=name, content_id=item)
        for name, item in [*( (name, "TARGET") for name in categories),
                           *(("systems", item) for item in REQUIRED_SYSTEM_OBLIGATIONS)]
    ]
    with pytest.raises(ValueError, match="requires its target inventory"):
        require_semantic_training_gate(payload, require_scope_complete=True)
    with pytest.raises(ValueError, match="method-obligation baseline"):
        require_semantic_training_gate(payload, require_scope_complete=True, targets=targets)
    baseline = dict(payload, obligations=[dict(row, status="UNREVIEWED")
                                         for row in payload["obligations"]])
    require_semantic_training_gate(payload, require_scope_complete=True, targets=targets,
                                   baseline=baseline)
    baseline["obligations"].append(dict(base, obligation_id="cards:TARGET:extra-method",
                                       category="cards", content_id="TARGET", status="UNREVIEWED"))
    with pytest.raises(ValueError, match="extra-method=MISSING"):
        require_semantic_training_gate(payload, require_scope_complete=True, targets=targets,
                                       baseline=baseline)
    baseline["obligations"].pop()
    targets["categories"]["cards"].append({"content_id": "MISSING_CARD"})
    with pytest.raises(ValueError, match="MISSING_CARD=MISSING"):
        require_semantic_training_gate(payload, require_scope_complete=True, targets=targets,
                                       baseline=baseline)
