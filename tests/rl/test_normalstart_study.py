"""CPU regressions for the isolated normal-start Act2 comparison."""

import copy
import random
from dataclasses import replace
from types import SimpleNamespace

import pytest
import torch

from sls.curriculum import IRONCLAD_A20_ACT2
from sls.model import ModelConfig, Policy, PolicyBatch
from sls.rl import PPOConfig, PPOTrainer, load_checkpoint, save_checkpoint
from sls.rl.checkpoint import CheckpointContractMismatch
from sls.rl.learning_diagnostics import act_diagnostics
from sls.rl.ppo import advantage_domain_diagnostics, normalize_advantages
from sls.rl.value_adaptation import (
    ValueAdaptationConfig,
    ValueAdapter,
    complete_returns,
)
from sls.rl.workers import VectorWorkerPool
from tools.verify_act12_diagnostics import assert_same


def small_model(dropout=0.):
    return Policy(ModelConfig(embedding_dim=32, transformer_layers=1, attention_heads=4,
                  feedforward_dim=64, recurrent_hidden_dim=32, dropout=dropout,
                  architecture="sls-recurrent-relational-policy-v6"))


def small_ppo(**kwargs):
    return PPOConfig(rollout_steps=4, recurrent_sequence_length=2, minibatch_sequences=1,
                     epochs=1, reward_schema="sls-curriculum-win-v1", failure_progress_scale=0., **kwargs)


@pytest.mark.parametrize("mode", ["GLOBAL", "none", "", None, 1])
def test_reject_invalid_normalization(mode):
    with pytest.raises(ValueError):
        PPOConfig(advantage_normalization=mode)


def test_global_scale_is_common_and_domain_default_is_preserved():
    assert PPOConfig().advantage_normalization == "domain"
    values = torch.tensor([[1., 2., 100.], [2., 4., 200.]])
    domains = torch.tensor([[0, 1, 2], [0, 1, 2]])
    normalized = normalize_advantages(values)
    assert abs(float(normalized.mean())) < 1e-6
    assert abs(float(normalized.std(unbiased=False)) - 1) < 1e-6
    metrics = advantage_domain_diagnostics(values, normalized, domains, normalization="global")
    assert metrics["advantage_scale_run"] == metrics["advantage_scale_combat"] == metrics["advantage_scale_choice"]
    assert metrics["advantage_normalized_std_choice"] != metrics["advantage_normalized_std_combat"]


def test_act_metadata_and_actual_terminal_counts():
    rollout = SimpleNamespace(acts=torch.tensor([[1, 2], [2, 2]]), advantages=torch.tensor([[1., 2.], [3., 4.]]),
        returns=torch.ones(2, 2), old_values=torch.zeros(2, 2), normalized_entropies=torch.ones(2, 2) * .5,
        termination_reasons=((None, "death"), (None, "success")))
    result = act_diagnostics(rollout)
    assert result["act_1_decisions"] == 1 and result["act_2_decisions"] == 3
    assert result["act_2_success"] == result["act_2_death"] == 1
    assert result["act_2_value_mse_old"] == 1


def test_global_optimization_records_actual_scaling_and_rollout_acts():
    torch.set_num_threads(2)
    with VectorWorkerPool(IRONCLAD_A20_ACT2, 2) as workers:
        trainer = PPOTrainer(small_model(), workers, small_ppo(advantage_normalization="global"), seed=150)
        rollout = trainer.collect()
        assert rollout.acts.shape == rollout.advantages.shape
        metrics = trainer.optimize(rollout)
        assert metrics["advantage_normalization_global"] == 1
        assert sum(metrics[f"act_{act}_decisions"] for act in range(1, 5)) == 8
        expected = 1 / (float(rollout.advantages.std(unbiased=False)) + 1e-8)
        for name in ("combat", "run", "choice"):
            if metrics[f"samples_{name}_fraction"]:
                assert metrics[f"advantage_scale_{name}"] == expected


def test_adaptation_config_is_a_strict_checkpoint_contract(tmp_path):
    with VectorWorkerPool(IRONCLAD_A20_ACT2, 1) as workers:
        trainer = PPOTrainer(small_model(), workers, small_ppo(), seed=151)
        trainer.value_adaptation_config = {"episodes": 512, "fit_episodes": 384}
        trainer.value_adaptation_state = {"status": "ADAPTED", "decisions": 12}
        path = save_checkpoint(tmp_path / "safe.pt", trainer)
        trainer.value_adaptation_state = None
        load_checkpoint(path, trainer)
        assert trainer.value_adaptation_state["decisions"] == 12
        trainer.value_adaptation_config["fit_episodes"] = 383
        with pytest.raises(CheckpointContractMismatch, match="fit_episodes"):
            load_checkpoint(path, trainer)


