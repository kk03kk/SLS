from __future__ import annotations

import pytest

from sls.contracts import Observation, Player, RunContext, ScreenType
from sls.rl.reward import act_one_potential, shape_act_one_reward


def _state(floor: int, hp: int) -> Observation:
    return Observation(
        Player("IRONCLAD", hp, 80, 0, 0, 3),
        RunContext(0, 1, floor, 99, False, False, False),
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
