from tools.find_act1_boss_seeds import TARGETS, find_lowest
from sls.backends.simulator import SimulatorBackend
from sls.curriculum import IRONCLAD_A0_ACT1


def test_finds_lowest_seed_for_every_act_one_boss() -> None:
    result = find_lowest(100)
    assert set(result) == TARGETS
    backend = SimulatorBackend(IRONCLAD_A0_ACT1)
    for boss, seed in result.items():
        assert all(
            backend.reset(earlier).observation.run.visible_boss_id != boss
            for earlier in range(seed)
        )
