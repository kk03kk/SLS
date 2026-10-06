import json
from pathlib import Path

from sls.backends.simulator import SimulatorBackend
from sls.contracts import Action
from sls.curriculum import IRONCLAD_A20_ACT2


def test_byrd_dying_to_thorns_cancels_remaining_normal_hits():
    data = json.loads(Path(
        'tests/fixtures/regressions/byrd-thorns-cancel-8000011000005.json'
    ).read_text(encoding='utf-8'))
    backend = SimulatorBackend(IRONCLAD_A20_ACT2)
    backend.load_checkpoint(data['before'])
    decision = backend.step(Action.from_dict(data['action'])).decision
    assert decision.observation.to_dict() == data['expected']
