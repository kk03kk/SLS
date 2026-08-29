"""Episode horizons layered over unmodified FullRun game semantics."""

from __future__ import annotations

from dataclasses import dataclass
from enum import Enum, IntEnum

from sls.contracts import Observation, ScreenType


class RunPhase(str, Enum):
    NEOW = "NEOW"
    MAP = "MAP"
    ROOM = "ROOM"
    COMBAT = "COMBAT"
    REWARD = "REWARD"
    BOSS_REWARD = "BOSS_REWARD"
    ACT_TRANSITION = "ACT_TRANSITION"
    VICTORY = "VICTORY"
    DEATH = "DEATH"


class EpisodeHorizon(IntEnum):
    ACT_1 = 1
    ACT_2 = 2
    ACT_3 = 3
    HEART, FULL_RUN = 4, 5


@dataclass(frozen=True, slots=True)
class CurriculumProfile:
    profile_id: str
    character_id: str
    ascension: int
    horizon: EpisodeHorizon
    start_distribution: str = "NATURAL_RUN_START"
    version: int = 2


IRONCLAD_A0_ACT1 = CurriculumProfile("IRONCLAD_A0_ACT1", "IRONCLAD", 0, EpisodeHorizon.ACT_1)
IRONCLAD_A0_ACT2 = CurriculumProfile("IRONCLAD_A0_ACT2", "IRONCLAD", 0, EpisodeHorizon.ACT_2)
IRONCLAD_A0_ACT3 = CurriculumProfile("IRONCLAD_A0_ACT3", "IRONCLAD", 0, EpisodeHorizon.ACT_3)
IRONCLAD_A0_HEART = CurriculumProfile("IRONCLAD_A0_HEART", "IRONCLAD", 0, EpisodeHorizon.HEART)


@dataclass(frozen=True, slots=True)
class HorizonDecision:
    terminated: bool
    success: bool
    reason: str | None


def phase_of(observation: Observation) -> RunPhase:
    if observation.screen is ScreenType.GAME_OVER:
        return RunPhase.DEATH if observation.player.current_hp <= 0 else RunPhase.VICTORY
    if observation.screen is ScreenType.COMBAT:
        return RunPhase.COMBAT
    if observation.screen in {ScreenType.COMBAT_REWARD, ScreenType.CARD_REWARD}:
        return RunPhase.REWARD
    if observation.screen is ScreenType.BOSS_REWARD:
        return RunPhase.BOSS_REWARD
    if observation.screen is ScreenType.ACT_TRANSITION:
        return RunPhase.ACT_TRANSITION
    if observation.screen is ScreenType.MAP:
        return RunPhase.MAP
    if observation.screen is ScreenType.NEOW:
        return RunPhase.NEOW
    return RunPhase.ROOM


def evaluate_horizon(
    profile: CurriculumProfile,
    observation: Observation,
    *,
    act_completed: int | None = None,
) -> HorizonDecision:
    phase = phase_of(observation)
    if phase is RunPhase.DEATH:
        return HorizonDecision(True, False, "DEATH")
    if phase is RunPhase.VICTORY:
        if profile.horizon is EpisodeHorizon.HEART and observation.run.act < 4:
            return HorizonDecision(True, False, "HEART_NOT_REACHED")
        return HorizonDecision(True, True, "GAME_VICTORY")
    completed = act_completed
    if completed is None and phase is RunPhase.ACT_TRANSITION:
        completed = max(0, observation.run.act - 1)
    if (
        profile.horizon is not EpisodeHorizon.FULL_RUN
        and completed is not None
        and completed >= int(profile.horizon)
    ):
        return HorizonDecision(True, True, f"ACT_{int(profile.horizon)}_CLEARED")
    return HorizonDecision(False, False, None)


def completed_act_between(previous: Observation, current: Observation) -> int | None:
    """Return the highest act completed by an observed forward transition.

    The native simulator transitions directly from the boss-relic continuation
    to the next act's map, so an ``ACT_TRANSITION`` screen is not a reliable
    episode boundary.  Comparing public act numbers works for both backends and
    deliberately waits until the transition has actually happened.
    """

    previous_act = int(previous.run.act)
    current_act = int(current.run.act)
    if current_act <= previous_act:
        return None
    return current_act - 1


def ironclad_fullrun_profile(
    ascension: int, *, require_heart: bool = False,
) -> CurriculumProfile:
    if ascension < 0 or ascension > 20:
        raise ValueError("Ironclad ascension must be between 0 and 20")
    suffix = "HEART" if require_heart else "FULLRUN"
    horizon = EpisodeHorizon.HEART if require_heart else EpisodeHorizon.FULL_RUN
    return CurriculumProfile(
        f"IRONCLAD_A{ascension}_{suffix}", "IRONCLAD", ascension, horizon,
    )


IRONCLAD_FULLRUN_PROFILES = tuple(
    ironclad_fullrun_profile(ascension, require_heart=require_heart)
    for ascension in range(21)
    for require_heart in (False, True)
)
IRONCLAD_A0_FULLRUN = IRONCLAD_FULLRUN_PROFILES[0]
IRONCLAD_A20_FULLRUN = IRONCLAD_FULLRUN_PROFILES[-2]
IRONCLAD_A20_HEART = IRONCLAD_FULLRUN_PROFILES[-1]
IRONCLAD_CURRICULUM_PROFILES = (
    IRONCLAD_A0_ACT1, IRONCLAD_A0_ACT2, IRONCLAD_A0_ACT3,
    *IRONCLAD_FULLRUN_PROFILES,
)
CURRICULUM_PROFILES_BY_ID = {
    profile.profile_id: profile for profile in IRONCLAD_CURRICULUM_PROFILES
}
