"""Canonical FullRun actor/value transformer using relational policy input v3."""

from __future__ import annotations

from dataclasses import asdict, dataclass
from typing import Any, Mapping, Self

import torch
from torch import nn

from sls.model.encoding import (
    ACTION_TYPE_IDS,
    ENCODING_SCHEMA,
    ENTITY_TYPES,
    NUMERIC_FIELDS,
    SCREEN_GROUPS,
    policy_vocabulary,
    vocabulary_hash,
)


@dataclass(frozen=True, slots=True)
class ModelConfig:
    embedding_dim: int = 128
    transformer_layers: int = 4
    attention_heads: int = 4
    feedforward_dim: int = 256
    recurrent_hidden_dim: int = 256
    recurrent_layers: int = 1
    dropout: float = 0.0
    architecture: str = "sls-recurrent-relational-policy-v5"

    def __post_init__(self) -> None:
        values = asdict(self)
        if any(value <= 0 for value in values.values() if isinstance(value, int)):
            raise ValueError("model dimensions must be positive")
        if self.architecture != "sls-recurrent-relational-policy-v5":
            raise ValueError(f"unsupported policy architecture: {self.architecture}")
        if self.recurrent_layers != 1:
            raise ValueError("the v5 policy supports exactly one recurrent layer")
        if self.embedding_dim % self.attention_heads:
            raise ValueError("embedding_dim must be divisible by attention_heads")

    def to_dict(self) -> dict[str, int | float | str]:
        return {**asdict(self), "encoding_schema": ENCODING_SCHEMA, "vocabulary_hash": vocabulary_hash()}

    @classmethod
    def from_dict(cls, payload: Mapping[str, Any]) -> Self:
        """Load constructor fields plus legacy embedded input identity metadata."""

        values = dict(payload)
        metadata_fields = {"encoding_schema", "vocabulary_hash"}
        present = metadata_fields.intersection(values)
        if present and present != metadata_fields:
            missing = ", ".join(sorted(metadata_fields - present))
            raise ValueError(f"model config identity metadata is incomplete: {missing}")
        if present:
            if values.pop("encoding_schema") != ENCODING_SCHEMA:
                raise ValueError("model config encoding schema is incompatible")
            if values.pop("vocabulary_hash") != vocabulary_hash():
                raise ValueError("model config vocabulary is incompatible")
        try:
            return cls(**values)
        except TypeError as error:
            raise ValueError(f"model config fields are invalid: {error}") from error


