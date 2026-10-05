"""Full-horizon comparisons do not condition pairing on reaching the next act."""

import copy
import json
from pathlib import Path

import pytest

from tools.analyze_act12_pilot import paired_act12


def evaluation(won, *, reached_act2):
    rows = [{"seed": 10 + i, "success": success, "reason": "ACT_2_CLEARED" if success else "DEATH",
             "bosses": {"1": "HEXAGHOST", **({"2": "CHAMP"} if reached_act2 else {})},
             "entered_bosses": ["ACT_1:HEXAGHOST"] + (["ACT_2:CHAMP"] if success else []),
             "floor": 33 if success else 20 if reached_act2 else 10, "steps": 20}
            for i, success in enumerate(won)]
    count = sum(won)
    result = {"seed_results": rows, "episodes": len(rows), "successes": count,
              "success_rate": count / len(rows),
              "boss_successes": {"ACT_1:HEXAGHOST": len(rows) if reached_act2 else 0,
                                 **({"ACT_2:CHAMP": count} if reached_act2 else {})},
              "boss_attempts": {"ACT_1:HEXAGHOST": len(rows),
                                **({"ACT_2:CHAMP": len(rows)} if reached_act2 else {})},
              "reached_act2": len(rows) if reached_act2 else 0,
              "reached_act2_rate": float(reached_act2),
              **dict.fromkeys(("backend_errors", "backend_truncations", "step_limits", "cycle_limits", "timeouts"), 0)}
    return {"seeds": [10, 10 + len(rows)], "evaluation_role": "development-confirmation",
            "runtime": {"device": "fixture"}, "simulator": {"native": "fixture"}, "result": result}


def test_compare_all_normal_starts_even_when_reach_rates_differ():
    left = evaluation([False, False], reached_act2=False)
    right = evaluation([True, False], reached_act2=True)
    result = paired_act12(left, right)
    assert result["paired_seeds"] == 2 and result["net"] == 1


def test_act1_clear_is_not_an_act12_success():
    left = evaluation([False, False], reached_act2=False)
    right = evaluation([True, False], reached_act2=True)
    right["result"]["seed_results"][0]["reason"] = "ACT_1_CLEARED"
    with pytest.raises(ValueError, match="complete Act1-2"):
        paired_act12(left, right)


@pytest.mark.parametrize("field", ["runtime", "simulator", "seeds", "environment", "reward", "decoding"])
def test_incomparable_identity_is_rejected(field):
    left = evaluation([False, False], reached_act2=False)
    right = copy.deepcopy(left)
    right[field] = {"different": True} if field != "seeds" else [12, 14]
    with pytest.raises(ValueError):
        paired_act12(left, right)


def test_no_boss_entries_reports_undefined_conditional_rate():
    from tools.analyze_reward_screen import outcomes
    record = evaluation([False, False], reached_act2=False)
    for row in record["result"]["seed_results"]:
        row["entered_bosses"] = []
    assert outcomes(record["result"], (10, 12))["bosses"]["HEXAGHOST"]["entry_win_rate"] is None


def test_real_frozen70_artifact_separates_act1_clears_from_two_act_wins():
    from tools.analyze_reward_screen import outcomes
    path = Path(__file__).resolve().parents[1] / "docs/results/act12-readiness-20261004/zero-shot-act12.json"
    raw = json.loads(path.read_text())
    result = outcomes(raw["result"], tuple(raw["seeds"]), horizon=2)
    assert result["wins"] == 0 and result["episodes"] == 32
    assert result["acts"]["1"]["act_clears"] == result["acts"]["2"]["reached"] == 26
    assert result["acts"]["2"]["act_clears"] == 0
    assert sum(group["boss_entries"] for group in result["acts"]["2"]["bosses"].values()) == 2
    assert result["acts"]["2"]["bosses"]["AUTOMATON"]["boss_entry_clear_rate"] == 0
    assert result["acts"]["2"]["bosses"]["CHAMP"]["boss_entry_clear_rate"] is None


@pytest.mark.parametrize("change", ["act1-count", "act2-count", "reach", "extra-boss"])
def test_per_act_diagnostics_reject_inconsistent_aggregates(change):
    from tools.analyze_reward_screen import outcomes
    result = evaluation([True, False], reached_act2=True)["result"]
    if change == "act1-count":
        result["boss_successes"]["ACT_1:HEXAGHOST"] = 1
    elif change == "act2-count":
        result["boss_successes"]["ACT_2:CHAMP"] = 2
    elif change == "reach":
        result["reached_act2"] = 1
    else:
        result["boss_attempts"]["ACT_2:AUTOMATON"] = 0
    with pytest.raises(ValueError):
        outcomes(result, (10, 12), horizon=2)


def test_no_act2_entries_keeps_normal_start_denominator_and_undefined_conditional_rate():
    from tools.analyze_reward_screen import outcomes
    result = outcomes(evaluation([False, False], reached_act2=False)["result"], (10, 12), horizon=2)
    assert result["episodes"] == 2 and result["acts"]["2"]["reach_rate"] == 0
    assert result["acts"]["2"]["full_horizon_wins_given_reach"] is None


def test_act1_analysis_retains_legacy_summary():
    from tools.analyze_reward_screen import outcomes
    result = evaluation([True, False], reached_act2=False)["result"]
    result["boss_successes"]["ACT_1:HEXAGHOST"] = 1
    summary = outcomes(result, (10, 12))
    assert summary["wins"] == summary["bosses"]["HEXAGHOST"]["wins"] == 1
    assert "acts" not in summary


def test_false_success_flag_cannot_hide_an_act2_clear_reason():
    left = evaluation([False, False], reached_act2=False)
    right = copy.deepcopy(left)
    right["result"]["seed_results"][0]["reason"] = "ACT_2_CLEARED"
    with pytest.raises(ValueError, match="complete Act1-2"):
        paired_act12(left, right)
