from __future__ import annotations

import copy
import json
import tomllib
from pathlib import Path

import pytest

from sls.rl.training_contract import source_sha256
from tools.analyze_act12_lambda_study import decision
from tools.prepare_act12_pilot import build_configuration
from tools.submit_act12_lambda_study import validate_study


def registered_configuration(overrides):
    original = tomllib.loads(Path('configs/train/ironclad_a20_act12_win_pilot.toml').read_text(encoding='utf-8'))
    plan = json.loads(Path('docs/results/act12-pilot-20261006/original-bound-plan.json').read_text(encoding='utf-8'))
    recipe = {**plan['recipe'], 'ppo_overrides': overrides,
              'periodic_seed_start': 8000009000000, 'confirmation_seed_start': 8000010000000}
    return build_configuration(original, plan['parent'], recipe, run_name='test-lambda')


def test_lambda_override_changes_only_registered_ppo_scalar():
    control = registered_configuration({'gae_lambda': 0.98})
    experimental = registered_configuration({'gae_lambda': 1.0})
    assert control['ppo']['gae_lambda'] == 0.98
    experimental['ppo']['gae_lambda'] = 0.98
    assert control == experimental


@pytest.mark.parametrize('overrides', [{'gamma': 0.99}, {'gae_lambda': True},
                                     {'gae_lambda': 1.01}, {'gae_lambda': '1'}])
def test_unregistered_training_changes_are_rejected(overrides):
    with pytest.raises(ValueError, match='gae_lambda override'):
        registered_configuration(overrides)


@pytest.mark.parametrize('change', ['learning_rate', 'seed', 'parent', 'output'])
def test_study_rejects_hidden_second_variable(tmp_path, monkeypatch, change):
    control = registered_configuration({'gae_lambda': 0.98})
    experimental = copy.deepcopy(control)
    experimental['ppo']['gae_lambda'] = 1.0
    experimental['run']['output'] = 'local/runs/experimental'
    experimental['run']['benchmark'] = 'local/runs/experimental-benchmark.json'
    if change == 'learning_rate':
        experimental['ppo']['learning_rate'] *= 2
    elif change == 'seed':
        experimental['run']['seed'] += 1
    elif change == 'output':
        experimental['run']['output'] = control['run']['output']
    study = {'schema': 'sls-act12-lambda-study-v1', 'status': 'READY', 'arms': {}}
    configurations = {}
    for name, config in [('control', control), ('experimental', experimental)]:
        path = tmp_path / f'{name}.json'
        parent = {'sha': 'parent' if change != 'parent' or name == 'control' else 'different'}
        path.write_text(json.dumps({'parent': parent, 'arm': name}), encoding='utf-8')
        config_path = tmp_path / f'{name}.toml'
        from tools.prepare_act12_pilot import configuration_toml
        config_path.write_text(configuration_toml(config), encoding='utf-8')
        configurations[name] = config_path
        study['arms'][name] = {'plan': path.name, 'plan_sha256': source_sha256(path)}
    monkeypatch.setattr('tools.submit_act12_lambda_study.validate_plan',
                        lambda plan, **kwargs: configurations[plan['arm']])
    with pytest.raises(ValueError):
        validate_study(study, root=tmp_path)


def test_small_or_uncertain_joint_gain_does_not_pass():
    rows = [{'net': 21, 'paired_seeds': 2048, 'exact_mcnemar_p': 0.01}] * 2
    windows = [{'successes': 1}, {'successes': 2}]
    assert decision(rows, windows) == 'SUPPORTS_SECOND_TRAINING_SEED_REPLICATION'
    assert decision([{**rows[0], 'net': 10}, rows[1]], windows) == 'INCONCLUSIVE'
    assert decision([{**rows[0], 'exact_mcnemar_p': 0.06}, rows[1]], windows) == 'INCONCLUSIVE'
    assert decision([{**rows[0], 'exact_mcnemar_p': 0.03}] * 2, windows) == 'INCONCLUSIVE'
    assert decision(rows, [{'successes': 0}, {'successes': 1}]) == 'INCONCLUSIVE'


@pytest.mark.parametrize('control_code,interrupt,expected_calls', [(0, False, 3),
                                                                 (1, False, 1),
                                                                 (0, True, 1)])
def test_single_node_runner_stops_before_second_arm_on_failure_or_signal(
        tmp_path, monkeypatch, control_code, interrupt, expected_calls):
    from tools import run_act12_lambda_study as runner
    (tmp_path / 'local/runs').mkdir(parents=True)
    path = tmp_path / 'study.json'
    path.write_text(json.dumps({'wall_limit_hours': 48}), encoding='utf-8')
    plans = []
    for arm in ('control', 'experimental'):
        p = tmp_path / f'{arm}.json'
        p.write_text(json.dumps({'config': f'{arm}.toml'}), encoding='utf-8')
        plans.append(p)
    monkeypatch.setattr(runner, 'ROOT', tmp_path)
    monkeypatch.setattr(runner, 'validate_study', lambda study: plans)
    monkeypatch.setattr(runner, 'require_completed_arm', lambda plan: None)
    monkeypatch.setenv('SLURM_JOB_ID', 'TEST_ONLY')
    monkeypatch.setattr('sys.argv', ['runner', '--study', str(path)])
    handlers, calls, signals = {}, [], []
    monkeypatch.setattr(runner.signal, 'signal', lambda number, callback: handlers.update({number: callback}))
    class Child:
        def __init__(self, command, **kwargs):
            calls.append((command, kwargs))
            self.index = len(calls)
        def poll(self):
            return None
        def send_signal(self, number):
            signals.append(number)
        def wait(self):
            if self.index == 1 and interrupt:
                handlers[runner.signal.SIGTERM](runner.signal.SIGTERM, None)
            return control_code if self.index == 1 else 0
    monkeypatch.setattr(runner.subprocess, 'Popen', Child)
    result = runner.main()
    assert len(calls) == expected_calls
    assert result == (143 if interrupt else control_code)
    assert float(calls[0][1]['env']['SLS_STUDY_AVAILABLE_SECONDS']) > 0
    if interrupt:
        assert signals == [runner.signal.SIGTERM]
    if expected_calls == 3:
        assert 'experimental.toml' in str(calls[1][0])
        assert 'analyze_act12_lambda_study.py' in str(calls[2][0])


def test_second_arm_wall_gate_respects_allocation_remainder(monkeypatch):
    from tools.prepare_and_train import budget_estimate
    config = registered_configuration({'gae_lambda': 1.0})
    benchmark = {'results': [{'workers': 64, 'shards': 16, 'decisions_per_second': 100}]}
    monkeypatch.setenv('SLS_STUDY_AVAILABLE_SECONDS', '3600')
    result = budget_estimate(config, benchmark, elapsed=10)
    assert result['available_seconds'] == 3600
    assert not result['fits']


def test_zero_exit_interrupted_manifest_does_not_qualify_as_completed_arm(tmp_path, monkeypatch):
    from tools import run_act12_lambda_study as runner
    config = registered_configuration({'gae_lambda': 0.98})
    from tools.prepare_act12_pilot import configuration_toml
    (tmp_path / 'config.toml').write_text(configuration_toml(config), encoding='utf-8')
    folder = tmp_path / config['run']['output']
    folder.mkdir(parents=True)
    (folder / 'run-manifest.json').write_text(json.dumps({'status': 'INTERRUPTED'}), encoding='utf-8')
    monkeypatch.setattr(runner, 'ROOT', tmp_path)
    with pytest.raises(ValueError, match='next arm blocked'):
        runner.require_completed_arm({'config': 'config.toml'})
