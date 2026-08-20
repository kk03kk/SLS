"""Parity-only state. These values are never policy input."""

from __future__ import annotations

from dataclasses import dataclass, field
from typing import Any, Mapping


@dataclass(frozen=True, slots=True)
class ValidationSnapshot:
    public_state: Mapping[str, Any]
    rng_streams: Mapping[str, Any] = field(default_factory=dict)
    continuation: Mapping[str, Any] = field(default_factory=dict)
