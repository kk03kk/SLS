"""Training objective: terminal progress scoring plus potential shaping."""

from __future__ import annotations

from sls.contracts import Observation
from sls.curriculum import CurriculumProfile, EpisodeHorizon

REWARD_SCHEMA = "sls-curriculum-progress-v3"
DEFAULT_FAILURE_PROGRESS_SCALE = 0.8


def curriculum_floor_progress(
    observation: Observation,
    profile: CurriculumProfile,
) -> float:
    """Return bounded progress toward the profile's terminal floor."""

    horizon_floor = {
        EpisodeHorizon.ACT_1: 16,
        EpisodeHorizon.ACT_2: 33,
        EpisodeHorizon.ACT_3: 50,
        EpisodeHorizon.FULL_RUN: 50,
        EpisodeHorizon.HEART: 55,
    }[profile.horizon]
    return min(horizon_floor, max(0, observation.run.floor)) / horizon_floor


def curriculum_terminal_reward(
    observation: Observation,
    profile: CurriculumProfile,
    *,
    success: bool,
    failure_progress_scale: float = DEFAULT_FAILURE_PROGRESS_SCALE,
) -> float:
    """Score one real terminal outcome, ranking farther failures above earlier ones."""

    if not 0.0 < failure_progress_scale < 1.0:
        raise ValueError("failure progress scale must be between zero and one")
    if success:
        return 1.0
    return -1.0 + failure_progress_scale * curriculum_floor_progress(
        observation, profile,
    )


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


def curriculum_potential(
    observation: Observation,
    profile: CurriculumProfile,
    *,
    terminal: bool = False,
) -> float:
    if terminal:
        return 0.0
    floor_progress = curriculum_floor_progress(observation, profile)
    hp_fraction = observation.player.current_hp / max(1, observation.player.max_hp)
    key_progress = (
        int(observation.run.has_ruby_key)
        + int(observation.run.has_emerald_key)
        + int(observation.run.has_sapphire_key)
    ) / 3.0
    key_weight = 0.2 if profile.horizon is EpisodeHorizon.HEART else 0.0
    return (0.8 - key_weight) * floor_progress + 0.2 * hp_fraction + key_weight * key_progress


def shape_curriculum_reward(
    reward: float,
    current: Observation,
    following: Observation,
    profile: CurriculumProfile,
    *,
    gamma: float,
    scale: float,
    terminal: bool,
) -> float:
    return float(reward) + scale * (
        gamma * curriculum_potential(following, profile, terminal=terminal)
        - curriculum_potential(current, profile)
    )
