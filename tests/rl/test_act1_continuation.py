"""A continuation retains learning state and only rebinds approved scheduling identity."""

from __future__ import annotations

import json
import subprocess
import sys
from pathlib import Path

import pytest
import torch

from sls.curriculum import IRONCLAD_A0_ACT1, IRONCLAD_A20_ACT1
from sls.model import ModelConfig, Policy
from sls.rl import PPOConfig, PPOTrainer, WorkerPool, load_checkpoint, save_checkpoint
from sls.rl.preparation import read_config
from sls.rl.training_contract import native_source_digest, sha256_file
from tools.initialize_act1_continuation import initialize
from tools.train_full_run import _training_identity


@pytest.mark.parametrize("profile", [IRONCLAD_A0_ACT1, IRONCLAD_A20_ACT1])
def test_continuation_preserves_learning_and_refuses_unreviewed_changes(
    tmp_path, monkeypatch, profile
):
    monkeypatch.setattr(torch.cuda, "is_available", lambda: False)
    torch.use_deterministic_algorithms(True)
    torch.backends.cudnn.benchmark = False
    torch.set_float32_matmul_precision("highest")
    root = Path(__file__).resolve().parents[2]
    text = (root / "configs/train/ironclad_a0_act1_5m.toml").read_text()
    source = tmp_path / "parent"
    selection = source / "stages/train/selection"
    selection.mkdir(parents=True)
    text = (
        text.replace("embedding_dim = 128", "embedding_dim = 32")
        .replace("transformer_layers = 4", "transformer_layers = 1")
        .replace("feedforward_dim = 256", "feedforward_dim = 64")
        .replace("recurrent_hidden_dim = 256", "recurrent_hidden_dim = 32")
        .replace("rollout_steps = 256", "rollout_steps = 2")
        .replace("recurrent_sequence_length = 64", "recurrent_sequence_length = 1")
        .replace("epochs = 2", "epochs = 1")
    )
    text = text.replace("IRONCLAD_A0_ACT1", profile.profile_id)
    (source / "training-config.toml").write_text(text)
    config = read_config(source / "training-config.toml")
    with WorkerPool(profile, 1) as workers:
        trainer = PPOTrainer(
            Policy(ModelConfig(**config["model"])),
            workers,
            PPOConfig(**config["ppo"]),
            seed=10000000,
            native_contract_digest=native_source_digest(),
            training_config_digest=_training_identity(config, workers=1, shards=1),
            training_seed_limit=2000000000000,
        )
        trainer.train_update()
        parent = save_checkpoint(selection / "best_progress.pt", trainer)
        digest = sha256_file(parent)
        (source / "final-evaluation.json").write_text(
            json.dumps({"checkpoint_sha256": digest})
        )
        (selection / "best_progress.json").write_text(
            json.dumps({"environment_steps": 2, "update": 1})
        )
        expected = trainer.train_update()
        weights = {k: v.clone() for k, v in trainer.model.state_dict().items()}
        benchmark = tmp_path / "benchmark.json"
        benchmark.write_text(json.dumps({"selected_workers": 1, "selected_shards": 1}))
        target = tmp_path / "child"
        child_text = text.replace(
            "target_environment_steps = 5000000", "target_environment_steps = 10000000"
        ).replace("evaluate_every_steps = 500000", "evaluate_every_steps = 250000")
        child_text = child_text.replace(
            'output = "local/runs/ironclad-a0-act1-v4-5m"',
            f'output = "{target.as_posix()}"\ncontinuation_from = "{source.as_posix()}"\ncontinuation_checkpoint_sha256 = "{digest}"',
        )
        child_text = child_text.replace(
            'benchmark = "local/runs/preparation/ironclad-a0-act1-v4-5m/benchmark.json"',
            f'benchmark = "{benchmark.as_posix()}"',
        )
        path = tmp_path / "config.toml"
        path.write_text(child_text, encoding="utf-8")
        # Use the actual preparation launcher, with no inherited Python path.
        # In-process imports previously masked the direct-script bootstrap bug.
        from tools.prepare_and_train import run_tool

        monkeypatch.delenv("PYTHONPATH", raising=False)
        monkeypatch.setenv("PYTHONUTF8", "1")
        monkeypatch.setenv("CUDA_VISIBLE_DEVICES", "-1")
        run_tool("initialize_act1_continuation.py", "--config", path)
        marker = json.loads((target / "continuation.json").read_text(encoding="utf-8"))
        assert marker["parent_environment_steps"] == 2
        assert sha256_file(parent) == digest
        assert initialize(path) == marker
        import tools.preflight_training as preflight

        monkeypatch.syspath_prepend(str(root / "tools"))
        report = tmp_path / "preflight.json"
        monkeypatch.setattr(
            sys,
            "argv",
            [
                "preflight",
                "--allow-cpu",
                "--skip-build",
                "--config",
                str(path),
                "--benchmark",
                str(benchmark),
                "--checkpoint",
                str(target / "latest.pt"),
                "--output",
                str(report),
            ],
        )
        assert preflight.main() == 0
        assert json.loads(report.read_text())[
            "source_checkpoint_sha256"
        ] == sha256_file(target / "latest.pt")
        run_tool(
            "preflight_training.py", "--allow-cpu", "--skip-build",
            "--config", path, "--benchmark", benchmark,
            "--checkpoint", target / "latest.pt", "--output", report,
        )
        assert json.loads(report.read_text())["exact_resume"] == "PASS"
        # Re-entry must not rewrite the existing latest checkpoint.
        child_sha = sha256_file(target / "latest.pt")
        run_tool("initialize_act1_continuation.py", "--config", path)
        assert sha256_file(target / "latest.pt") == child_sha
        assert sha256_file(parent) == digest
        child = read_config(path)
        trainer.training_config_digest = _training_identity(child, workers=1, shards=1)
        load_checkpoint(target / "latest.pt", trainer)
        assert trainer.environment_steps == 2
        assert trainer.train_update() == expected
        assert all(
            torch.equal(v, weights[k]) for k, v in trainer.model.state_dict().items()
        )
        half_text = child_text.replace(
            str(target.as_posix()), str((tmp_path / "half-lr").as_posix())
        ).replace("learning_rate = 0.00025", "learning_rate = 0.000125")
        half_path = tmp_path / "half.toml"
        half_path.write_text(half_text, encoding="utf-8")
        # An interruption before publication cannot leave a partial target.
        def fail_publish(*args):
            raise OSError("simulated publication interruption")

        with monkeypatch.context() as interrupted:
            interrupted.setattr("tools.initialize_act1_continuation.os.rename", fail_publish)
            with pytest.raises(OSError, match="publication interruption"):
                initialize(half_path)
        assert not (tmp_path / "half-lr").exists()
        assert sha256_file(parent) == digest
        # Also exclude cwd/environment path injection and user site packages.
        subprocess.run(
            [sys.executable, "-I", "-X", "utf8",
             str(root / "tools/initialize_act1_continuation.py"),
             "--config", str(half_path)], cwd=root, check=True,
        )
        half_marker = initialize(half_path)
        assert half_marker["old_learning_rate"] == 0.00025
        assert half_marker["new_learning_rate"] == 0.000125
        half_config = read_config(half_path)
        trainer.config = PPOConfig(**half_config["ppo"])
        trainer.training_config_digest = _training_identity(
            half_config, workers=1, shards=1
        )
        half_checkpoint = tmp_path / "half-lr/latest.pt"
        load_checkpoint(half_checkpoint, trainer)
        assert trainer.optimizer.param_groups[0]["lr"] == 0.000125
        saved_parent = torch.load(parent, weights_only=False)
        saved_half = torch.load(half_checkpoint, weights_only=False)
        for key, state in saved_parent["optimizer"]["state"].items():
            for field, value in state.items():
                assert torch.equal(value, saved_half["optimizer"]["state"][key][field])
        trainer.train_update()
        assert any(
            not torch.equal(v, weights[k])
            for k, v in trainer.model.state_dict().items()
        )
        # A second continuation must preserve the selected best, not final state.
        half_source = tmp_path / "half-lr"
        half_best = half_source / "stages/train/selection/best_progress.pt"
        half_sha = sha256_file(half_best)
        (half_source / "final-evaluation.json").write_text(
            json.dumps({"checkpoint_sha256": half_sha}), encoding="utf-8"
        )
        grandchild = tmp_path / "grandchild"
        grand_text = half_text.replace(
            f'output = "{half_source.as_posix()}"',
            f'output = "{grandchild.as_posix()}"',
        ).replace(
            f'continuation_from = "{source.as_posix()}"',
            f'continuation_from = "{half_source.as_posix()}"',
        ).replace(digest, half_sha).replace(
            "learning_rate = 0.000125", "learning_rate = 0.0000625"
        ).replace("target_environment_steps = 10000000", "target_environment_steps = 20000000").replace(
            "evaluate_every_steps = 250000", "evaluate_every_steps = 1000000"
        )
        grand_path = tmp_path / "grand.toml"
        grand_path.write_text(grand_text, encoding="utf-8")
        run_tool("initialize_act1_continuation.py", "--config", grand_path)
        grand_config = read_config(grand_path)
        trainer.config = PPOConfig(**grand_config["ppo"])
        trainer.training_config_digest = _training_identity(grand_config, workers=1, shards=1)
        load_checkpoint(grandchild / "latest.pt", trainer)
        assert trainer.environment_steps == 2
        assert trainer.optimizer.param_groups[0]["lr"] == 0.0000625
        assert sha256_file(half_best) == half_sha
        assert json.loads((grandchild / "stages/train/selection/best_progress.json").read_text())["environment_steps"] == 2
        trainer.train_update()
        path.write_text(
            child_text.replace("gamma = 1.0", "gamma = 0.9"), encoding="utf-8"
        )
        with pytest.raises(ValueError, match="PPO/reward"):
            initialize(path)
        path.write_text(child_text.replace(digest, "0" * 64), encoding="utf-8")
        with pytest.raises(ValueError, match="SHA256"):
            initialize(path)


