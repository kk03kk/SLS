"""Training-only potential shaping; backend rewards remain authoritative."""

from __future__ import annotations

from sls.contracts import Observation

REWARD_SCHEMA = "sls-act1-potential-v1"


def act_one_potential(observation: Observation, *, terminal: bool = False) -> float:
    if terminal:
        return 0.0
    hp_fraction = observation.player.current_hp / max(1, observation.player.max_hp)
    floor_fraction = min(16, max(0, observation.run.floor)) / 16.0
    return 0.75 * floor_fraction + 0.25 * hp_fraction


def shape_act_one_reward(
    reward: float,
    current: Observation,
    following: Observation,
    *,
    gamma: float,
    scale: float,
    terminal: bool,
) -> float:
    return float(reward) + scale * (
        gamma * act_one_potential(following, terminal=terminal)
        - act_one_potential(current)
    )
