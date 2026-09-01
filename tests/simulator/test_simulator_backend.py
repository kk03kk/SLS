from __future__ import annotations

import gzip
import hashlib
import json
from copy import deepcopy
from pathlib import Path

import pytest

from sls.contracts import (
    Action,
    ActionKind,
    Decision,
    Observation,
    Player,
    RunContext,
    ScreenType,
)
from sls.model.batching import encode_decision


def _terminal_transition(
    backend: object, *, outcome: int, act: int, hp: int,
) -> object:
    previous = backend.reset(0).observation
    raw = deepcopy(backend.raw_state)
    raw["public_run"]["outcome"] = outcome
    raw["public_run"]["act"] = act
    raw["player_state"]["current_hp"] = hp
    raw["legal_actions"] = []
    return backend._transition_from_raw(previous, raw)


def test_native_full_run_reaches_a_canonical_decision() -> None:
    pytest.importorskip("sls.backends.simulator.native", exc_type=ImportError)
    from sls.backends.simulator import IRONCLAD_A0_ACT1, SimulatorBackend

    backend = SimulatorBackend(IRONCLAD_A0_ACT1)
    decision = backend.reset(0)
    assert not decision.terminal
    assert decision.actions
    assert decision.observation.run.act == 1


@pytest.mark.parametrize(
    ("task", "source", "option_count", "legal_indices", "controls"),
    (
        ("CODEX", "GENERATED", 3, (0, 1, 2, 3), ({"choice_index": 3, "kind": "SKIP"},)),
        ("HEADBUTT", "DISCARD", 6, (0, 1, 2, 3, 4, 5), ()),
        ("ARMAMENTS", "HAND", 6, (1, 4), ()),
        ("DISCOVERY", "GENERATED", 3, (0, 1, 2), ()),
    ),
)
def test_combat_choice_actions_always_reference_encoded_entities(
    task: str,
    source: str,
    option_count: int,
    legal_indices: tuple[int, ...],
    controls: tuple[dict[str, object], ...],
) -> None:
    from sls.backends.simulator.environment import _screen_entities, _semantic_actions

    raw = {
        "public_run": {"outcome": 1, "screen_state": 9, "current_event_id": ""},
        "progress_state": {},
        "public_combat": {
            "monsters": [],
            "choice": {
                "task": task,
                "source": source,
                "options": [
                    {
                        "choice_index": index,
                        "content_id": "STRIKE_RED",
                        "upgrades": 0,
                    }
                    for index in range(option_count)
                ],
                "controls": list(controls),
            },
        },
        "public_screen": {},
        "legal_actions": [
            {
                "bits": index + 1,
                "action_type": 2,
                "source_index": index,
                "target_index": 0,
                "domain": "COMBAT",
                "requires_target": False,
            }
            for index in legal_indices
        ],
    }
    actions, _ = _semantic_actions(raw, ())
    choices = _screen_entities(raw)["choice"]
    decision = Decision(
        Observation(
            Player("IRONCLAD", 80, 80, 0, 3, 3),
            RunContext(0, 2, 20, 99, False, False, False),
            ScreenType.COMBAT,
            choice_options=choices,
        ),
        actions,
    )

    encoded = encode_decision(decision)

    assert [choice.instance_id for choice in choices] == [
        f"CHOICE:{index}" for index in legal_indices
    ]
    assert encoded.action_reference_mask[:, 0].all()


def test_combat_choice_contract_rejects_an_unpublished_control() -> None:
    from sls.backends.simulator.environment import _screen_entities

    raw = {
        "public_run": {"outcome": 1, "screen_state": 9, "current_event_id": ""},
        "public_combat": {
            "choice": {
                "task": "CODEX",
                "source": "GENERATED",
                "options": [
                    {"choice_index": index, "content_id": "STRIKE_RED", "upgrades": 0}
                    for index in range(3)
                ],
                "controls": [],
            },
        },
        "public_screen": {},
        "legal_actions": [{
            "bits": 4,
            "action_type": 2,
            "source_index": 3,
            "target_index": 0,
            "domain": "COMBAT",
            "requires_target": False,
        }],
    }

    with pytest.raises(
        ValueError,
        match=r"native CODEX choice actions lack public entities: \['CHOICE:3'\]",
    ):
        _screen_entities(raw)


