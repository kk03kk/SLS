"""Fail closed on malformed public inputs and native command families."""
from dataclasses import replace

import pytest

from sls.backends.simulator import SimulatorBackend
from sls.backends.simulator.environment import _semantic_actions
from sls.contracts import Action, ActionKind, Decision
from sls.contracts.observation import validate_policy_observation
from sls.model import encode_decision
from sls.model.batching import _number


@pytest.mark.parametrize("action", [
    Action(ActionKind.CHOOSE_EVENT_OPTION, option_id="missing-event:0"),
    Action(ActionKind.PROCEED, option_id="anything-at-all"),
    Action(ActionKind.CHOOSE_EVENT_OPTION, option_id="combat-selection"),
])
def test_unknown_option_reference_is_not_synthesized(action):
    observation = SimulatorBackend().reset(132000000).observation
    with pytest.raises(ValueError, match="unresolved action option_id"):
        encode_decision(Decision(observation, (action,)))


@pytest.mark.parametrize("value", [float("nan"), float("inf"), -float("inf")])
def test_nonfinite_observation_is_rejected_at_contract_and_encoder(value):
    decision = SimulatorBackend().reset(132000000)
    with pytest.raises(ValueError, match="non-finite public value"):
        replace(decision.observation, player=replace(decision.observation.player, current_hp=value))
    wire = decision.observation.to_dict()
    wire["player"]["current_hp"] = value
    with pytest.raises(ValueError, match="non-finite public value"):
        validate_policy_observation(wire)
    with pytest.raises(ValueError, match="must be finite"):
        _number(value, path="test")


@pytest.mark.parametrize("field", ["zone", "source", "intent", "screen", "item_type", "visible_boss"])
def test_action_category_metadata_must_not_be_silently_discarded(field):
    observation = SimulatorBackend().reset(132000000).observation
    action = Action(ActionKind.END_TURN, metadata=((field, "HAND"),))
    with pytest.raises(ValueError, match="categorical action metadata"):
        encode_decision(Decision(observation, (action,)))


@pytest.mark.parametrize("action_type", [-1, 5, 99])
def test_unknown_combat_command_cannot_become_end_turn(action_type):
    raw = {"public_run": {"outcome": 1, "screen_state": 9}, "progress_state": {},
           "legal_actions": [{"domain": "COMBAT", "action_type": action_type,
                              "source_index": 0, "target_index": 0, "bits": 42}]}
    with pytest.raises(ValueError, match="unsupported native combat action type"):
        _semantic_actions(raw, ())


def test_real_end_turn_still_maps_to_the_original_command():
    raw = {"public_run": {"outcome": 1, "screen_state": 9}, "progress_state": {},
           "legal_actions": [{"domain": "COMBAT", "action_type": 4,
                              "source_index": 0, "target_index": 0, "bits": 42}]}
    actions, mapping = _semantic_actions(raw, ())
    assert actions[0].kind is ActionKind.END_TURN
    assert mapping[actions[0].candidate_id] == 42
