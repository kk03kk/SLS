import json
from pathlib import Path

from sls.backends.simulator import SimulatorBackend
from sls.contracts import Action
from sls.curriculum import IRONCLAD_A20_ACT2


def test_black_blood_replaces_burning_blood_at_its_stock_relic_position():
    fixture = json.loads(Path("tests/fixtures/regressions/act2-black-blood-order-131100065.json").read_text())
    backend = SimulatorBackend(IRONCLAD_A20_ACT2)
    backend.load_checkpoint(fixture["before"])
    decision = backend.step(Action.from_dict(fixture["action"])).decision
    assert [r.content_id for r in decision.observation.relics] == fixture["expected_relic_ids"]
    assert len(decision.observation.relics) == 3
