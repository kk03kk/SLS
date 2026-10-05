from __future__ import annotations

import json
import sys
import zipfile
from pathlib import Path

from sls.runtime import live_setup, window


def test_setup_finds_workshop_mods_and_exported_model(tmp_path: Path, monkeypatch) -> None:
    steamapps = tmp_path / "steamapps"
    game = steamapps / "common" / "SlayTheSpire"
    game.mkdir(parents=True)
    (game / "desktop-1.0.jar").touch()
    workshop = steamapps / "workshop" / "content" / "646570"
    for name in ("ModTheSpire.jar", "BaseMod.jar", "CommunicationMod.jar"):
        directory = workshop / name.removesuffix(".jar")
        directory.mkdir(parents=True)
        (directory / name).touch()
    mods = game / "mods"
    mods.mkdir()
    with zipfile.ZipFile(mods / "SpirecommParity.jar", "w") as archive:
        archive.writestr("spirecomm/parity/CardStatePatch$AddRewardPreviews.class", b"patch")
        archive.writestr("spirecomm/parity/EventStatePatch.class", b"patch")
        archive.writestr("spirecomm/parity/OracleMode.class", b"patch")
    config = tmp_path / "config.properties"
    mod_lists = tmp_path / "mod_lists.json"
    mod_lists.write_text(json.dumps({
        "defaultList": "<Default>",
        "lists": {"<Default>": [
            "BaseMod.jar", "CommunicationMod.jar", "SpirecommParity.jar",
        ]},
    }))
    inspector = Path(__file__).resolve().parents[2] / "tools" / "play_live_inspector.py"
    config.write_text(
        f'command="{sys.executable}" "{inspector.as_posix()}"\nrunAtGameStart=true\n'
    )
    monkeypatch.setattr(live_setup, "discover_policy_artifacts", lambda _roots: ({
        "name": "example", "path": str(tmp_path / "model" / "example.pt"),
        "goal": "ACT1", "ascension_min": 20, "ascension_max": 20,
    },))
    monkeypatch.setattr(live_setup, "load_policy_artifact", lambda _path: object())

    result = live_setup.inspect_live_setup(
        game_dir=game, config=config, model_root=tmp_path / "model",
        mod_lists_path=mod_lists,
    )

    assert result["ready"] is True
    assert all(result["mods"].values())
    assert result["oracle_patches_present"] is True
    assert result["verified_model_count"] == 1
    config.write_text("command=old\nrunAtGameStart=false\n")
    assert live_setup.inspect_live_setup(
        game_dir=game, config=config, model_root=tmp_path / "model",
        mod_lists_path=mod_lists,
    )["ready"] is False


def test_game_detection_uses_steam_libraryfolders(tmp_path: Path, monkeypatch) -> None:
    steam = tmp_path / "Steam"
    custom = tmp_path / "Library"
    (steam / "steamapps").mkdir(parents=True)
    game = custom / "steamapps" / "common" / "SlayTheSpire"
    game.mkdir(parents=True)
    (game / "desktop-1.0.jar").touch()
    (steam / "steamapps" / "libraryfolders.vdf").write_text(
        f'"libraryfolders" {{ "1" {{ "path" "{custom.as_posix()}" }} }}',
        encoding="utf-8",
    )
    monkeypatch.setenv("SLS_STEAM_LIBRARIES", str(steam))
    assert live_setup.default_game_dir() == game


def test_window_launches_edge_app_and_falls_back(monkeypatch, tmp_path: Path) -> None:
    edge = tmp_path / "msedge.exe"
    edge.touch()
    calls = []
    fallback = []
    monkeypatch.setattr(window.sys, "platform", "win32")
    monkeypatch.setattr(window, "edge_executable", lambda: edge)
    monkeypatch.setattr(window.subprocess, "Popen", lambda argv, **_kwargs: calls.append(argv))
    monkeypatch.setattr(window.webbrowser, "open", fallback.append)

    assert window.open_inspector_window("http://127.0.0.1:8765/") == "window"
    assert calls[0][1] == "--app=http://127.0.0.1:8765/"
    assert fallback == []
    monkeypatch.setattr(window, "edge_executable", lambda: None)
    assert window.open_inspector_window("http://127.0.0.1:8765/") == "browser"
    assert fallback == ["http://127.0.0.1:8765/"]
