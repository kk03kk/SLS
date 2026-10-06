import json
from pathlib import Path

from sls.backends.simulator import SimulatorBackend
from sls.contracts import Action
from sls.curriculum import IRONCLAD_A20_ACT2


def test_ritual_dagger_kill_updates_live_masterdeck_at_stock_boundary():
    data = json.loads(Path(
        'tests/fixtures/regressions/ritual-dagger-8000011000002.json'
    ).read_text(encoding='utf-8'))
    backend = SimulatorBackend(IRONCLAD_A20_ACT2)
    backend.load_checkpoint(data['before'])
    decision = backend.step(Action.from_dict(data['action'])).decision
    assert decision.observation.to_dict() == data['expected']
    checkpoint = backend.checkpoint()
    restored = SimulatorBackend(IRONCLAD_A20_ACT2)
    restored.load_checkpoint(checkpoint)
    assert restored.checkpoint() == checkpoint
