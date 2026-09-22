import pytest

from tools.compare_checkpoints import _exact_mcnemar, compare_seed_results


def _row(seed: int, success: bool, boss: str = "HEXAGHOST") -> dict:
    return {"seed": seed, "success": success, "bosses": {"1": boss}}


def test_paired_comparison_counts_transitions_and_boss_groups() -> None:
    left = [_row(1, True), _row(2, True), _row(3, False), _row(4, False, "SLIME_BOSS")]
    right = [_row(4, True, "SLIME_BOSS"), _row(3, False), _row(2, False), _row(1, True)]
    report = compare_seed_results(left, right)
    assert report["all"]["both_win"] == 1
    assert report["all"]["left_only"] == 1
    assert report["all"]["right_only"] == 1
    assert report["all"]["both_loss"] == 1
    assert report["all"]["paired_success_rate_delta"] == 0.0
    assert report["SLIME_BOSS"]["right_only"] == 1
    assert report["all"]["transition_seeds"]["left_only"] == [2]


def test_paired_comparison_rejects_seed_or_boss_mismatch() -> None:
    with pytest.raises(ValueError, match="different seed sets"):
        compare_seed_results([_row(1, True)], [_row(2, True)])
    with pytest.raises(ValueError, match="scheduled boss differs"):
        compare_seed_results([_row(1, True)], [_row(1, True, "SLIME_BOSS")])


def test_exact_mcnemar_is_two_sided_and_bounded() -> None:
    assert _exact_mcnemar(0, 0) == 1.0
    assert _exact_mcnemar(1, 1) == 1.0
    assert _exact_mcnemar(0, 10) == pytest.approx(2 / 2**10)