def test_half_lr_reuses_measured_layout_without_relabeling_report(tmp_path, monkeypatch):
    import sls.rl.preparation as preparation
    root = Path(__file__).resolve().parents[2]
    text = (root / "configs/train/ironclad_a0_act1_5m.toml").read_text()
    parent = tmp_path / "parent"
    parent.mkdir()
    (parent / "training-config.toml").write_text(text)
    original = read_config(parent / "training-config.toml")
    config = read_config(root / "configs/train/ironclad_a0_act1_10m.toml")
    config["run"]["continuation_from"] = str(parent)
    layout = {"workload_contract": preparation.workload_contract(original)}
    assert preparation.benchmark_matches_workload(config, layout)
    assert layout == {"workload_contract": preparation.workload_contract(original)}
    config["ppo"]["epochs"] = 3
    assert not preparation.benchmark_matches_workload(config, layout)


def test_neow_metrics_count_decisions_without_changing_objective():
    with WorkerPool(IRONCLAD_A0_ACT1, 1) as workers:
        trainer = PPOTrainer(Policy(ModelConfig(embedding_dim=32, transformer_layers=1,
                            attention_heads=4, feedforward_dim=64)), workers,
                            PPOConfig(rollout_steps=1,recurrent_sequence_length=1,
                                      minibatch_sequences=1,epochs=1), seed=7)
        metrics = trainer.train_update()
        assert metrics["neow_decisions"] == 1
        assert sum(metrics[f"neow_option_{i}_count"] for i in range(4)) == 1
        assert 0 <= metrics["neow_mean_swap_probability"] <= 1
        assert 0 <= metrics["neow_mean_normalized_entropy"] <= 1