@dataclass(frozen=True, slots=True)
class PolicyOutput:
    logits: torch.Tensor
    value: torch.Tensor
    state: torch.Tensor
    next_memory: torch.Tensor


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
        self.map_out_relation = nn.Linear(d, d, bias=False)
        self.map_in_relation = nn.Linear(d, d, bias=False)
        layer = nn.TransformerEncoderLayer(
            d_model=d, nhead=config.attention_heads, dim_feedforward=config.feedforward_dim,
            dropout=config.dropout, batch_first=True, norm_first=True,
        )
        self.backbone = nn.TransformerEncoder(layer, config.transformer_layers, enable_nested_tensor=False)
        self.memory = nn.GRUCell(d, config.recurrent_hidden_dim)
        self.previous_action = nn.Embedding(len(ACTION_TYPE_IDS) + 1, d, padding_idx=0)
        self.previous_reward = nn.Linear(1, d, bias=False)
        h = config.recurrent_hidden_dim
        self.action_numeric = nn.Linear(len(NUMERIC_FIELDS) * 2, d)
        self.action_type = nn.Embedding(len(ACTION_TYPE_IDS), d)
        self.reference_role = nn.Embedding(5, d)
        self.state_queries = nn.ModuleList(nn.Linear(h, d, bias=False) for _ in SCREEN_GROUPS)
        self.action_keys = nn.ModuleList(nn.Linear(d, d, bias=False) for _ in SCREEN_GROUPS)
        self.action_biases = nn.ModuleList(nn.Linear(d, 1) for _ in SCREEN_GROUPS)
        self.value_head = nn.Sequential(nn.LayerNorm(h), nn.Linear(h, 1))

    def initial_memory(
        self,
        batch_size: int,
        device: str | torch.device | None = None,
    ) -> torch.Tensor:
        if batch_size <= 0:
            raise ValueError("memory batch size must be positive")
        target = device or self.cls.device
        return torch.zeros(batch_size, self.config.recurrent_hidden_dim, device=target)

    def forward(
        self, screen_types: torch.Tensor, entity_numeric: torch.Tensor,
        entity_numeric_present: torch.Tensor,
        entity_types: torch.Tensor, entity_content: torch.Tensor,
        entity_categories: torch.Tensor, entity_adjacency: torch.Tensor,
        entity_padding: torch.Tensor, action_numeric: torch.Tensor,
        action_numeric_present: torch.Tensor, action_types: torch.Tensor,
        action_references: torch.Tensor, action_reference_mask: torch.Tensor,
        action_padding: torch.Tensor,
        memory: torch.Tensor | None = None,
        episode_start_mask: torch.Tensor | None = None,
        previous_action_types: torch.Tensor | None = None,
        previous_rewards: torch.Tensor | None = None,
    ) -> PolicyOutput:
        batch, entity_count, _ = entity_numeric.shape
        numeric = torch.cat((entity_numeric, entity_numeric_present.to(entity_numeric.dtype)), dim=-1)
        entities = self.entity_numeric(numeric) + self.entity_type(entity_types)
        entities = entities + self.content(entity_content).sum(dim=-2)
        entities = entities + self.category(entity_categories).sum(dim=-2)
        adjacency = entity_adjacency.to(entities.dtype)
        out_degree = adjacency.sum(dim=-1, keepdim=True).clamp_min(1.0)
        incoming = adjacency.transpose(1, 2)
        in_degree = incoming.sum(dim=-1, keepdim=True).clamp_min(1.0)
        entities = entities + self.map_out_relation(
            torch.bmm(adjacency / out_degree, entities)
        ) + self.map_in_relation(torch.bmm(incoming / in_degree, entities))
        cls = self.cls.expand(batch, -1, -1)
        cls_padding = torch.zeros(batch, 1, dtype=torch.bool, device=entity_padding.device)
        hidden = self.backbone(
            torch.cat((cls, entities), dim=1),
            src_key_padding_mask=torch.cat((cls_padding, entity_padding), dim=1),
        )
        encoded_state, entity_hidden = hidden[:, 0], hidden[:, 1:]
        if memory is None:
            memory = self.initial_memory(batch, encoded_state.device)
        if memory.shape != (batch, self.config.recurrent_hidden_dim):
            raise ValueError("recurrent memory has an incompatible shape")
        if episode_start_mask is not None:
            if episode_start_mask.shape != (batch,):
                raise ValueError("episode start mask has an incompatible shape")
            reset_mask = episode_start_mask.to(
                device=memory.device, dtype=torch.bool,
            ).unsqueeze(1)
            memory = memory.masked_fill(reset_mask, 0.0)
        if previous_action_types is None:
            previous_action_types = torch.zeros(batch, dtype=torch.long, device=encoded_state.device)
        if previous_rewards is None:
            previous_rewards = torch.zeros(batch, dtype=encoded_state.dtype, device=encoded_state.device)
        if previous_action_types.shape != (batch,) or previous_rewards.shape != (batch,):
            raise ValueError("previous action and reward inputs must match the policy batch")
        experience = self.previous_action(previous_action_types) + self.previous_reward(
            previous_rewards.to(encoded_state.dtype).unsqueeze(1)
        )
        next_memory = self.memory(encoded_state + experience, memory)
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
        logits = torch.empty(
            candidates.shape[:2], dtype=candidates.dtype, device=candidates.device,
        )
        for group, (query_head, key_head, bias_head) in enumerate(zip(
            self.state_queries, self.action_keys, self.action_biases,
        )):
            selected = screen_types == group
            if not torch.any(selected):
                continue
            query = query_head(next_memory[selected]).unsqueeze(1)
            keys = key_head(candidates[selected])
            logits[selected] = (
                (query * keys).sum(dim=-1) / self.config.embedding_dim**0.5
                + bias_head(candidates[selected]).squeeze(-1)
            )
        logits = logits.masked_fill(
            action_padding, torch.finfo(logits.dtype).min,
        )
        if torch.any(action_padding.all(dim=1)):
            raise ValueError("every decision requires at least one legal semantic candidate")
        return PolicyOutput(
            logits,
            self.value_head(next_memory).squeeze(-1),
            encoded_state,
            next_memory,
        )
