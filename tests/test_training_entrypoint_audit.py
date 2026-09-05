"""Exercise shutdown/finalization through the CLI, without training or evaluation."""

from __future__ import annotations

import json
import signal
from pathlib import Path
from types import SimpleNamespace

import pytest

from sls.curriculum import IRONCLAD_A0_FULLRUN
from tools import train_full_run as cli


@pytest.mark.parametrize("change", [{"reached_act2_rate": 0.59}, {"reached_act3_rate": 0.039},
                                    {"backend_errors": 1}, {"episodes": 256}])
def test_warm_start_retention_gate_rejects_weak_or_invalid_baseline(change):
    evaluation = {"episodes": 1000, "reached_act2_rate": 0.655, "reached_act3_rate": 0.061, **change}
    with pytest.raises(ValueError):
        cli._validate_baseline_retention(evaluation, {
            "minimum_evaluation_episodes": 1000,
            "baseline_minimum_reached_act2_rate": 0.60, "baseline_minimum_reached_act3_rate": 0.04,
        })


def test_local_migration_report_cannot_authorize_server_training():
    manifest = {"initialization": {"schema": "sls-model-input-migration-v1", "target_stage": "train",
                                   "validation_passed": True, "production_ready": False}}
    with pytest.raises(ValueError, match="does not authorize"):
        cli._require_predecessor_promotion(manifest, "train")


@pytest.fixture
def fake_run(tmp_path: Path, monkeypatch: pytest.MonkeyPatch):
    output = tmp_path / "run"
    output.mkdir()
    (output / "latest.pt").touch()
    config = tmp_path / "config.toml"
    config.write_text('''
[run]
seed = 10
device = "cpu"
benchmark = "benchmark.json"
output = "run"
periodic_evaluation_seed_start = 1000
periodic_evaluation_seed_count = 2
final_evaluation_seed_start = 2000
final_evaluation_seed_count = 2
diagnostic_evaluation_seed_start = 3000
diagnostic_evaluation_seed_count = 2
diagnostic_rotation_stride = 2
evaluation_max_steps = 5
[stages.train]
profile = "IRONCLAD_A0_FULLRUN"
target_environment_steps = 10
evaluate_every_steps = 1
diagnose_every_steps = 1
checkpoint_every_steps = 5
[model]
[ppo]
''', encoding="utf-8")
    manifest = {"status": "RUNNING", "stages": {}}
    manifest_path = output / "run-manifest.json"
    manifest_path.write_text(json.dumps(manifest), encoding="utf-8")
    monkeypatch.setattr(cli, "ROOT", tmp_path)
    monkeypatch.setattr(cli.sys, "argv", ["train", "--stage", "train", "--config", str(config)])
    monkeypatch.setattr(cli.torch.cuda, "is_available", lambda: False)
    monkeypatch.setattr(cli, "native_source_digest", lambda: "native")
    monkeypatch.setattr(cli, "native_artifact", lambda: {"sha256": "binary"})
    monkeypatch.setattr(cli, "git_state", lambda: {"commit": "git"})
    monkeypatch.setattr(cli, "_load_benchmark", lambda *a, **kw: (1, 1))
    monkeypatch.setattr(cli, "_training_identity", lambda *a, **kw: "identity")
    monkeypatch.setattr(cli, "_validate_existing_manifest", lambda *a, **kw: None)
    monkeypatch.setattr(cli, "_require_predecessor_promotion", lambda *a, **kw: None)
    monkeypatch.setattr(cli, "_last_evaluation_step", lambda *a: 0)
    monkeypatch.setattr(cli, "_baseline_evaluation", lambda *a: None)
    monkeypatch.setattr(cli, "Policy", lambda *a: SimpleNamespace())
    monkeypatch.setattr(cli, "_load_exact_or_runtime_rebind", lambda *a: "exact")
    monkeypatch.setattr(cli.torch, "load", lambda *a, **kw: {
        "contract": {"profile": IRONCLAD_A0_FULLRUN},
    })
    controller = cli.StopController()
    monkeypatch.setattr(controller, "install", lambda: None)
    monkeypatch.setattr(cli, "StopController", lambda: controller)

    class Pool:
        def __init__(self, *a, **kw):
            pass

        def __enter__(self):
            return self

        def __exit__(self, *a):
            pass

    class Trainer:
        environment_steps = 0
        update = 0
        episodes = 0
        termination_counts = {}
        model = None

        def __init__(self, *a, **kw):
            pass

        def train_update(self):
            self.environment_steps += 1
            self.update += 1
            return {"loss": 0.0}

    saved = []
    monkeypatch.setattr(cli, "ShardedWorkerPool", Pool)
    monkeypatch.setattr(cli, "PPOTrainer", Trainer)
    monkeypatch.setattr(cli, "save_checkpoint", lambda path, trainer: saved.append(
        (Path(path).name, trainer.environment_steps),
    ))
    return manifest_path, controller, saved


def test_completed_stage_rejected_without_overwriting_manifest(fake_run):
    path, _, _ = fake_run
    manifest = {"status": "COMPLETE", "stages": {"train": {
        "status": "COMPLETE", "completed_environment_steps": 10,
    }}}
    path.write_text(json.dumps(manifest), encoding="utf-8")
    before = path.read_bytes()
    with pytest.raises(ValueError, match="already completed"):
        cli.main()
    assert path.read_bytes() == before


def test_diagnostic_signal_saves_latest_and_marks_interrupted(fake_run, monkeypatch):
    path, controller, saved = fake_run
    calls = []

    def interrupted(*args, **kwargs):
        calls.append(1)
        controller.handler(signal.SIGTERM, None)
        raise InterruptedError("safe boundary")

    monkeypatch.setattr(cli, "evaluate", interrupted)
    assert cli.main() == 0
    assert calls == [1]
    assert saved == [("latest.pt", 1)]
    assert json.loads(path.read_text())["status"] == "INTERRUPTED"
    record = json.loads((path.parent / "stages/train/metrics.jsonl").read_text())
    assert record["diagnostic_evaluation_interrupted"] is True
