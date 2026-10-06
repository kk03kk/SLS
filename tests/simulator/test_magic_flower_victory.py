import json
from pathlib import Path

from sls.backends.simulator import SimulatorBackend
from sls.contracts import Action
from sls.curriculum import IRONCLAD_A20_ACT2


def test_magic_flower_applies_to_burning_blood_at_stock_combat_end():
    fixture = json.loads(Path('tests/fixtures/regressions/magic-flower-victory-8000011000002.json').read_text())
    backend = SimulatorBackend(IRONCLAD_A20_ACT2)
    backend.load_checkpoint(fixture['before'])
    decision = backend.step(Action.from_dict(fixture['action'])).decision
    assert decision.observation.player.current_hp == fixture['expected_hp']
