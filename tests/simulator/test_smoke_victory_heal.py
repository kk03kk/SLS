import json
from pathlib import Path

from sls.backends.simulator import SimulatorBackend
from sls.contracts import Action, ScreenType
from sls.curriculum import IRONCLAD_A20_ACT2


def test_smoke_escape_runs_stock_victory_heal_without_rewards():
    fixture = json.loads(Path('tests/fixtures/regressions/act2-smoke-victory-heal-131100063.json').read_text())
    backend = SimulatorBackend(IRONCLAD_A20_ACT2)
    backend.load_checkpoint(fixture['before'])
    decision = backend.step(Action.from_dict(fixture['action'])).decision
    assert decision.observation.screen is ScreenType.MAP
    assert decision.observation.player.current_hp == fixture['expected_hp']
