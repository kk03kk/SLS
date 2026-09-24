from dataclasses import replace

import pytest
import torch

from sls.backends.simulator import SimulatorBackend
from sls.curriculum import (
    CURRICULUM_PROFILES_BY_ID,
    IRONCLAD_A20_CURRICULUM,
    IRONCLAD_A20_HEART,
)
from sls.model import ModelConfig, Policy
from sls.rl import PPOConfig, PPOTrainer, VectorWorkerPool, save_checkpoint
from sls.rl.act1_transfer import initialize_act1_weights
from sls.rl.training_contract import sha256_file
from tests.simulator.test_fullrun_structure import _structural_action


@pytest.mark.parametrize("profile", IRONCLAD_A20_CURRICULUM)
@pytest.mark.parametrize("seed", [0, 3, 4])
def test_a20_curriculum_keeps_keys_and_stops_at_its_actual_horizon(profile, seed):
    assert CURRICULUM_PROFILES_BY_ID[profile.profile_id] == profile
    assert profile.ascension == 20 and profile.allows_keys
    backend = SimulatorBackend(profile)
    decision = backend.reset(seed)
    backend._native._set_skip_battles_for_testing(True)
    visited = {1}
    for _ in range(300):
        transition = backend.step(_structural_action(decision))
        decision = transition.decision
        visited.add(decision.observation.run.act)
        if decision.terminal:
            break
    assert decision.terminal and transition.info["success"]
    target_act = int(profile.horizon)
    # With all keys the native engine enters the Act 4 map immediately after
    # the second Act 3 boss. The ACT3 horizon stops at that boundary, before
    # the policy can choose an Act 4 room.
    terminal_act = 4 if target_act == 3 else target_act
    assert visited == set(range(1, terminal_act + 1))
    assert decision.observation.run.act == terminal_act
    if target_act == 3:
        assert transition.info["reason"] == "ACT_3_CLEARED"
        assert not decision.actions
    assert decision.observation.run.has_ruby_key
    assert decision.observation.run.has_sapphire_key
    assert decision.observation.run.has_emerald_key
    if target_act == 4:
        assert decision.observation.run.visible_boss_id == "THE_HEART"
    if target_act >= 3:
        assert backend.raw_state["run_state"]["second_boss"] != 0


def test_a20_heart_without_keys_cannot_count_act3_as_success():
    backend = SimulatorBackend(replace(IRONCLAD_A20_HEART, allow_key_acquisition=False))
    decision = backend.reset(0)
    backend._native._set_skip_battles_for_testing(True)
    for _ in range(300):
        transition = backend.step(_structural_action(decision))
        decision = transition.decision
        if decision.terminal:
            break
    assert decision.terminal and not transition.info["success"]
    assert transition.info["reason"] == "HEART_NOT_REACHED"


@pytest.mark.parametrize("source_profile,target_profile", list(zip(
    IRONCLAD_A20_CURRICULUM, IRONCLAD_A20_CURRICULUM[1:],
)))
def test_adjacent_curriculum_weight_transfer_is_explicit_and_fresh(tmp_path, source_profile, target_profile):
    model = ModelConfig(embedding_dim=32, transformer_layers=1, attention_heads=4,
                        feedforward_dim=64, recurrent_hidden_dim=32)
    ppo = PPOConfig(rollout_steps=1, recurrent_sequence_length=1, epochs=1)
    source = tmp_path / "parent" / "best.pt"
    with VectorWorkerPool(source_profile, 1) as workers:
        parent = PPOTrainer(Policy(model), workers, ppo, seed=9)
        parent.train_update()
        save_checkpoint(source, parent)
    digest = sha256_file(source)
    config = {
        "run": {"output": "child"},
        "stages": {"train": {"target_environment_steps": 3}},
        "warm_start": {"checkpoint": "parent/best.pt", "checkpoint_sha256": digest,
                       "parent_environment_steps": 1},
    }
    with VectorWorkerPool(target_profile, 1) as workers:
        trainer = PPOTrainer(Policy(model), workers, ppo, seed=100)
        with pytest.raises(ValueError, match="curriculum-stage"):
            initialize_act1_weights(trainer, config, root=tmp_path)
        config["warm_start"]["transfer_kind"] = "curriculum-stage"
        before_rng = torch.get_rng_state().clone()
        initial_environment = workers.checkpoints()
        record = initialize_act1_weights(trainer, config, root=tmp_path)
        assert record["target_profile"] == target_profile.profile_id
        assert not record["exact_resume_of_parent_experiment"]
        assert trainer.environment_steps == 1 and trainer.update == 0
        assert not trainer.optimizer.state
        assert torch.equal(torch.get_rng_state(), before_rng)
        assert workers.checkpoints() == initial_environment
        assert all(torch.equal(value, parent.model.state_dict()[key])
                   for key, value in trainer.model.state_dict().items())
        trainer.train_update()
        assert trainer.environment_steps == 2
    assert sha256_file(source) == digest
