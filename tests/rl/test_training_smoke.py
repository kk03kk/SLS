from __future__ import annotations

from pathlib import Path

import pytest


torch = pytest.importorskip("torch")
pytest.importorskip("sls.backends.simulator.native", exc_type=ImportError)

from sls.curriculum import IRONCLAD_A0_ACT1
from sls.model import ModelConfig, Policy
from sls.rl import (
    PPOConfig, PPOTrainer, ShardedWorkerPool, WorkerPool, evaluate, load_checkpoint,
    load_model_weights, save_checkpoint,
)
from sls.rl.training_mode import TrainingMode


def test_sharded_worker_hosts_multiple_environments_per_process() -> None:
    with ShardedWorkerPool(IRONCLAD_A0_ACT1, 4, shard_count=2) as workers:
        decisions = workers.reset((0, 1, 2, 3))
        transitions = workers.step(tuple(item.actions[0].candidate_id for item in decisions))
        assert len(transitions) == 4
        states = workers.checkpoints()
        restored = workers.load_checkpoints(states)
        assert len(restored) == 4


def test_one_native_ppo_update_and_exact_resume(tmp_path: Path) -> None:
    model = Policy(ModelConfig(
        embedding_dim=32,
        transformer_layers=1,
        attention_heads=4,
        feedforward_dim=64,
    ))
    config = PPOConfig(rollout_steps=1, epochs=1, minibatch_size=1)
    with WorkerPool(IRONCLAD_A0_ACT1, 1) as workers:
        trainer = PPOTrainer(model, workers, config, seed=0)
        metrics = trainer.train_update()
        assert trainer.update == 1
        assert all(torch.isfinite(torch.tensor(value)) for value in metrics.values())
        assert {
            "approx_kl", "approx_kl_final", "approx_kl_epoch_1",
            "clip_fraction", "gradient_norm",
        } <= metrics.keys()
        path = save_checkpoint(tmp_path / "checkpoint.pt", trainer)
        saved_payload = torch.load(path, map_location="cpu", weights_only=False)
        assert saved_payload["contract"]["training_mode"] == "EXPERIMENTAL"
        assert saved_payload["contract"]["policy_transfer_verified"] is False
        assert saved_payload["contract"]["git_commit"] == "TEST_OR_UNSPECIFIED"
        assert "training_config_sha256" in saved_payload["contract"]
        expected_limits = [item.to_dict() for item in trainer.episode_limits]
        expected_seed = trainer.next_seed
        expected_metrics = trainer.train_update()
        expected_model = {
            key: value.detach().clone()
            for key, value in trainer.model.state_dict().items()
        }
        load_checkpoint(path, trainer)
        assert trainer.update == 1
        assert trainer.next_seed == expected_seed
        assert [item.to_dict() for item in trainer.episode_limits] == expected_limits
        actual_metrics = trainer.train_update()
        assert actual_metrics == pytest.approx(expected_metrics, rel=0.0, abs=0.0)
        for key, value in trainer.model.state_dict().items():
            assert torch.equal(value, expected_model[key]), key

        transferred = Policy(model.config)
        metadata = load_model_weights(path, transferred)
        assert metadata["profile"] == IRONCLAD_A0_ACT1.profile_id
        assert metadata["update"] == 1
        saved_weights = torch.load(path, map_location="cpu", weights_only=False)["model"]
        for key, value in transferred.state_dict().items():
            assert torch.equal(value, saved_weights[key]), key
        incompatible = Policy(ModelConfig(
            embedding_dim=64, transformer_layers=1,
            attention_heads=4, feedforward_dim=64,
        ))
        with pytest.raises(ValueError, match="architecture is incompatible"):
            load_model_weights(path, incompatible)
        with pytest.raises(ValueError, match="cannot be used for production"):
            load_model_weights(
                path, Policy(model.config),
                target_training_mode=TrainingMode.PRODUCTION,
            )

        legacy = tmp_path / "legacy-v1.pt"
        torch.save({"schema": "sls-full-run-ppo-v1"}, legacy)
        with pytest.raises(ValueError, match="unsupported training checkpoint"):
            load_checkpoint(legacy, trainer)
        legacy_v2 = tmp_path / "legacy-v2.pt"
        torch.save({"schema": "sls-full-run-ppo-v2"}, legacy_v2)
        with pytest.raises(ValueError, match="unsupported training checkpoint"):
            load_checkpoint(legacy_v2, trainer)
        trainer.training_mode = TrainingMode.PRODUCTION
        trainer.policy_transfer_verified = True
        with pytest.raises(ValueError, match="contract does not match"):
            load_checkpoint(path, trainer)


def test_synthetic_step_limit_is_a_failure_terminal() -> None:
    model = Policy(ModelConfig(embedding_dim=32, transformer_layers=1, attention_heads=4, feedforward_dim=64))
    config = PPOConfig(
        rollout_steps=1, epochs=1, minibatch_size=1,
        max_episode_steps=1, potential_shaping=False,
    )
    with WorkerPool(IRONCLAD_A0_ACT1, 1) as workers:
        trainer = PPOTrainer(model, workers, config, seed=0)
        rollout = trainer.collect()
        assert rollout.returns.tolist() == pytest.approx([-1.0], abs=1e-6)
        assert trainer.last_collect_terminations["step_limit"] == 1
        assert trainer.episodes == 1


def test_evaluation_can_stop_at_safe_inference_boundary() -> None:
    model = Policy(ModelConfig(embedding_dim=32, transformer_layers=1, attention_heads=4, feedforward_dim=64))
    with pytest.raises(InterruptedError):
        evaluate(model, IRONCLAD_A0_ACT1, (0,), stop_requested=lambda: True)


@pytest.mark.skipif(not torch.cuda.is_available(), reason="CUDA is unavailable")
def test_cuda_checkpoint_keeps_cpu_rng_state_loadable(tmp_path: Path) -> None:
    model = Policy(ModelConfig(
        embedding_dim=32, transformer_layers=1, attention_heads=4,
        feedforward_dim=64,
    ))
    config = PPOConfig(rollout_steps=1, epochs=1, minibatch_size=1)
    with WorkerPool(IRONCLAD_A0_ACT1, 1) as workers:
        trainer = PPOTrainer(model, workers, config, device="cuda", seed=0)
        path = save_checkpoint(tmp_path / "cuda.pt", trainer)
        load_checkpoint(path, trainer)
        assert trainer.device.type == "cuda"
