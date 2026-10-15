"""Real native PPO finalization keeps the endpoint intact across a v5 control."""

import copy
import json
import tomllib
from pathlib import Path

import pytest
import torch

import sls.rl.preparation as preparation
from sls.curriculum import IRONCLAD_A20_ACT1
from sls.model import ModelConfig, Policy
from sls.rl import PPOConfig, PPOTrainer, save_checkpoint
from sls.rl.training_contract import native_artifact, native_source_digest, sha256_file
from sls.rl.workers import VectorWorkerPool
from tools import train_full_run as cli
from tools.prepare_act12_long_run import toml_text
from tools.verify_act12_diagnostics import assert_same


@pytest.mark.parametrize(("interrupt_reference", "with_adaptation", "interrupt_adaptation"), [
    (False, False, False), (True, False, False), (False, True, False), (True, True, False), (False, True, True),
])
def test_v5_reference_v6_endpoint_and_selected_finalization(tmp_path, monkeypatch, interrupt_reference, with_adaptation, interrupt_adaptation):
    source = Path(__file__).resolve().parents[2] / "configs/train/ironclad_a0_act1_5m.toml"
    config = tomllib.loads(source.read_text(encoding="utf-8"))
    config["model"].update(embedding_dim=32, transformer_layers=1, feedforward_dim=64, recurrent_hidden_dim=32)
    config["ppo"].update(rollout_steps=2, recurrent_sequence_length=2, minibatch_sequences=2, epochs=1)
    config["ppo"].update(failure_progress_scale=0.0, reward_schema="sls-curriculum-win-v1")
    if with_adaptation:
        config["ppo"]["max_episode_steps"] = 3
    parent_path = tmp_path / "parent/final.pt"
    with VectorWorkerPool(IRONCLAD_A20_ACT1, 1) as workers:
        parent = PPOTrainer(Policy(ModelConfig(**config["model"])), workers, PPOConfig(**config["ppo"]), seed=19)
        if with_adaptation:
            with torch.no_grad():
                parent.model.value_head[-1].bias.fill_(10.)
        parent.environment_steps = 16
        save_checkpoint(parent_path, parent)
    parent_hash = sha256_file(parent_path)
    config["model"]["architecture"] = "sls-recurrent-relational-policy-v6"
    config["run"].update(
        profile="IRONCLAD_A20_ACT2", output="run", benchmark="benchmark.json", device="cpu",
        periodic_evaluation_seed_count=2, final_evaluation_seed_count=2, evaluation_max_steps=4,
        evaluate_fixed_endpoint=True, development_reference_checkpoint="parent/final.pt",
        development_reference_sha256=parent_hash, development_reference_profile="IRONCLAD_A20_ACT1",
        selection_objective="ACT2_CLEAR_COUNT",
    )
    config["stages"]["train"].update(
        profile="IRONCLAD_A20_ACT2", target_environment_steps=32 if with_adaptation else 20, evaluate_every_steps=100,
        checkpoint_every_steps=4, minimum_evaluation_episodes=2, minimum_final_evaluation_episodes=2,
    )
    config["warm_start"] = {
        "checkpoint": "parent/final.pt", "checkpoint_sha256": parent_hash, "parent_environment_steps": 16,
        "transfer_kind": "curriculum-stage", "model_migration": "v5-to-v6-role-bindings",
    }
    if with_adaptation:
        config["value_adaptation"] = dict(episodes=4, fit_episodes=3, seed_start=192_000_000,
                                           max_decisions=200, epochs=2, batch_size=8)
    path = tmp_path / "config.toml"
    path.write_text(toml_text(config), encoding="utf-8")
    (tmp_path / "benchmark.json").write_text(json.dumps({
        "schema": "sls-worker-benchmark-v2", "selected_workers": 2, "selected_shards": 1,
        "native_source_sha256": native_source_digest(), "native_artifact": native_artifact(),
    }), encoding="utf-8")
    monkeypatch.setattr(cli, "ROOT", tmp_path)
    monkeypatch.setattr(preparation, "require_preparation", lambda *_: {})
    monkeypatch.setattr(cli.sys, "argv", ["train", "--stage", "train", "--config", str(path)])
    trainers = []
    original_trainer = cli.PPOTrainer

    def record_trainer(*args, **kwargs):
        trainer = original_trainer(*args, **kwargs)
        trainers.append(trainer)
        return trainer

    monkeypatch.setattr(cli, "PPOTrainer", record_trainer)
    evaluate = cli.evaluate
    reference_calls = []
    endpoint_before = []

    def check_evaluate(model, *args, **kwargs):
        trainer = trainers[-1]
        if model.config.architecture.endswith("v5"):
            assert model is not trainer.model
            assert trainer.model.config.architecture.endswith("v6")
            reference_calls.append(True)
            endpoint_before.append(copy.deepcopy(trainer.model.state_dict()))
            if interrupt_reference and len(reference_calls) == 1:
                raise RuntimeError("injected frozen-reference failure")
        elif endpoint_before:
            assert_same(trainer.model.state_dict(), endpoint_before[-1])
        return evaluate(model, *args, **kwargs)

    monkeypatch.setattr(cli, "evaluate", check_evaluate)
    output = tmp_path / "run"
    if interrupt_adaptation:
        from sls.rl.value_adaptation import ValueAdapter
        controllers = []
        controller_class = cli.StopController
        def controller_factory():
            controller = controller_class()
            controllers.append(controller)
            return controller
        monkeypatch.setattr(cli, "StopController", controller_factory)
        advance = ValueAdapter.advance
        def stop_during_collection(adapter, **kwargs):
            result = advance(adapter, max_decisions=2, **kwargs)
            controllers[-1].requested = True
            return result
        monkeypatch.setattr(ValueAdapter, "advance", stop_during_collection)
        assert cli.main() == 0
        stopped = json.loads((output / "run-manifest.json").read_text(encoding="utf-8"))
        assert stopped["status"] == "INTERRUPTED"
        safe = torch.load(output / "latest.pt", map_location="cpu", weights_only=False)
        assert safe["trainer"]["environment_steps"] == 16 and safe["trainer"]["update"] == 0
        assert safe["trainer"]["value_adaptation"] is None and not safe["optimizer"]["state"]
        monkeypatch.setattr(ValueAdapter, "advance", advance)
    if interrupt_reference:
        with pytest.raises(RuntimeError, match="frozen-reference failure"):
            cli.main()
        failed = json.loads((output / "run-manifest.json").read_text(encoding="utf-8"))
        assert failed["status"] == "FAILED"
        diagnostic = json.loads((output / failed["failure_diagnostics"]["path"]).read_text(encoding="utf-8"))
        assert diagnostic["operation"] == "evaluation:frozen-reference-development"
        recovery_state = torch.load(output / "latest.pt", map_location="cpu", weights_only=False)
        assert_same(trainers[-1].model.state_dict(), endpoint_before[-1])
        assert cli.main() == 0
        assert_same(trainers[-1].model.state_dict(), recovery_state["model"])
        assert trainers[-1].update == 1
    else:
        assert cli.main() == 0
    manifest = json.loads((output / "run-manifest.json").read_text(encoding="utf-8"))
    assert manifest["status"] == "COMPLETE"
    assert manifest["environment_steps"] == (32 if with_adaptation else 20)
    assert sha256_file(parent_path) == parent_hash
    reference = json.loads((output / "reference-evaluation.json").read_text(encoding="utf-8"))
    final = json.loads((output / "final-evaluation.json").read_text(encoding="utf-8"))
    endpoint = torch.load(output / "final.pt", map_location="cpu", weights_only=False)
    latest = torch.load(output / "latest.pt", map_location="cpu", weights_only=False)
    assert reference["model"]["architecture"].endswith("v5")
    assert final["model"]["architecture"].endswith("v6")
    assert final["checkpoint_environment_steps"] == (28 if with_adaptation else 16)
    if with_adaptation:
        assert latest["trainer"]["value_adaptation"]["decisions"] == 12
        assert latest["trainer"]["value_adaptation"]["status"] == "ADAPTED"
    assert torch.equal(torch.get_rng_state(), endpoint["torch_rng"])
    # Act2 limits count as policy failures, so this also exercises actual export
    # with the existing gate; zero wins are not a policy-quality qualification.
    from sls.runtime import load_policy_artifact

    artifact = load_policy_artifact(output / "run.pt", device="cpu")
    selected = torch.load(output / "stages/train/selection/best_progress.pt", map_location="cpu", weights_only=False)
    assert_same(artifact.model.state_dict(), selected["model"])
    if with_adaptation and not interrupt_reference:
        from tools import preflight_training
        monkeypatch.setattr(preflight_training.sys, "argv", [
            "preflight", "--config", str(path), "--benchmark", str(tmp_path / "benchmark.json"),
            "--checkpoint", str(output / "latest.pt"), "--allow-cpu", "--skip-build",
            "--output", str(tmp_path / "c-resume-preflight.json"),
        ])
        latest_bytes = (output / "latest.pt").read_bytes()
        assert preflight_training.main() == 0
        assert (output / "latest.pt").read_bytes() == latest_bytes
    assert (output / "training-bundle.json").is_file()
    for key in ("model", "optimizer", "trainer", "environments", "python_rng", "torch_rng", "cuda_rng"):
        assert_same(endpoint[key], latest[key])
    assert_same(trainers[-1].model.state_dict(), endpoint["model"])
    if interrupt_reference:
        assert manifest["previous_failures"][0]["failure_diagnostics"]
        assert "failure_diagnostics" not in manifest
        receipt = manifest["previous_failures"][0]["failure_diagnostics"]
        bundle = json.loads((output / "training-bundle.json").read_text(encoding="utf-8"))
        assert bundle["files"][receipt["path"]] == receipt["sha256"]