def test_probe_is_observational_including_dropout_and_future_update(tmp_path):
    torch.set_num_threads(2)
    with VectorWorkerPool(IRONCLAD_A20_ACT2, 1) as workers:
        trainer = PPOTrainer(small_model(.15), workers, small_ppo(), seed=142)
        path = save_checkpoint(tmp_path / "safe.pt", trainer)
        rollout = trainer.collect()
        rng = torch.get_rng_state().clone()
        py_rng = random.getstate()
        worker_state = workers.checkpoints()
        model = copy.deepcopy(trainer.model.state_dict())
        adam = copy.deepcopy(trainer.optimizer.state_dict())
        from sls.rl.learning_diagnostics import shared_gradients
        from sls.rl.ppo import normalize_advantages_by_domain
        trainer.model.train()
        trainer.last_batch_metrics = {}
        trainer.model.cls.grad = torch.ones_like(trainer.model.cls)
        grad = trainer.model.cls.grad.clone()
        probe = shared_gradients(trainer, rollout, trainer._sequence_chunks(rollout)[:1],
            normalize_advantages_by_domain(rollout.advantages, rollout.encoded_decisions))
        assert "shared_grad_cosine" in probe
        assert torch.equal(trainer.model.cls.grad, grad)
        assert torch.equal(torch.get_rng_state(), rng) and random.getstate() == py_rng
        assert_same(trainer.model.state_dict(), model)
        assert_same(trainer.optimizer.state_dict(), adam)
        assert_same(workers.checkpoints(), worker_state)
        load_checkpoint(path, trainer)
        trainer.train_update()
        expected_model = copy.deepcopy(trainer.model.state_dict())
        expected_adam = copy.deepcopy(trainer.optimizer.state_dict())
        expected_rng = torch.get_rng_state().clone()
        load_checkpoint(path, trainer)
        trainer.config = replace(trainer.config, gradient_diagnostic_interval=1)
        metrics = trainer.train_update()
        assert "shared_grad_cosine" in metrics
        assert_same(trainer.model.state_dict(), expected_model)
        assert_same(trainer.optimizer.state_dict(), expected_adam)
        assert torch.equal(torch.get_rng_state(), expected_rng)
        trainer.config = replace(trainer.config, advantage_normalization="global", gradient_diagnostic_interval=0)
        with pytest.raises(CheckpointContractMismatch, match="advantage_normalization"):
            load_checkpoint(path, trainer)


def test_complete_returns_do_not_cross_episode_boundaries():
    assert complete_returns([1, 2, -1]).tolist() == [2, 1, -1]
    assert complete_returns([8]).tolist() == [8]


def adapter(model, directory, **kwargs):
    config = ValueAdaptationConfig(episodes=4, fit_episodes=3, epochs=2, batch_size=8,
                                   seed_start=192_000_000, max_decisions=200)
    # Finite training-only episode limit keeps this real native test small.
    return ValueAdapter(model, IRONCLAD_A20_ACT2, small_ppo(max_episode_steps=3), config, directory, **kwargs)


def test_collection_fit_resume_freezing_and_rng(tmp_path):
    torch.set_num_threads(2)
    model = small_model().eval()
    from sls.backends.simulator.environment import SimulatorBackend
    decision = SimulatorBackend(IRONCLAD_A20_ACT2).reset(192_000_010)
    batch = PolicyBatch.from_decisions([decision], model.config)
    with torch.no_grad():
        before = model(*batch.model_inputs())
    weights = copy.deepcopy(model.state_dict())
    rng, py_rng = torch.get_rng_state().clone(), random.getstate()
    a = adapter(model, tmp_path / "uninterrupted")
    result = a.advance()
    assert result["complete_episodes"] == 4 and result["decisions"] == 12
    assert torch.equal(torch.get_rng_state(), rng) and random.getstate() == py_rng
    assert_same(model.state_dict(), weights)
    b = adapter(model, tmp_path / "resume")
    b.advance(max_decisions=2)
    assert b.state["active"] is not None
    b = adapter(model, tmp_path / "resume")
    b.advance(max_fit_batches=1)
    b = adapter(model, tmp_path / "resume")
    result_b = b.advance()
    assert result_b["status"] == result["status"]
    assert result_b["history"] == result["history"]
    assert_same(a.best_head, b.best_head)
    assert_same(a.optimizer.state_dict(), b.optimizer.state_dict())
    assert [x["split"] for x in b.state["episodes"]] == ["fit", "fit", "fit", "validation"]
    if result["status"] == "ADAPTED":
        b.apply(model)
        assert all(torch.equal(value, model.state_dict()[key]) for key, value in weights.items()
                   if not key.startswith("value_head."))
        with torch.no_grad():
            after = model(*batch.model_inputs())
        assert torch.equal(before.logits, after.logits)
        assert torch.equal(before.next_memory, after.next_memory)
    episode = tmp_path / "resume" / b.state["episodes"][0]["path"]
    episode.write_bytes(episode.read_bytes() + b"tampered")
    original_model = copy.deepcopy(model)
    original_model.load_state_dict(weights)
    with pytest.raises(ValueError, match="identity mismatch"):
        adapter(original_model, tmp_path / "resume")


