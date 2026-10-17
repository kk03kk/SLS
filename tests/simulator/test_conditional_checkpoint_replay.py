import copy
import json
from pathlib import Path

import pytest

from sls.backends.simulator import SimulatorBackend
from sls.curriculum import IRONCLAD_A20_ACT2


def fixture():
    return json.loads(Path('tests/fixtures/regressions/discovery-conditional-checkpoint-131100066.json').read_text())


def test_stock_measured_inputs_survive_full_history_checkpoint_replay():
    data = fixture()
    checkpoint = copy.deepcopy(data['old_checkpoint'])
    checkpoint['validation_replay_contract'] = data['expected_contract']
    checkpoint['replay_validation_inputs'] = data['independent_stock_inputs']
    # The independently captured Guardian is a single-slot encounter. Current
    # checkpoints additionally retain the private array extent; preserve every
    # historical field and explicitly assert the new value, rather than drop it.
    expected = copy.deepcopy(checkpoint)
    assert [m['id'] for m in expected['combat_checkpoint']['game_state']['combat_state']['monsters']] == ['THE_GUARDIAN']
    expected['combat_checkpoint']['game_state']['combat_state']['_internal']['monster_count'] = 1
    internal = expected['combat_checkpoint']['game_state']['combat_state']['_internal']
    internal['selector_memory'] = copy.deepcopy(internal['choice'])
    expected['public_combat']['choice']['pick_count'] = 1
    backend = SimulatorBackend(IRONCLAD_A20_ACT2)
    backend.load_checkpoint(checkpoint)
    assert json.loads(json.dumps(backend.checkpoint())) == expected
    restored = SimulatorBackend(IRONCLAD_A20_ACT2)
    restored.load_checkpoint(json.loads(json.dumps(backend.checkpoint())))
    assert restored.checkpoint() == backend.checkpoint()


def test_legacy_conditional_capture_without_inputs_is_not_whitelisted():
    with pytest.raises(ValueError, match='Deterministic replay'):
        SimulatorBackend(IRONCLAD_A20_ACT2).load_checkpoint(fixture()['old_checkpoint'])


@pytest.mark.parametrize('field,value', [('discovery_retrieval_updates', 0), ('unknown', 15)])
def test_invalid_conditional_replay_inputs_are_rejected(field, value):
    data = fixture()
    checkpoint = data['old_checkpoint']
    checkpoint['validation_replay_contract'] = data['expected_contract']
    checkpoint['replay_validation_inputs'] = [{'action_index': 126, field: value}]
    with pytest.raises(ValueError, match='Invalid validation replay input'):
        SimulatorBackend(IRONCLAD_A20_ACT2).load_checkpoint(checkpoint)
