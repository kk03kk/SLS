"""Canonical base-game content identifiers."""

from sls.content.registry import ContentRegistry, load_content_registry
from sls.content.scope import (
    IRONCLAD_A0_SCOPE_ID,
    ironclad_a0_scope_hash,
    load_ironclad_a0_scope,
)

__all__ = [
    "ContentRegistry", "IRONCLAD_A0_SCOPE_ID", "ironclad_a0_scope_hash",
    "load_content_registry", "load_ironclad_a0_scope",
]
