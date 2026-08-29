"""Fixed Ironclad Heart curriculum and anti-forgetting stage sampling."""

from __future__ import annotations

from dataclasses import dataclass
import random

from sls.curriculum import (
    CurriculumProfile, IRONCLAD_A0_ACT1, IRONCLAD_A0_ACT2, IRONCLAD_A0_ACT3,
    ironclad_fullrun_profile,
)


@dataclass(frozen=True, slots=True)
class CurriculumStage:
    stage_id: str
    profiles: tuple[CurriculumProfile, ...]


IRONCLAD_HEART_CURRICULUM = (
    CurriculumStage("A0_ACT1", (IRONCLAD_A0_ACT1,)),
    CurriculumStage("A0_ACT2", (IRONCLAD_A0_ACT2,)),
    CurriculumStage("A0_ACT3", (IRONCLAD_A0_ACT3,)),
    CurriculumStage("A0_HEART", (ironclad_fullrun_profile(0, require_heart=True),)),
    CurriculumStage("A1_A5_HEART", tuple(
        ironclad_fullrun_profile(value, require_heart=True) for value in range(1, 6)
    )),
    CurriculumStage("A6_A10_HEART", tuple(
        ironclad_fullrun_profile(value, require_heart=True) for value in range(6, 11)
    )),
    CurriculumStage("A11_A15_HEART", tuple(
        ironclad_fullrun_profile(value, require_heart=True) for value in range(11, 16)
    )),
    CurriculumStage("A16_A20_HEART", tuple(
        ironclad_fullrun_profile(value, require_heart=True) for value in range(16, 21)
    )),
)


class CurriculumScheduler:
    """Promote after three >=20% evaluations while retaining older stages."""

    def __init__(self, *, seed: int = 0, promotion_rate: float = 0.20) -> None:
        if not 0.0 < promotion_rate <= 1.0:
            raise ValueError("promotion_rate must be between zero and one")
        self.random = random.Random(seed)
        self.promotion_rate = promotion_rate
        self.stage_index = 0
        self._recent: list[float] = []

    @property
    def current(self) -> CurriculumStage:
        return IRONCLAD_HEART_CURRICULUM[self.stage_index]

    def observe_evaluation(self, success_rate: float) -> bool:
        if not 0.0 <= success_rate <= 1.0:
            raise ValueError("success_rate must be between zero and one")
        self._recent = [*self._recent[-2:], success_rate]
        if (
            len(self._recent) == 3
            and min(self._recent) >= self.promotion_rate
            and self.stage_index + 1 < len(IRONCLAD_HEART_CURRICULUM)
        ):
            self.stage_index += 1
            self._recent.clear()
            return True
        return False

    def sample_profile(self) -> CurriculumProfile:
        index = self.stage_index
        if index and self.random.random() < 0.5:
            index = self.random.randrange(index)
        return self.random.choice(IRONCLAD_HEART_CURRICULUM[index].profiles)
