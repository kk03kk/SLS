from __future__ import annotations

from dataclasses import replace

from sls.contracts import Observation, Player, RunContext, ScreenType
from sls.curriculum import (
    IRONCLAD_A0_ACT1,
    IRONCLAD_A0_ACT2,
    completed_act_between,
    evaluate_horizon,
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
