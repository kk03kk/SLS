import pytest

from sls.audit.semantic_coverage import COVERAGE_SCHEMA, validate_semantic_coverage
from tools.audit_act1_mechanics import (
    attach_branch_evidence,
    normalized_state,
    normalized_values,
)


def test_java_unsigned_rng_state_is_preserved_without_float_conversion():
    result = normalized_state({"counter": 9, "seed0": str(2**64 - 1), "seed1": str(2**63)})
    assert result == {"counter": 9, "seed0": 2**64 - 1, "seed1": 2**63}


def test_json_float32_roundtrip_is_compared_by_bits():
    assert normalized_values({"unit_float": 0.5916757}) == normalized_values({"unit_float": 0.5916756987571716})
    assert normalized_values({"random_long": -4163609976294081632})["random_long"] == -4163609976294081632


def _evidence():
    coverage = {"schema": COVERAGE_SCHEMA, "stock_jar_sha256": "a" * 64,
                "native_source_sha256": "b" * 64,
                "obligations": [{"obligation_id": "systems:RNG_STREAMS:all-branches",
                                 "category": "systems", "content_id": "RNG_STREAMS", "status": "UNREVIEWED"}]}
    result = {"passed": True, "stock_jar_sha256": "a" * 64, "native_source_sha256": "b" * 64,
              "native_artifact_sha256": "c" * 64, "probe_source_sha256": "d" * 64,
              "comparisons_sha256": "e" * 64}
    return coverage, result


def test_branch_evidence_remains_blocking_and_preserves_the_baseline():
    coverage, result = _evidence()
    reviewed = attach_branch_evidence(coverage, result)
    assert reviewed["obligations"][0]["status"] == "BRANCH_PARTIAL"
    assert coverage["obligations"][0]["status"] == "UNREVIEWED"
    assert validate_semantic_coverage(reviewed)["ready_for_training"] is False
    coverage["obligations"][0]["status"] = "SEMANTIC_DIFFERENCE"
    assert attach_branch_evidence(coverage, result)["obligations"][0]["status"] == "SEMANTIC_DIFFERENCE"


@pytest.mark.parametrize("field,value", [("passed", False), ("native_source_sha256", "f" * 64)])
def test_stale_or_failed_branch_evidence_is_rejected(field, value):
    coverage, result = _evidence()
    result[field] = value
    with pytest.raises(ValueError):
        attach_branch_evidence(coverage, result)
