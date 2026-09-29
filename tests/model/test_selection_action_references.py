from dataclasses import replace

import pytest

from sls.backends.simulator import SimulatorBackend
from sls.backends.simulator.environment import _screen_entities, _semantic_actions
from sls.contracts import Action, ActionKind, Decision, ScreenType
from sls.model import encode_decision


@pytest.mark.parametrize("room,event", [(1, "INVALID"), (0, "NEOW")])
def test_standalone_bowl_index_five_is_not_a_select_card_reference(room, event):
    raw = {
        "public_run": {"outcome": 1, "screen_state": 2, "current_event_id": event},
        "progress_state": {"current_room": room},
        "public_screen": {"card_rewards": [[{"content_id": "ANGER"}, {"content_id": "BASH"}]],
                          "gold": [], "potions": [], "relics": []},
        "legal_actions": [{"bits": i + 1, "reward_type": 0, "idx1": 0, "idx2": i,
                           "potion": False} for i in (0, 1, 5, 6)],
    }
    actions, mapping = _semantic_actions(raw, ())
    base = SimulatorBackend().reset(0).observation
    observation = replace(base, screen=ScreenType.CARD_REWARD,
                          reward_options=_screen_entities(raw)["reward"])
    # Reproduce the precise old failure, retaining fail-closed model validation.
    with pytest.raises(ValueError, match="unresolved action subject_id: select-card:5"):
        encode_decision(Decision(observation, (Action(ActionKind.SELECT_CARD, subject_id="select-card:5"),)))
    bowl = next(a for a in actions if a.kind is ActionKind.TAKE_SINGING_BOWL)
    assert bowl.subject_id is None and bowl.option_id == "reward-card:0"
    assert mapping[bowl.candidate_id] == 6
    encode_decision(Decision(observation, actions))


@pytest.mark.parametrize("select_type", ["TRANSFORM", "TRANSFORM_UPGRADE", "UPGRADE", "REMOVE",
                                       "DUPLICATE", "OBTAIN", "BOTTLE", "BONFIRE_SPIRITS"])
def test_grid_selection_index_five_is_a_real_card_across_selection_types(select_type):
    raw = {
        "public_run": {"outcome": 1, "screen_state": 4, "current_event_id": "INVALID"},
        "progress_state": {},
        "public_screen": {"select_type": select_type, "card_options": [
            {"instance_id": f"select-card:{i}", "content_id": "STRIKE_RED", "deck_index": i + 3}
            for i in range(8)]},
        "legal_actions": [{"bits": i + 1, "reward_type": 0, "idx1": i, "idx2": 0,
                           "potion": False} for i in range(8)],
    }
    actions, _ = _semantic_actions(raw, ())
    observation = replace(SimulatorBackend().reset(0).observation,
                          screen=ScreenType.CARD_REWARD, reward_options=_screen_entities(raw)["reward"])
    assert any(a.subject_id == "select-card:5" for a in actions)
    encode_decision(Decision(observation, actions))


def test_preflight_exercises_the_selection_encoding_regression():
    from tools.preflight_training import check_standalone_reward_encoding

    check_standalone_reward_encoding(SimulatorBackend().reset(0))


@pytest.mark.parametrize("index", [-1, 3])
def test_standalone_selection_rejects_indices_outside_public_card_options(index):
    raw = {
        "public_run": {"outcome": 1, "screen_state": 2, "current_event_id": "NEOW"},
        "progress_state": {},
        "public_screen": {"card_rewards": [[{"content_id": "ANGER"}]],
                          "gold": [], "potions": [], "relics": []},
        "legal_actions": [{"bits": 1, "reward_type": 0, "idx1": 0, "idx2": index, "potion": False}],
    }
    with pytest.raises(ValueError, match="outside public options"):
        _semantic_actions(raw, ())
