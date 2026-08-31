from pathlib import Path

from sls.curriculum import EpisodeHorizon
from tools.capture_policy_trajectory import _profile_for_goal
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


def test_canary_uses_the_artifact_curriculum_horizon() -> None:
    assert _profile_for_goal("ACT1").horizon is EpisodeHorizon.ACT_1
    assert _profile_for_goal("ACT2").horizon is EpisodeHorizon.ACT_2
    assert _profile_for_goal("ACT3").horizon is EpisodeHorizon.ACT_3
    assert _profile_for_goal("FULLRUN").horizon is EpisodeHorizon.FULL_RUN
    assert _profile_for_goal("HEART").horizon is EpisodeHorizon.HEART
