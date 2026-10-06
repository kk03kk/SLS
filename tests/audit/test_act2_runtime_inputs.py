import pytest

from tools.replay_act2_system_batch import stock_runtime_inputs


def test_stock_timing_is_explicit_control_input_and_adapter_cannot_forge_it():
    before = {'stock_raw': {'_timing_evidence': {'discovery_completion_serial': 0}}}
    after = {'stock_raw': {'_timing_evidence': {'discovery_completion_serial': 1,
                                               'discovery_retrieval_updates': 15}}}
    assert stock_runtime_inputs(before, after) == {'discovery_retrieval_updates': 15}
    after['previous_action_validation_evidence'] = {'discovery_retrieval_updates': 14}
    with pytest.raises(ValueError, match='disagrees'):
        stock_runtime_inputs(before, after)


def test_unchanged_stock_serial_does_not_inject_timing():
    record = {'stock_raw': {'_timing_evidence': {'discovery_completion_serial': 1,
                                                'discovery_retrieval_updates': 15}}}
    assert stock_runtime_inputs(record, record) == {}
