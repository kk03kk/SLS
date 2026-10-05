from __future__ import annotations

import json
import tomllib
from pathlib import Path

import pytest
import torch

from sls.curriculum import IRONCLAD_A20_ACT1
from sls.model import ModelConfig, Policy
from sls.rl import PPOConfig, PPOTrainer, WorkerPool, save_checkpoint
from sls.rl.training_contract import (
    native_source_digest,
    sha256_file,
    training_implementation_digest,
)
from tools.initialize_act1_continuation import initialize
from tools.train_full_run import _training_identity


def _same(left, right):
    if isinstance(left, torch.Tensor):
        assert torch.equal(left, right)
    elif isinstance(left, dict):
        assert left.keys() == right.keys()
        for key in left:
            _same(left[key], right[key])
    elif isinstance(left, (list, tuple)):
        assert len(left) == len(right)
        for a, b in zip(left, right):
            _same(a, b)
    else:
        assert left == right


def test_complete_endpoint_retains_all_learning_state_and_resets_selection(tmp_path):
    root = Path(__file__).resolve().parents[2]
    source = tmp_path / "source"
    source.mkdir()
    text = (root / "configs/train/ironclad_a20_act1_plateau_win_2m_r1.toml").read_text()
    for old, new in (
        ("embedding_dim = 128", "embedding_dim = 32"),
        ("transformer_layers = 4", "transformer_layers = 1"),
        ("feedforward_dim = 256", "feedforward_dim = 64"),
        ("recurrent_hidden_dim = 256", "recurrent_hidden_dim = 32"),
        ("rollout_steps = 256", "rollout_steps = 2"),
        ("recurrent_sequence_length = 64", "recurrent_sequence_length = 1"),
        ("minibatch_sequences = 16", "minibatch_sequences = 1"),
    ):
        text = text.replace(old, new)
    text = text.replace("worker_layout = [64, 16]", "worker_layout = [1, 1]")
    benchmark = tmp_path / "parent-benchmark.json"
    text = text.replace(
        'benchmark = "local/runs/preparation/ironclad-a20-act1-plateau-win-2m-r1/benchmark.json"',
        f'benchmark = "{benchmark.as_posix()}"',
    )
    benchmark.write_text(json.dumps({"selected_workers": 1, "selected_shards": 1}))
    (source / "training-config.toml").write_text(text, encoding="utf-8")
    config = tomllib.loads(text)
    with WorkerPool(IRONCLAD_A20_ACT1, 1) as workers:
        trainer = PPOTrainer(Policy(ModelConfig(**config["model"])), workers, PPOConfig(**config["ppo"]),
                             seed=120000000, training_seed_limit=2000000000000,
                             training_config_digest=_training_identity(config, workers=1, shards=1))
        trainer.train_update()
        trainer.environment_steps = 58015744
        save_checkpoint(source / "latest.pt", trainer)
    old = torch.load(source / "latest.pt", weights_only=False)
    digest = sha256_file(source / "latest.pt")
    implementation = training_implementation_digest()
    manifest = {"status": "COMPLETE", "stages": {"train": {"status": "COMPLETE"}},
                "environment_steps": 58015744, "updates": 1,
                "training_implementation_sha256": implementation,
                "training_identity_sha256": old["contract"]["training_config_sha256"]}
    (source / "run-manifest.json").write_text(json.dumps(manifest))
    (source / "training-bundle.json").write_text(json.dumps({"files": {
        name: sha256_file(source / name) for name in ("latest.pt", "run-manifest.json", "training-config.toml")}}))
    target = tmp_path / "child"
    child_benchmark = tmp_path / "child-preparation/benchmark.json"
    child = text.split("[warm_start]")[0]
    child = child.replace("target_environment_steps = 58000000", "target_environment_steps = 70000000")
    child = child.replace('output = "local/runs/ironclad-a20-act1-plateau-win-2m-r1"',
                          f'output = "{target.as_posix()}"')
    child = child.replace(f'benchmark = "{benchmark.as_posix()}"',
                          f'benchmark = "{child_benchmark.as_posix()}"')
    child = child.replace('selection_progress_guard = true',
                          'selection_progress_guard = true\n'
                          f'continuation_from = "{source.as_posix()}"\n'
                          f'continuation_checkpoint_sha256 = "{digest}"\n'
                          'continuation_selection_evidence = "completed-endpoint"')
    path = tmp_path / "child.toml"
    path.write_text(child, encoding="utf-8")
    report = initialize(path)
    assert report["old_training_implementation_sha256"] == implementation
    saved = torch.load(target / "latest.pt", weights_only=False)
    for key in ("model", "optimizer", "trainer", "python_rng", "torch_rng", "cuda_rng", "environments"):
        _same(old[key], saved[key])
    assert not (target / "stages/train/selection/best_progress.pt").exists()
    assert child_benchmark.read_bytes() == benchmark.read_bytes()
    # A corrupt or unreviewed endpoint cannot be silently accepted.
    manifest["training_implementation_sha256"] = "a" * 64
    (source / "run-manifest.json").write_text(json.dumps(manifest))
    with pytest.raises(ValueError, match="bundle evidence"):
        initialize(path)
    (source / "training-bundle.json").write_text(json.dumps({"files": {
        name: sha256_file(source / name) for name in ("latest.pt", "run-manifest.json", "training-config.toml")}}))
    with pytest.raises(ValueError, match="exact reviewed implementation transition"):
        initialize(path)
    assert native_source_digest() == saved["contract"]["native_source_sha256"]
