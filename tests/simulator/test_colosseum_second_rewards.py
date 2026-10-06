import json
from pathlib import Path

from sls.backends.simulator import SimulatorBackend
from sls.contracts import Action
from sls.curriculum import IRONCLAD_A20_ACT2


def test_second_colosseum_victory_creates_normal_card_reward():
    """Actual stock second victory: complete rewards, resources and RNG."""
    fixture = json.loads(Path('tests/fixtures/regressions/colosseum-second-reward-131100066.json').read_text())
    backend = SimulatorBackend(IRONCLAD_A20_ACT2)
    backend.load_checkpoint(fixture['before'])
    decision = backend.step(Action.from_dict(fixture['action'])).decision
    assert any(action.kind.value == 'CHOOSE_CARD_REWARD' for action in decision.actions)
    assert decision.observation.to_dict() == fixture['expected_observation']
    assert dict(backend.validation_snapshot().rng_streams) == fixture['expected_rng']
