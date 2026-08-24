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


def test_noncombat_potion_actions_preserve_the_current_screen() -> None:
    pytest.importorskip("sls.backends.simulator.native", exc_type=ImportError)
    from sls.backends.simulator import IRONCLAD_A0_HEART, SimulatorBackend

    simulator = SimulatorBackend(IRONCLAD_A0_HEART)
    decision = simulator.reset(0)
    while decision.observation.screen.value != "MAP":
        decision = simulator.step(decision.actions[0]).decision

    checkpoint = simulator.checkpoint()
    checkpoint["player_state"]["potion_count"] = 3
    checkpoint["player_state"]["potions"] = [26, 7, 41]  # Fruit, Blood, Strength.
    decision = simulator.load_checkpoint(checkpoint)

    uses = {
        action.subject_id for action in decision.actions
        if action.kind is ActionKind.USE_POTION
    }
    discards = {
        action.subject_id for action in decision.actions
        if action.kind is ActionKind.DISCARD_POTION
    }
    assert uses == {"POTION:0", "POTION:1"}
    assert discards == {"POTION:0", "POTION:1", "POTION:2"}

    use_fruit = next(
        action for action in decision.actions
        if action.kind is ActionKind.USE_POTION and action.subject_id == "POTION:0"
    )
    decision = simulator.step(use_fruit).decision
    assert decision.observation.screen.value == "MAP"
    assert decision.observation.player.max_hp == 85
    assert any(action.kind is ActionKind.CHOOSE_MAP_NODE for action in decision.actions)
    assert [potion.instance_id for potion in decision.observation.potions] == [
        "POTION:1", "POTION:2",
    ]


def test_checkpoint_restore_rederives_legal_actions() -> None:
    pytest.importorskip("sls.backends.simulator.native", exc_type=ImportError)
    from sls.backends.simulator import IRONCLAD_A0_HEART, SimulatorBackend

    simulator = SimulatorBackend(IRONCLAD_A0_HEART)
    decision = simulator.reset(0)
    decision = simulator.step(decision.actions[0]).decision
    checkpoint = simulator.checkpoint()
    assert checkpoint["replay_actions"]
    expected_actions = decision.actions

    checkpoint["legal_actions"] = []
    restored = SimulatorBackend(IRONCLAD_A0_HEART)
    restored_decision = restored.load_checkpoint(checkpoint)

    assert restored_decision.actions == expected_actions


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
