"""Operator gates and diagnostic interpretation, never synthetic training claims."""

import json

import pytest

from sls.rl.training_contract import sha256_file
from tools.analyze_act12_pilot import (
    late_success_windows,
    require_pilot_health,
    value_diagnostics,
)
from tools.import_act12_parent import import_parent
from tools.prepare_and_train import budget_estimate


def test_budget_gate_keeps_layout_and_accounts_for_evaluation():
    config = {"run": {"worker_layout": [64, 16], "preparation_wall_hours": 24,
                      "preparation_safety_factor": 1.5, "preparation_evaluation_reserve_hours": 4},
              "warm_start": {"parent_environment_steps": 90_013_696},
              "stages": {"train": {"target_environment_steps": 94_013_696}}}
    benchmark = {"results": [{"workers": 64, "shards": 16, "decisions_per_second": 100}]}
    assert budget_estimate(config, benchmark, elapsed=100)["fits"]
    benchmark["results"][0]["decisions_per_second"] = 50
    assert not budget_estimate(config, benchmark, elapsed=100)["fits"]
    assert budget_estimate(config, benchmark, elapsed=100, completed_steps=93_013_696)["fits"]


def test_value_conversion_unshapes_without_clipping():
    evaluation = {"result": {"seed_results": [
        {"success": True, "act_entries": {"2": {"value_shaped": 1.2, "potential": 0.5}}},
        {"success": False, "act_entries": {"2": {"value_shaped": -0.1, "potential": 0.5}}},
    ]}}
    result = value_diagnostics(evaluation, {"gamma": 1, "failure_progress_scale": 0,
                                          "potential_scale": 0.2, "potential_shaping": True})["acts"]["2"]
    assert result["out_of_range"] == 1
    assert result["mean_prediction_unclipped"] == pytest.approx(0.825)
    assert result["mean_squared_error_unclipped"] == pytest.approx((0.15**2 + 0.5**2)/2)


def test_late_windows_do_not_double_count_rollouts():
    rows = [{"environment_steps": step, "terminations_success": count}
            for step, count in [(1_999_999, 9), (2_000_000, 2), (2_999_999, 3), (3_000_000, 4)]]
    assert [w["successes"] for w in late_success_windows(rows, 0)] == [5, 4]


@pytest.mark.parametrize("bad", ["evaluation", "training", "nonfinite"])
def test_health_rejects_bad_control_or_training_even_if_selected_is_healthy(bad):
    evaluation = dict.fromkeys(("backend_errors", "backend_truncations", "step_limits", "cycle_limits", "timeouts"), 0)
    row = {"update_seconds": 1.0, "terminations_backend_truncated": 0,
           "terminations_step_limit": 0, "terminations_cycle_limit": 0}
    assert require_pilot_health([evaluation], [row])["evaluations_checked"] == 1
    if bad == "evaluation":
        evaluation["step_limits"] = 1
    elif bad == "training":
        row["terminations_cycle_limit"] = 1
    else:
        row["entropy"] = float("nan")
    with pytest.raises(ValueError, match="health|non-finite"):
        require_pilot_health([evaluation], [row])


def test_parent_critic_is_not_reinterpreted_as_act2_probability():
    result = value_diagnostics({"source_profile": "IRONCLAD_A20_ACT1"}, {})
    assert result["acts"] == {}


def test_parent_copy_preserves_source_rejects_corruption_and_overwrite(tmp_path):
    source = tmp_path / "old"
    source.mkdir()
    (source / "final.pt").write_bytes(b"unit-fixture-not-a-model")
    digest = sha256_file(source / "final.pt")
    (source / "training-bundle.json").write_text(json.dumps({"files": {"final.pt": digest}}))
    root = tmp_path / "new"
    root.mkdir()
    plan = {"parent": {"run": "parent", "evidence": {"final.pt": digest}}}
    destination = import_parent(source, plan, root=root)
    assert (source / "final.pt").exists() and sha256_file(destination / "final.pt") == digest
    with pytest.raises(FileExistsError):
        import_parent(source, plan, root=root)
    plan["parent"]["run"] = "bad"
    (source / "final.pt").write_bytes(b"corrupt")
    with pytest.raises(ValueError, match="mismatch"):
        import_parent(source, plan, root=root)
    assert not (root / "bad").exists()
