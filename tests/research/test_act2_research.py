from __future__ import annotations

import copy
import json
import random
from types import SimpleNamespace

import pytest
import torch

from sls.backends.simulator import SimulatorBackend
from sls.contracts import Action
from sls.curriculum import IRONCLAD_A20_ACT2
from sls.diagnostics.cpu import decision_record
from sls.model import ModelConfig, Policy
from sls.research.bank import (
    NaturalBank,
    NaturalInitializer,
    coverage,
    rebuild,
    write_gzip,
)
from sls.research.checkpoint import load, save
from sls.research.diagnostics import ResearchDiagnostics
from sls.research.protocol import (
    NATIVE_SHA256,
    PARENT_SHA256,
    RANGES,
    digest,
    paired,
    scan_registrations,
    should_stop,
)
from sls.rl import PPOConfig, PPOTrainer, VectorWorkerPool, save_checkpoint
from sls.rl.episode_limit import EpisodeLimitState
from sls.rl.training_contract import sha256_file


def trainer(hooks=None, initializer=None):
    torch.set_num_threads(1)
    config = ModelConfig(embedding_dim=32, transformer_layers=1, attention_heads=4,
                         feedforward_dim=64, recurrent_hidden_dim=64)
    ppo = PPOConfig(rollout_steps=4, recurrent_sequence_length=2, minibatch_sequences=2,
                    epochs=1, final_kl_samples=8)
    return PPOTrainer(Policy(config), VectorWorkerPool(IRONCLAD_A20_ACT2, 2), ppo,
                      seed=13, research_hooks=hooks, episode_initializer=initializer)


def test_diagnostics_preserve_rng_actions_losses_updates_and_gae():
    torch.manual_seed(7)
    baseline = trainer()
    torch.manual_seed(7)
    observed = trainer(ResearchDiagnostics(2, prefix_workers=2))
    rng = torch.get_rng_state()
    python_rng = random.getstate()
    first = baseline.collect()
    expected_rng = torch.get_rng_state()
    torch.set_rng_state(rng)
    second = observed.collect()
    assert torch.equal(torch.get_rng_state(), expected_rng)
    for name in ("action_indices", "old_log_probabilities", "old_values", "advantages", "returns",
                 "episode_starts", "input_memories", "previous_action_types", "previous_rewards"):
        assert torch.equal(getattr(first, name), getattr(second, name)), name
    rng = torch.get_rng_state()
    a = baseline.optimize(first)
    torch.set_rng_state(rng)
    b = observed.optimize(second)
    assert a == b
    for key, value in baseline.model.state_dict().items():
        assert torch.equal(value, observed.model.state_dict()[key]), key
    assert baseline.optimizer.state_dict()["param_groups"] == observed.optimizer.state_dict()["param_groups"]
    for key, state in baseline.optimizer.state_dict()["state"].items():
        for field, value in state.items():
            assert torch.equal(value, observed.optimizer.state_dict()["state"][key][field])
    assert random.getstate() == python_rng
    assert observed.research_hooks.records[0]["gradients"]["actor_norm"] > 0


def test_unupdated_sequence_recomputation():
    torch.manual_seed(8)
    subject = trainer()
    rollout = subject.collect()
    subject.model.eval()
    chunks = subject._sequence_chunks(rollout)
    logp, values, _ = subject._evaluate_sequences(rollout, chunks)
    torch.testing.assert_close(logp, subject._select_sequences(rollout.old_log_probabilities, chunks), atol=2e-6, rtol=2e-6)
    torch.testing.assert_close(values, subject._select_sequences(rollout.old_values, chunks), atol=2e-6, rtol=2e-6)