def test_positive_hp_native_loss_is_a_negative_terminal() -> None:
    pytest.importorskip("sls.backends.simulator.native", exc_type=ImportError)
    from sls.backends.simulator import SimulatorBackend
    from sls.curriculum import IRONCLAD_A0_FULLRUN

    transition = _terminal_transition(
        SimulatorBackend(IRONCLAD_A0_FULLRUN), outcome=0, act=1, hp=17,
    )

    assert transition.terminated and transition.decision.terminal
    assert transition.info == {
        "reason": "DEATH",
        "success": False,
        "terminal_outcome": "PLAYER_LOSS",
    }
    assert transition.reward == -1.0


def test_positive_hp_native_victory_requires_the_fullrun_horizon() -> None:
    pytest.importorskip("sls.backends.simulator.native", exc_type=ImportError)
    from sls.backends.simulator import SimulatorBackend
    from sls.curriculum import IRONCLAD_A0_FULLRUN

    premature = _terminal_transition(
        SimulatorBackend(IRONCLAD_A0_FULLRUN), outcome=2, act=2, hp=17,
    )
    victory = _terminal_transition(
        SimulatorBackend(IRONCLAD_A0_FULLRUN), outcome=2, act=3, hp=17,
    )

    assert premature.terminated and not premature.info["success"]
    assert premature.info["reason"] == "ACT_3_NOT_REACHED"
    assert victory.terminated and victory.info["success"]
    assert victory.info["reason"] == "GAME_VICTORY"
    assert victory.info["terminal_outcome"] == "PLAYER_VICTORY"
    assert victory.reward == 1.0


def test_native_reported_act_two_boss_clear_ends_before_rewards() -> None:
    pytest.importorskip("sls.backends.simulator.native", exc_type=ImportError)
    from sls.backends.simulator import SimulatorBackend
    from sls.curriculum import IRONCLAD_A0_ACT2

    backend = SimulatorBackend(IRONCLAD_A0_ACT2)
    previous = backend.reset(0).observation
    raw = deepcopy(backend.raw_state)
    raw["public_run"].update({
        "act": 2, "floor": 33, "outcome": 1, "screen_state": 2,
        "completed_act": 2,
    })
    raw["progress_state"].update({"current_room": 6, "screen_state": 2})
    raw["public_screen"] = {
        "card_rewards": [], "gold": [], "relics": [], "potions": [],
        "emerald_key": False, "sapphire_key": False,
    }
    raw["legal_actions"] = [{
        "bits": 1, "domain": "RUN", "idx1": 0, "idx2": 0,
        "reward_type": 6, "potion": False,
    }]
    transition = backend._transition_from_raw(previous, raw)

    assert transition.terminated and transition.info["success"]
    assert transition.info["reason"] == "ACT_2_CLEARED"
    assert transition.decision.actions == ()


def test_seed_zero_neow_transform_uses_the_stock_rng_counter() -> None:
    """The fixed boss option consumes one draw; transform is the sixth draw."""

    pytest.importorskip("sls.backends.simulator.native", exc_type=ImportError)
    from sls.backends.simulator import IRONCLAD_A0_ACT1, SimulatorBackend

    backend = SimulatorBackend(IRONCLAD_A0_ACT1)
    decision = backend.reset(0)
    transform = next(
        action for action in decision.actions
        if action.kind is ActionKind.CHOOSE_NEOW_OPTION
        and action.option_id == "event-option:0"
    )
    decision = backend.step(transform).decision

    assert decision.observation.screen.value == "CARD_REWARD"
    assert backend.checkpoint()["rng"]["neow"]["counter"] == 5
    fifth_strike = next(
        action for action in decision.actions
        if action.kind is ActionKind.SELECT_CARD
        and action.subject_id == "select-card:4"
    )
    decision = backend.step(fifth_strike).decision

    assert decision.observation.screen.value == "MAP"
    assert [card.card_id for card in decision.observation.deck][-1] == "WILD_STRIKE"
    assert backend.checkpoint()["rng"]["neow"]["counter"] == 6


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
