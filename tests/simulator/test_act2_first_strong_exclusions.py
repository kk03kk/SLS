import json
from pathlib import Path

from sls.backends.simulator import SimulatorBackend
from sls.contracts import Action
from sls.curriculum import IRONCLAD_A20_ACT2


def test_first_strong_encounter_rejects_byrds_after_three_byrds():
    data = json.loads(Path(
        'tests/fixtures/regressions/act2-first-strong-8000011000005.json'
    ).read_text(encoding='utf-8'))
    backend = SimulatorBackend(IRONCLAD_A20_ACT2)
    decision = backend.reset(data['seed'])
    for payload in data['actions']:
        decision = backend.step(Action.from_dict(payload)).decision
    assert decision.observation.to_dict() == data['expected']
