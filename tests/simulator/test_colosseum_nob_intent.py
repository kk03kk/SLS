import json
from pathlib import Path

from sls.backends.simulator import SimulatorBackend
from sls.contracts import Action
from sls.curriculum import IRONCLAD_A20_ACT2


def test_colosseum_nob_uses_stock_vulnerable_skull_bash_intent():
    fixture = json.loads(Path('tests/fixtures/regressions/colosseum-nob-intent-131100066.json').read_text())
    backend = SimulatorBackend(IRONCLAD_A20_ACT2)
    backend.load_checkpoint(fixture['before'])
    decision = backend.step(Action.from_dict(fixture['action'])).decision
    assert decision.observation.to_dict()['enemies'] == fixture['expected']['enemies']
    assert dict(backend.validation_snapshot().rng_streams) == fixture['expected_rng']
