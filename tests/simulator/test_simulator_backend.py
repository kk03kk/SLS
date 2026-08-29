from __future__ import annotations

import gzip
import hashlib
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


def test_worker_crash_dump_replays_the_exact_native_boundary(tmp_path: Path) -> None:
    pytest.importorskip("sls.backends.simulator.native", exc_type=ImportError)
    from sls.backends.simulator import IRONCLAD_A0_ACT1, SimulatorBackend
    from sls.rl.workers import _crash_payload, _write_crash_dump
    from tools.replay_failed_state import replay_dump

    backend = SimulatorBackend(IRONCLAD_A0_ACT1)
    expected = backend.reset(997)
    checkpoint_before = json.loads(json.dumps(backend.checkpoint()))
    payload = _crash_payload(
        backend,
        error=ValueError("a non-terminal decision must expose a legal action"),
        worker_index=23,
        episode_ordinal=1,
        seed=997,
        last_semantic_action=None,
        profile=IRONCLAD_A0_ACT1,
    )
    path = _write_crash_dump(tmp_path, payload)
    assert json.loads(json.dumps(backend.checkpoint())) == checkpoint_before
    replayed = replay_dump(path)
    assert replayed["terminal"] is False
    assert replayed["screen"] == expected.observation.screen.value
    assert replayed["actions"] == [action.to_dict() for action in expected.actions]


NUS_SEED_8335_DUMP = Path(__file__).resolve().parents[1] / "fixtures" / "regressions" / (
    "nus-worker-23-seed-8335-invalid-decision.json"
)
NUS_SEED_8335_SHA256 = "bbd6fa5644223ebee07681849d5e2654466cc21e27affbd69cf688a0404eb4a7"


def test_nus_seed_8335_start_of_combat_victory_reaches_rewards() -> None:
    """Reproduce the worker-23 failure from its exact native action history."""

    pytest.importorskip("sls.backends.simulator.native", exc_type=ImportError)
    from sls.backends.simulator import IRONCLAD_A0_ACT1, SimulatorBackend

    encoded = NUS_SEED_8335_DUMP.read_bytes()
    assert hashlib.sha256(encoded).hexdigest() == NUS_SEED_8335_SHA256
    payload = json.loads(encoded)
    assert payload["seed"] == 8335
    assert payload["worker_index"] == 23
    assert payload["worker_episode_ordinal"] == 6
    assert payload["last_semantic_action"]["kind"] == "CHOOSE_MAP_NODE"
    assert payload["last_semantic_action"]["node_id"] == "map:1:9"

    backend = SimulatorBackend(IRONCLAD_A0_ACT1)
    backend.reset(payload["seed"])
    raw = None
    for bits in payload["raw_backend_state"]["replay_actions"]:
        raw = backend._native.step(bits)
    assert raw is not None
    decision = backend._adapt(raw)

    assert decision.terminal is False
    assert decision.observation.screen.value == "COMBAT_REWARD"
    assert decision.observation.run.floor == 10
    assert [item.content_id for item in decision.observation.reward_options] == [
        "FLEX", "DROPKICK", "CLEAVE", "GOLD",
    ]
    assert [action.kind.value for action in decision.actions] == [
        "TAKE_REWARD", "CHOOSE_CARD_REWARD", "CHOOSE_CARD_REWARD",
        "CHOOSE_CARD_REWARD", "SKIP_REWARD",
    ]


def test_nus_seed_8335_v1_dump_migrates_and_replays() -> None:
    """The existing crash JSON remains loadable despite its terminal sub-checkpoint."""

    pytest.importorskip("sls.backends.simulator.native", exc_type=ImportError)
    from tools.replay_failed_state import replay_dump

    replayed = replay_dump(NUS_SEED_8335_DUMP)

    assert replayed["source"]["seed"] == 8335
    assert replayed["source"]["terminal_flag"] is False
    assert replayed["source"]["generated_actions"] == []
    assert replayed["terminal"] is False
    assert replayed["screen"] == "COMBAT_REWARD"
    assert replayed["restored"]["public_run_state"]["floor"] == 10
    assert replayed["restored"]["raw_legal_action_groups"]


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
