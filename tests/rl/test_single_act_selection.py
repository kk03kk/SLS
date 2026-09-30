"""Clear-count selection keeps healthy winners and does not veto rescued failures."""

from __future__ import annotations

from sls.rl.best_checkpoint import (
    PROGRESS_REGRESSION_FLOOR_MARGIN,
    best_checkpoint_record,
    evaluation_rank,
    passes_progress_guard,
)


def _single_act(**changes: object) -> dict[str, object]:
    value: dict[str, object] = {
        "episodes": 512,
        "successes": 375,
        "success_rate": 375 / 512,
        "reached_act2": 0,
        "reached_act3": 0,
        "reached_act2_rate": 0.0,
        "reached_act3_rate": 0.0,
        "mean_reward": 0.7,
        "mean_steps": 170.0,
        "self_loops": 0,
        "timeouts": 0,
        "step_limits": 0,
        "cycle_limits": 0,
        "backend_truncations": 0,
        "backend_errors": 0,
        "median_failure_floor": 16.0,
        "boss_success_rate": {"ACT_1:HEXAGHOST": 0.74},
        "boss_successes": {"ACT_1:HEXAGHOST": 526},
        "boss_attempts": {"ACT_1:HEXAGHOST": 711},
        "boss_action_metrics": {},
        "selection_objective": "ACT1_CLEAR_COUNT",
    }
    value.update(changes)
    return value


def test_clear_count_guard_does_not_veto_rescuing_deep_failures() -> None:
    incumbent = _single_act()
    regressed = _single_act(
        successes=380,
        median_failure_floor=16.0 - PROGRESS_REGRESSION_FLOOR_MARGIN,
    )
    # Rescuing deep failures changes the remaining-failure population. Its
    # median must not override the actual number of full clears.
    assert passes_progress_guard(regressed, incumbent)


def test_single_act_guard_allows_a_one_floor_shift_and_a_real_improvement() -> None:
    incumbent = _single_act()
    assert passes_progress_guard(_single_act(median_failure_floor=15.0), incumbent)
    # A clear success-interval separation is still accepted outright.
    assert passes_progress_guard(_single_act(successes=430), incumbent)


def test_single_act_guard_rejects_runtime_health_regressions() -> None:
    incumbent = _single_act()
    for field in ("step_limits", "cycle_limits", "timeouts", "backend_truncations"):
        assert not passes_progress_guard(_single_act(**{field: 1}), incumbent), field
    assert not passes_progress_guard(_single_act(backend_errors=1), incumbent)


def test_single_act_rank_keeps_success_count_primary() -> None:
    fewer = _single_act(successes=370)
    more = _single_act(successes=380)
    assert evaluation_rank(more) > evaluation_rank(fewer)


def test_single_act_rank_keeps_earliest_tie_regardless_of_failure_depth() -> None:
    shallow = _single_act(successes=375, median_failure_floor=13.0)
    deep = _single_act(successes=375, median_failure_floor=16.0)
    assert evaluation_rank(deep) == evaluation_rank(shallow)


def test_single_act_rank_penalizes_limit_terminations_on_success_ties() -> None:
    clean = _single_act(successes=375)
    looping = _single_act(successes=375, cycle_limits=4)
    assert evaluation_rank(clean) > evaluation_rank(looping)


def test_record_round_trip_feeds_the_guard() -> None:
    evaluation = _single_act()
    record = best_checkpoint_record(evaluation, update=610)
    assert record["selection_excludes_mean_reward"] is True
    assert record["median_failure_floor"] == 16.0
    assert passes_progress_guard(record, record)


def test_clear_count_guard_rejects_unhealthy_candidate_even_with_many_more_wins() -> None:
    incumbent = _single_act()
    assert not passes_progress_guard(_single_act(successes=500, step_limits=1), incumbent)
    assert evaluation_rank(_single_act(successes=500, step_limits=1)) < evaluation_rank(incumbent)


def test_single_stage_final_export_requires_complete_healthy_evaluation() -> None:
    from tools.train_full_run import _single_stage_final_passes

    assert _single_stage_final_passes(_single_act(), 512)
    assert not _single_stage_final_passes(_single_act(episodes=511), 512)
    for field in ("backend_errors", "backend_truncations", "step_limits", "cycle_limits", "timeouts"):
        assert not _single_stage_final_passes(_single_act(**{field: 1}), 512)
