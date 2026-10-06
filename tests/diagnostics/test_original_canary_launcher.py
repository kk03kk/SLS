from pathlib import Path

import pytest

from sls.curriculum import EpisodeHorizon
from tools.capture_policy_trajectory import _profile_for_goal
from tools.run_original_canary import (
    BackupJournal,
    launcher_command,
    original_runtime_paths,
    stop_owned_and_restore,
)


def test_original_runtime_uses_explicit_game_path(tmp_path, monkeypatch):
    monkeypatch.setattr('tools.run_original_canary.sys.platform', 'win32')
    monkeypatch.setenv('LOCALAPPDATA', str(tmp_path / 'user'))
    game = tmp_path / 'another library' / 'game'
    game.mkdir(parents=True)
    (game / 'desktop-1.0.jar').write_bytes(b'fixture')
    local, resolved = original_runtime_paths(game)
    assert local == tmp_path / 'user' / 'ModTheSpire'
    assert resolved == game.resolve()


def test_original_runtime_discovers_steam_instead_of_fixed_drive(tmp_path, monkeypatch):
    monkeypatch.setattr('tools.run_original_canary.sys.platform', 'win32')
    monkeypatch.setenv('LOCALAPPDATA', str(tmp_path / 'user'))
    (tmp_path / 'desktop-1.0.jar').write_bytes(b'fixture')
    monkeypatch.setattr('sls.runtime.live_setup.default_game_dir', lambda: tmp_path)
    assert original_runtime_paths(None)[1] == tmp_path.resolve()


def test_original_runtime_rejects_missing_game_and_unsupported_platform(tmp_path, monkeypatch):
    monkeypatch.setattr('tools.run_original_canary.sys.platform', 'linux')
    with pytest.raises(RuntimeError, match='Windows only'):
        original_runtime_paths(tmp_path)
    monkeypatch.setattr('tools.run_original_canary.sys.platform', 'win32')
    monkeypatch.delenv('LOCALAPPDATA', raising=False)
    with pytest.raises(RuntimeError, match='LOCALAPPDATA'):
        original_runtime_paths(tmp_path)
    monkeypatch.setenv('LOCALAPPDATA', str(tmp_path))
    with pytest.raises(FileNotFoundError, match='missing Original game'):
        original_runtime_paths(tmp_path)


def test_canary_help_does_not_require_windows_runtime(monkeypatch):
    from tools import run_original_canary

    monkeypatch.delenv('LOCALAPPDATA', raising=False)
    monkeypatch.setattr(run_original_canary.sys, 'argv', ['run_original_canary.py', '--help'])
    with pytest.raises(SystemExit) as result:
        run_original_canary.main()
    assert result.value.code == 0


def test_backup_journal_restores_existing_and_created_files(tmp_path: Path) -> None:
    existing = tmp_path / "existing.txt"
    created = tmp_path / "created.txt"
    existing.write_text("before", encoding="utf-8")
    journal = BackupJournal(tmp_path / "evidence" / "journal.json")
    journal.backup(existing)
    journal.backup(created)

    existing.write_text("after", encoding="utf-8")
    created.write_text("temporary", encoding="utf-8")
    journal.restore()

    assert existing.read_text(encoding="utf-8") == "before"
    assert not created.exists()
    assert journal.data["status"] == "RECOVERED"
    assert journal.data["recovery_failures"] == []


def test_recovery_catches_owned_exit_writes_and_new_save_files(tmp_path):
    root = tmp_path / 'saves'
    root.mkdir()
    save = root / 'original.save'
    save.write_bytes(b'original')
    journal = BackupJournal(tmp_path / 'evidence' / 'journal.json')
    journal.backup_tree(root)
    extra = root / 'runtime.save'

    class OwnedLateWriter:
        def poll(self):
            return None

        def terminate(self):
            save.write_bytes(b'late exit write')
            extra.write_bytes(b'new runtime save')

        def wait(self, timeout):
            return 0

    stop_owned_and_restore(journal, OwnedLateWriter())
    assert save.read_bytes() == b'original'
    assert not extra.exists()
    assert journal.data['status'] == 'RECOVERED'


def test_launcher_pins_only_the_required_mods(tmp_path: Path) -> None:
    command = launcher_command(tmp_path / "game", tmp_path / "ModTheSpire.jar")
    assert "--skip-launcher" in command
    assert "--skip-intro" in command
    assert command[-1] == "basemod,CommunicationMod,spirecomm-parity"
    assert "SuperFastMode" not in " ".join(command)


def test_launcher_can_opt_in_to_speed_mod_for_audit(tmp_path: Path) -> None:
    command = launcher_command(
        tmp_path / "game", tmp_path / "ModTheSpire.jar", superfast=True,
    )
    assert command[-1] == (
        "basemod,CommunicationMod,spirecomm-parity,superfastmode"
    )


def test_canary_uses_the_artifact_curriculum_horizon() -> None:
    assert _profile_for_goal("ACT1").horizon is EpisodeHorizon.ACT_1
    assert _profile_for_goal("ACT2").horizon is EpisodeHorizon.ACT_2
    assert _profile_for_goal("ACT3").horizon is EpisodeHorizon.ACT_3
    assert _profile_for_goal("FULLRUN").horizon is EpisodeHorizon.FULL_RUN
    assert _profile_for_goal("HEART").horizon is EpisodeHorizon.HEART

def test_canary_uses_explicit_a20_profile_instead_of_a0_goal_default():
    from dataclasses import asdict
    from types import SimpleNamespace

    from sls.curriculum import IRONCLAD_A20_ACT1
    from tools.capture_policy_trajectory import _profile_for_artifact

    metadata = SimpleNamespace(goal='ACT1', environment_profile=asdict(IRONCLAD_A20_ACT1))
    assert _profile_for_artifact(metadata) == IRONCLAD_A20_ACT1