def test_no_gain_cap_and_failure_transactions(tmp_path, monkeypatch):
    model = small_model()
    a = adapter(model, tmp_path / "no-gain")
    # Force a tied validation error: baseline must win, no silent adoption.
    monkeypatch.setattr(a, "_validation_mse", lambda: 1.)
    assert a.advance()["status"] == "NO_ADAPTATION_GAIN"
    with pytest.raises(ValueError, match="no validated gain"):
        a.apply(model)
    b = adapter(model, tmp_path / "failure")
    b.advance(max_decisions=1)
    original = b.backend.step
    def fail_step(action):
        original(action)
        raise RuntimeError("injected step failure")
    monkeypatch.setattr(b.backend, "step", fail_step)
    with pytest.raises(RuntimeError, match="injected"):
        b.advance()
    assert b.state["decisions"] == 1
    assert b.report()["decisions"] == 2  # failed simulator calls still consume budget
    b = adapter(model, tmp_path / "failure")
    b.advance(max_fit_batches=0)
    head = copy.deepcopy(b.head.state_dict())
    original_step = b.optimizer.step
    def fail_adam():
        original_step()
        raise RuntimeError("injected Adam failure")
    monkeypatch.setattr(b.optimizer, "step", fail_adam)
    with pytest.raises(RuntimeError, match="injected"):
        b.advance()
    assert_same(head, b.head.state_dict())
    assert not b.optimizer.state and b.state["cursor"] == 0
    c = ValueAdapter(model, IRONCLAD_A20_ACT2, small_ppo(max_episode_steps=3),
                     replace(b.config, max_decisions=1), tmp_path / "cap")
    with pytest.raises(RuntimeError, match="cap exhausted"):
        c.advance()
    assert c.state["phase"] == "FAILED" and c.state["decisions"] == 1


def test_epoch_validation_failure_rolls_back_fit_transaction(tmp_path, monkeypatch):
    a = adapter(small_model(), tmp_path)
    a.advance(max_fit_batches=1)
    before = copy.deepcopy(a.head.state_dict())
    before_optimizer = copy.deepcopy(a.optimizer.state_dict())
    before_rng = a.generator.get_state()
    assert a.state["cursor"] == 8  # next batch completes the nine-decision fit set
    def fail_validation():
        raise RuntimeError("injected validation failure")
    monkeypatch.setattr(a, "_validation_mse", fail_validation)
    with pytest.raises(RuntimeError, match="validation failure"):
        a.advance()
    assert a.state["cursor"] == 8
    assert_same(a.head.state_dict(), before)
    assert_same(a.optimizer.state_dict(), before_optimizer)
    assert torch.equal(a.generator.get_state(), before_rng)


def test_normal_start_never_opens_entry_bank(monkeypatch):
    import sls.rl.start_curriculum as module
    def forbidden(*_, **__):
        raise AssertionError("entry bank accessed")
    monkeypatch.setattr(module, "StartCurriculum", forbidden)
    assert module.from_config({}, None) is None


def test_adaptation_journal_failure_preserves_original_exception(tmp_path, monkeypatch):
    a = adapter(small_model(), tmp_path)
    safe = a.journal.read_bytes()
    def fail_step():
        raise RuntimeError("original collector failure")
    def fail_save():
        raise OSError("injected journal disk failure")
    monkeypatch.setattr(a, "_collect_one", fail_step)
    monkeypatch.setattr(a, "_save", fail_save)
    with pytest.raises(RuntimeError, match="original collector failure") as caught:
        a.advance()
    assert "journal disk failure" in caught.value.__notes__[0]
    assert a.journal.read_bytes() == safe
