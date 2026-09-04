from __future__ import annotations

import math
from dataclasses import replace
from pathlib import Path

import pytest

torch = pytest.importorskip("torch")
pytest.importorskip("sls.backends.simulator.native", exc_type=ImportError)

from sls.curriculum import IRONCLAD_A0_ACT1, IRONCLAD_A0_ACT2
from sls.model import ModelConfig, Policy, vocabulary_hash
from sls.rl import (
    CheckpointContractMismatch,
    PPOConfig,
    PPOTrainer,
    ShardedWorkerPool,
    WorkerPool,
    evaluate,
    load_checkpoint,
    load_checkpoint_environment_migration,
    load_checkpoint_runtime_rebind,
    save_checkpoint,
)
from sls.rl.training_contract import sha256_file
from tools.train_full_run import _resume_or_migrate_environment


def _migration_record(
    source_payload: dict[str, object], target_payload: dict[str, object], source_sha256: str,
) -> dict[str, object]:
    source_contract = source_payload["contract"]
    target_contract = target_payload["contract"]
    source_state = source_payload["trainer"]
    assert isinstance(source_contract, dict)
    assert isinstance(target_contract, dict)
    assert isinstance(source_state, dict)
    return {
        "schema": "sls-learning-environment-migration-v2",
        "environment_steps": source_state["environment_steps"],
        "update": source_state["update"],
        "source_checkpoint_sha256": source_sha256,
        "old_profile": source_contract["profile"].profile_id,
        "new_profile": target_contract["profile"].profile_id,
        "old_git_commit": source_contract["git_commit"],
        "new_git_commit": target_contract["git_commit"],
        "old_native_source_sha256": source_contract["native_source_sha256"],
        "new_native_source_sha256": target_contract["native_source_sha256"],
        "old_content_scope_sha256": source_contract["content_scope_sha256"],
        "new_content_scope_sha256": target_contract["content_scope_sha256"],
        "old_training_identity_sha256": source_contract["training_config_sha256"],
        "new_training_identity_sha256": target_contract["training_config_sha256"],
    }


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


def test_environment_migration_preserves_learning_and_resets_episode_state(
    tmp_path: Path,
) -> None:
    config = PPOConfig(
        rollout_steps=1, recurrent_sequence_length=1,
        minibatch_sequences=1, epochs=1,
    )
    model_config = ModelConfig(
        embedding_dim=32, transformer_layers=1, attention_heads=4,
        feedforward_dim=64, recurrent_hidden_dim=64,
    )
    with WorkerPool(IRONCLAD_A0_ACT1, 1) as workers:
        source = PPOTrainer(
            Policy(model_config), workers, config, seed=0,
            native_contract_digest="old-native", git_commit="old-git",
            training_config_digest="same-training",
            training_seed_limit=1_000_000_000_000,
        )
        source.environment_steps = 104_448
        source.update = 51
        source.memory.fill_(1.0)
        expected_model = {
            key: value.detach().clone() for key, value in source.model.state_dict().items()
        }
        expected_next_seed = source.next_seed
        path = save_checkpoint(tmp_path / "old.pt", source)
        payload = torch.load(path, map_location="cpu", weights_only=False)
        payload["contract"]["content_scope_sha256"] = "old-scope-hash"
        payload["contract"]["profile"] = replace(IRONCLAD_A0_ACT1, version=2)
        payload["contract"]["curriculum_version"] = 2
        torch.save(payload, path)

    with WorkerPool(IRONCLAD_A0_ACT1, 1) as workers:
        migrated = PPOTrainer(
            Policy(model_config), workers, config, seed=0,
            native_contract_digest="new-native", git_commit="new-git",
            training_config_digest="approved-new-training-schedule",
            training_seed_limit=3_000_000_000_000,
        )
        with pytest.raises(ValueError, match="contract does not match"):
            load_checkpoint(path, migrated)
        load_checkpoint_environment_migration(path, migrated)

        assert migrated.environment_steps == 104_448
        assert migrated.update == 51
        assert migrated.next_seed == expected_next_seed + workers.size
        assert torch.count_nonzero(migrated.memory) == 0
        assert migrated.episode_starts.tolist() == [True]
        assert [limit.steps for limit in migrated.episode_limits] == [0]
        for key, value in migrated.model.state_dict().items():
            assert torch.equal(value, expected_model[key]), key


