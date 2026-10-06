import copy
import json

import pytest

from tools import check_training_configs, generate_content_registry


def test_config_check_supports_external_root(tmp_path, capsys):
    config = tmp_path / 'external.toml'
    config.write_text('[run]\noutput = "example"\n[ppo]\n', encoding='utf-8')
    assert check_training_configs.main(['--root', str(tmp_path)]) == 0
    assert config.resolve().as_posix() in capsys.readouterr().out


def test_registry_check_is_read_only_even_when_stale(tmp_path, monkeypatch):
    output = tmp_path / 'registry.json'
    output.write_bytes(b'original-evidence')
    monkeypatch.setattr(generate_content_registry, 'OUTPUT', output)
    monkeypatch.setattr(generate_content_registry, 'build_registry', lambda: {'new': True})
    assert generate_content_registry.main(['--check']) == 1
    assert output.read_bytes() == b'original-evidence'
    assert not output.with_suffix('.json.tmp').exists()


@pytest.mark.parametrize('change, expected', [
    ('hash', 'config_sha256'),
    ('schedule', 'disagrees'),
    ('leak', 'overlap'),
])
def test_experiment_check_rejects_drift_and_final_seed_leak(change, expected):
    root = check_training_configs.ROOT
    path = root / 'configs/experiments/win-90m-20261001.json'
    plan = json.loads(path.read_text(encoding='utf-8'))
    if change == 'hash':
        plan['config_sha256'] = '0' * 64
    elif change == 'schedule':
        plan['development_confirmation_seeds'][0] += 1
    else:
        plan['reserved_final_seeds'] = plan['periodic_selection_seeds'].copy()
    assert any(expected in problem for problem in check_training_configs.validate_json(path, plan))


def test_json_scan_is_read_only_and_rejects_unknown_schema(tmp_path):
    path = tmp_path / 'plan.json'
    raw = b'{"schema": "unknown"}'
    path.write_bytes(raw)
    assert check_training_configs.main(['--root', str(tmp_path)]) == 1
    assert path.read_bytes() == raw


def test_compatibility_check_rejects_nonhex_and_duplicate_transitions(tmp_path):
    path = tmp_path / 'compatibility/state-preserving-source-transitions.json'
    record = {'from': 'a' * 64, 'to': 'b' * 64, 'reason': 'reviewed'}
    assert 'duplicate' in ' '.join(check_training_configs.validate_json(path, [record, record]))
    record['from'] = 'z' * 64
    assert 'invalid SHA256' in ' '.join(check_training_configs.validate_json(path, [record]))


def test_completed_plan_preserves_bindings_but_cannot_be_resubmitted():
    from tools.submit_act12_pilot import validate_plan

    path = check_training_configs.ROOT / 'configs/experiments/act12-win-pilot.json'
    plan = json.loads(path.read_text(encoding='utf-8'))
    assert not check_training_configs.validate_json(path, plan)
    with pytest.raises(ValueError, match='bound'):
        validate_plan(plan, deep=False)
    corrupted = copy.deepcopy(plan)
    corrupted['target_training_implementation_sha256'] = '0' * 64
    assert any('historical' in e for e in check_training_configs.validate_json(path, corrupted))


def test_active_plan_still_rejects_stale_implementation():
    path = check_training_configs.ROOT / 'configs/experiments/act12-win-pilot.json'
    plan = json.loads((check_training_configs.ROOT /
        'docs/results/act12-pilot-20261006/original-bound-plan.json').read_text(encoding='utf-8'))
    assert 'bound implementation changed' in check_training_configs.validate_json(path, plan)
