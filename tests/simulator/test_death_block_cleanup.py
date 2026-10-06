import json
from pathlib import Path

from sls.backends.simulator import SimulatorBackend
from sls.contracts import Action
from sls.curriculum import IRONCLAD_A20_ACT2


def test_combust_death_clears_stock_remaining_block():
    fixture = json.loads(Path('tests/fixtures/regressions/act2-combust-death-block-8000011000001.json').read_text())
    backend = SimulatorBackend(IRONCLAD_A20_ACT2)
    backend.load_checkpoint(fixture['before'])
    decision = backend.step(Action.from_dict(fixture['action'])).decision
    assert decision.terminal and decision.observation.player.current_hp == 0
    assert decision.observation.player.block == fixture['expected_block']
