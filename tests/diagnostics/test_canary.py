from __future__ import annotations

import json
from dataclasses import asdict
from pathlib import Path
from unittest.mock import patch

import pytest
import torch

from sls.backends.simulator import SimulatorBackend
from sls.curriculum import IRONCLAD_A0_ACT1
from sls.diagnostics.canary import (
    capture_policy_trajectory,
    compare_trajectories,
    read_trajectory,
    tensor_hash,
)
from sls.model import ModelConfig, Policy
from sls.runtime.artifact import (
    LoadedPolicyArtifact,
    PolicyArtifactMetadata,
    model_state_sha256,
)


def _write(path: Path, backend: str, boundaries: list[dict[str, object]]) -> None:
    metadata = {
        "record_type": "metadata", "schema": "sls-policy-trajectory-v2",
        "backend": backend, "seed": 0,
        "policy": {
            "source_git_commit": "abc", "architecture": "v4",
            "encoding_schema": "input", "vocabulary_sha256": "vocab",
            "model_sha256": "weights",
        },
    }
    path.write_text(
        "\n".join(json.dumps(record) for record in [metadata, *boundaries]) + "\n",
        encoding="utf-8",
    )


def _boundary(**updates: object) -> dict[str, object]:
    value: dict[str, object] = {
        "record_type": "boundary", "memory_input_sha256": "m0",
        "observation_sha256": "o", "candidate_actions_sha256": "a",
        "chosen_action_sha256": "c", "memory_output_sha256": "m1",
        "terminal": False, "success": False, "observation": {"run": {"floor": 0}},
        "candidate_actions": [],
    }
    value.update(updates)
    return value


def test_tensor_hash_is_bit_stable() -> None:
    value = torch.tensor([[0.0, 1.5]], dtype=torch.float32)
    assert tensor_hash(value) == tensor_hash(value.clone())
    assert tensor_hash(value) != tensor_hash(value + 1)


def test_capture_uses_same_recurrent_context_as_live_runtime(tmp_path: Path) -> None:
    torch.manual_seed(17)
    config = ModelConfig(
        embedding_dim=32, transformer_layers=1, attention_heads=4,
        feedforward_dim=64, recurrent_hidden_dim=32,
    )
    model = Policy(config).eval()
    artifact = LoadedPolicyArtifact(
        model,
        PolicyArtifactMetadata(
            model=config.to_dict(), encoding_schema=config.to_dict()["encoding_schema"],
            vocabulary_sha256=config.to_dict()["vocabulary_hash"], simulator_only=True,
            source_git_commit="test", native_source_sha256="native",
            training_config_sha256="config",
            model_sha256=model_state_sha256(model.state_dict()),
            recurrent_memory_size=32, ascension_min=0, ascension_max=0, goal="ACT1",
            environment_profile=asdict(IRONCLAD_A0_ACT1),
        ),
    )
    trajectory = tmp_path / "trajectory.jsonl"
    with patch.object(model, "forward", wraps=model.forward) as forward:
        result = capture_policy_trajectory(
            SimulatorBackend(IRONCLAD_A0_ACT1), artifact,
            backend_name="simulator", seed=7, output=trajectory, max_actions=2,
            diagnostic_state=True,
        )
    metadata, boundaries = read_trajectory(trajectory)
    assert boundaries[0]["diagnostic_state"]["run_state"]["ascension"] == 0
    assert boundaries[0]["diagnostic_rng"]
    assert 0 < boundaries[0]["chosen_action_probability"] <= 1

    assert metadata["recurrent_context"] == "PREVIOUS_ACTION_AND_REWARD"
    assert metadata["policy"]["model_sha256"] == artifact.metadata.model_sha256
    comparison_copy = tmp_path / "original-copy.jsonl"
    original_metadata = {**metadata, "backend": "original"}
    comparison_copy.write_text(
        "\n".join(json.dumps(row) for row in [original_metadata, *boundaries]) + "\n",
        encoding="utf-8",
    )
    assert compare_trajectories(trajectory, comparison_copy)["passed"] is True
    assert result["actions"] == 2
    assert boundaries[0]["previous_action_type"] == 0
    assert boundaries[1]["previous_action_type"] > 0
    assert torch.equal(
        forward.call_args_list[0].kwargs["previous_action_types"],
        torch.zeros(1, dtype=torch.long),
    )
    assert int(forward.call_args_list[1].kwargs["previous_action_types"][0]) > 0
    assert "previous_rewards" in forward.call_args_list[1].kwargs


