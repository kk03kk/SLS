from __future__ import annotations

import math
from pathlib import Path

import pytest

torch = pytest.importorskip("torch")
pytest.importorskip("sls.backends.simulator.native", exc_type=ImportError)

from sls.curriculum import IRONCLAD_A0_ACT1
from sls.model import ModelConfig, Policy
from sls.rl import (
    PPOConfig,
    PPOTrainer,
    ShardedWorkerPool,
    WorkerPool,
    evaluate,
    load_checkpoint,
    save_checkpoint,
)


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
        recurrent_hidden_dim=64,
    ))
    config = PPOConfig(
        rollout_steps=1, recurrent_sequence_length=1,
        minibatch_sequences=1, epochs=1,
    )
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
        assert saved_payload["contract"]["simulator_only"] is True
        assert saved_payload["contract"]["git_commit"] == "TEST_OR_UNSPECIFIED"
        assert "training_config_sha256" in saved_payload["contract"]
        expected_limits = [item.to_dict() for item in trainer.episode_limits]
        expected_seed = trainer.next_seed
        expected_steps = trainer.environment_steps
        expected_memory = trainer.memory.detach().clone()
        expected_metrics = trainer.train_update()
        expected_model = {
            key: value.detach().clone()
            for key, value in trainer.model.state_dict().items()
        }
        load_checkpoint(path, trainer)
        assert trainer.update == 1
        assert trainer.next_seed == expected_seed
        assert trainer.environment_steps == expected_steps
        assert torch.equal(trainer.memory, expected_memory)
        assert [item.to_dict() for item in trainer.episode_limits] == expected_limits
        actual_metrics = trainer.train_update()
        assert actual_metrics == pytest.approx(expected_metrics, rel=0.0, abs=0.0)
        for key, value in trainer.model.state_dict().items():
            assert torch.equal(value, expected_model[key]), key

        legacy = tmp_path / "legacy-v1.pt"
        torch.save({"schema": "sls-full-run-ppo-v1"}, legacy)
        with pytest.raises(ValueError, match="unsupported training checkpoint"):
            load_checkpoint(legacy, trainer)
        legacy_v2 = tmp_path / "legacy-v4.pt"
        torch.save({"schema": "sls-full-run-ppo-v4"}, legacy_v2)
        with pytest.raises(ValueError, match="unsupported training checkpoint"):
            load_checkpoint(legacy_v2, trainer)


def test_synthetic_step_limit_is_a_failure_terminal() -> None:
    model = Policy(ModelConfig(embedding_dim=32, transformer_layers=1, attention_heads=4, feedforward_dim=64))
    config = PPOConfig(
        rollout_steps=1, recurrent_sequence_length=1,
        minibatch_sequences=1, epochs=1,
        max_episode_steps=1, potential_shaping=False,
    )
    with WorkerPool(IRONCLAD_A0_ACT1, 1) as workers:
        trainer = PPOTrainer(model, workers, config, seed=0)
        rollout = trainer.collect()
        assert rollout.returns[:, 0].tolist() == pytest.approx([-1.0], abs=1e-6)
        assert trainer.last_collect_terminations["step_limit"] == 1
        assert trainer.episodes == 1


def test_recurrent_rollout_preserves_time_and_environment_axes() -> None:
    model = Policy(ModelConfig(
        embedding_dim=32, transformer_layers=1, attention_heads=4,
        feedforward_dim=64, recurrent_hidden_dim=64,
    ))
    config = PPOConfig(
        rollout_steps=4, recurrent_sequence_length=2,
        minibatch_sequences=1, epochs=1,
    )
    with WorkerPool(IRONCLAD_A0_ACT1, 1) as workers:
        trainer = PPOTrainer(model, workers, config, seed=0)
        rollout = trainer.collect()
        assert rollout.shape == (4, 1)
        assert rollout.episode_starts.shape == (4, 1)
        assert rollout.input_memories.shape == (4, 1, 64)
        metrics = trainer.optimize(rollout)
        assert math.isfinite(metrics["loss"])


def test_training_seed_allocator_cannot_enter_evaluation_namespace() -> None:
    model = Policy(ModelConfig(
        embedding_dim=32, transformer_layers=1, attention_heads=4,
        feedforward_dim=64, recurrent_hidden_dim=64,
    ))
    config = PPOConfig(
        rollout_steps=1, recurrent_sequence_length=1,
        minibatch_sequences=1, epochs=1, max_episode_steps=1,
    )
    with WorkerPool(IRONCLAD_A0_ACT1, 1) as workers:
        trainer = PPOTrainer(
            model, workers, config, seed=0, training_seed_limit=1,
        )
        with pytest.raises(RuntimeError, match="held-out evaluation seeds"):
            trainer.collect()


def test_evaluation_can_stop_at_safe_inference_boundary() -> None:
    model = Policy(ModelConfig(embedding_dim=32, transformer_layers=1, attention_heads=4, feedforward_dim=64))
    with pytest.raises(InterruptedError):
        evaluate(model, IRONCLAD_A0_ACT1, (0,), stop_requested=lambda: True)


def test_recurrent_evaluation_is_deterministic_for_fixed_seeds() -> None:
    torch.manual_seed(29)
    model = Policy(ModelConfig(
        embedding_dim=32, transformer_layers=1, attention_heads=4,
        feedforward_dim=64, recurrent_hidden_dim=64,
    ))
    first = evaluate(model, IRONCLAD_A0_ACT1, (11, 12), max_steps=16)
    second = evaluate(model, IRONCLAD_A0_ACT1, (11, 12), max_steps=16)
    assert first == second


@pytest.mark.skipif(not torch.cuda.is_available(), reason="CUDA is unavailable")
def test_cuda_checkpoint_keeps_cpu_rng_state_loadable(tmp_path: Path) -> None:
    model = Policy(ModelConfig(
        embedding_dim=32, transformer_layers=1, attention_heads=4,
        feedforward_dim=64,
    ))
    config = PPOConfig(
        rollout_steps=1, recurrent_sequence_length=1,
        minibatch_sequences=1, epochs=1,
    )
    with WorkerPool(IRONCLAD_A0_ACT1, 1) as workers:
        trainer = PPOTrainer(model, workers, config, device="cuda", seed=0)
        path = save_checkpoint(tmp_path / "cuda.pt", trainer)
        load_checkpoint(path, trainer)
        assert trainer.device.type == "cuda"