def tiny_bank(tmp_path):
    seed = RANGES["bank"][0]
    backend = SimulatorBackend(IRONCLAD_A20_ACT2)
    decision = backend.reset(seed)
    limits = EpisodeLimitState.initial(decision)
    action = decision.actions[0]
    transition = backend.step(action)
    row = {**decision_record(decision), "chosen_action": action.to_dict(),
           "raw_reward": float(transition.reward), "terminal": False}
    boundary = transition.decision
    assert limits.observe(boundary, max_steps=4096, max_boundary_visits=4) is None
    public = {"prefix": [row], "boundary": decision_record(boundary)}
    private = {"native": backend.checkpoint(), "limits": limits.to_dict()}
    write_gzip(tmp_path / "public/p.json.gz", public)
    write_gzip(tmp_path / "private/p.json.gz", private)
    anchor = {"id": "anchor", "seed": seed, "stratum": "entry", "boss": None, "step": 1,
              "public_path": "public/p.json.gz", "private_path": "private/p.json.gz",
              "public_sha256": sha256_file(tmp_path / "public/p.json.gz"),
              "private_sha256": sha256_file(tmp_path / "private/p.json.gz"),
              "boundary_sha256": digest(public["boundary"])}
    manifest = {"schema": "sls-act2-training-bank-v1", "role": "training-only", "native_sha256": NATIVE_SHA256,
                "teacher_sha256": PARENT_SHA256, "seed_range": list(RANGES["bank"]),
                "anchors": [anchor], "coverage": coverage([anchor])}
    (tmp_path / "manifest.json").write_text(json.dumps(manifest))
    return NaturalBank(tmp_path, require_qualified=False), public, private


def test_public_rebuild_private_isolation_and_historical_limits(tmp_path):
    bank, public, private = tiny_bank(tmp_path)
    assert bank.qualify()["passed"]
    subject = trainer()
    memory, action, reward = rebuild(subject.model, public["prefix"])
    assert memory.shape == (1, 64)
    assert action > 0 and isinstance(reward, float)
    assert private["limits"]["steps"] == 1
    assert "native" not in public
    # Qualification fails if counters are changed even with re-signed file metadata.
    tampered = copy.deepcopy(private)
    tampered["limits"]["visits"] = {"fake": 1}
    write_gzip(tmp_path / "private/t.json.gz", tampered)
    bank.manifest["anchors"][0]["private_path"] = "private/t.json.gz"
    bank.manifest["anchors"][0]["private_sha256"] = sha256_file(tmp_path / "private/t.json.gz")
    with pytest.raises(ValueError, match="limiter mismatch"):
        bank.qualify()


def test_sampler_preserves_previous_inputs_and_invalidates_cache(tmp_path, monkeypatch):
    bank, public, _ = tiny_bank(tmp_path)
    # Sampling one test anchor in every stratum isolates reset semantics from production coverage.
    bank.groups = {key: bank.groups["entry"] for key in bank.groups}
    sampler = NaturalInitializer(bank, probability=1., seed=9)
    subject = trainer(initializer=sampler)
    calls = []
    import sls.research.bank as module
    original = module.rebuild
    def measured(*args):
        calls.append(1)
        return original(*args)
    monkeypatch.setattr(module, "rebuild", measured)
    first = sampler.reset_many(subject, [0])[0]
    second = sampler.reset_many(subject, [1])[0]
    assert len(calls) == 1 and not first.start
    assert first.limits.steps == len(public["prefix"])
    assert first.previous_action > 0
    first.memory.zero_()
    assert second.memory.abs().sum() > 0
    subject.update += 1
    sampler.reset_many(subject, [0])
    assert len(calls) == 2


def test_research_checkpoint_exact_resume_and_identity_rejection(tmp_path):
    bank = SimpleNamespace(identity="test-bank", groups={})
    subject = trainer(ResearchDiagnostics(2, prefix_workers=1), NaturalInitializer(bank, probability=0., seed=6))
    subject.train_update()
    checkpoint = tmp_path / "research.pt"
    save(checkpoint, subject, {"arm": "control"})
    expected = subject.collect()
    restored = trainer(ResearchDiagnostics(2, prefix_workers=1), NaturalInitializer(bank, probability=0., seed=6))
    load(checkpoint, restored, {"arm": "control"})
    actual = restored.collect()
    assert torch.equal(expected.action_indices, actual.action_indices)
    assert torch.equal(expected.returns, actual.returns)
    assert subject.episode_initializer.rng.getstate() == restored.episode_initializer.rng.getstate()
    with pytest.raises(ValueError, match="identity"):
        load(checkpoint, restored, {"arm": "curriculum"})
    with pytest.raises(ValueError, match="envelope"):
        save_checkpoint(tmp_path / "naked.pt", restored)


def test_statistics_and_namespace_gate(tmp_path):
    result = paired([True] * 20, [False] * 20, bootstrap_samples=100)
    assert result["lost"] == 20 and result["gained"] == 0
    assert result["exact_p_two_sided"] == pytest.approx(2 ** -19)
    assert should_stop([result, result]) and not should_stop([result])
    (tmp_path / "registered.json").write_text(json.dumps({"seed": RANGES["bank"][0]}))
    with pytest.raises(ValueError, match="collision"):
        scan_registrations([tmp_path])


