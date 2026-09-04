from __future__ import annotations

import http.client
import json
import threading
import time
from dataclasses import asdict
from pathlib import Path

import pytest
import torch

from sls.backends.simulator import SimulatorBackend
from sls.contracts import (
    Action,
    ActionKind,
    Decision,
    Observation,
    Player,
    PublicEntity,
    RunContext,
    ScreenType,
)
from sls.curriculum import IRONCLAD_A0_ACT1
from sls.model import ModelConfig, Policy
from sls.runtime.artifact import (
    POLICY_ARTIFACT_SCHEMA,
    LoadedPolicyArtifact,
    PolicyArtifactMetadata,
    model_state_sha256,
)
from sls.runtime.controller import AgentRuntime
from sls.runtime.inspector import (
    InspectorLauncher,
    InteractiveAgentRuntime,
    _action_presentation,
    create_server,
    discover_policy_artifacts,
)


def test_card_reward_presentation_exposes_composite_ui_action() -> None:
    reward = PublicEntity("reward-card:0:1", "CLEAVE")
    decision = Decision(
        Observation(
            player=Player("IRONCLAD", 70, 80, 0, 0, 3),
            run=RunContext(0, 1, 3, 99, False, False, False),
            screen=ScreenType.COMBAT_REWARD,
            reward_options=(reward,),
        ),
        (Action(ActionKind.CHOOSE_CARD_REWARD, subject_id=reward.instance_id),),
    )

    presentation = _action_presentation(decision, decision.actions[0])

    assert presentation["label"] == "选择卡牌奖励：CLEAVE"
    assert presentation["composite"] is True
    assert presentation["ui_steps"] == ["打开三选一卡牌奖励", "选择 CLEAVE"]
    assert "2 个游戏点击" in presentation["execution_note"]


def _artifact() -> LoadedPolicyArtifact:
    config = ModelConfig(
        embedding_dim=32, transformer_layers=1, attention_heads=4,
        feedforward_dim=64, recurrent_hidden_dim=32,
    )
    model = Policy(config).eval()
    metadata = PolicyArtifactMetadata(
        model=config.to_dict(), encoding_schema=config.to_dict()["encoding_schema"],
        vocabulary_sha256=config.to_dict()["vocabulary_hash"], simulator_only=True,
        source_git_commit="test", native_source_sha256="native",
        training_config_sha256="config", model_sha256=model_state_sha256(model.state_dict()),
        recurrent_memory_size=32, ascension_min=0, ascension_max=0, goal="ACT1",
    )
    return LoadedPolicyArtifact(model, metadata)


class _AttachableSimulator:
    def __init__(self) -> None:
        self.simulator = SimulatorBackend(IRONCLAD_A0_ACT1)
        self.decision = self.simulator.reset(7)
        self.calls: list[str] = []
        self.goal = ""
        self.card_reward_preview_seconds = 0.0

    def set_card_reward_preview_seconds(self, seconds: float) -> None:
        self.card_reward_preview_seconds = seconds

    def configure_goal(self, goal: str) -> None:
        self.goal = goal

    def attach(self):  # type: ignore[no-untyped-def]
        return self.decision

    def step(self, action):  # type: ignore[no-untyped-def]
        self.calls.append(action.candidate_id)
        transition = self.simulator.step(action)
        self.decision = transition.decision
        return transition


def _wait_status(
    runtime: InteractiveAgentRuntime, status: str, timeout: float = 3.0,
    *, after_revision: int = -1,
):  # type: ignore[no-untyped-def]
    deadline = time.monotonic() + timeout
    while time.monotonic() < deadline:
        state = runtime.state()
        if state["status"] == status and state["revision"] > after_revision:
            return state
        time.sleep(0.01)
    raise AssertionError(f"runtime did not reach {status}: {runtime.state()}")


def test_score_is_complete_and_does_not_commit_memory() -> None:
    backend = _AttachableSimulator()
    runtime = AgentRuntime(backend, _artifact())  # type: ignore[arg-type]
    before = runtime.memory.clone()
    score = runtime.score(backend.decision)
    assert len(score.actions) == len(backend.decision.actions)
    assert sum(item.probability for item in score.actions) == pytest.approx(1.0)
    assert sorted(item.index for item in score.actions) == list(range(len(score.actions)))
    assert runtime.memory.equal(before)


