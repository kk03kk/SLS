import copy

import pytest

from sls.audit.decision_identity import canonical_projection, mapped_action


def observation(identifier):
    return {"choice_options": [{"instance_id": identifier, "content_id": "DEFEND_RED",
                                "properties": {"current_cost": 1, "upgrades": 0, "source": "HAND"}}]}


def test_opaque_choice_id_alias_keeps_card_properties_and_action_mask():
    left, right = observation("CHOICE:0"), observation("CHOICE:3")
    action = {"kind": "SELECT_CARD", "subject_id": "CHOICE:0"}
    mapped = mapped_action(action, left, right)
    assert mapped["subject_id"] == "CHOICE:3"
    assert canonical_projection(left, [action], False) == canonical_projection(right, [mapped], False)
    assert canonical_projection(left, [action], False) != canonical_projection(right, [], False)
    assert left["choice_options"][0]["instance_id"] == "CHOICE:0"


@pytest.mark.parametrize("field,value", [("current_cost", 0), ("upgrades", 1), ("source", "DISCARD")])
def test_alias_refuses_to_hide_material_choice_difference(field, value):
    left = observation("CHOICE:0")
    right = copy.deepcopy(observation("CHOICE:3"))
    right["choice_options"][0]["properties"][field] = value
    with pytest.raises(ValueError, match="different card properties"):
        mapped_action({"kind": "SELECT_CARD", "subject_id": "CHOICE:0"}, left, right)
    assert canonical_projection(left, [], False) != canonical_projection(right, [], False)
