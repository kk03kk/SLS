import json
from pathlib import Path

from sls.audit.decision_identity import mapped_action
from sls.backends.simulator import SimulatorBackend
from sls.contracts import Action
from sls.curriculum import IRONCLAD_A20_ACT2


def test_plated_armor_decrement_waits_for_headbutt_selection():
    fixture = json.loads(Path('tests/fixtures/regressions/plated-armor-headbutt-131100070.json').read_text())
    backend = SimulatorBackend(IRONCLAD_A20_ACT2)
    backend.load_checkpoint(fixture['before'])
    decision = backend.step(Action.from_dict(fixture['action'])).decision
    assert decision.observation.to_dict()['powers'] == fixture['stock_selection']['powers']
    action = mapped_action(fixture['selection_action'], fixture['stock_selection'], decision.observation.to_dict())
    decision = backend.step(Action.from_dict(action)).decision
    for key, expected in fixture['after_selection'].items():
        assert decision.observation.to_dict()[key] == expected
