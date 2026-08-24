"""Integrity checks for the committed Ironclad semantic-audit ledger."""

from __future__ import annotations

import json
from pathlib import Path
from typing import Any

from sls.content.scope import ROOT, ironclad_a0_scope_hash


SEMANTIC_AUDIT_SCHEMA = "sls-ironclad-semantic-audit-v1"
SEMANTIC_AUDIT_PATH = (
    ROOT / "configs" / "validation" / "ironclad_a0_semantic_audit.json"
)


def load_semantic_audit() -> dict[str, Any]:
    from sls.rl.training_contract import canonical_digest

    payload = json.loads(SEMANTIC_AUDIT_PATH.read_text(encoding="utf-8"))
    if payload.get("schema") != SEMANTIC_AUDIT_SCHEMA:
        raise ValueError("unsupported Ironclad semantic audit")
    supplied = payload.get("audit_sha256")
    unsigned = dict(payload)
    unsigned.pop("audit_sha256", None)
    if supplied != canonical_digest(unsigned):
        raise ValueError("Ironclad semantic audit digest mismatch")
    if payload.get("content_scope_sha256") != ironclad_a0_scope_hash():
        raise ValueError("Ironclad semantic audit is stale for the content scope")
    return payload


def semantic_audit_hash() -> str:
    return str(load_semantic_audit()["audit_sha256"])


def verify_semantic_audit(*, require_pilot_ready: bool = False) -> dict[str, Any]:
    payload = load_semantic_audit()
    summary = payload.get("summary") or {}
    if require_pilot_ready and not bool(summary.get("act1_pilot_ready", False)):
        raise ValueError(
            "Ironclad semantic audit is incomplete; Act 1 pilot remains blocked"
        )
    return {
        "valid": True,
        "audit_sha256": payload["audit_sha256"],
        "act1_pilot_ready": bool(summary.get("act1_pilot_ready", False)),
        "status_counts": dict(summary.get("status_counts") or {}),
    }

