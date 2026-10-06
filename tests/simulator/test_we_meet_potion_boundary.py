import json
from pathlib import Path

from sls.backends.simulator import SimulatorBackend
from sls.contracts import ActionKind
from sls.curriculum import IRONCLAD_A20_ACT2


def test_we_meet_potion_lock_persists_until_leaving_room():
    fixture = json.loads(Path('tests/fixtures/regressions/act1-we-meet-map-8000011000000.json').read_text())
    backend = SimulatorBackend(IRONCLAD_A20_ACT2)
    decision = backend.load_checkpoint(fixture['before'])
    assert not any(a.kind in {ActionKind.USE_POTION, ActionKind.DISCARD_POTION}
                   for a in decision.actions)
    action = next(a for a in decision.actions if a.kind is ActionKind.CHOOSE_MAP_NODE)
    decision = backend.step(action).decision
    assert any(a.kind is ActionKind.DISCARD_POTION for a in decision.actions)
