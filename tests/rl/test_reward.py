from __future__ import annotations

import pytest

from sls.contracts import Observation, Player, RunContext, ScreenType
from sls.curriculum import IRONCLAD_A0_FULLRUN
from sls.rl.reward import (
    act_one_potential,
    curriculum_floor_progress,
    curriculum_terminal_reward,
    shape_act_one_reward,
    shape_curriculum_reward,
)


def _state(
    floor: int, hp: int, *, keys: tuple[bool, bool, bool] = (False, False, False),
) -> Observation:
    return Observation(
        Player("IRONCLAD", hp, 80, 0, 0, 3),
        RunContext(0, 1, floor, 99, *keys),
        ScreenType.MAP,
    )


def test_act_one_potential_matches_the_committed_formula() -> None:
    assert act_one_potential(_state(8, 40)) == pytest.approx(0.5)
    assert act_one_potential(_state(16, 80)) == pytest.approx(1.0)
    assert act_one_potential(_state(16, 80), terminal=True) == 0.0


def test_potential_shaping_preserves_terminal_reward_component() -> None:
    current = _state(15, 40)
    following = _state(16, 40)
    shaped = shape_act_one_reward(
        1.0, current, following, gamma=0.99, scale=0.2, terminal=True,
    )
    assert shaped == pytest.approx(1.0 - 0.2 * act_one_potential(current))


def test_terminal_reward_ranks_failure_floor_but_keeps_success_strictly_best() -> None:
    floor_one = curriculum_terminal_reward(
        _state(1, 80), IRONCLAD_A0_FULLRUN, success=False,
    )
    floor_sixteen = curriculum_terminal_reward(
        _state(16, 1), IRONCLAD_A0_FULLRUN, success=False,
    )
    success = curriculum_terminal_reward(
        _state(50, 1), IRONCLAD_A0_FULLRUN, success=True,
    )

    assert curriculum_floor_progress(
        _state(16, 1), IRONCLAD_A0_FULLRUN,
    ) == pytest.approx(16 / 50)
    assert floor_one == pytest.approx(-0.984)
    assert floor_sixteen == pytest.approx(-0.744)
    assert -1.0 < floor_one < floor_sixteen < 0.0 < success
    assert success == 1.0


def _discounted_failure_return(*, delay: int, gamma: float) -> float:
    state = _state(3, 80)
    living = shape_curriculum_reward(
        0.0, state, state, IRONCLAD_A0_FULLRUN,
        gamma=gamma, scale=0.2, terminal=False,
    )
    terminal = curriculum_terminal_reward(
        state, IRONCLAD_A0_FULLRUN, success=False,
    )
    failure = shape_curriculum_reward(
        terminal, state, state, IRONCLAD_A0_FULLRUN,
        gamma=gamma, scale=0.2, terminal=True,
    )
    rewards = [living] * delay + [failure]
    return sum(gamma**step * reward for step, reward in enumerate(rewards))


def test_canonical_discount_does_not_reward_delaying_the_same_failure() -> None:
    assert _discounted_failure_return(delay=500, gamma=0.999) > (
        _discounted_failure_return(delay=0, gamma=0.999)
    )
    assert _discounted_failure_return(delay=500, gamma=1.0) == pytest.approx(
        _discounted_failure_return(delay=0, gamma=1.0)
    )


def test_fullrun_failure_and_key_reward_properties_are_fail_closed() -> None:
    failures = [
        curriculum_terminal_reward(_state(floor, 1), IRONCLAD_A0_FULLRUN, success=False)
        for floor in (-10, 0, 1, 25, 49, 50, 500)
    ]
    assert all(-1.0 <= reward < 0.0 for reward in failures)
    assert failures == sorted(failures)
    no_keys = _state(25, 40)
    all_keys = _state(25, 40, keys=(True, True, True))
    assert curriculum_terminal_reward(
        no_keys, IRONCLAD_A0_FULLRUN, success=False,
    ) == curriculum_terminal_reward(
        all_keys, IRONCLAD_A0_FULLRUN, success=False,
    )
    assert shape_curriculum_reward(
        0.0, no_keys, no_keys, IRONCLAD_A0_FULLRUN,
        gamma=1.0, scale=0.2, terminal=False,
    ) == shape_curriculum_reward(
        0.0, all_keys, all_keys, IRONCLAD_A0_FULLRUN,
        gamma=1.0, scale=0.2, terminal=False,
    )
