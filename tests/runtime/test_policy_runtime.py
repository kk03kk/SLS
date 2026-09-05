from __future__ import annotations

from dataclasses import replace
from pathlib import Path

import pytest
import torch

from sls.backends.original import LiveGameBackend
from sls.backends.simulator import SimulatorBackend
from sls.content.scope import IRONCLAD_A0_SCOPE_ID, ironclad_a0_scope_hash
from sls.contracts import Transition
from sls.curriculum import IRONCLAD_A0_ACT1
from sls.model import ENCODING_SCHEMA, ModelConfig, Policy, vocabulary_hash
from sls.rl.training_contract import TRAINING_CHECKPOINT_SCHEMA
from sls.runtime.artifact import (
    POLICY_ARTIFACT_SCHEMA,
    LoadedPolicyArtifact,
    PolicyArtifactMetadata,
    export_policy_artifact,
    load_policy_artifact,
    model_state_sha256,
)
from sls.runtime.controller import AgentRuntime, boundary_id


def _runtime_artifact() -> LoadedPolicyArtifact:
    config = ModelConfig(
        embedding_dim=32, transformer_layers=1, attention_heads=4,
        feedforward_dim=64,
    )
    model = Policy(config).eval()
    metadata = PolicyArtifactMetadata(
        model=config.to_dict(), encoding_schema=config.to_dict()["encoding_schema"],
        vocabulary_sha256=config.to_dict()["vocabulary_hash"],
        simulator_only=True,
        source_git_commit="test", native_source_sha256="test-native",
        training_config_sha256="test-config",
        model_sha256=model_state_sha256(model.state_dict()),
        recurrent_memory_size=config.recurrent_hidden_dim,
        ascension_min=0, ascension_max=20, goal="HEART",
    )
    return LoadedPolicyArtifact(model, metadata)


class _FixedRuntime(AgentRuntime):
    def choose(self, decision):  # type: ignore[no-untyped-def]
        return 0, 1.0


class _DisconnectBackend:
    def __init__(self, first, second, mode: str):  # type: ignore[no-untyped-def]
        self.current = first
        self.second = second
        self.mode = mode
        self.calls = []

    def attach(self):  # type: ignore[no-untyped-def]
        return self.current

    def step(self, action):  # type: ignore[no-untyped-def]
        self.calls.append(action.candidate_id)
        if self.mode == "before":
            raise ConnectionError("disconnected before send")
        self.current = self.second
        if self.mode == "after":
            raise ConnectionError("disconnected after send")
        return Transition(self.second, 0.0, False, False, {})


def _two_boundaries():  # type: ignore[no-untyped-def]
    backend = SimulatorBackend(IRONCLAD_A0_ACT1)
    first = backend.reset(7)
    second = backend.step(first.actions[0]).decision
    assert boundary_id(first) != boundary_id(second)
    return first, second


def test_policy_artifact_round_trip_is_strict_and_standalone(tmp_path: Path) -> None:
    config = ModelConfig(
        embedding_dim=32, transformer_layers=1, attention_heads=4,
        feedforward_dim=64,
    )
    model = Policy(config)
    checkpoint = tmp_path / "checkpoint.pt"
    torch.save({
        "contract": {
            "model": config.to_dict(), "simulator_only": True,
            "git_commit": "test",
            "native_source_sha256": "a" * 64,
            "training_config_sha256": "test-config",
            "encoding_schema": ENCODING_SCHEMA,
            "vocabulary_sha256": vocabulary_hash(),
            "content_scope_id": IRONCLAD_A0_SCOPE_ID,
            "content_scope_sha256": ironclad_a0_scope_hash(),
        },
        "model": model.state_dict(),
        "schema": TRAINING_CHECKPOINT_SCHEMA,
    }, checkpoint)
    artifact = export_policy_artifact(
        checkpoint, tmp_path / "policy.pt",
        ascension_min=0, ascension_max=20, goal="HEART",
    )
    loaded = load_policy_artifact(artifact)
    assert loaded.metadata.goal == "HEART"
    assert loaded.metadata.ascension_min == 0
    assert loaded.metadata.ascension_max == 20
    assert POLICY_ARTIFACT_SCHEMA == "sls-policy-artifact-v5"
    for expected, actual in zip(model.parameters(), loaded.model.parameters()):
        assert torch.equal(expected, actual)


def test_boundary_id_is_candidate_order_independent() -> None:
    decision = SimulatorBackend(IRONCLAD_A0_ACT1).reset(7)
    reordered = type(decision)(
        decision.observation, tuple(reversed(decision.actions)), decision.terminal,
    )
    assert boundary_id(decision) == boundary_id(reordered)


