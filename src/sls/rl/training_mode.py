"""Typed safety levels shared by training, checkpoints, and deployment."""

from __future__ import annotations

from enum import Enum
from typing import Mapping


class TrainingMode(str, Enum):
    EXPERIMENTAL = "EXPERIMENTAL"
    PRODUCTION = "PRODUCTION"


def parse_training_mode(value: object, *, field: str = "training_mode") -> TrainingMode:
    if value is None:
        raise ValueError(f"missing required {field}")
    if isinstance(value, TrainingMode):
        return value
    try:
        return TrainingMode(str(value).upper())
    except ValueError as error:
        allowed = ", ".join(item.value for item in TrainingMode)
        raise ValueError(f"invalid {field}: {value!r}; expected one of {allowed}") from error


def require_artifact_mode(
    provenance: Mapping[str, object], *, production: bool,
) -> TrainingMode:
    mode = parse_training_mode(provenance.get("training_mode"))
    verified = provenance.get("policy_transfer_verified")
    if not isinstance(verified, bool):
        raise ValueError("artifact policy_transfer_verified marker is missing")
    if production and (mode is not TrainingMode.PRODUCTION or not verified):
        raise ValueError(
            "experimental or unverified artifact cannot be used for production"
        )
    return mode
