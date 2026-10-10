"""Evidence-chain tests using tiny synthetic payloads, never game/model execution."""

import json

import pytest

from tools.verify_act12_archived_evidence import digest, verify_clock_archive


def fixture(tmp_path):
    def put(name, data):
        path = tmp_path / name
        path.write_text(json.dumps(data), encoding='utf-8')
        return path

    environment = {'profile_id': 'IRONCLAD_A20_ACT2'}
    trajectory = tmp_path / 'run.jsonl'
    trajectory.write_text('\n'.join(map(json.dumps, [
        {'seed': 10, 'backend': 'original', 'environment': environment},
        {'screen': 'NEOW', 'act': 1, 'floor': 0, 'terminal': False,
         'chosen_action': {'kind': 'CHOOSE_NEOW_OPTION'},
         'observation': {'run': {'ascension': 20}}},
        {'screen': 'GAME_OVER', 'act': 2, 'floor': 20, 'terminal': True},
    ])), encoding='utf-8')
    journal = put('journal.json', {'status': 'RECOVERED'})
    stdout = tmp_path / 'stdout.log'
    stdout.write_text('SLS_DISCOVERY_CLOCK_V1 seed=10 floor=0 serial=1 updates=15\n')
    batch = put('batch.json', {'execution_complete': True, 'runs': [
        {'seed': 10, 'output': str(trajectory), 'sha256': digest(trajectory)}]})
    launch = put('batch.launch.json', {'mode': 'production', 'recovery_status': 'RECOVERED',
        'completion': {'exit_code': 0}, 'recovery_journal': str(journal), 'oracle_sha256': 'abc'})
    report = put('report.json', {
        'schema': 'sls-act2-clock-conditioned-replay-v1',
        'status': 'CONDITIONAL_PUBLIC_TRAJECTORY_MATCH', 'first_divergence': None,
        'terminal_reason_verified': True,
        'purpose': 'CONDITIONAL_RULES_DIAGNOSTIC_NOT_UNCONDITIONAL_PRODUCTION_PASS',
        'seed': 10, 'boundaries_checked': 2, 'evaluation_environment': environment,
        'clock_inputs': [{'boundary': 0, 'seed': 10, 'floor': 0, 'serial': 1, 'updates': 15}],
        'batch_sha256': digest(batch), 'launch_evidence_sha256': digest(launch),
        'stock_clock_log_sha256': digest(stdout), 'trajectory_sha256': digest(trajectory),
        'oracle_build_sha256': 'abc',
    })
    return report, batch


def test_archived_witness_integrity_is_not_new_parity(tmp_path):
    report, batch = fixture(tmp_path)
    result = verify_clock_archive(report, batch)
    assert result['unconditional_production_pass'] is False
    assert result['new_native_execution'] is False


@pytest.mark.parametrize('field,value', [
    ('status', 'TRAJECTORY_MATCH'), ('terminal_reason_verified', False),
    ('trajectory_sha256', 'wrong'), ('stock_clock_log_sha256', 'wrong'),
    ('boundaries_checked', 3),
])
def test_archived_witness_rejects_tampering(tmp_path, field, value):
    report, batch = fixture(tmp_path)
    data = json.loads(report.read_text())
    data[field] = value
    report.write_text(json.dumps(data))
    with pytest.raises(ValueError):
        verify_clock_archive(report, batch)
