import copy

import pytest

from sls.audit.act12_qualification import EXPECTED_NATURAL, assess


def evidence():
    source = "a" * 64
    system_rows = [{"seed": s, "status": "SYSTEM_BRANCH_MATCH", "first_divergence": None,
        "terminal_reason_verified": True, "stock_action_unavailable": None,
        "qualification_scope": "UNCONDITIONAL_STOCK_SYSTEM_REPLAY", "controlled_runtime_inputs": [],
        "act2_boundaries": 1, "reward_boundaries": 1, "checkpoint_replays": 4} for s in range(131100063, 131100072)]
    systems = [{"native_source_sha256": source, "stock_capture_sha256": str(i), "runs": system_rows[i*3:i*3+3]}
               for i in range(3)]
    replacement = {"native_source_sha256": source, "stock_capture_sha256": "replacement", "runs": [copy.deepcopy(system_rows[0])]}
    return dict(source=source, model_sha256="model", controlled={"native_source_sha256": source,
        "native_view": "production-public", "continue_after_divergence": False,
        "runs": [{"seed": s, "status": "HARNESS_MATCH", "first_divergence": None} for s in range(131100000, 131100063)]},
        systems=systems, conditional_systems=copy.deepcopy(systems), replacement=replacement,
        conditional_replacement=copy.deepcopy(replacement), natural=[{"seed": s, "native_source_sha256": source,
            "evaluation_environment": {"profile_id": "IRONCLAD_A20_ACT2", "frozen_model_sha256": "model"},
            "status": "TRAJECTORY_MATCH", "first_divergence": None, "terminal_reason_verified": True}
            for s in EXPECTED_NATURAL], conditional_natural=[],
        gremlin={"native_source_sha256": source, "runs": [{"seed": s, "status": "HARNESS_MATCH", "first_divergence": None}
            for s in range(132300000, 132300003)]}, boundaries={"native_source_sha256": source, "device": "cpu",
            "profile": {"profile_id": "IRONCLAD_A20_ACT2", "ascension": 20, "start_distribution": "NATURAL_RUN_START"},
            "training_updates": 0, "decisions": 10000, "continuous_restores": 100,
            "screen_counts": {"ACT1:MAP": 9000, "ACT2:COMBAT": 1000},
            "restore_screen_counts": {"ACT1:MAP": 70, "ACT2:COMBAT": 30},
            "controlled_multi_selection": [{"passed": True, "continuous_actions": 4}]})


def test_finite_independent_contract():
    data = evidence()
    result = assess(**data)
    assert result["controlled_obligations"] + result["system_obligations"] == 72
    assert result["natural_unconditional_matches"] == 8


@pytest.mark.parametrize("change", ["profile", "source", "missing", "duplicate", "subset", "terminal", "restore", "conditional_as_raw"])
def test_refuse_incomplete_or_relabelled_evidence(change):
    data = evidence()
    if change == "profile":
        data["boundaries"]["profile"]["ascension"] = 0
    elif change == "source":
        data["controlled"]["native_source_sha256"] = "b" * 64
    elif change == "missing":
        data["controlled"]["runs"].pop()
    elif change == "duplicate":
        data["natural"].append(data["natural"][0])
    elif change == "subset":
        data["controlled"]["runs"][0]["terminal_comparison_scope"] = "RESOURCE_SUBSET"
    elif change == "terminal":
        data["natural"][0]["terminal_reason_verified"] = False
    elif change == "restore":
        data["boundaries"]["screen_counts"]["ACT2:EVENT"] = 1
    elif change == "conditional_as_raw":
        data["systems"][1]["runs"][0]["controlled_runtime_inputs"] = [{"fake": 1}]
    with pytest.raises(ValueError):
        assess(**data)
