import hashlib
import json

import pytest

from tools import replay_act2_production_trajectory, replay_act2_system_batch
from tools.capture_act2_stock_policy_system_batch import (
    ValidationPolicyBackend,
    convert_trajectory,
)


def test_controlled_actor_requires_stock_validation_evidence():
    with pytest.raises(ValueError, match='validation RNG'):
        ValidationPolicyBackend.require_isolation({'_oracle_mode': 'production'})
    raw = {'_oracle_mode': 'validation', '_rng': {}, 'game_state': {'combat_state': {'turn': 1}}}
    with pytest.raises(ValueError, match='independent stock objects'):
        ValidationPolicyBackend.require_isolation(raw)


def test_conversion_preserves_stock_values_and_single_actor_script(tmp_path):
    source, output = tmp_path / 'actor.jsonl', tmp_path / 'script.jsonl'
    raw = {'game_state': {'hp': 27}, '_rng': {'ai': {'counter': 42}}}
    record = {'observation': {'hp': 27}, 'candidate_actions': [{'kind': 'END_TURN'}],
              'terminal': False, 'chosen_action': {'kind': 'END_TURN'},
              'diagnostic_state': {'stock_payload': raw,
                  'previous_action_validation_evidence': {'discovery_retrieval_updates': 15}}}
    source.write_text(json.dumps({'record_type': 'metadata'}) + '\n' + json.dumps(record) + '\n')
    assert convert_trajectory(source, output) == 1
    result = json.loads(output.read_text())
    assert result['stock_raw'] == raw
    assert result['requested_action'] == record['chosen_action']
    assert result['previous_action_validation_evidence'] == record['diagnostic_state']['previous_action_validation_evidence']
    with pytest.raises(FileExistsError):
        convert_trajectory(source, output)


@pytest.mark.parametrize('records,declared_terminal,message', [
    ([], False, 'missing system boundaries'),
    ([{'terminal': False, 'requested_action': None}], True, 'terminal stock boundary'),
])
def test_replay_rejects_missing_or_falsely_completed_capture(
    tmp_path, monkeypatch, records, declared_terminal, message
):
    class UnusedBackend:
        def __init__(self, profile):
            pass

        def reset(self, seed):
            return None

    monkeypatch.setattr(replay_act2_system_batch, 'SimulatorBackend', UnusedBackend)
    path = tmp_path / 'stock.jsonl'
    path.write_text(''.join(json.dumps(record) + '\n' for record in records))
    row = {'stock_capture': str(path), 'sha256': hashlib.sha256(path.read_bytes()).hexdigest(),
           'seed': 131100063, 'boundaries': len(records), 'terminal': declared_terminal}
    with pytest.raises(ValueError, match=message):
        replay_act2_system_batch.replay(row)


def test_system_gate_rejects_old_execution_even_when_declared_complete(tmp_path, monkeypatch):
    import sys

    capture = tmp_path / 'capture.json'
    capture.write_text(json.dumps({'execution_complete': True,
        'original_execution_contract': 'sls-original-choice-public-boundary-v6'}))
    monkeypatch.setattr(sys, 'argv', ['replay', str(capture), '--output', str(tmp_path / 'result.json')])
    with pytest.raises(ValueError, match='stale original execution contract'):
        replay_act2_system_batch.main()


def test_production_gate_rejects_old_execution_before_comparison(tmp_path, monkeypatch):
    monkeypatch.setattr(replay_act2_production_trajectory, 'read_trajectory',
        lambda _: ({'environment': {'original_execution_contract':
            'sls-original-choice-public-boundary-v6'}}, []))
    with pytest.raises(ValueError, match='stale original execution contract'):
        replay_act2_production_trajectory.replay(tmp_path / 'trajectory.jsonl')
