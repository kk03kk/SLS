from __future__ import annotations

from sls.rl.curriculum_scheduler import CurriculumScheduler, IRONCLAD_HEART_CURRICULUM


def test_heart_curriculum_covers_every_ascension_and_promotes_after_three_passes() -> None:
    profiles = {
        profile.profile_id
        for stage in IRONCLAD_HEART_CURRICULUM for profile in stage.profiles
    }
    assert "IRONCLAD_A0_ACT1" in profiles
    assert all(f"IRONCLAD_A{value}_HEART" in profiles for value in range(21))
    scheduler = CurriculumScheduler(seed=0)
    assert not scheduler.observe_evaluation(0.2)
    assert not scheduler.observe_evaluation(0.3)
    assert scheduler.observe_evaluation(0.25)
    assert scheduler.current.stage_id == "A0_ACT2"
