from __future__ import annotations

from pathlib import Path

import pytest


torch = pytest.importorskip("torch")
pytest.importorskip("sls.backends.simulator.native", exc_type=ImportError)

from sls.curriculum import IRONCLAD_A0_ACT1
from sls.model import ModelConfig, Policy
from sls.rl import PPOConfig, PPOTrainer, WorkerPool, load_checkpoint, save_checkpoint


def test_one_native_ppo_update_and_exact_resume(tmp_path: Path) -> None:
    model = Policy(ModelConfig(
        entity_feature_dim=16,
        action_feature_dim=12,
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
        path = save_checkpoint(tmp_path / "checkpoint.pt", trainer)
        expected_seed = trainer.next_seed
        expected_metrics = trainer.train_update()
        expected_model = {
            key: value.detach().clone()
            for key, value in trainer.model.state_dict().items()
        }
        load_checkpoint(path, trainer)
        assert trainer.update == 1
        assert trainer.next_seed == expected_seed
        actual_metrics = trainer.train_update()
        assert actual_metrics == pytest.approx(expected_metrics, rel=0.0, abs=0.0)
        for key, value in trainer.model.state_dict().items():
            assert torch.equal(value, expected_model[key]), key
