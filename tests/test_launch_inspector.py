from __future__ import annotations

import sys

import pytest

from tools import launch_inspector as launch


def test_live_game_reuses_controller_without_reconfiguration(monkeypatch):
    monkeypatch.setattr(sys, "argv", ["launch_inspector"])
    monkeypatch.setattr(launch, "inspector_state", lambda _: {"status": "PAUSED"})
    monkeypatch.setattr(launch, "game_process_ids", lambda: [123])
    opened = []
    monkeypatch.setattr(launch, "open_inspector_window", opened.append)
    monkeypatch.setattr(launch, "configure", lambda *a, **k: pytest.fail("must not reconfigure live game"))
    assert launch.main() == 0
    assert opened == ["http://127.0.0.1:8765"]


def test_disconnected_server_is_retired_before_relaunch(monkeypatch):
    monkeypatch.setattr(sys, "argv", ["launch_inspector", "--configure-only"])
    monkeypatch.setattr(launch, "inspector_state", lambda _: {"status": "ERROR"})
    monkeypatch.setattr(launch, "game_process_ids", lambda: [])
    events = []
    monkeypatch.setattr(launch, "retire_disconnected_inspector", lambda _: events.append("retired"))
    monkeypatch.setattr(launch, "inspect_live_setup", lambda **k: {"game": True, "mods": {"all": True}, "invalid_models": [], "model_count": 1})
    monkeypatch.setattr(launch, "configure", lambda *a, **k: (events.append("configured") or "backup", "command"))
    monkeypatch.setattr(launch, "select_required_mods", lambda _: None)
    assert launch.main() == 0
    assert events == ["retired", "configured"]


def test_retire_never_stops_a_running_game(monkeypatch):
    monkeypatch.setattr(launch, "game_process_ids", lambda: [123])
    monkeypatch.setattr(launch.urllib.request, "urlopen", lambda *a, **k: pytest.fail("must not stop live controller"))
    with pytest.raises(RuntimeError, match="still running"):
        launch.retire_disconnected_inspector("http://127.0.0.1:8765")


def test_setup_server_can_stop_without_connecting_model():
    from sls.runtime.inspector import InspectorLauncher

    launcher = InspectorLauncher([], lambda _: pytest.fail("must not connect"))
    assert launcher.submit({"command": "stop"})["accepted"]
    assert launcher.state()["status"] == "STOPPED"


def test_connection_reset_while_old_server_closes_is_disconnected(monkeypatch):
    def closing(*args, **kwargs):
        raise ConnectionResetError("server closed connection")

    monkeypatch.setattr(launch.urllib.request, "urlopen", closing)
    assert launch.inspector_state("http://127.0.0.1:8765") is None


def test_disconnected_setup_server_releases_port_before_relaunch(monkeypatch):
    import threading

    from sls.runtime.inspector import InspectorLauncher, create_server

    launcher = InspectorLauncher([], lambda _: pytest.fail("must not load a model"))
    server = create_server(launcher, "127.0.0.1", 0)
    server.timeout = 0.1

    def serve():
        try:
            while launcher.state()["status"] != "STOPPED":
                server.handle_request()
        finally:
            server.server_close()

    thread = threading.Thread(target=serve, daemon=True)
    thread.start()
    monkeypatch.setattr(launch, "game_process_ids", lambda: [])
    url = f"http://127.0.0.1:{server.server_port}"
    try:
        launch.retire_disconnected_inspector(url)
        thread.join(timeout=2)
        assert not thread.is_alive()
        assert launch.inspector_state(url) is None
    finally:
        launcher.submit({"command": "stop"})
        thread.join(timeout=2)
