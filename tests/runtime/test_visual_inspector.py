from __future__ import annotations

import json
from dataclasses import replace

import pytest

from sls.contracts import MapNode
from sls.runtime.inspector import InteractiveAgentRuntime
from sls.runtime.visual import VisualHistory, public_context
from tests.runtime.test_inspector import _artifact, _AttachableSimulator, _wait_status


def test_visual_metadata_is_whitelisted_and_does_not_change_policy_input():
    backend = _AttachableSimulator()
    before = backend.decision.observation.to_dict()
    backend.raw_payload = {
        "game_state": {"seed": 123, "screen_state": {"body_text": "Visible text"}},
        "_parity_run": {"current_map_x": 2, "current_map_y": 3},
        "_rng": {"secret": 42}, "_continuation": {"hidden": True},
    }
    context = public_context(backend, backend.decision)
    assert context["node_id"] == "map:2:3"
    assert context["seed"] == "123"
    assert "secret" not in json.dumps(context)
    assert "hidden" not in json.dumps(context)
    assert backend.decision.observation.to_dict() == before


def test_map_is_exact_public_topology_and_explicit_boss_destination():
    backend = _AttachableSimulator()
    nodes = (MapNode("map:2:14", 2, 14, "REST", True, ("map:boss",)),)
    decision = replace(backend.decision, observation=replace(backend.decision.observation, map_nodes=nodes))
    history = VisualHistory()
    visual = history.snapshot(decision)
    assert visual["map"][0]["outgoing_node_ids"] == ("map:boss",)
    assert visual["map"][1]["node_id"] == "map:boss"
    assert decision.observation.map_nodes == nodes
    assert history.route == []  # No position is fabricated from a selected action.


def test_history_is_confirmed_paginated_and_journaled(tmp_path):
    backend = _AttachableSimulator()
    log = tmp_path / "live.jsonl"
    runtime = InteractiveAgentRuntime(backend, _artifact(), log_path=log)
    runtime.start()
    initial = _wait_status(runtime, "PAUSED")
    runtime.submit({"command": "step", "boundary_id": initial["boundary_id"]})
    state = _wait_status(runtime, "PAUSED", after_revision=initial["revision"])
    history = runtime.history()
    assert history["total"] == 1
    entry = history["entries"][0]
    assert entry["confirmation"] == "ACK"
    assert entry["selected_candidate_id"] == initial["recommended_candidate_id"]
    assert entry["resulting_observation"] == state["observation"]
    assert entry["actions"] == initial["actions"]
    assert entry["summary"] == initial["summary"]
    assert entry["inventory_changes"]["gold_delta"] == state["summary"]["gold"] - initial["summary"]["gold"]
    assert state["visual"]["history_count"] == 1
    assert runtime.history(1)["entries"] == []
    with pytest.raises(ValueError):
        runtime.history(-1)
    assert any(json.loads(row).get("phase") == "VISUAL_ACK" for row in log.read_text().splitlines())
    runtime.submit({"command": "stop"})
    _wait_status(runtime, "STOPPED")


def test_process_ownership_blocks_another_controller(tmp_path):
    import subprocess
    import sys

    from sls.runtime.ownership import _owners, acquire_live_owner

    lock = tmp_path / "owner.lock"
    acquire_live_owner(lock)
    try:
        with pytest.raises(RuntimeError, match="another controller"):
            acquire_live_owner(lock)
        result = subprocess.run([sys.executable, "-c",
                                 "from pathlib import Path; import sys; from sls.runtime.ownership import acquire_live_owner; acquire_live_owner(Path(sys.argv[1]))", str(lock)],
                                capture_output=True, text=True)
        assert result.returncode != 0
        assert "another controller" in result.stderr
    finally:
        _owners.pop(lock.resolve()).close()


def test_visual_route_tracks_only_observed_positions_and_node_results():
    backend = _AttachableSimulator()
    backend.raw_payload = {"_parity_run": {"current_map_x": 2, "current_map_y": 0}, "game_state": {}}
    history = VisualHistory()
    history.observe(backend, backend.decision)
    history.observe(backend, backend.decision)
    assert history.route == ["map:2:0"]
    backend.raw_payload["_parity_run"]["current_map_x"] = 3
    backend.raw_payload["_parity_run"]["current_map_y"] = 1
    history.observe(backend, backend.decision)
    assert history.route == ["map:2:0", "map:3:1"]
    assert history.nodes["map:3:1"]["hp_before"] == backend.decision.observation.player.current_hp


def test_stale_step_does_not_execute_and_terminal_stop_is_immediate():
    backend = _AttachableSimulator()
    runtime = InteractiveAgentRuntime(backend, _artifact())
    runtime.start()
    _wait_status(runtime, "PAUSED")
    with pytest.raises(RuntimeError, match="stale"):
        runtime.submit({"command": "step", "boundary_id": "old"})
    assert backend.calls == []
    runtime.submit({"command": "stop"})
    _wait_status(runtime, "STOPPED")
    runtime._snapshot["status"] = "TERMINAL"
    runtime.submit({"command": "stop"})
    assert runtime.state()["status"] == "STOPPED"


