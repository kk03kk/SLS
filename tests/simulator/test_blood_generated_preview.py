import json
from pathlib import Path

from sls.backends.simulator import SimulatorBackend
from sls.contracts import Action
from sls.curriculum import IRONCLAD_A20_ACT2


def test_generated_blood_preview_replays_stock_prior_damage_count():
    fixture = json.loads(Path('tests/fixtures/regressions/act2-blood-preview-131100067.json').read_text())
    backend = SimulatorBackend(IRONCLAD_A20_ACT2)
    backend.load_checkpoint(fixture['before'])
    decision = backend.step(Action.from_dict(fixture['action'])).decision
    actual = decision.observation.to_dict()['choice_options']
    for row in actual:
        row.pop('instance_id')
    expected = [dict(c) for c in fixture['expected_choices']]
    for row in expected:
        row.pop('instance_id')
    assert actual == expected
