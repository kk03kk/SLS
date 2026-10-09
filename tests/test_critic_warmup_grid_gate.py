"""Lightweight GRID gate regression: no model, rollout, Torch or GPU."""
import json
from pathlib import Path
from types import SimpleNamespace

import pytest

from sls.contracts import Action, ActionKind
from tools.verify_act12_critic_warmup import legal_action, verify_grid

ROOT = Path(__file__).resolve().parents[1]


def test_missing_schema_reproduces_original_failure():
    with pytest.raises(ValueError, match="unsupported action schema -1"):
        Action.from_dict({"kind": "REMOVE_CARD", "subject_id": "select-card:0"})


def test_current_candidate_is_reused_with_full_metadata_and_identity():
    candidate = Action(ActionKind.REMOVE_CARD, subject_id="select-card:0",
                       metadata=(("selected", True),))
    decision = SimpleNamespace(actions=(candidate, Action(ActionKind.PROCEED)))
    assert legal_action(decision, ActionKind.REMOVE_CARD, "select-card:0") is candidate
    assert Action(ActionKind.REMOVE_CARD, subject_id="select-card:0").candidate_id != candidate.candidate_id
    with pytest.raises(RuntimeError, match="got 0"):
        legal_action(SimpleNamespace(actions=()), ActionKind.REMOVE_CARD, "select-card:0")
    with pytest.raises(RuntimeError, match="got 2"):
        legal_action(SimpleNamespace(actions=(candidate, candidate)), ActionKind.REMOVE_CARD, "select-card:0")


def fixture():
    return json.loads((ROOT / "tests/fixtures/regressions/act2-empty-cage-grid-131100069.json").read_text())


class RecordingBackend:
    """A few actual native CPU decisions; enforce use of current Action objects."""
    def __init__(self):
        from sls.backends.simulator import SimulatorBackend
        from sls.curriculum import IRONCLAD_A20_ACT2
        self.backend = SimulatorBackend(IRONCLAD_A20_ACT2)
        self.current = None
        self.used = []

    def load_checkpoint(self, snapshot):
        self.current = self.backend.load_checkpoint(snapshot)
        return self.current

    def checkpoint(self):
        return self.backend.checkpoint()

    def step(self, action):
        assert any(action is candidate for candidate in self.current.actions)
        self.used.append(action)
        result = self.backend.step(action)
        self.current = result.decision
        return result


def test_complete_stock_grid_gate_uses_current_actions_and_distinct_instances():
    backend, restored = RecordingBackend(), RecordingBackend()
    encoded = []
    # The real encoder check remains in main; this callback checks that the
    # same selected decision is passed to it, without importing Torch locally.
    def check_selection(decision):
        assert decision.observation.to_dict()["selected_cards"] == fixture()["stock_partial_selection"]
        encoded.append(decision)
    verify_grid(backend, restored, fixture(), check_selection)
    assert len(encoded) == 1
    assert len(backend.used) == 4 and len(restored.used) == 1
    assert backend.used[0].subject_id == backend.used[1].subject_id
    assert backend.used[-2].subject_id != backend.used[-1].subject_id
    assert all(action.kind is ActionKind.REMOVE_CARD for action in backend.used)


@pytest.mark.parametrize("fault", ["projection", "restoration", "commit", "final_action"])
def test_gate_still_rejects_grid_semantic_regressions(fault):
    backend, restored = RecordingBackend(), RecordingBackend()
    source = fixture()
    expected = ""
    if fault == "projection":
        source["stock_candidate_count"] += 1
        expected = "partial projection"
    elif fault == "restoration":
        original = restored.checkpoint
        restored.checkpoint = lambda: original() | {"corrupted": True}
        expected = "cancellation/restoration"
    elif fault == "commit":
        source["before"]["public_inventory"]["deck"].append(
            source["before"]["public_inventory"]["deck"][0])
        # Change expected deck only, after initial loading, not simulator state.
        original = backend.load_checkpoint
        def load(snapshot):
            assert snapshot is source["before"]
            return original(fixture()["before"])
        backend.load_checkpoint = load
        expected = "distinct-instance commit"
    else:
        original = backend.step
        def step(action):
            result = original(action)
            if len(backend.used) == 4:
                return SimpleNamespace(decision=SimpleNamespace(actions=(action,)))
            return result
        backend.step = step
        expected = "final pick did not commit"
    with pytest.raises(RuntimeError, match=expected):
        verify_grid(backend, restored, source, lambda decision: None)


def test_encoding_failure_cannot_be_swallowed_by_grid_gate():
    def reject(decision):
        raise RuntimeError("GRID selected state missing from policy encoding")
    with pytest.raises(RuntimeError, match="missing from policy encoding"):
        verify_grid(RecordingBackend(), RecordingBackend(), fixture(), reject)


def test_gate_rejects_missing_legal_cancellation():
    backend, restored = RecordingBackend(), RecordingBackend()
    original = backend.step
    subject = fixture()["action"]["subject_id"]
    def step(action):
        result = original(action)
        if len(backend.used) == 1:
            return SimpleNamespace(decision=SimpleNamespace(
                observation=result.decision.observation,
                actions=tuple(a for a in result.decision.actions if a.subject_id != subject)))
        return result
    backend.step = step
    with pytest.raises(RuntimeError, match="expected one legal REMOVE_CARD.*got 0"):
        verify_grid(backend, restored, fixture(), lambda decision: None)
