"""Canonical FullRun actor/value transformer."""

from __future__ import annotations

from dataclasses import asdict, dataclass

import torch
from torch import nn


@dataclass(frozen=True, slots=True)
class ModelConfig:
    entity_feature_dim: int = 32
    action_feature_dim: int = 24
    entity_type_count: int = 16
    action_type_count: int = 32
    embedding_dim: int = 128
    transformer_layers: int = 4
    attention_heads: int = 4
    feedforward_dim: int = 256
    dropout: float = 0.0

    def __post_init__(self) -> None:
        values = asdict(self)
        if any(value <= 0 for value in values.values() if isinstance(value, int)):
            raise ValueError("model dimensions must be positive")
        if self.embedding_dim % self.attention_heads:
            raise ValueError("embedding_dim must be divisible by attention_heads")

    def to_dict(self) -> dict[str, int | float]:
        return asdict(self)


@dataclass(frozen=True, slots=True)
class PolicyOutput:
    logits: torch.Tensor
    value: torch.Tensor
    state: torch.Tensor


class Policy(nn.Module):
    """One actor/value model for combat and every run-level screen.

    Entity and action features are produced from canonical public contracts. Candidate
    index is never semantic identity: the same candidate features receive the
    same score regardless of list position.
    """

    def __init__(self, config: ModelConfig = ModelConfig()) -> None:
        super().__init__()
        self.config = config
        d = config.embedding_dim
        self.cls = nn.Parameter(torch.zeros(1, 1, d))
        self.entity_projection = nn.Linear(config.entity_feature_dim, d)
        self.entity_type = nn.Embedding(config.entity_type_count, d)
        layer = nn.TransformerEncoderLayer(
            d_model=d, nhead=config.attention_heads,
            dim_feedforward=config.feedforward_dim, dropout=config.dropout,
            batch_first=True, norm_first=True,
        )
        self.backbone = nn.TransformerEncoder(
            layer, config.transformer_layers, enable_nested_tensor=False,
        )
        self.action_projection = nn.Linear(config.action_feature_dim, d)
        self.action_type = nn.Embedding(config.action_type_count, d)
        self.state_query = nn.Linear(d, d, bias=False)
        self.action_key = nn.Linear(d, d, bias=False)
        self.action_bias = nn.Linear(d, 1)
        self.value_head = nn.Sequential(nn.LayerNorm(d), nn.Linear(d, 1))

    def forward(
        self,
        entity_features: torch.Tensor,
        entity_types: torch.Tensor,
        entity_padding: torch.Tensor,
        action_features: torch.Tensor,
        action_types: torch.Tensor,
        action_padding: torch.Tensor,
    ) -> PolicyOutput:
        if entity_features.ndim != 3 or action_features.ndim != 3:
            raise ValueError("entity/action features must be [batch, items, features]")
        batch = entity_features.shape[0]
        if action_features.shape[0] != batch:
            raise ValueError("entity and action batches differ")
        entities = self.entity_projection(entity_features) + self.entity_type(entity_types)
        cls = self.cls.expand(batch, -1, -1)
        tokens = torch.cat((cls, entities), dim=1)
        cls_padding = torch.zeros(batch, 1, dtype=torch.bool, device=entity_padding.device)
        hidden = self.backbone(
            tokens, src_key_padding_mask=torch.cat((cls_padding, entity_padding), dim=1),
        )
        state = hidden[:, 0]
        candidates = self.action_projection(action_features) + self.action_type(action_types)
        query = self.state_query(state).unsqueeze(1)
        logits = (query * self.action_key(candidates)).sum(dim=-1) / self.config.embedding_dim**0.5
        logits = logits + self.action_bias(candidates).squeeze(-1)
        logits = logits.masked_fill(action_padding, torch.finfo(logits.dtype).min)
        if torch.any(action_padding.all(dim=1)):
            raise ValueError("every decision requires at least one legal semantic candidate")
        return PolicyOutput(logits, self.value_head(state).squeeze(-1), state)