def test_comparator_reports_first_observation_path(tmp_path: Path) -> None:
    simulator, original = tmp_path / "sim.jsonl", tmp_path / "original.jsonl"
    _write(simulator, "simulator", [_boundary(), _boundary(
        observation_sha256="left", observation={"run": {"floor": 2}},
    )])
    _write(original, "original", [_boundary(), _boundary(
        observation_sha256="right", observation={"run": {"floor": 3}},
    )])
    result = compare_trajectories(simulator, original)
    assert result["matched_boundaries"] == 1
    divergence = result["first_divergence"]
    assert isinstance(divergence, dict)
    assert divergence["index"] == 1
    assert divergence["classification"] == "OBSERVATION_DIVERGENCE"
    assert divergence["details"]["different_paths"] == ["run.floor"]


def test_comparator_detects_memory_before_equal_observation(tmp_path: Path) -> None:
    simulator, original = tmp_path / "sim.jsonl", tmp_path / "original.jsonl"
    _write(simulator, "simulator", [_boundary(memory_input_sha256="left")])
    _write(original, "original", [_boundary(memory_input_sha256="right")])
    result = compare_trajectories(simulator, original)
    assert result["first_divergence"]["classification"] == "RECURRENT_MEMORY_DIVERGENCE"


def test_comparator_classifies_transform_result_as_rng(tmp_path: Path) -> None:
    simulator, original = tmp_path / "sim.jsonl", tmp_path / "original.jsonl"
    selection = {"kind": "SELECT_CARD", "subject_id": "select-card:4"}
    matched = _boundary(chosen_action=selection)
    _write(simulator, "simulator", [matched, _boundary(
        observation_sha256="left", observation={"deck": [{"card_id": "A"}]},
    )])
    _write(original, "original", [matched, _boundary(
        observation_sha256="right", observation={"deck": [{"card_id": "B"}]},
    )])
    result = compare_trajectories(simulator, original)
    assert result["first_divergence"]["classification"] == "RNG_DIVERGENCE"


def test_empty_trajectories_do_not_pass(tmp_path: Path) -> None:
    simulator, original = tmp_path / "sim.jsonl", tmp_path / "original.jsonl"
    _write(simulator, "simulator", [])
    _write(original, "original", [])
    assert compare_trajectories(simulator, original)["passed"] is False


@pytest.mark.parametrize("field,value", [("model_sha256", "other"), ("model_sha256", None)])
def test_comparison_requires_same_model_weights(tmp_path: Path, field, value) -> None:
    simulator, original = tmp_path / "sim.jsonl", tmp_path / "original.jsonl"
    _write(simulator, "simulator", [_boundary()])
    _write(original, "original", [_boundary()])
    lines = original.read_text(encoding="utf-8").splitlines()
    metadata = json.loads(lines[0])
    metadata["policy"][field] = value
    lines[0] = json.dumps(metadata)
    original.write_text("\n".join(lines) + "\n", encoding="utf-8")
    assert compare_trajectories(simulator, original)["passed"] is False


def test_comparison_rejects_two_simulator_captures(tmp_path: Path) -> None:
    simulator = tmp_path / "sim.jsonl"
    _write(simulator, "simulator", [_boundary()])
    assert compare_trajectories(simulator, simulator)["passed"] is False


@pytest.mark.parametrize("field", ["previous_action_type", "previous_reward", "policy_input_sha256"])
def test_comparison_checks_recurrent_policy_input(tmp_path: Path, field) -> None:
    simulator, original = tmp_path / "sim.jsonl", tmp_path / "original.jsonl"
    _write(simulator, "simulator", [_boundary(**{field: 1})])
    _write(original, "original", [_boundary(**{field: 2})])
    result = compare_trajectories(simulator, original)
    assert result["passed"] is False
    assert result["first_divergence"]["details"]["field"] == field


@pytest.mark.parametrize("boss,enemy", [
    ("AUTOMATON", "BRONZE_AUTOMATON"), ("CHAMP", "THE_CHAMP"),
    ("COLLECTOR", "THE_COLLECTOR"), ("DONU_AND_DECA", "DONU"),
    ("THE_HEART", "CORRUPT_HEART"),
])
def test_comparison_counts_boss_encounter_aliases(tmp_path: Path, boss, enemy) -> None:
    simulator, original = tmp_path / "sim.jsonl", tmp_path / "original.jsonl"
    boundary = _boundary(observation={
        "screen": "COMBAT", "run": {"act": 2, "visible_boss_id": boss},
        "enemies": [{"monster_id": enemy}],
    })
    _write(simulator, "simulator", [boundary])
    _write(original, "original", [boundary])
    result = compare_trajectories(simulator, original)
    assert result["passed"] is True
    assert result["bosses"] == [f"ACT_2:{boss}"]
    assert result["trajectory_complete"] is False