def test_coverage_and_wrong_bank_role(tmp_path):
    bank, _, _ = tiny_bank(tmp_path)
    assert not bank.manifest["coverage"]["qualified"]
    with pytest.raises(ValueError, match="coverage"):
        NaturalBank(tmp_path)
    manifest = bank.manifest
    manifest["role"] = "diagnostics-only"
    (tmp_path / "manifest.json").write_text(json.dumps(manifest))
    with pytest.raises(ValueError, match="identity"):
        NaturalBank(tmp_path, require_qualified=False)


def test_teacher_prefix_actual_actions_are_not_student_samples(tmp_path):
    bank, public, _ = tiny_bank(tmp_path)
    model = trainer().model
    action = Action.from_dict(public["prefix"][0]["chosen_action"])
    assert action.to_dict() in public["prefix"][0]["actions"]
    assert "advantage" not in public["prefix"][0]
    assert bank.manifest["role"] == "training-only"
    rebuild(model, public["prefix"])


def test_sampler_disabled_during_critic_warmup_and_counts_episode_not_decisions():
    bank = SimpleNamespace(identity="test-bank", groups={})
    sampler = NaturalInitializer(bank, probability=1., seed=5)
    subject = trainer(initializer=sampler)
    subject.critic_warmup = SimpleNamespace(active=True)
    before = sampler.rng.getstate()
    initial = sampler.reset_many(subject, [0, 1])
    assert all(item.start and item.source["kind"] == "normal" for item in initial)
    assert sampler.rng.getstate() == before
    assert sampler.counts == {"normal": 4, "bank": 0}  # includes the two original starts


def test_cpu_entry_rejects_cuda_before_torch_import(tmp_path):
    import os
    import subprocess
    import sys
    from pathlib import Path
    root = Path(__file__).resolve().parents[2]
    result = subprocess.run([sys.executable, str(root / "tools/run_act2_research.py"), "gpu-gate",
        "--parent", "absent.pt", "--output", str(tmp_path)],
        env={**os.environ, "CUDA_VISIBLE_DEVICES": "0"}, text=True, capture_output=True)
    assert result.returncode and "before importing Torch" in result.stderr


def test_supervisor_archives_failure_and_never_runs_later_stage(tmp_path):
    import sys
    import tarfile
    import time

    from tools.submit_act2_learning_research import supervise
    output = tmp_path / "run"
    with pytest.raises(RuntimeError, match="failed"):
        supervise([("fault", [sys.executable, "-c", "raise SystemExit(7)"]),
                   ("must-not-run", [sys.executable, "-c", "raise AssertionError()"] )],
                  output, deadline=time.time() + 10)
    manifest = json.loads((output / "inventory.json").read_text())
    assert manifest["status"] == "failed"
    assert not (output / "must-not-run.log").exists()
    with tarfile.open(output.with_suffix(".tar.gz")) as archive:
        assert "run/failure.json" in archive.getnames()


def test_login_entry_is_torch_free():
    import subprocess
    import sys
    result = subprocess.run([sys.executable, "-c", "import sys; import tools.submit_act2_learning_research; assert 'torch' not in sys.modules"], capture_output=True, text=True)
    assert result.returncode == 0, result.stderr


def test_warmup_diagnostics_and_normal_initializer_preserve_updates():
    from dataclasses import replace

    from sls.rl.critic_warmup import CriticWarmupConfig, CriticWarmupState
    bank = SimpleNamespace(identity="test-bank", groups={})
    torch.manual_seed(17)
    baseline = trainer()
    torch.manual_seed(17)
    observed = trainer(ResearchDiagnostics(2), NaturalInitializer(bank, probability=1.))
    for item in (baseline, observed):
        item.config = replace(item.config, max_episode_steps=2)
        item.critic_warmup = CriticWarmupState(CriticWarmupConfig(rollout_updates=2, batch_size=4), 2, 2)
        item._apply_warmup_freeze()
    for _ in range(2):
        rng = torch.get_rng_state()
        first = baseline.train_update()
        expected_rng = torch.get_rng_state()
        torch.set_rng_state(rng)
        second = observed.train_update()
        assert first == second
        assert torch.equal(expected_rng, torch.get_rng_state())
        assert all(torch.equal(v, observed.model.state_dict()[k]) for k, v in baseline.model.state_dict().items())
    assert observed.research_hooks.records[0]["shared_value_norm"] == 0
    assert observed.episode_initializer.counts["bank"] == 0
