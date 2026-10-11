import pytest

from tools.qualify_act12_foundation import timing_contract_row


def conditional():
    return {"seed": 1, "status": "CONDITIONAL_SYSTEM_BRANCH_MATCH", "first_divergence": None,
            "terminal_reason_verified": True, "stock_action_unavailable": None,
            "controlled_runtime_inputs": [{"boundary": 10, "stock_measured_inputs": {
                "discovery_retrieval_updates": 15}}]}


def test_complete_independent_clock_evidence_can_explain_but_not_rename_difference():
    original = {"seed": 1, "status": "SEMANTIC_DIFFERENCE"}
    assert timing_contract_row(original, conditional(), natural=False, source="source")
    assert original["status"] == "SEMANTIC_DIFFERENCE"


@pytest.mark.parametrize("changes", [
    {"status": "INCOMPLETE_PREFIX_MATCH"}, {"terminal_reason_verified": False},
    {"controlled_runtime_inputs": []}, {"stock_action_unavailable": 12},
    {"controlled_runtime_inputs": [{"stock_measured_inputs": {"unknown": 15}}]},
])
def test_missing_prefix_or_other_runtime_changes_cannot_clear_gate(changes):
    report = conditional()
    report.update(changes)
    assert not timing_contract_row({"seed": 1}, report, natural=False, source="source")
