import pytest
import torch

from sls.curriculum import IRONCLAD_A0_ACT1
from sls.model import ModelConfig, Policy
from sls.rl import PPOConfig, PPOTrainer, load_checkpoint, save_checkpoint
from sls.rl.workers import VectorWorkerPool
from tools.train_full_run import _write_failure_diagnostics


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


def test_backward_oom_after_partial_update_recovers_from_safe_checkpoint(tmp_path):
    import copy
    import json

    torch.set_num_threads(2)
    model = ModelConfig(embedding_dim=32, transformer_layers=1, attention_heads=4,
                        feedforward_dim=64, recurrent_hidden_dim=32)
    config = PPOConfig(rollout_steps=2, recurrent_sequence_length=1, minibatch_sequences=1, epochs=1)
    with VectorWorkerPool(IRONCLAD_A0_ACT1, 1) as workers:
        trainer = PPOTrainer(Policy(model), workers, config, seed=42, device="cpu")
        latest = save_checkpoint(tmp_path / "latest.pt", trainer)
        safe_bytes = latest.read_bytes()
        expected_metrics = trainer.train_update()
        expected_model = copy.deepcopy(trainer.model.state_dict())
        expected_adam = copy.deepcopy(trainer.optimizer.state_dict())
        load_checkpoint(latest, trainer)
        error = torch.OutOfMemoryError("injected second-minibatch backward failure")

        def fail_after_one_step(gradient):
            if trainer.last_update_context["optimizer_steps_completed"]:
                raise error
            return gradient

        hook = trainer.model.cls.register_hook(fail_after_one_step)
        try:
            with pytest.raises(torch.OutOfMemoryError) as caught:
                trainer.train_update()
            assert caught.value is error
        finally:
            hook.remove()
        assert trainer.last_update_context["phase"] == "backward"
        assert trainer.last_update_context["optimizer_steps_completed"] == 1
        assert latest.read_bytes() == safe_bytes
        receipt = _write_failure_diagnostics(
            tmp_path, latest, trainer, device="cpu", operation="ppo_update", error=error,
        )
        record = json.loads((tmp_path / receipt["path"]).read_text(encoding="utf-8"))
        assert record["update_context"]["batch"]["sequence_length"] == 1
        assert record["recovery_checkpoint"]["environment_steps"] == 0
        assert record["recovery_checkpoint"]["update"] == 0
        load_checkpoint(latest, trainer)
        assert trainer.train_update() == expected_metrics
        from tools.verify_act12_diagnostics import assert_same

        assert_same(trainer.model.state_dict(), expected_model)
        assert_same(trainer.optimizer.state_dict(), expected_adam)
