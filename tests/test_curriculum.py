from __future__ import annotations

from dataclasses import replace

import pytest

from sls.contracts import (
    Observation,
    Player,
    RunContext,
    ScreenType,
)
from sls.curriculum import (
    IRONCLAD_A0_ACT1,
    IRONCLAD_A0_ACT2,
    IRONCLAD_A0_FULLRUN,
    IRONCLAD_A0_HEART,
    IRONCLAD_A20_FULLRUN,
    IRONCLAD_A20_HEART,
    completed_act_between,
    evaluate_horizon,
    ironclad_fullrun_profile,
)


def _observation(act: int, screen: ScreenType, *, hp: int = 80) -> Observation:
    return Observation(
        player=Player("IRONCLAD", hp, 80, 0, 0, 3),
        run=RunContext(0, act, 16, 99, False, False, False),
        screen=screen,
    )


def test_act_one_waits_for_actual_act_change() -> None:
    boss_reward = _observation(1, ScreenType.BOSS_REWARD)
    selection = _observation(1, ScreenType.CARD_REWARD)
    assert not evaluate_horizon(IRONCLAD_A0_ACT1, boss_reward).terminated
    assert not evaluate_horizon(IRONCLAD_A0_ACT1, selection).terminated
    assert completed_act_between(boss_reward, selection) is None


def test_act_one_completes_on_entry_to_act_two() -> None:
    previous = _observation(1, ScreenType.BOSS_REWARD)
    current = _observation(2, ScreenType.MAP)
    completed = completed_act_between(previous, current)
    result = evaluate_horizon(IRONCLAD_A0_ACT1, current, act_completed=completed)
    assert completed == 1
    assert result.terminated and result.success
    assert result.reason == "ACT_1_CLEARED"


def test_act_two_uses_the_same_cross_act_rule() -> None:
    previous = _observation(2, ScreenType.BOSS_REWARD)
    current = _observation(3, ScreenType.MAP)
    result = evaluate_horizon(
        IRONCLAD_A0_ACT2,
        current,
        act_completed=completed_act_between(previous, current),
    )
    assert result.terminated and result.success
    assert result.reason == "ACT_2_CLEARED"


def test_death_takes_precedence_over_completed_act() -> None:
    dead = _observation(2, ScreenType.GAME_OVER, hp=0)
    result = evaluate_horizon(IRONCLAD_A0_ACT1, dead, act_completed=1)
    assert result.terminated and not result.success
    assert result.reason == "DEATH"


def test_profile_contract_version_changed() -> None:
    assert IRONCLAD_A0_ACT1.version == 2
    assert replace(IRONCLAD_A0_ACT1).version == 2


def test_fullrun_and_heart_have_distinct_terminal_goals() -> None:
    act_three_ending = _observation(3, ScreenType.GAME_OVER)
    fullrun = evaluate_horizon(IRONCLAD_A0_FULLRUN, act_three_ending)
    heart = evaluate_horizon(IRONCLAD_A0_HEART, act_three_ending)
    assert fullrun.terminated and fullrun.success
    assert heart.terminated and not heart.success
    assert heart.reason == "HEART_NOT_REACHED"

    heart_ending = _observation(4, ScreenType.GAME_OVER)
    assert evaluate_horizon(IRONCLAD_A0_HEART, heart_ending).success


def test_fullrun_stops_after_act_three_even_when_a_key_route_would_continue() -> None:
    previous = _observation(3, ScreenType.BOSS_REWARD)
    act_four = _observation(4, ScreenType.MAP)
    completed = completed_act_between(previous, act_four)
    fullrun = evaluate_horizon(
        IRONCLAD_A0_FULLRUN, act_four, act_completed=completed,
    )
    heart = evaluate_horizon(
        IRONCLAD_A0_HEART, act_four, act_completed=completed,
    )
    assert fullrun.terminated and fullrun.success
    assert fullrun.reason == "ACT_3_CLEARED"
    assert not heart.terminated


def test_all_fullrun_ascensions_are_real_profiles() -> None:
    assert ironclad_fullrun_profile(0) == IRONCLAD_A0_FULLRUN
    assert ironclad_fullrun_profile(20) == IRONCLAD_A20_FULLRUN
    assert ironclad_fullrun_profile(20, require_heart=True) == IRONCLAD_A20_HEART
    for ascension in range(21):
        profile = ironclad_fullrun_profile(ascension)
        assert profile.ascension == ascension
        assert profile.profile_id == f"IRONCLAD_A{ascension}_FULLRUN"
    with pytest.raises(ValueError, match="between 0 and 20"):
        ironclad_fullrun_profile(21)
