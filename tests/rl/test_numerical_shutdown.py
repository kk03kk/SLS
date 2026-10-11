import pytest
import torch

from sls.curriculum import IRONCLAD_A0_ACT1
from sls.model import ModelConfig, Policy
from sls.rl import PPOConfig, PPOTrainer
from sls.rl.workers import VectorWorkerPool


def test_nonfinite_gradient_stops_before_adam_changes_weights():
    torch.set_num_threads(2)
    config = ModelConfig(embedding_dim=32, transformer_layers=1, attention_heads=4,
                         feedforward_dim=64, recurrent_hidden_dim=64)
    ppo = PPOConfig(rollout_steps=2, recurrent_sequence_length=2, minibatch_sequences=1, epochs=1)
    with VectorWorkerPool(IRONCLAD_A0_ACT1, 1) as workers:
        trainer = PPOTrainer(Policy(config), workers, ppo, seed=42)
        rollout = trainer.collect()
        before = {k: v.clone() for k, v in trainer.model.state_dict().items()}
        parameter = next(trainer.model.parameters())
        hook = parameter.register_hook(lambda gradient: torch.full_like(gradient, float("nan")))
        with pytest.raises(RuntimeError, match="non-finite"):
            trainer.optimize(rollout)
        hook.remove()
        assert not trainer.optimizer.state
        assert all(torch.equal(before[k], v) for k, v in trainer.model.state_dict().items())
