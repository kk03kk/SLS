import json
from pathlib import Path

from sls.backends.simulator import SimulatorBackend
from sls.curriculum import IRONCLAD_A20_ACT2


def test_stock_taskmaster_lethal_damage_prevents_queued_rng_roll():
    fixture = json.loads(Path('tests/fixtures/regressions/act12-taskmaster-lethal-131100063.json').read_text())
    backend = SimulatorBackend(IRONCLAD_A20_ACT2)
    decision = backend.load_checkpoint(fixture['before'])
    assert dict(backend.validation_snapshot().rng_streams)['ai'] == fixture['stock_before_ai_rng']
    action = next(a for a in decision.actions if a.to_dict() == fixture['action'])
    restored = SimulatorBackend(IRONCLAD_A20_ACT2)
    restored.load_checkpoint(json.loads(json.dumps(backend.checkpoint())))
    transition = backend.step(action)
    assert transition == restored.step(action)
    assert transition.decision.terminal
    assert transition.info['reason'] == 'DEATH'
    assert transition.decision.observation.player.current_hp == fixture['stock_terminal_hp'] == 0
    assert dict(backend.validation_snapshot().rng_streams)['ai'] == fixture['stock_terminal_ai_rng']
    assert backend.checkpoint() == restored.checkpoint()
