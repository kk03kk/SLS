from sls.rl.best_checkpoint import evaluation_rank
from tools.train_full_run import _single_stage_final_passes


def test_more_act2_clears_win_even_with_policy_failures_or_lower_act1_reach():
    lower = {"selection_objective": "ACT2_CLEAR_COUNT", "successes": 3,
             "cycle_limits": 0, "reached_act2": 800}
    higher = {"selection_objective": "ACT2_CLEAR_COUNT", "successes": 8,
              "cycle_limits": 10, "reached_act2": 600}
    assert evaluation_rank(higher) > evaluation_rank(lower)
    assert evaluation_rank({**higher, "successes": 3}) == evaluation_rank(lower)
    assert evaluation_rank({**higher, "backend_errors": 1}) < evaluation_rank(lower)


def test_policy_limits_are_failures_only_in_explicit_new_contract():
    result = {"episodes": 2048, "cycle_limits": 10, "step_limits": 1}
    assert not _single_stage_final_passes(result, 2048)
    assert _single_stage_final_passes(result, 2048, policy_limits_as_failures=True)
    assert not _single_stage_final_passes({**result, "timeouts": 1}, 2048, policy_limits_as_failures=True)