def _create_completed_act2_migration(tmp_path: Path) -> tuple[
    Path, Path, dict[str, object], PPOConfig, ModelConfig,
]:
    config = PPOConfig(
        rollout_steps=1, recurrent_sequence_length=1,
        minibatch_sequences=1, epochs=1,
    )
    model_config = ModelConfig(
        embedding_dim=32, transformer_layers=1, attention_heads=4,
        feedforward_dim=64, recurrent_hidden_dim=64,
    )
    latest = tmp_path / "latest.pt"
    backup = tmp_path / "latest.pre-pilot-migration.pt"
    with WorkerPool(IRONCLAD_A0_ACT1, 1) as workers:
        source = PPOTrainer(
            Policy(model_config), workers, config, seed=17,
            native_contract_digest="act1-native", git_commit="act1-git",
            training_config_digest="act1-training", training_seed_limit=10**12,
        )
        source.environment_steps = 5_001_216
        source.update = 407
        save_checkpoint(latest, source)
        source_payload = torch.load(latest, map_location="cpu", weights_only=False)

    with WorkerPool(IRONCLAD_A0_ACT2, 1) as workers:
        target = PPOTrainer(
            Policy(model_config), workers, config, seed=17,
            native_contract_digest="act2-native", git_commit="act2-git",
            training_config_digest="act2-training", training_seed_limit=3 * 10**12,
        )
        previous, mode = _resume_or_migrate_environment(
            latest, backup, target, {"learning_migrations": []},
        )
        assert mode == "migration"
        assert previous is not None
        save_checkpoint(latest, target)
        target_payload = torch.load(latest, map_location="cpu", weights_only=False)

    manifest = {"learning_migrations": [
        _migration_record(source_payload, target_payload, sha256_file(backup)),
    ]}
    return latest, backup, manifest, config, model_config


def test_first_environment_migration_retains_the_exact_source_backup(
    tmp_path: Path,
) -> None:
    latest, backup, manifest, _, _ = _create_completed_act2_migration(tmp_path)
    migration = manifest["learning_migrations"][0]
    assert isinstance(migration, dict)
    assert sha256_file(backup) == migration["source_checkpoint_sha256"]
    assert sha256_file(latest) != sha256_file(backup)
    source = torch.load(backup, map_location="cpu", weights_only=False)
    assert source["trainer"]["environment_steps"] == 5_001_216
    assert source["trainer"]["update"] == 407


def test_completed_environment_migration_resumes_exactly(tmp_path: Path) -> None:
    latest, backup, manifest, config, model_config = _create_completed_act2_migration(
        tmp_path,
    )
    with WorkerPool(IRONCLAD_A0_ACT2, 1) as workers:
        resumed = PPOTrainer(
            Policy(model_config), workers, config, seed=17,
            native_contract_digest="act2-native", git_commit="act2-git",
            training_config_digest="act2-training", training_seed_limit=3 * 10**12,
        )
        previous, mode = _resume_or_migrate_environment(
            latest, backup, resumed, manifest,
        )
        assert previous is None
        assert mode == "exact"
        assert resumed.environment_steps == 5_001_216
        assert resumed.update == 407


