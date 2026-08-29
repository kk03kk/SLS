from pathlib import Path

from tools.run_original_canary import BackupJournal, launcher_command


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


def test_launcher_pins_only_the_required_mods(tmp_path: Path) -> None:
    command = launcher_command(tmp_path / "game", tmp_path / "ModTheSpire.jar")
    assert "--skip-launcher" in command
    assert "--skip-intro" in command
    assert command[-1] == "basemod,CommunicationMod,spirecomm-parity"
    assert "SuperFastMode" not in " ".join(command)
