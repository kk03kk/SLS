import json
from pathlib import Path

import pytest

from sls.backends.simulator import SimulatorBackend
from sls.contracts import Action
from sls.curriculum import IRONCLAD_A20_ACT2


def test_boss_calling_bell_discards_stock_card_roll_without_offering_cards():
    fixture = json.loads(Path("tests/fixtures/regressions/act2-calling-bell-boss-131100064.json").read_text())
    backend = SimulatorBackend(IRONCLAD_A20_ACT2)
    backend.load_checkpoint(fixture["before"])
    backend.step(Action.from_dict(fixture["action"]))
    assert backend.raw_state["rng"]["card"] == fixture["expected_card_rng"]
    assert not backend.raw_state["public_screen"].get("card_rewards")
    assert len(backend.raw_state["public_screen"]["relics"]) == 3


@pytest.mark.parametrize("legacy_label", [False, True])
def test_boss_relic_extra_rewards_restore_continues_to_next_act(legacy_label):
    fixture = json.loads(Path("tests/fixtures/regressions/act2-calling-bell-boss-131100064.json").read_text())
    uninterrupted = SimulatorBackend(IRONCLAD_A20_ACT2)
    uninterrupted.load_checkpoint(fixture["before"])
    decision = uninterrupted.step(Action.from_dict(fixture["action"])).decision
    restored = SimulatorBackend(IRONCLAD_A20_ACT2)
    checkpoint = json.loads(json.dumps(uninterrupted.checkpoint()))
    if legacy_label:
        checkpoint["screen_info"]["continuation"] = "map"
    restored.load_checkpoint(checkpoint)
    for reward_id in ("reward-relic:2", "reward-relic:0", "reward-relic:0", None):
        kind = "TAKE_REWARD" if reward_id else "SKIP_REWARD"
        action = next(a for a in decision.actions if a.kind.value == kind and a.reward_id == reward_id)
        decision = uninterrupted.step(action).decision
        restored.step(action)
        assert restored.checkpoint() == uninterrupted.checkpoint()
    assert decision.observation.run.act == 2
