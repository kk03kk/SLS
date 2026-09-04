from __future__ import annotations

import pytest

from sls.audit.semantic_coverage import (
    COVERAGE_SCHEMA,
    require_semantic_training_gate,
    validate_semantic_coverage,
)


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
