import json
import zipfile
from pathlib import Path

import pytest

from tools.build_oracle import build, deterministic_jar, resource_payloads
from tools.run_original_canary import launcher_command
from tools.verify_oracle import install, require_no_running_game


def test_committed_oracle_resources_are_complete_and_unique():
    payloads = resource_payloads()
    assert len(payloads) == 9
    assert json.loads(payloads['ModTheSpire.json'])['version'] == '1.3.1'
    assert len(json.loads(payloads['spirecomm/parity/act2-scenes.json'])['scenes']) == 24
    assert len(json.loads(payloads['spirecomm/parity/fullrun-scenes.json'])['scenes']) == 12
    assert len(json.loads(payloads['spirecomm/parity/fullrun-regressions.json'])['scenes']) == 1


def test_jar_is_deterministic_across_member_insertion_orders(tmp_path):
    first, second = tmp_path / 'first.jar', tmp_path / 'second.jar'
    deterministic_jar(first, {'b.class': b'B', 'a.class': b'A'})
    deterministic_jar(second, {'a.class': b'A', 'b.class': b'B'})
    assert first.read_bytes() == second.read_bytes()
    with zipfile.ZipFile(first) as archive:
        assert archive.namelist() == ['a.class', 'b.class']
        assert all(info.date_time == (1980, 1, 1, 0, 0, 0) for info in archive.infolist())


def test_build_rejects_existing_evidence_before_compiling(tmp_path):
    executable = tmp_path / 'javac'
    executable.touch()
    dependencies = {name: executable for name in ('game', 'mod_the_spire', 'base_mod', 'communication_mod')}
    output = tmp_path / 'existing.jar'
    output.write_bytes(b'original')
    with pytest.raises(FileExistsError):
        build(javac=executable, dependencies=dependencies, output=output)
    assert output.read_bytes() == b'original'


def test_build_rejects_dependency_output_even_with_force(tmp_path):
    dependency = tmp_path / 'dependency.jar'
    dependency.write_bytes(b'preserve')
    dependencies = {name: dependency for name in ('game', 'mod_the_spire', 'base_mod', 'communication_mod')}
    with pytest.raises(ValueError, match='dependency'):
        build(javac=dependency, dependencies=dependencies, output=dependency, force=True)
    assert dependency.read_bytes() == b'preserve'


def test_validation_launcher_records_mode_and_production_is_explicit(tmp_path: Path):
    command = launcher_command(tmp_path, tmp_path / 'mts.jar')
    assert '-Dsls.oracle.mode=validation' in command
    assert '-Dsls.oracle.mode=production' in launcher_command(tmp_path, tmp_path / 'mts.jar', oracle_mode='production')
    with pytest.raises(ValueError):
        launcher_command(tmp_path, tmp_path / 'mts.jar', oracle_mode='typo')


def test_runtime_qualification_rejects_existing_game_before_mutation(tmp_path, monkeypatch):
    from types import SimpleNamespace

    game = tmp_path / 'game'
    monkeypatch.setattr('tools.verify_oracle.subprocess.run', lambda *a, **k: SimpleNamespace(stdout=json.dumps([
        {'ProcessId': 123, 'Name': 'javaw.exe', 'CommandLine': f'javaw -jar {game}/mts.jar'}])))
    with pytest.raises(RuntimeError, match='no files changed'):
        require_no_running_game(game, game / 'mts.jar')


def test_install_preserves_old_jar_and_is_idempotent(tmp_path, monkeypatch):
    import hashlib

    game = tmp_path / 'game'
    mods = game / 'mods'
    mods.mkdir(parents=True)
    old = mods / 'SpirecommParity.jar'
    old.write_bytes(b'old-evidence')
    candidate = tmp_path / 'candidate.jar'
    candidate.write_bytes(b'new-qualified')
    digest = hashlib.sha256(candidate.read_bytes()).hexdigest()
    monkeypatch.setattr('tools.verify_oracle.ROOT', tmp_path)
    monkeypatch.setattr('tools.verify_oracle.inspect', lambda p: {'output_sha256': digest})
    monkeypatch.setattr('tools.verify_oracle.original_runtime_paths', lambda p: (tmp_path, game))
    monkeypatch.setattr('tools.verify_oracle.require_no_running_game', lambda *a: None)
    result = install(candidate, game)
    assert old.read_bytes() == b'new-qualified'
    assert Path(result['backup']).read_bytes() == b'old-evidence'
    assert install(candidate, game)['status'] == 'ALREADY_INSTALLED'