def test_interrupted_act2_training_runtime_rebind_is_an_exact_resume(
    tmp_path: Path,
) -> None:
    latest, backup, manifest, config, model_config = _create_completed_act2_migration(
        tmp_path,
    )
    with WorkerPool(IRONCLAD_A0_ACT2, 1) as workers:
        running = PPOTrainer(
            Policy(model_config), workers, config, seed=17,
            native_contract_digest="act2-native", git_commit="act2-git",
            training_config_digest="act2-training", training_seed_limit=3 * 10**12,
        )
        _resume_or_migrate_environment(latest, backup, running, manifest)
        running.train_update()
        save_checkpoint(latest, running)
        resume_steps = running.environment_steps
        resume_update = running.update
        expected_metrics = running.train_update()
        expected_model = {
            key: value.detach().clone() for key, value in running.model.state_dict().items()
        }

    with WorkerPool(IRONCLAD_A0_ACT2, 1) as workers:
        rebound = PPOTrainer(
            Policy(model_config), workers, config, seed=17,
            native_contract_digest="fixed-native", git_commit="fixed-git",
            training_config_digest="act2-training", training_seed_limit=3 * 10**12,
        )
        previous, mode = _resume_or_migrate_environment(
            latest, backup, rebound, manifest,
        )
        assert previous is None
        assert mode == "runtime-rebind"
        assert rebound.environment_steps == resume_steps
        assert rebound.update == resume_update
        actual_metrics = rebound.train_update()
        assert actual_metrics == pytest.approx(expected_metrics, rel=0.0, abs=0.0)
        for key, value in rebound.model.state_dict().items():
            assert torch.equal(value, expected_model[key]), key


def test_completed_migration_rejects_a_tampered_source_backup(tmp_path: Path) -> None:
    latest, backup, manifest, config, model_config = _create_completed_act2_migration(
        tmp_path,
    )
    with backup.open("r+b") as stream:
        first = stream.read(1)
        stream.seek(0)
        stream.write(bytes([first[0] ^ 0xFF]))

    with WorkerPool(IRONCLAD_A0_ACT2, 1) as workers:
        resumed = PPOTrainer(
            Policy(model_config), workers, config, seed=17,
            native_contract_digest="act2-native", git_commit="act2-git",
            training_config_digest="act2-training", training_seed_limit=3 * 10**12,
        )
        with pytest.raises(
            FileExistsError,
            match="backup has no unique recorded source identity",
        ):
            _resume_or_migrate_environment(latest, backup, resumed, manifest)


def test_runtime_rebind_does_not_relax_policy_or_training_identity(tmp_path: Path) -> None:
    latest, _, _, config, model_config = _create_completed_act2_migration(tmp_path)
    payload = torch.load(latest, map_location="cpu", weights_only=False)
    payload["contract"]["git_commit"] = "old-git"
    payload["contract"]["native_source_sha256"] = "old-native"
    payload["contract"]["vocabulary_sha256"] = "tampered-vocabulary"
    torch.save(payload, latest)

    with WorkerPool(IRONCLAD_A0_ACT2, 1) as workers:
        resumed = PPOTrainer(
            Policy(model_config), workers, config, seed=17,
            native_contract_digest="act2-native", git_commit="act2-git",
            training_config_digest="act2-training", training_seed_limit=3 * 10**12,
        )
        with pytest.raises(CheckpointContractMismatch) as captured:
            load_checkpoint_runtime_rebind(latest, resumed)
        differences = {
            item["path"]: item for item in captured.value.differences
        }
        assert differences["git_commit"]["runtime_rebind_allowed"] is True
        assert differences["native_source_sha256"]["runtime_rebind_allowed"] is True
        assert differences["vocabulary_sha256"] == {
            "path": "vocabulary_sha256",
            "checkpoint": "tampered-vocabulary",
            "current": vocabulary_hash(),
            "runtime_rebind_allowed": False,
        }


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


def test_sharded_evaluation_matches_serial_seed_order_and_metrics() -> None:
    torch.manual_seed(31)
    model = Policy(ModelConfig(
        embedding_dim=32, transformer_layers=1, attention_heads=4,
        feedforward_dim=64, recurrent_hidden_dim=64,
    ))
    progress: list[tuple[int, int, int]] = []
    serial = evaluate(model, IRONCLAD_A0_ACT1, (21, 22, 23, 24), max_steps=24)
    sharded = evaluate(
        model, IRONCLAD_A0_ACT1, (21, 22, 23, 24), max_steps=24,
        environment_shards=2,
        progress_callback=lambda completed, total, decisions: progress.append(
            (completed, total, decisions)
        ),
    )

    assert sharded == serial
    assert progress
    assert progress[-1][1:] == (4, int(sharded.mean_steps * sharded.episodes))


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
