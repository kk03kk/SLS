from __future__ import annotations

import gzip
import json
from pathlib import Path

import pytest

from sls.contracts import Action, ActionKind


def test_native_full_run_reaches_a_canonical_decision() -> None:
    pytest.importorskip("sls.backends.simulator.native", exc_type=ImportError)
    from sls.backends.simulator import IRONCLAD_A0_ACT1, SimulatorBackend

    backend = SimulatorBackend(IRONCLAD_A0_ACT1)
    decision = backend.reset(0)
    assert not decision.terminal
    assert decision.actions
    assert decision.observation.run.act == 1


def test_optional_multi_select_checkpoint_preserves_pending_cards() -> None:
    pytest.importorskip("sls.backends.simulator.native", exc_type=ImportError)
    from sls.backends.simulator import IRONCLAD_A0_HEART, SimulatorBackend

    path = Path(__file__).resolve().parents[2] / "tests" / "fixtures" / "regressions" / (
        "original-purity-multi-select.json.gz"
    )
    with gzip.open(path, "rt", encoding="utf-8") as stream:
        fixture = json.load(stream)
    simulator = SimulatorBackend(IRONCLAD_A0_HEART)
    decision = simulator.load_checkpoint(fixture["simulator_checkpoint"])
    for action, evidence in zip(
        fixture["action_suffix"], fixture["action_evidence_suffix"], strict=True,
    ):
        decision = simulator.step(Action.from_dict(action), validation_evidence=evidence).decision
    assert [action.kind for action in decision.actions] == [
        ActionKind.SELECT_CARD, ActionKind.SELECT_CARD, ActionKind.CONFIRM,
    ]
    decision = simulator.step(decision.actions[0]).decision
    assert [action.kind for action in decision.actions] == [
        ActionKind.SELECT_CARD, ActionKind.CONFIRM,
    ]
    checkpoint = json.loads(json.dumps(simulator.checkpoint()))
    assert checkpoint["_policy_multi_selection"] == [0]
    restored = SimulatorBackend(IRONCLAD_A0_HEART)
    restored_decision = restored.load_checkpoint(checkpoint)
    assert restored_decision == decision
    confirm = next(action for action in decision.actions if action.kind is ActionKind.CONFIRM)
    assert restored.step(confirm).decision == simulator.step(confirm).decision


def test_discard_potion_preserves_suspended_card_choice() -> None:
    pytest.importorskip("sls.backends.simulator.native", exc_type=ImportError)
    from sls.backends.simulator import IRONCLAD_A0_HEART, SimulatorBackend

    path = Path(__file__).resolve().parents[2] / "tests" / "fixtures" / "regressions" / (
        "potion-during-card-choice.json.gz"
    )
    with gzip.open(path, "rt", encoding="utf-8") as stream:
        fixture = json.load(stream)
    simulator = SimulatorBackend(IRONCLAD_A0_HEART)
    decision = simulator.load_checkpoint(fixture["simulator_checkpoint"])
    for index, action in enumerate(fixture.get("action_suffix", ())):
        decision = simulator.step(
            Action.from_dict(action),
            validation_evidence=(fixture.get("action_evidence_suffix") or [])[index],
        ).decision
    discard = next(
        action for action in decision.actions if action.kind is ActionKind.DISCARD_POTION
    )
    decision = simulator.step(discard).decision
    assert decision.observation.choice_options
    assert any(action.kind is ActionKind.SELECT_CARD for action in decision.actions)
    assert not decision.observation.potions
