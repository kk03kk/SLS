"""Architecture isolation, binding sensitivity and explicit weights transfer."""
from dataclasses import replace

import pytest
import torch

from sls.backends.simulator import SimulatorBackend
from sls.content.scope import IRONCLAD_ASCENSION_SCOPE_ID
from sls.model import ModelConfig, Policy, PolicyBatch
from sls.model.encoding import (
    ENCODING_SCHEMA,
    content_token,
    policy_vocabulary,
    vocabulary_hash,
)
from sls.rl.checkpoint import CHECKPOINT_SCHEMA
from sls.rl.model_migration import migrate_v5_role_policy


def config(architecture="sls-recurrent-relational-policy-v5"):
    return ModelConfig(embedding_dim=32, transformer_layers=1, attention_heads=4,
                       feedforward_dim=64, recurrent_hidden_dim=64, architecture=architecture)


def payload(model):
    return {"schema": CHECKPOINT_SCHEMA, "model": model.state_dict(), "contract": {
        "model": model.config.to_dict(), "encoding_schema": ENCODING_SCHEMA,
        "vocabulary_sha256": vocabulary_hash(), "simulator_only": True,
        "content_scope_id": IRONCLAD_ASCENSION_SCOPE_ID,
        "content_scope_sha256": "1" * 64, "native_source_sha256": "2" * 64,
    }}


def test_v5_state_and_default_remain_unchanged():
    old = Policy(config())
    assert old.config.architecture.endswith("v5")
    assert not any("scale" in key or "option_variant" in key for key in old.state_dict())
    new = Policy(config("sls-recurrent-relational-policy-v6"))
    with pytest.raises(RuntimeError):
        new.load_state_dict(old.state_dict(), strict=True)


def test_migration_preserves_real_neow_and_following_decisions():
    torch.manual_seed(9)
    old = Policy(config()).eval()
    new, report = migrate_v5_role_policy(payload(old))
    new.eval()
    backend = SimulatorBackend()
    decision = backend.reset(132000000)
    memory = old.initial_memory(1)
    previous_action = torch.zeros(1, dtype=torch.long)
    previous_reward = torch.zeros(1)
    for _ in range(24):
        batch = PolicyBatch.from_decisions((decision,))
        with torch.no_grad():
            a, b = [model(*batch.model_inputs(), memory=memory,
                          previous_action_types=previous_action, previous_rewards=previous_reward)
                    for model in (old, new)]
        for name in ("logits", "value", "state", "next_memory"):
            torch.testing.assert_close(getattr(a, name), getattr(b, name), atol=1e-5, rtol=1e-5)
        index = int(a.logits.argmax())
        transition = backend.step(decision.actions[index])
        if transition.terminated:
            break
        memory = a.next_memory
        previous_action = batch.action_types[:, index] + 1
        previous_reward = torch.tensor([transition.reward])
        decision = transition.decision
    assert report["exact_resume"] is False and report["training_approved"] is False
    for name, value in old.state_dict().items():
        assert torch.equal(new.state_dict()[name], value)
    # Zero residual initialization must not prevent the new bindings learning.
    batch = PolicyBatch.from_decisions((SimulatorBackend().reset(132000000),))
    new(*batch.model_inputs()).logits[0, 0].backward()
    for name in ("category_field_scale", "reference_role_scale", "option_variant.weight"):
        gradient = dict(new.named_parameters())[name].grad
        assert torch.isfinite(gradient).all() and gradient.norm() > 0