def test_interactive_runtime_starts_paused_steps_and_executes_manual_action(
    tmp_path,
) -> None:  # type: ignore[no-untyped-def]
    backend = _AttachableSimulator()
    log = tmp_path / "inspector.jsonl"
    runtime = InteractiveAgentRuntime(
        backend, _artifact(), delay_seconds=0.0, log_path=log,
    )  # type: ignore[arg-type]
    runtime.start()
    initial = _wait_status(runtime, "PAUSED")
    assert backend.calls == []
    assert initial["mode"] == "PAUSED"
    first_boundary = initial["boundary_id"]

    runtime.submit({"command": "step"})
    stepped = _wait_status(runtime, "PAUSED", after_revision=initial["revision"])
    assert len(backend.calls) == 1
    assert stepped["boundary_id"] != first_boundary

    selected = stepped["actions"][-1]["candidate_id"]
    runtime.submit({
        "command": "execute",
        "boundary_id": stepped["boundary_id"],
        "candidate_id": selected,
    })
    _wait_status(runtime, "PAUSED", after_revision=stepped["revision"])
    assert len(backend.calls) == 2
    assert backend.calls[-1] == selected
    records = [
        json.loads(line) for line in log.read_text(encoding="utf-8").splitlines()
    ]
    intents = [record for record in records if record.get("phase") == "INTENT"]
    acknowledgements = [
        record for record in records if record.get("phase") == "ACK"
    ]
    assert intents[-1]["selection_source"] == "manual"
    assert intents[-1]["model_top_candidate_id"]
    assert len(intents[-1]["action_rankings"]) == len(stepped["actions"])
    assert intents[-1]["schema"] == "sls-live-action-v4"
    assert intents[-1]["observation"]["screen"] == stepped["observation"]["screen"]
    assert intents[-1]["selected_action"]["kind"]
    assert intents[-1]["session_id"]
    assert acknowledgements[-1]["schema"] == "sls-live-action-v4"
    assert acknowledgements[-1]["session_id"] == intents[-1]["session_id"]
    runtime.submit({"command": "stop"})
    _wait_status(runtime, "STOPPED")


def test_interactive_runtime_rejects_stale_and_duplicate_boundary_commands() -> None:
    runtime = InteractiveAgentRuntime(_AttachableSimulator(), _artifact())  # type: ignore[arg-type]
    runtime.start()
    state = _wait_status(runtime, "PAUSED")
    with pytest.raises(RuntimeError, match="stale"):
        runtime.submit({
            "command": "execute", "boundary_id": "old",
            "candidate_id": state["actions"][0]["candidate_id"],
        })
    runtime.submit({"command": "step"})
    with pytest.raises(RuntimeError, match="already queued|requires a paused"):
        runtime.submit({"command": "resume"})
    _wait_status(runtime, "PAUSED")
    runtime.submit({"command": "stop"})


def test_card_reward_preview_delay_can_be_changed_while_paused() -> None:
    backend = _AttachableSimulator()
    runtime = InteractiveAgentRuntime(
        backend, _artifact(), card_reward_preview_seconds=3.0,
    )  # type: ignore[arg-type]
    runtime.start()
    initial = _wait_status(runtime, "PAUSED")
    assert initial["card_reward_preview_seconds"] == 3.0
    assert backend.card_reward_preview_seconds == 3.0

    runtime.submit({
        "command": "set_card_reward_preview",
        "card_reward_preview_seconds": 6.5,
    })
    updated = _wait_status(runtime, "PAUSED", after_revision=initial["revision"])
    assert updated["card_reward_preview_seconds"] == 6.5
    assert backend.card_reward_preview_seconds == 6.5
    runtime.submit({"command": "stop"})


def test_inspector_http_api_and_loopback_guard() -> None:
    runtime = InteractiveAgentRuntime(_AttachableSimulator(), _artifact())  # type: ignore[arg-type]
    with pytest.raises(ValueError, match="loopback"):
        create_server(runtime, "0.0.0.0", 8765)
    server = create_server(runtime, "127.0.0.1", 0)
    thread = threading.Thread(target=server.serve_forever, daemon=True)
    thread.start()
    try:
        connection = http.client.HTTPConnection("127.0.0.1", server.server_port)
        connection.request("GET", "/api/state")
        response = connection.getresponse()
        assert response.status == 200
        assert b'"status": "CONNECTING"' in response.read()
        connection.request("POST", "/api/control", body="{}", headers={
            "Content-Type": "text/plain",
        })
        assert connection.getresponse().status == 415
    finally:
        server.shutdown()
        server.server_close()
        thread.join(timeout=2)


