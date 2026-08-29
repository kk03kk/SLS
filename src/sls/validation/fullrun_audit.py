"""Integrity checks for the conservative Ironclad FullRun evidence ledger."""

from __future__ import annotations

import json
from pathlib import Path
from typing import Any

from sls.content.scope import ROOT
from sls.rl.training_contract import canonical_digest


FULLRUN_INVENTORY_PATH = ROOT / "configs" / "validation" / "ironclad_fullrun_inventory.json"
FULLRUN_AUDIT_PATH = ROOT / "configs" / "validation" / "ironclad_fullrun_semantic_audit.json"
FULLRUN_INVENTORY_SCHEMA = "sls-ironclad-fullrun-reachable-v1"
FULLRUN_AUDIT_SCHEMA = "sls-ironclad-fullrun-semantic-audit-v1"


def _verified_payload(path: Path, schema: str, digest_key: str) -> dict[str, Any]:
    payload = json.loads(path.read_text(encoding="utf-8"))
    supplied = payload.get(digest_key)
    unsigned = dict(payload)
    unsigned.pop(digest_key, None)
    if payload.get("schema") != schema or supplied != canonical_digest(unsigned):
        raise ValueError(f"invalid or stale FullRun artifact: {path.name}")
    return payload


def load_fullrun_inventory() -> dict[str, Any]:
    return _verified_payload(FULLRUN_INVENTORY_PATH, FULLRUN_INVENTORY_SCHEMA, "inventory_sha256")


def load_fullrun_audit() -> dict[str, Any]:
    payload = _verified_payload(FULLRUN_AUDIT_PATH, FULLRUN_AUDIT_SCHEMA, "audit_sha256")
    inventory = load_fullrun_inventory()
    if payload.get("inventory_sha256") != inventory["inventory_sha256"]:
        raise ValueError("FullRun semantic audit is stale for the inventory")
    if payload.get("summary", {}).get("fullrun_training_ready") is not False:
        raise ValueError("FullRun readiness may not be inferred from an incomplete audit")
    return payload
