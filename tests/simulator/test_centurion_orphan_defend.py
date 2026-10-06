import json
from pathlib import Path

from sls.backends.simulator import SimulatorBackend
from sls.contracts import Action
from sls.curriculum import IRONCLAD_A20_ACT2


def test_committed_defend_falls_back_to_self_when_mystic_dies():
    fixture = json.loads(Path('tests/fixtures/regressions/centurion-orphan-defend-131100070.json').read_text())
    backend = SimulatorBackend(IRONCLAD_A20_ACT2)
    backend.load_checkpoint(fixture['before'])
    decision = backend.step(Action.from_dict(fixture['action'])).decision
    for key, expected in fixture['expected'].items():
        assert decision.observation.to_dict()[key] == expected
    for key, expected in fixture['expected_rng'].items():
        assert backend.validation_snapshot().rng_streams[key] == expected
