import json
from pathlib import Path

from sls.backends.simulator import SimulatorBackend
from sls.contracts import Action
from sls.curriculum import IRONCLAD_A20_ACT2


def test_inferno_upgrades_existing_burns_and_adds_three_upgraded_burns():
    fixture = json.loads(Path('tests/fixtures/regressions/hexaghost-inferno-131100070.json').read_text())
    backend = SimulatorBackend(IRONCLAD_A20_ACT2)
    backend.load_checkpoint(fixture['before'])
    decision = backend.step(Action.from_dict(fixture['action'])).decision
    actual = decision.observation.to_dict()
    for key, expected in fixture['expected'].items():
        assert actual[key] == expected, key
    streams = backend.validation_snapshot().rng_streams
    for key, expected in fixture['expected_rng'].items():
        assert streams[key] == expected, key
