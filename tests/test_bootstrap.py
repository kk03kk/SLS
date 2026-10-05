from tools import bootstrap


def test_simulator_bootstrap_does_not_collect_model_suite(monkeypatch):
    calls = []
    monkeypatch.setattr(bootstrap, 'run', lambda *args: calls.append(tuple(map(str, args))))
    assert bootstrap.main([]) == 0
    assert not any('model.lock' in item for command in calls for item in command)
    assert calls[-1][1:3] == ('-m', 'pytest')
    assert 'tests/contracts' in calls[-1]
    assert 'tests/model' not in calls[-1]


def test_ci_bootstrap_can_install_without_duplicating_test_run(monkeypatch):
    calls = []
    monkeypatch.setattr(bootstrap, 'run', lambda *args: calls.append(tuple(map(str, args))))
    assert bootstrap.main(['--with-model', '--skip-tests']) == 0
    assert any('model.lock' in item for command in calls for item in command)
    assert not any('pytest' in command for command in calls)
    assert any('build_native.py' in item for command in calls for item in command)