def test_contract_diagnostic_direct_script_bootstrap():
    root = Path(__file__).resolve().parents[2]
    subprocess.run(
        [sys.executable, "-I", str(root / "tools/diagnose_checkpoint_contract.py"),
         "--help"], cwd=root, check=True, capture_output=True,
    )


def test_multigeneration_benchmark_reuse_is_lr_only(tmp_path):
    from sls.rl.preparation import benchmark_matches_workload, workload_contract

    root = Path(__file__).resolve().parents[2]
    base_text = (root / "configs/train/ironclad_a0_act1_5m.toml").read_text()
    parent = tmp_path / "5m"
    parent.mkdir()
    (parent / "training-config.toml").write_text(base_text, encoding="utf-8")
    original = read_config(parent / "training-config.toml")
    child = tmp_path / "10m"
    child.mkdir()
    child_text = (root / "configs/train/ironclad_a0_act1_10m.toml").read_text().replace(
        'continuation_from = "local/runs/ironclad-a0-act1-v4-5m"',
        f'continuation_from = "{parent.as_posix()}"',
    )
    child_path = child / "training-config.toml"
    child_path.write_text(child_text, encoding="utf-8")
    config = read_config(root / "configs/train/ironclad_a0_act1_20m.toml")
    config["run"]["continuation_from"] = str(child)
    layout = {"workload_contract": workload_contract(original)}
    assert benchmark_matches_workload(config, layout)
    assert layout == {"workload_contract": workload_contract(original)}
    child_path.write_text(child_text.replace("epochs = 2", "epochs = 3"), encoding="utf-8")
    assert not benchmark_matches_workload(config, layout)
    child_path.write_text(child_text.replace(parent.as_posix(), child.as_posix()), encoding="utf-8")
    assert not benchmark_matches_workload(config, layout)


def test_20m_plan_keeps_learning_contract_and_disjoint_heldout():
    root = Path(__file__).resolve().parents[2]
    old = read_config(root / "configs/train/ironclad_a0_act1_10m.toml")
    new = read_config(root / "configs/train/ironclad_a0_act1_20m.toml")
    assert new["run"]["continuation_from"] == old["run"]["output"]
    assert new["run"]["output"] != old["run"]["output"]
    assert new["run"]["continuation_checkpoint_sha256"] == "c8e113c4e486c9615baf5f763398383b7e740f88abbd7a39d5a97b38dbae691d"
    assert new["stages"]["train"]["target_environment_steps"] == 20000000
    assert new["stages"]["train"]["evaluate_every_steps"] == 1000000
    assert new["stages"]["train"]["checkpoint_every_steps"] == 250000
    assert new["model"] == old["model"]
    assert new["ppo"] == {**old["ppo"], "learning_rate": 0.0000625}
    assert new["run"]["periodic_evaluation_seed_start"] == old["run"]["periodic_evaluation_seed_start"]
    assert new["run"]["periodic_evaluation_seed_count"] == 512
    assert new["run"]["final_evaluation_seed_count"] == 1024
    assert new["run"]["final_evaluation_seed_start"] >= old["run"]["final_evaluation_seed_start"] + 1024
    assert new["run"]["training_seed_limit"] == old["run"]["training_seed_limit"]
