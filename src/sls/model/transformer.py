"""Canonical FullRun actor/value transformer using structural policy input v2."""

from __future__ import annotations

from dataclasses import asdict, dataclass

import torch
from torch import nn

from sls.model.encoding import (
    ACTION_TYPE_IDS, CATEGORICAL_FIELDS, ENCODING_SCHEMA, ENTITY_TYPES,
    NUMERIC_FIELDS, policy_vocabulary, vocabulary_hash,
)


@dataclass(frozen=True, slots=True)
class ModelConfig:
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

    def to_dict(self) -> dict[str, int | float | str]:
        return {**asdict(self), "encoding_schema": ENCODING_SCHEMA, "vocabulary_hash": vocabulary_hash()}


@dataclass(frozen=True, slots=True)
class PolicyOutput:
    logits: torch.Tensor
    value: torch.Tensor
    state: torch.Tensor


class Policy(nn.Module):
    def __init__(self, config: ModelConfig = ModelConfig()) -> None:
        super().__init__()
        self.config = config
        d = config.embedding_dim
        vocabulary = policy_vocabulary()
        self.cls = nn.Parameter(torch.zeros(1, 1, d))
        self.entity_numeric = nn.Linear(len(NUMERIC_FIELDS) * 2, d)
        self.entity_type = nn.Embedding(len(ENTITY_TYPES), d)
        self.content = nn.Embedding(len(vocabulary["content"]), d, padding_idx=0)
        self.category = nn.Embedding(len(vocabulary["categorical"]), d, padding_idx=0)
        self.map_relation = nn.Linear(d, d, bias=False)
        layer = nn.TransformerEncoderLayer(
            d_model=d, nhead=config.attention_heads, dim_feedforward=config.feedforward_dim,
            dropout=config.dropout, batch_first=True, norm_first=True,
        )
        self.backbone = nn.TransformerEncoder(layer, config.transformer_layers, enable_nested_tensor=False)
        self.action_numeric = nn.Linear(len(NUMERIC_FIELDS) * 2, d)
        self.action_type = nn.Embedding(len(ACTION_TYPE_IDS), d)
        self.reference_role = nn.Embedding(5, d)
        self.state_query = nn.Linear(d, d, bias=False)
        self.action_key = nn.Linear(d, d, bias=False)
        self.action_bias = nn.Linear(d, 1)
        self.value_head = nn.Sequential(nn.LayerNorm(d), nn.Linear(d, 1))

    def forward(
        self, entity_numeric: torch.Tensor, entity_numeric_present: torch.Tensor,
        entity_types: torch.Tensor, entity_content: torch.Tensor,
        entity_categories: torch.Tensor, entity_adjacency: torch.Tensor,
        entity_padding: torch.Tensor, action_numeric: torch.Tensor,
        action_numeric_present: torch.Tensor, action_types: torch.Tensor,
        action_references: torch.Tensor, action_reference_mask: torch.Tensor,
        action_padding: torch.Tensor,
    ) -> PolicyOutput:
        batch, entity_count, _ = entity_numeric.shape
        numeric = torch.cat((entity_numeric, entity_numeric_present.to(entity_numeric.dtype)), dim=-1)
        entities = self.entity_numeric(numeric) + self.entity_type(entity_types)
        entities = entities + self.content(entity_content).sum(dim=-2)
        entities = entities + self.category(entity_categories).sum(dim=-2)
        adjacency = entity_adjacency.to(entities.dtype)
        degree = adjacency.sum(dim=-1, keepdim=True).clamp_min(1.0)
        entities = entities + self.map_relation(torch.bmm(adjacency / degree, entities))
        cls = self.cls.expand(batch, -1, -1)
        cls_padding = torch.zeros(batch, 1, dtype=torch.bool, device=entity_padding.device)
        hidden = self.backbone(
            torch.cat((cls, entities), dim=1),
            src_key_padding_mask=torch.cat((cls_padding, entity_padding), dim=1),
        )
        state, entity_hidden = hidden[:, 0], hidden[:, 1:]
        action_values = torch.cat((action_numeric, action_numeric_present.to(action_numeric.dtype)), dim=-1)
        candidates = self.action_numeric(action_values) + self.action_type(action_types)
        safe_refs = action_references.clamp(0, max(0, entity_count - 1))
        expanded = entity_hidden.unsqueeze(1).expand(-1, safe_refs.shape[1], -1, -1)
        gathered = torch.gather(
            expanded, 2,
            safe_refs.unsqueeze(-1).expand(-1, -1, -1, self.config.embedding_dim),
        )
        roles = self.reference_role(torch.arange(5, device=entities.device)).view(1, 1, 5, -1)
        candidates = candidates + (
            (gathered + roles) * action_reference_mask.unsqueeze(-1)
        ).sum(dim=2)
        query = self.state_query(state).unsqueeze(1)
        logits = (query * self.action_key(candidates)).sum(dim=-1) / self.config.embedding_dim**0.5
        logits = (logits + self.action_bias(candidates).squeeze(-1)).masked_fill(
            action_padding, torch.finfo(logits.dtype).min,
        )
        if torch.any(action_padding.all(dim=1)):
            raise ValueError("every decision requires at least one legal semantic candidate")
        return PolicyOutput(logits, self.value_head(state).squeeze(-1), state)
