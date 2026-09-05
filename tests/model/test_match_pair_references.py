"""Match pairs must refer to both publicly visible slots."""

import json
from dataclasses import replace

import torch

from sls.backends.simulator import SimulatorBackend
from sls.contracts import Decision
from sls.model import encode_decision
from sls.model.encoding import NUMERIC_FIELD_IDS


def test_match_pair_encoding_contains_both_slots_and_their_known_cards():
    backend = SimulatorBackend()
    backend.reset(0)
    backend._native.reset_event_probe(0, "MATCH_AND_KEEP", backend.raw_state["rng"])
    decision = backend._adapt(backend._native.snapshot())
    assert len(decision.actions) == 66
    for action in decision.actions:
        _, left, right = action.option_id.split(":")
        assert action.subject_id == f"match-slot:{left}"
        assert action.target_id == f"match-slot:{right}"
    slots = list(decision.observation.event_options)
    for index, content in enumerate(["BASH", "STRIKE_RED", "BASH"]):
        slots[index] = replace(slots[index], content_id=content,
                               properties=(("known", True), ("removed", False)))
    observed = Decision(replace(decision.observation, event_options=tuple(slots)), decision.actions)
    encoded = encode_decision(observed)
    assert encoded.action_reference_mask[:, :2].all()
    matching = next(i for i, a in enumerate(decision.actions) if a.option_id == "match-pair:0:2")
    other = next(i for i, a in enumerate(decision.actions) if a.option_id == "match-pair:1:2")
    pair_contents = encoded.entity_content[encoded.action_references[:, :2]]
    assert not torch.equal(pair_contents[matching], pair_contents[other])
    assert torch.equal(pair_contents[matching, 0], pair_contents[matching, 1])


def test_hidden_match_cards_are_not_used_as_pair_features():
    backend = SimulatorBackend()
    backend.reset(0)
    backend._native.reset_event_probe(0, "MATCH_AND_KEEP", backend.raw_state["rng"])
    decision = backend._adapt(backend._native.snapshot())
    assert all(s.content_id == "HIDDEN_CARD" for s in decision.observation.event_options)
    encoded = encode_decision(decision)
    pair_contents = encoded.entity_content[encoded.action_references[:, :2]]
    assert torch.equal(pair_contents, pair_contents[0, 0].expand_as(pair_contents))


def test_match_attempts_decrement_and_survive_pending_event_checkpoint():
    backend = SimulatorBackend()
    # Enter through a real run: a probe-only event has no replayable history.
    decision = backend.reset(7)
    for _ in range(150):
        if backend.raw_state["public_screen"].get("match_slots"):
            break
        action = decision.actions[0]
        if decision.observation.screen.value == "MAP":
            event_nodes = {node.node_id for node in decision.observation.map_nodes
                           if node.visible_room_type == "EVENT"}
            action = next((a for a in decision.actions if a.node_id in event_nodes), action)
        decision = backend.step(action).decision
    assert backend.raw_state["public_screen"].get("match_slots")
    field = NUMERIC_FIELD_IDS["attempts_remaining"]
    previous = None
    for remaining in range(5, 0, -1):
        assert dict(decision.observation.public_context)["attempts_remaining"] == remaining
        encoded = encode_decision(decision)
        assert encoded.entity_numeric_present[1, field]
        if previous is not None:
            assert encoded.entity_numeric[1, field] < previous
        previous = encoded.entity_numeric[1, field]
        restored = SimulatorBackend()
        assert restored.load_checkpoint(json.loads(json.dumps(backend.checkpoint()))) == decision
        action = decision.actions[0]
        decision = backend.step(action).decision
        assert restored.step(action).decision == decision
    assert "attempts_remaining" not in dict(decision.observation.public_context)
