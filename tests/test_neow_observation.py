import re
from copy import deepcopy
from pathlib import Path

import pytest
import torch

from sls.backends.simulator import SimulatorBackend
from sls.content.neow import NEOW_BONUSES, NEOW_DRAWBACKS, neow_properties
from sls.model import PolicyBatch


def test_native_enum_mapping_matches_source():
    source = (Path(__file__).parents[1] / 'native/simulator/include/game/Neow.h').read_text()
    for enum, expected in [('Bonus', NEOW_BONUSES), ('Drawback', NEOW_DRAWBACKS)]:
        body = re.search(r'enum class ' + enum + r'\s*\{(.*?)\};', source, re.S)[1]
        names = tuple(x for x in re.findall(r'\b[A-Z][A-Z_0-9]+\b', body) if x != 'INVALID')
        assert names == expected


@pytest.mark.parametrize('bonus', range(len(NEOW_BONUSES)))
@pytest.mark.parametrize('drawback', range(1, len(NEOW_DRAWBACKS) + 1))
def test_offer_features_are_semantic_and_do_not_contain_outcomes(bonus, drawback):
    fields = dict(neow_properties(bonus, drawback))
    assert len(fields) == 2
    assert all(v is True for v in fields.values())
    assert all(k.startswith(('neow_bonus_', 'neow_drawback_')) for k in fields)


def test_dynamic_offers_change_actual_model_inputs_without_changing_action_identity():
    backend = SimulatorBackend()
    first = backend.reset(0)
    raw = deepcopy(backend.raw_state)
    raw['public_screen']['neow_options'][0] = {'bonus': 10, 'drawback': 1}
    gold = backend._adapt(raw)
    raw['public_screen']['neow_options'][0] = {'bonus': 1, 'drawback': 4}
    curse = backend._adapt(raw)
    assert gold.actions == curse.actions == first.actions
    assert gold.observation.event_options[0].content_id == curse.observation.event_options[0].content_id
    x, y = PolicyBatch.from_decisions((gold,)), PolicyBatch.from_decisions((curse,))
    assert not torch.equal(x.entity_numeric, y.entity_numeric)
    assert dict(gold.observation.event_options[0].properties) == dict(neow_properties(10, 1))


def test_stale_native_cannot_silently_drop_neow_information():
    backend = SimulatorBackend()
    backend.reset(0)
    raw = deepcopy(backend.raw_state)
    del raw['public_screen']['neow_options']
    with pytest.raises(ValueError, match='rebuild'):
        backend._adapt(raw)


def test_original_and_simulator_offer_semantics_match_including_boss_swap():
    from sls.backends.original.adapter import adapt_original
    from tests.original.test_adapter import base_game
    offers = [{'bonus': 'HUNDRED_GOLD', 'drawback': 'NONE'},
              {'bonus': 'BOSS_RELIC', 'drawback': 'NONE'}]
    payload = {'in_game': True, 'ready_for_command': True, 'available_commands': ['choose'],
               'game_state': base_game(floor=0, screen_type='EVENT', choice_list=['first', 'second'],
                                      screen_state={'event_id': 'Neow', 'neow_options': offers})}
    result = adapt_original(payload).decision
    assert dict(result.observation.event_options[0].properties) == dict(neow_properties(10, 1))
    assert dict(result.observation.event_options[1].properties) == dict(neow_properties(18, 6))
    del payload['game_state']['screen_state']['neow_options']
    with pytest.raises(ValueError, match='observation oracle'):
        adapt_original(payload)
