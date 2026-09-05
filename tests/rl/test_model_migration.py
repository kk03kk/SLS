from copy import deepcopy
from dataclasses import replace

import pytest
import torch

from sls.backends.simulator import SimulatorBackend
from sls.model import ModelConfig, Policy, PolicyBatch
from sls.model.encoding import policy_vocabulary
from sls.rl.model_migration import migrate_v3_policy, read_legacy_vocabulary
from sls.rl.training_contract import TRAINING_CHECKPOINT_SCHEMA


def legacy_payload():
    old = read_legacy_vocabulary()
    config = ModelConfig(embedding_dim=32, transformer_layers=1, attention_heads=4,
                         feedforward_dim=64, recurrent_hidden_dim=64)
    state = Policy(config).state_dict()
    for name in ("entity_numeric.weight", "action_numeric.weight"):
        state[name] = torch.randn(32, len(old["numeric_fields"]) * 2)
    for name, key in (("content.weight", "content"), ("category.weight", "categorical")):
        state[name] = torch.randn(len(old[key]), 32)
    model_config = config.to_dict()
    model_config.update(encoding_schema=old["schema"], vocabulary_hash=old["sha256"])
    return {"schema": TRAINING_CHECKPOINT_SCHEMA, "model": state, "contract": {
        "model": model_config, "encoding_schema": old["schema"],
        "vocabulary_sha256": old["sha256"],
    }}


def test_transfer_preserves_old_projection_including_presence_mask_offset():
    payload = legacy_payload()
    before = deepcopy(payload)
    model, report = migrate_v3_policy(payload)
    old, new = read_legacy_vocabulary(), policy_vocabulary()
    old_count, new_count = len(old["numeric_fields"]), len(new["numeric_fields"])
    x = torch.randn(9, old_count * 2)
    expanded = torch.randn(9, new_count * 2)  # New fields may be present at initialization.
    for i, name in enumerate(old["numeric_fields"]):
        j = new["numeric_fields"].index(name)
        expanded[:, j] = x[:, i]
        expanded[:, j + new_count] = x[:, i + old_count]
    for name in ("entity_numeric", "action_numeric"):
        expected = torch.nn.functional.linear(x, payload["model"][name + ".weight"],
                                              payload["model"][name + ".bias"])
        assert torch.allclose(getattr(model, name)(expanded), expected, atol=1e-5)
    for name, tensor in before["model"].items():
        assert torch.equal(payload["model"][name], tensor)
        if name not in {"entity_numeric.weight", "action_numeric.weight"}:
            assert torch.equal(model.state_dict()[name], tensor)
    assert report["exact_resume"] is False


def test_full_actor_critic_and_recurrent_output_preserved_on_old_input_subspace():
    payload = legacy_payload()
    migrated, _ = migrate_v3_policy(payload)
    reference = deepcopy(migrated)
    old = read_legacy_vocabulary()["numeric_fields"]
    indices = [policy_vocabulary()["numeric_fields"].index(name) for name in old]
    for name in ("entity_numeric", "action_numeric"):
        layer = torch.nn.Linear(len(old) * 2, reference.config.embedding_dim)
        layer.load_state_dict({"weight": payload["model"][name + ".weight"],
                               "bias": payload["model"][name + ".bias"]})
        setattr(reference, name, layer)
    batch = PolicyBatch.from_decisions((SimulatorBackend().reset(7),), migrated.config)
    legacy_batch = replace(batch, entity_numeric=batch.entity_numeric[..., indices],
                           entity_numeric_present=batch.entity_numeric_present[..., indices],
                           action_numeric=batch.action_numeric[..., indices],
                           action_numeric_present=batch.action_numeric_present[..., indices])
    migrated.eval()
    reference.eval()
    memory = torch.randn(1, migrated.config.recurrent_hidden_dim)
    with torch.no_grad():
        before = reference(*legacy_batch.model_inputs(), memory=memory)
        after = migrated(*batch.model_inputs(), memory=memory)
    for field in ("logits", "value", "state", "next_memory"):
        assert torch.allclose(getattr(before, field), getattr(after, field), atol=1e-5)


@pytest.mark.parametrize("corruption", ["hash", "shape", "nan"])
def test_transfer_rejects_unrecognized_or_corrupt_source(corruption):
    payload = legacy_payload()
    if corruption == "hash":
        payload["contract"]["vocabulary_sha256"] = "0" * 64
    elif corruption == "shape":
        payload["model"]["entity_numeric.weight"] = torch.zeros(32, 69)
    else:
        payload["model"]["cls"].fill_(float("nan"))
    with pytest.raises(ValueError):
        migrate_v3_policy(payload)