def test_pause_requested_during_execution_applies_at_next_boundary() -> None:
    class SlowBackend(_AttachableSimulator):
        def __init__(self) -> None:
            super().__init__()
            self.entered = threading.Event()
            self.release = threading.Event()

        def step(self, action):  # type: ignore[no-untyped-def]
            self.entered.set()
            assert self.release.wait(2)
            return super().step(action)

    backend = SlowBackend()
    runtime = InteractiveAgentRuntime(backend, _artifact(), delay_seconds=0.0)  # type: ignore[arg-type]
    runtime.start()
    initial = _wait_status(runtime, "PAUSED")
    runtime.submit({"command": "resume"})
    assert backend.entered.wait(2)
    executing = _wait_status(runtime, "EXECUTING")
    runtime.submit({"command": "pause"})
    backend.release.set()
    paused = _wait_status(runtime, "PAUSED", after_revision=executing["revision"])
    assert paused["boundary_id"] != initial["boundary_id"]
    assert len(backend.calls) == 1
    runtime.submit({"command": "stop"})


def test_delay_change_applies_to_the_current_countdown() -> None:
    backend = _AttachableSimulator()
    runtime = InteractiveAgentRuntime(backend, _artifact(), delay_seconds=5.0)  # type: ignore[arg-type]
    runtime.start()
    _wait_status(runtime, "PAUSED")
    runtime.submit({"command": "resume"})
    countdown = _wait_status(runtime, "COUNTDOWN")
    runtime.submit({"command": "set_delay", "delay_seconds": 0.2})
    deadline = time.monotonic() + 2
    while not backend.calls and time.monotonic() < deadline:
        time.sleep(0.01)
    assert len(backend.calls) == 1
    runtime.submit({"command": "pause"})
    paused = _wait_status(runtime, "PAUSED", after_revision=countdown["revision"])
    assert paused["delay_seconds"] == 0.2
    runtime.submit({"command": "stop"})


def test_discovery_lists_only_exported_policy_artifacts(tmp_path: Path) -> None:
    artifact = _artifact()
    model_path = tmp_path / "run" / "stages" / "smoke" / "act1.pt"
    model_path.parent.mkdir(parents=True)
    torch.save({
        "schema": POLICY_ARTIFACT_SCHEMA,
        "metadata": asdict(artifact.metadata),
        "model": artifact.model.state_dict(),
    }, model_path)
    torch.save({"schema": "training-checkpoint"}, model_path.with_name("not-policy.pt"))

    models = discover_policy_artifacts([tmp_path])

    assert len(models) == 1
    assert models[0]["path"] == str(model_path.resolve())
    assert models[0]["goal"] == "ACT1"
    assert models[0]["ascension_min"] == 0


def test_launcher_requires_explicit_model_selection_before_runtime_start() -> None:
    started = threading.Event()

    class FakeRuntime:
        def start(self) -> None:
            started.set()

        def state(self):  # type: ignore[no-untyped-def]
            return {"status": "CONNECTING", "mode": "PAUSED", "error": None}

        def submit(self, payload):  # type: ignore[no-untyped-def]
            return {"delegated": payload["command"]}

    model = {
        "model_id": "one", "name": "Act 1", "path": "model.pt", "goal": "ACT1",
        "ascension_min": 0, "ascension_max": 0, "model_sha256": "a" * 64,
        "size_mb": 5.0,
    }
    launcher = InspectorLauncher([model], lambda _path: FakeRuntime())  # type: ignore[arg-type]
    assert launcher.state()["status"] == "SETUP"
    with pytest.raises(RuntimeError, match="select a model"):
        launcher.submit({"command": "resume"})

    launcher.submit({"command": "start", "model_id": "one"})

    assert started.wait(2)
    assert launcher.state()["status"] == "CONNECTING"
    assert launcher.submit({"command": "pause"}) == {"delegated": "pause"}
