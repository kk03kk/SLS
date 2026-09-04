from __future__ import annotations

import sys
from pathlib import Path

import pytest

from tools.configure_live_inspector import configure, restore


def test_configure_live_inspector_backs_up_preserves_and_restores(tmp_path: Path) -> None:
    config = tmp_path / "config.properties"
    original = "# keep\nverbose=true\ncommand=old\nrunAtGameStart=false\n"
    config.write_text(original, encoding="utf-8")
    artifact = tmp_path / "act1.pt"
    artifact.write_bytes(b"artifact")

    backup, command = configure(
        config, python=Path(sys.executable), artifact=artifact, port=8765, delay=1.2,
    )

    configured = config.read_text(encoding="utf-8")
    assert backup.read_text(encoding="utf-8") == original
    assert "# keep" in configured
    assert "verbose=true" in configured
    assert "runAtGameStart=true" in configured
    assert "maxInitializationTimeout=900" in configured
    assert "play_live_inspector.py" in command
    assert "--delay 1.2" in command
    assert "\\:" in command

    safety_backup = restore(config, backup)
    assert config.read_text(encoding="utf-8") == original
    assert "play_live_inspector.py" in safety_backup.read_text(encoding="utf-8")


def test_configure_refuses_unrecognized_config_without_creating_backup(
    tmp_path: Path,
) -> None:
    config = tmp_path / "config.properties"
    config.write_text("unrelated=true\n", encoding="utf-8")
    artifact = tmp_path / "act1.pt"
    artifact.write_bytes(b"artifact")
    with pytest.raises(ValueError, match="exactly one"):
        configure(config, python=Path(sys.executable), artifact=artifact)
    assert list(tmp_path.glob("*.bak-*")) == []


def test_default_configuration_defers_model_selection_to_dashboard(tmp_path: Path) -> None:
    config = tmp_path / "config.properties"
    config.write_text("command=old\nrunAtGameStart=false\n", encoding="utf-8")
    _backup, command = configure(config, python=Path(sys.executable))
    assert "play_live_inspector.py --device cpu" in command
    assert ".pt" not in command
