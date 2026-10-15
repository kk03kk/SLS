"""Action-reference gathering must preserve learning without an action-sized gradient."""

import copy
import os

import pytest
import torch
from torch.profiler import ProfilerActivity, profile

import sls.model.transformer as transformer
from sls.model import ModelConfig, Policy, PolicyBatch
from tests.model.test_policy import _combat_decision


def legacy_gather(entities, references):
    expanded = entities.unsqueeze(1).expand(-1, references.shape[1], -1, -1)
    return torch.gather(expanded, 2, references.unsqueeze(-1).expand(-1, -1, -1, entities.shape[-1]))


def test_reference_backward_allocation_depends_on_entities_not_action_cross_product():
    entities = torch.randn(4, 128, 16, requires_grad=True)
    references = torch.randint(128, (4, 40, 5))
    gradients, allocations = [], []
    for gather in (legacy_gather, transformer._gather_action_entities):
        entities.grad = None
        with profile(activities=[ProfilerActivity.CPU], profile_memory=True, acc_events=True) as trace:
            gather(entities, references).sum().backward()
        gradients.append(entities.grad.clone())
        allocations.append(max(e.cpu_memory_usage for e in trace.events() if e.name == "aten::gather_backward"))
    torch.testing.assert_close(*gradients, atol=0, rtol=0)
    assert allocations[0] == entities.numel() * entities.element_size() * 40
    assert allocations[1] <= entities.numel() * entities.element_size()


@pytest.mark.parametrize("architecture", ["v5", "v6"])
@pytest.mark.parametrize("device", ["cpu", "cuda"])
def test_policy_outputs_gradients_and_adam_match_legacy(architecture, device, monkeypatch):
    if device == "cuda":
        if os.environ.get("SLS_TEST_CUDA") != "1":
            pytest.skip("CUDA validation requires explicit SLS_TEST_CUDA=1")
        if not torch.cuda.is_available():
            pytest.skip("CUDA unavailable")
    torch.manual_seed(809)
    policy = Policy(ModelConfig(
        embedding_dim=32, transformer_layers=1, attention_heads=4,
        feedforward_dim=64, recurrent_hidden_dim=32,
        architecture=f"sls-recurrent-relational-policy-{architecture}",
    )).to(device).train()
    reference = copy.deepcopy(policy)
    batch = PolicyBatch.from_decisions((
        _combat_decision(), _combat_decision(reverse_actions=True),
        _combat_decision(reverse_entities=True),
    )).to(device)
    memory = torch.randn(3, 32, device=device)
    arguments = {
        "memory": memory, "episode_start_mask": torch.tensor([False, True, False], device=device),
        "previous_action_types": torch.tensor([1, 0, 2], device=device),
        "previous_rewards": torch.tensor([0.1, 0, -0.1], device=device),
    }
    actual = policy(*batch.model_inputs(), **arguments)
    with monkeypatch.context() as patch:
        patch.setattr(transformer, "_gather_action_entities", legacy_gather)
        expected = reference(*batch.model_inputs(), **arguments)
    for field in ("logits", "value", "state", "next_memory"):
        torch.testing.assert_close(getattr(actual, field), getattr(expected, field), atol=0, rtol=0)
    optimizers = [torch.optim.Adam(model.parameters(), lr=3.125e-5) for model in (policy, reference)]
    for output in (actual, expected):
        loss = output.logits.square().mean() + output.value.square().mean() + output.next_memory.square().mean()
        loss.backward()
    for (name, parameter), (other_name, other) in zip(policy.named_parameters(), reference.named_parameters(), strict=True):
        assert name == other_name
        if parameter.grad is None:
            assert other.grad is None
        else:
            torch.testing.assert_close(parameter.grad, other.grad, atol=2e-6, rtol=2e-5, msg=name)
    for optimizer in optimizers:
        optimizer.step()
    for name, value in policy.state_dict().items():
        torch.testing.assert_close(value, reference.state_dict()[name], atol=2e-6, rtol=2e-5, msg=name)
    for index, state in optimizers[0].state_dict()["state"].items():
        for name, value in state.items():
            torch.testing.assert_close(value, optimizers[1].state_dict()["state"][index][name], atol=2e-7, rtol=3e-5)