def test_history_read_and_refresh_never_change_policy_memory():
    backend = _AttachableSimulator()
    runtime = InteractiveAgentRuntime(backend, _artifact())
    runtime.start()
    _wait_status(runtime, "PAUSED")
    memory = runtime.memory.clone()
    for _ in range(5):
        runtime.state()
        runtime.history()
        runtime.dashboard_seen()
    assert runtime.memory.equal(memory)
    assert backend.calls == []
    runtime.submit({"command": "stop"})


def test_menu_only_start_never_resets_existing_game():
    from dataclasses import asdict

    from sls.backends.original import LiveGameBackend, OriginalSession
    from sls.curriculum import IRONCLAD_A20_ACT1
    from tests.original.test_live_backend import ScriptedTransport, game_payload

    transport = ScriptedTransport([game_payload(["A", "B", "C", "D"])])
    backend = LiveGameBackend(OriginalSession(transport), allow_curriculum_goals=True)
    backend.configure_environment(asdict(IRONCLAD_A20_ACT1), "ACT1")
    with pytest.raises(RuntimeError, match="main menu"):
        backend.start_new_run(0)
    assert transport.sent == ["ready"]


def test_menu_only_start_uses_artifact_ascension_and_existing_start_protocol():
    from dataclasses import asdict

    from sls.backends.original import LiveGameBackend, OriginalSession
    from sls.curriculum import IRONCLAD_A20_ACT1
    from tests.original.test_live_backend import ScriptedTransport, game_payload

    neow = game_payload(["A", "B", "C", "D"])
    neow["game_state"]["ascension_level"] = 20
    transport = ScriptedTransport([{"in_game": False, "ready_for_command": True, "available_commands": ["start"]}, neow])
    backend = LiveGameBackend(OriginalSession(transport), allow_curriculum_goals=True)
    backend.configure_environment(asdict(IRONCLAD_A20_ACT1), "ACT1")
    backend.start_new_run(0)
    assert transport.sent == ["ready", "start IRONCLAD 20 0"]
    assert backend.attach().observation.run.ascension == 20


def test_failed_delivery_is_pending_not_confirmed_history(tmp_path):
    class BrokenBackend(_AttachableSimulator):
        def step(self, action):
            raise EOFError("game disconnected")

    runtime = InteractiveAgentRuntime(BrokenBackend(), _artifact(), log_path=tmp_path / "live.jsonl")
    runtime.start()
    initial = _wait_status(runtime, "PAUSED")
    runtime.submit({"command": "step", "boundary_id": initial["boundary_id"]})
    error = _wait_status(runtime, "ERROR")
    assert "disconnected" in error["error"]
    assert runtime.history()["total"] == 0
    assert error["visual"]["pending_action"]["confirmation"] == "PENDING"
    assert error["observation"] == initial["observation"]
    runtime.submit({"command": "stop"})


def test_confirmed_history_and_memory_resume_together(tmp_path):
    backend, artifact = _AttachableSimulator(), _artifact()
    log = tmp_path / "live.jsonl"
    first = InteractiveAgentRuntime(backend, artifact, log_path=log)
    first.start()
    initial = _wait_status(first, "PAUSED")
    first.submit({"command": "step"})
    stepped = _wait_status(first, "PAUSED", after_revision=initial["revision"])
    memory = first.memory.clone()
    first.submit({"command": "stop"})
    _wait_status(first, "STOPPED")
    second = InteractiveAgentRuntime(backend, artifact, log_path=log)
    second.start()
    restored = _wait_status(second, "PAUSED")
    assert second.memory.equal(memory)
    assert restored["actions"] == stepped["actions"]
    assert second.history()["entries"] == first.history()["entries"]
    assert len(backend.calls) == 1
    second.submit({"command": "step", "boundary_id": restored["boundary_id"]})
    latest = _wait_status(second, "PAUSED", after_revision=restored["revision"])
    assert latest["visual"]["history_count"] == 2
    assert len(backend.calls) == 2
    second.submit({"command": "stop"})


def test_http_ships_ui_legacy_and_validated_read_only_history():
    import http.client
    import threading

    from sls.runtime.inspector import create_server

    backend = _AttachableSimulator()
    runtime = InteractiveAgentRuntime(backend, _artifact())
    server = create_server(runtime, "127.0.0.1", 0)
    thread = threading.Thread(target=server.serve_forever, daemon=True)
    thread.start()
    try:
        connection = http.client.HTTPConnection("127.0.0.1", server.server_port)
        connection.request("GET", "/")
        response = connection.getresponse()
        assert response.status == 200
        assert b"/assets/" in response.read()
        connection.request("GET", "/legacy")
        response = connection.getresponse()
        assert response.status == 200
        assert b"api/state" in response.read()
        connection.request("GET", "/api/history?offset=-1")
        response = connection.getresponse()
        assert response.status == 400
        response.read()
        connection.request("GET", "/api/history?offset=0&limit=10")
        response = connection.getresponse()
        assert json.loads(response.read())["entries"] == []
        assert backend.calls == []
        connection.close()
    finally:
        server.shutdown()
        server.server_close()
        thread.join(timeout=2)
