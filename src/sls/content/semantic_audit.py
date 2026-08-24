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
CARD_SEMANTIC_AUDIT_SCHEMA = "sls-ironclad-card-semantics-v1"
CARD_SEMANTIC_AUDIT_PATH = (
    ROOT / "configs" / "validation" / "ironclad_a0_card_semantics.json"
)


def load_card_semantic_audit() -> dict[str, Any]:
    """Validate the committed, dynamically captured per-card evidence."""

    from sls.content.source_audit import java_sources, registry_game_ids
    from sls.rl.training_contract import canonical_digest, sha256_file

    payload = json.loads(CARD_SEMANTIC_AUDIT_PATH.read_text(encoding="utf-8"))
    supplied = payload.get("audit_sha256")
    unsigned = dict(payload)
    unsigned.pop("audit_sha256", None)
    if payload.get("schema") != CARD_SEMANTIC_AUDIT_SCHEMA:
        raise ValueError("unsupported Ironclad card semantic audit")
    if supplied != canonical_digest(unsigned):
        raise ValueError("Ironclad card semantic audit digest mismatch")
    if payload.get("scope_sha256") != ironclad_a0_scope_hash():
        raise ValueError("Ironclad card semantic audit is stale for the content scope")

    scope = json.loads((ROOT / "configs" / "validation" / "ironclad_a0_content_scope.json").read_text(encoding="utf-8"))
    expected_ids = sorted(map(str, scope["cards"]["ids"]))
    entries = list(payload.get("entries") or ())
    if [str(item.get("id")) for item in entries] != expected_ids:
        raise ValueError("Ironclad card semantic audit does not cover the exact scoped card set")
    game_ids = registry_game_ids("cards", expected_ids)
    sources = java_sources("cards")
    for entry in entries:
        identifier = str(entry["id"])
        source = sources[game_ids[identifier]]
        if entry.get("game_id") != game_ids[identifier]:
            raise ValueError(f"card semantic game ID mismatch: {identifier}")
        if entry.get("java_source") != source.path.relative_to(ROOT).as_posix() or \
                entry.get("java_sha256") != sha256_file(source.path):
            raise ValueError(f"card semantic source evidence is stale: {identifier}")
        variants = list(entry.get("variants") or ())
        if [item.get("upgrades") for item in variants] != [0, 1]:
            raise ValueError(f"card semantic variants are incomplete: {identifier}")
        for variant in variants:
            hashes = list(variant.get("boundary_hashes") or ())
            if not hashes or variant.get("boundaries") != len(hashes):
                raise ValueError(f"card semantic boundary evidence is incomplete: {identifier}")
            if variant.get("effect_sha256") != canonical_digest(hashes):
                raise ValueError(f"card semantic effect digest mismatch: {identifier}")
            if not str(variant.get("setup_digest") or ""):
                raise ValueError(f"card semantic setup digest is missing: {identifier}")
    return payload


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
