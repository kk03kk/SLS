from __future__ import annotations

import json
from pathlib import Path

import torch

from sls.diagnostics.canary import compare_trajectories, tensor_hash


def _write(path: Path, backend: str, boundaries: list[dict[str, object]]) -> None:
    metadata = {
        "record_type": "metadata", "schema": "sls-policy-trajectory-v1",
        "backend": backend, "seed": 0,
        "policy": {
            "source_git_commit": "abc", "architecture": "v4",
            "encoding_schema": "input", "vocabulary_sha256": "vocab",
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
