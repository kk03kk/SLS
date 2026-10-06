import json
from pathlib import Path

from sls.backends.simulator import SimulatorBackend
from sls.contracts import Action
from sls.curriculum import IRONCLAD_A20_ACT2


def test_summoned_mad_gremlin_has_angry_and_stock_position_after_wizard():
    fixture = json.loads(Path('tests/fixtures/regressions/act2-gremlin-summon-131100068.json').read_text())
    backend = SimulatorBackend(IRONCLAD_A20_ACT2)
    backend.load_checkpoint(fixture['before'])
    decision = backend.step(Action.from_dict(fixture['action'])).decision
    actual = decision.observation.to_dict()
    assert actual['enemies'] == fixture['stock_enemies']
    assert actual['powers'] == fixture['stock_powers']
