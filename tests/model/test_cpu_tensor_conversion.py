from __future__ import annotations

import copy

import torch

from sls.model import ModelConfig, Policy, PolicyBatch, batching
from tests.model.test_policy import _combat_decision


def test_conversion_preserves_all_inputs_and_fixed_optimizer_update(monkeypatch):
    decisions = [_combat_decision(), _combat_decision(reverse_actions=True)]
    optimized = PolicyBatch.from_decisions(decisions)
    monkeypatch.setattr(batching, '_cpu_tensor', lambda values, dtype: torch.tensor(values, dtype=dtype))
    reference = PolicyBatch.from_decisions(decisions)
    for old, new in zip(reference.model_inputs(), optimized.model_inputs(), strict=True):
        assert old.dtype == new.dtype
        assert torch.equal(old, new)
    torch.manual_seed(132)
    config = ModelConfig(embedding_dim=32, transformer_layers=1, attention_heads=4,
                         feedforward_dim=64, recurrent_hidden_dim=32)
    first = Policy(config)
    second = copy.deepcopy(first)
    models = [(first, reference), (second, optimized)]
    outputs = []
    for model, batch in models:
        optimizer = torch.optim.Adam(model.parameters(), lr=0.0001)
        memory = torch.ones_like(model.initial_memory(2))
        output = model(*batch.model_inputs(), memory=memory)
        distribution = torch.distributions.Categorical(logits=output.logits)
        logprob = distribution.log_prob(torch.zeros(2, dtype=torch.long))
        loss = -(logprob * torch.tensor([0.7, -0.2])).mean() + output.value.square().mean()
        loss.backward()
        optimizer.step()
        outputs.append((output.logits, output.value, logprob))
    for old, new in zip(outputs[0], outputs[1], strict=True):
        assert torch.equal(old, new)
    for old, new in zip(first.parameters(), second.parameters(), strict=True):
        assert torch.equal(old, new)
