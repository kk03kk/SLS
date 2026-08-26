from __future__ import annotations

from pathlib import Path

import pytest


torch = pytest.importorskip("torch")
pytest.importorskip("sls.backends.simulator.native", exc_type=ImportError)

from sls.curriculum import IRONCLAD_A0_ACT1
from sls.model import ModelConfig, Policy
from sls.rl import PPOConfig, PPOTrainer, WorkerPool, evaluate, load_checkpoint, save_checkpoint


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
        assert {"approx_kl", "gradient_norm"} <= metrics.keys()
        path = save_checkpoint(tmp_path / "checkpoint.pt", trainer)
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

        legacy = tmp_path / "legacy-v1.pt"
        torch.save({"schema": "sls-full-run-ppo-v1"}, legacy)
        with pytest.raises(ValueError, match="unsupported training checkpoint"):
            load_checkpoint(legacy, trainer)
        legacy_v2 = tmp_path / "legacy-v2.pt"
        torch.save({"schema": "sls-full-run-ppo-v2"}, legacy_v2)
        with pytest.raises(ValueError, match="unsupported training checkpoint"):
            load_checkpoint(legacy_v2, trainer)


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
