import json
from pathlib import Path

from sls.backends.simulator import SimulatorBackend
from sls.contracts import Action
from sls.curriculum import IRONCLAD_A20_ACT2


def test_anger_copy_preserves_stock_confusion_costs():
    fixture = json.loads(Path("tests/fixtures/regressions/act2-anger-confusion-131100063.json").read_text())
    backend = SimulatorBackend(IRONCLAD_A20_ACT2)
    backend.load_checkpoint(fixture["before"])
    decision = backend.step(Action.from_dict(fixture["action"])).decision
    costs = [{"base_cost": c.base_cost, "current_cost": c.current_cost}
             for c in decision.observation.discard_pile if c.card_id == "ANGER"]
    assert costs == fixture["expected_anger_discard_costs"]
