"""Canonical variable-candidate FullRun policy.

Vocabulary contracts are usable without torch; tensor/model exports load only
when requested.
"""

from __future__ import annotations

from importlib import import_module
from typing import Any

from sls.model.encoding import ENCODING_SCHEMA, policy_vocabulary, vocabulary_hash

__all__ = [
    "ENCODING_SCHEMA", "ModelConfig", "Policy", "PolicyBatch", "PolicyOutput",
    "encode_decision", "policy_vocabulary", "vocabulary_hash",
]


_LAZY_EXPORTS = {
    "ModelConfig": ("sls.model.transformer", "ModelConfig"),
    "Policy": ("sls.model.transformer", "Policy"),
    "PolicyBatch": ("sls.model.batching", "PolicyBatch"),
    "PolicyOutput": ("sls.model.transformer", "PolicyOutput"),
    "encode_decision": ("sls.model.batching", "encode_decision"),
}


def __getattr__(name: str) -> Any:
    try:
        module_name, attribute = _LAZY_EXPORTS[name]
    except KeyError as error:
        raise AttributeError(f"module {__name__!r} has no attribute {name!r}") from error
    value = getattr(import_module(module_name), attribute)
    globals()[name] = value
    return value