@pytest.mark.parametrize("encoding", ["sls-policy-input-v2", "sls-policy-input-v3"])
def test_artifact_rejects_pre_ownership_encoding(encoding: str) -> None:
    with pytest.raises(ValueError, match="encoding schema is incompatible"):
        replace(_runtime_artifact().metadata, encoding_schema=encoding).validate()


def test_disconnect_before_send_never_blindly_retries_same_boundary(tmp_path: Path) -> None:
    first, second = _two_boundaries()
    backend = _DisconnectBackend(first, second, "before")
    log = tmp_path / "actions.jsonl"
    runtime = _FixedRuntime(backend, _runtime_artifact(), log_path=log)
    with pytest.raises(ConnectionError):
        runtime.run(max_actions=1)
    with pytest.raises(RuntimeError, match="refusing to resend"):
        _FixedRuntime(backend, _runtime_artifact(), log_path=log).run(max_actions=1)
    assert len(backend.calls) == 1


def test_disconnect_after_send_rejects_unprovable_advanced_boundary(tmp_path: Path) -> None:
    first, second = _two_boundaries()
    backend = _DisconnectBackend(first, second, "after")
    log = tmp_path / "actions.jsonl"
    with pytest.raises(ConnectionError):
        _FixedRuntime(backend, _runtime_artifact(), log_path=log).run(max_actions=1)
    first_candidate = backend.calls[0]
    backend.mode = "normal"
    with pytest.raises(RuntimeError, match="cannot prove"):
        _FixedRuntime(backend, _runtime_artifact(), log_path=log).run(max_actions=1)
    assert backend.calls.count(first_candidate) == 1


def test_disconnect_after_state_read_still_requires_a_durable_ack(tmp_path: Path) -> None:
    first, second = _two_boundaries()
    backend = _DisconnectBackend(first, second, "normal")
    log = tmp_path / "actions.jsonl"
    runtime = _FixedRuntime(backend, _runtime_artifact(), log_path=log)
    normal_log = runtime._log

    def fail_ack(record):  # type: ignore[no-untyped-def]
        if record.get("phase") == "ACK":
            raise ConnectionError("disconnected after state read")
        normal_log(record)

    runtime._log = fail_ack  # type: ignore[method-assign]
    with pytest.raises(ConnectionError):
        runtime.run(max_actions=1)
    first_candidate = backend.calls[0]
    with pytest.raises(RuntimeError, match="cannot prove"):
        _FixedRuntime(backend, _runtime_artifact(), log_path=log).run(max_actions=1)
    assert backend.calls.count(first_candidate) == 1


def test_artifact_identity_includes_model_weights() -> None:
    first = _runtime_artifact()
    second = _runtime_artifact()
    with torch.no_grad():
        next(second.model.parameters()).add_(1.0)
    second_metadata = replace(
        second.metadata,
        model_sha256=model_state_sha256(second.model.state_dict()),
    )
    first_runtime = AgentRuntime(LiveGameBackend(), first)
    second_runtime = AgentRuntime(
        LiveGameBackend(), LoadedPolicyArtifact(second.model, second_metadata),
    )
    assert first_runtime.artifact_id != second_runtime.artifact_id


def test_recurrent_runtime_rejects_midrun_attach_without_matching_journal(
    tmp_path: Path,
) -> None:
    _, second = _two_boundaries()
    backend = _DisconnectBackend(second, second, "normal")
    with pytest.raises(RuntimeError, match="only start at Neow"):
        AgentRuntime(
            backend, _runtime_artifact(), log_path=tmp_path / "new.jsonl",
        ).run(max_actions=1)


def test_acknowledged_recurrent_memory_resumes_at_matching_boundary(
    tmp_path: Path,
) -> None:
    first, second = _two_boundaries()
    backend = _DisconnectBackend(first, second, "normal")
    log = tmp_path / "actions.jsonl"
    artifact = _runtime_artifact()
    AgentRuntime(backend, artifact, log_path=log).run(max_actions=1)
    AgentRuntime(backend, artifact, log_path=log).run(max_actions=1)
    assert len(backend.calls) == 2


def test_live_backend_uses_artifact_fullrun_goal_instead_of_forcing_heart() -> None:
    backend = LiveGameBackend()
    backend.configure_goal("FULLRUN")
    assert backend.require_heart is False
    backend.configure_goal("HEART")
    assert backend.require_heart is True
