"""Explicit v3 -> v4 parameter transfer; never an exact training resume."""

from __future__ import annotations

import hashlib
import json
from dataclasses import fields
from pathlib import Path
from typing import Any, Mapping

import torch

from sls.model import ModelConfig, Policy
from sls.model.encoding import policy_vocabulary
from sls.rl.training_contract import TRAINING_CHECKPOINT_SCHEMA

MIGRATION_SCHEMA = "sls-model-input-migration-v1"
LEGACY_VOCABULARY = Path(__file__).resolve().parents[1] / "model/policy_vocabulary_v3.json"


def read_legacy_vocabulary() -> dict[str, Any]:
    value = json.loads(LEGACY_VOCABULARY.read_text(encoding="utf-8"))
    unsigned = dict(value)
    claimed = unsigned.pop("sha256")
    actual = hashlib.sha256(json.dumps(unsigned, sort_keys=True, separators=(",", ":")).encode()).hexdigest()
    if actual != claimed or value["schema"] != "sls-policy-input-v3":
        raise ValueError("legacy vocabulary provenance is invalid")
    return value


def migrate_v3_policy(payload: Mapping[str, Any]) -> tuple[Policy, dict[str, Any]]:
    """Copy every old parameter by semantic field/token, zero new input columns.

    Numeric values and presence masks are separate blocks: simply appending
    columns would shift every old mask and destroy the learned input mapping.
    New public relations/entities intentionally change live outputs. Equality
    is promised only for the old input subspace, not for the corrected MDP.
    Adam moments and in-flight episode state are deliberately not transferred.
    """
    old, new = read_legacy_vocabulary(), policy_vocabulary()
    if payload.get("schema") != TRAINING_CHECKPOINT_SCHEMA:
        raise ValueError("unsupported source checkpoint schema")
    contract = payload.get("contract", {})
    config = dict(contract.get("model", {}))
    if (contract.get("encoding_schema") != old["schema"]
            or contract.get("vocabulary_sha256") != old["sha256"]
            or config.pop("encoding_schema", None) != old["schema"]
            or config.pop("vocabulary_hash", None) != old["sha256"]):
        raise ValueError("source checkpoint is not the audited v3 input contract")
    if set(config) != {f.name for f in fields(ModelConfig)}:
        raise ValueError("unsupported model configuration")
    for key in ("categorical_fields", "reference_roles", "screen_groups", "action_types", "entity_types"):
        if old[key] != new[key]:
            raise ValueError(f"migration cannot change {key}")
    model = Policy(ModelConfig(**config))
    target = model.state_dict()
    source = payload.get("model", {})
    if set(source) != set(target):
        raise ValueError("source model parameter names differ")
    numeric_names = ("entity_numeric.weight", "action_numeric.weight")
    token_names = {"content.weight": "content", "category.weight": "categorical"}
    preserved = 0
    for name, destination in target.items():
        value = source[name]
        if not isinstance(value, torch.Tensor) or not torch.isfinite(value).all():
            raise ValueError(f"invalid source tensor: {name}")
        if value.dtype != destination.dtype:
            raise ValueError(f"source tensor dtype differs: {name}")
        if name in numeric_names:
            old_fields, new_fields = old["numeric_fields"], new["numeric_fields"]
            if value.shape != (destination.shape[0], len(old_fields) * 2):
                raise ValueError(f"invalid source numeric shape: {name}")
            destination.zero_()
            for index, field in enumerate(old_fields):
                if field not in new_fields:
                    raise ValueError(f"removed numeric field: {field}")
                mapped = new_fields.index(field)
                destination[:, mapped] = value[:, index]
                destination[:, mapped + len(new_fields)] = value[:, index + len(old_fields)]
        elif name in token_names:
            category = token_names[name]
            old_tokens, new_tokens = old[category], new[category]
            if value.shape != (len(old_tokens), destination.shape[1]):
                raise ValueError(f"invalid source vocabulary shape: {name}")
            destination.zero_()
            for index, token in enumerate(old_tokens):
                if token not in new_tokens:
                    raise ValueError(f"removed vocabulary token: {token}")
                destination[new_tokens.index(token)] = value[index]
        else:
            if value.shape != destination.shape:
                raise ValueError(f"incompatible parameter shape: {name}")
            destination.copy_(value)
        preserved += value.numel()
    model.load_state_dict(target, strict=True)
    return model, {
        "schema": MIGRATION_SCHEMA, "exact_resume": False,
        "source_encoding": old["schema"], "source_vocabulary_sha256": old["sha256"],
        "target_encoding": new["schema"], "target_vocabulary_sha256": new["sha256"],
        "preserved_parameter_elements": preserved,
        "target_parameter_elements": sum(t.numel() for t in target.values()),
        "new_numeric_fields": [n for n in new["numeric_fields"] if n not in old["numeric_fields"]],
        "new_input_initialization": "zero weights for values and presence masks",
        "reset": ["optimizer", "worker_environments", "recurrent_memory", "episode_limits",
                  "previous_actions", "previous_rewards", "rng_streams"],
    }