def test_new_architecture_binds_reference_roles_and_category_fields():
    torch.manual_seed(3)
    model = Policy(config("sls-recurrent-relational-policy-v6")).eval()
    backend = SimulatorBackend()
    decision = backend.reset(132000000)
    # Tensor-level role swap: proves capacity, not a reachable gameplay failure.
    batch = PolicyBatch.from_decisions((decision,))
    refs = batch.action_references.clone()
    masks = batch.action_reference_mask.clone()
    refs[..., 0] = 0
    refs[..., 1] = 1
    masks[..., :2] = True
    first = replace(batch, action_references=refs, action_reference_mask=masks)
    swapped = refs.clone()
    swapped[..., 0], swapped[..., 1] = refs[..., 1], refs[..., 0]
    categories = batch.entity_categories.clone()
    categories[..., 0], categories[..., 1] = batch.entity_categories[..., 1], batch.entity_categories[..., 0]
    with torch.no_grad():
        a = model(*first.model_inputs())
        b = model(*replace(first, action_references=swapped).model_inputs())
        c = model(*batch.model_inputs())
        d = model(*replace(batch, entity_categories=categories).model_inputs())
    assert not torch.allclose(a.logits, b.logits, atol=1e-6, rtol=0)
    assert not torch.allclose(c.state, d.state, atol=1e-6, rtol=0)


def test_options_use_independent_embedding_and_can_unlearn_legacy_alias():
    torch.manual_seed(8)
    old = Policy(config()).eval()
    new, _ = migrate_v5_role_policy(payload(old))
    new.eval()
    _, variant = content_token("NEOW:OPTION:0")
    assert policy_vocabulary()["content"][variant] != "OPTION:0"
    batch = PolicyBatch.from_decisions((SimulatorBackend().reset(132000000),))
    with torch.no_grad():
        before = new(*batch.model_inputs()).state
        # A legacy content row must no longer affect the option variant.
        assert (batch.entity_content[..., 1] == variant).any()
        new.content.weight[variant, 0].add_(5)
        assert torch.equal(before, new(*batch.model_inputs()).state)
        new.option_variant.weight[variant, 0].add_(5)
        assert not torch.allclose(before, new(*batch.model_inputs()).state)


def test_v6_preserves_candidate_and_entity_order_invariance():
    model = Policy(config("sls-recurrent-relational-policy-v6")).eval()
    decision = SimulatorBackend().reset(132000000)
    reordered = replace(decision, actions=tuple(reversed(decision.actions)), observation=replace(
        decision.observation, event_options=tuple(reversed(decision.observation.event_options)),
    ))
    with torch.no_grad():
        a, b = [model(*PolicyBatch.from_decisions((d,)).model_inputs()) for d in (decision, reordered)]
    torch.testing.assert_close(a.logits, b.logits.flip(1), atol=1e-5, rtol=1e-5)
    torch.testing.assert_close(a.value, b.value, atol=1e-5, rtol=1e-5)


def test_v6_training_and_exact_resume(tmp_path):
    from sls.curriculum import IRONCLAD_A0_ACT2
    from sls.rl import (
        PPOConfig,
        PPOTrainer,
        ShardedWorkerPool,
        load_checkpoint,
        save_checkpoint,
    )

    ppo = PPOConfig(rollout_steps=4, recurrent_sequence_length=2, minibatch_sequences=1, epochs=1)
    model = Policy(config("sls-recurrent-relational-policy-v6"))
    with ShardedWorkerPool(IRONCLAD_A0_ACT2, 2, shard_count=1) as workers:
        trainer = PPOTrainer(model, workers, ppo, device="cpu", seed=31,
                             native_contract_digest="native", git_commit="local")
        trainer.train_update()
        path = tmp_path / "v6.pt"
        save_checkpoint(path, trainer)
        first = trainer.train_update()
        expected = {key: value.clone() for key, value in model.state_dict().items()}
        load_checkpoint(path, trainer)
        second = trainer.train_update()
        # Phase timings are observational; optimizer results must replay exactly.
        for key in ("policy", "value", "entropy", "loss", "approx_kl"):
            assert first[key] == second[key]
        for key, value in model.state_dict().items():
            assert torch.equal(value, expected[key])


@pytest.mark.parametrize("corruption", ["nan", "vocabulary", "schema"])
def test_migration_rejects_invalid_identity_or_nonfinite_weights(corruption):
    source = payload(Policy(config()))
    if corruption == "nan":
        source["model"]["cls"].fill_(float("nan"))
    elif corruption == "vocabulary":
        source["contract"]["vocabulary_sha256"] = "0" * 64
    else:
        source["schema"] = "unknown"
    with pytest.raises(ValueError):
        migrate_v5_role_policy(source)
