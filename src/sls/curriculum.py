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
    HEART = 4


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
        return HorizonDecision(True, True, "GAME_VICTORY")
    completed = act_completed
    if completed is None and phase is RunPhase.ACT_TRANSITION:
        completed = max(0, observation.run.act - 1)
    if completed is not None and completed >= int(profile.horizon):
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
