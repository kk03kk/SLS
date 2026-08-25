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
POTION_SEMANTIC_AUDIT_SCHEMA = "sls-ironclad-potion-semantics-v1"
POTION_SEMANTIC_AUDIT_PATH = (
    ROOT / "configs" / "validation" / "ironclad_a0_potion_semantics.json"
)
RELIC_SEMANTIC_AUDIT_SCHEMA = "sls-ironclad-relic-semantics-v1"
RELIC_SEMANTIC_AUDIT_PATH = (
    ROOT / "configs" / "validation" / "ironclad_a0_relic_semantics.json"
)
MECHANISM_SEMANTIC_AUDIT_SCHEMA = "sls-ironclad-mechanism-semantics-v1"
MECHANISM_SEMANTIC_AUDIT_PATH = (
    ROOT / "configs" / "validation" / "ironclad_a0_mechanism_semantics.json"
)
ENCOUNTER_SEMANTIC_AUDIT_SCHEMA = "sls-ironclad-encounter-semantics-v1"
ENCOUNTER_SEMANTIC_AUDIT_PATH = (
    ROOT / "configs" / "validation" / "ironclad_a0_encounter_semantics.json"
)
EVENT_SEMANTIC_AUDIT_SCHEMA = "sls-ironclad-event-semantics-v1"
EVENT_SEMANTIC_AUDIT_PATH = (
    ROOT / "configs" / "validation" / "ironclad_a0_event_semantics.json"
)
FIRST_TURN_RELIC_EVIDENCE = (
    "AKABEKO", "BAG_OF_MARBLES", "BRIMSTONE", "BRONZE_SCALES",
    "CLOCKWORK_SOUVENIR", "GREMLIN_VISAGE", "LANTERN",
    "MUTAGENIC_STRENGTH", "ODDLY_SMOOTH_STONE", "RED_MASK",
    "THREAD_AND_NEEDLE", "VAJRA",
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


def load_potion_semantic_audit() -> dict[str, Any]:
    """Validate the committed Original/native potion effect evidence."""

    from sls.content.source_audit import java_sources, registry_game_ids
    from sls.rl.training_contract import canonical_digest, sha256_file

    payload = json.loads(POTION_SEMANTIC_AUDIT_PATH.read_text(encoding="utf-8"))
    supplied = payload.get("audit_sha256")
    unsigned = dict(payload)
    unsigned.pop("audit_sha256", None)
    if payload.get("schema") != POTION_SEMANTIC_AUDIT_SCHEMA:
        raise ValueError("unsupported Ironclad potion semantic audit")
    if supplied != canonical_digest(unsigned):
        raise ValueError("Ironclad potion semantic audit digest mismatch")
    if payload.get("scope_sha256") != ironclad_a0_scope_hash():
        raise ValueError("Ironclad potion semantic audit is stale for the content scope")
    scope = json.loads((ROOT / "configs" / "validation" / "ironclad_a0_content_scope.json").read_text(encoding="utf-8"))
    expected_ids = sorted(map(str, scope["potions"]["ids"]))
    entries = list(payload.get("entries") or ())
    if [str(item.get("id")) for item in entries] != expected_ids:
        raise ValueError("Ironclad potion audit does not cover the exact scoped set")
    game_ids = registry_game_ids("potions", expected_ids)
    sources = java_sources("potions")
    for entry in entries:
        identifier = str(entry["id"])
        source = sources[game_ids[identifier]]
        if entry.get("game_id") != game_ids[identifier] or \
                entry.get("java_source") != source.path.relative_to(ROOT).as_posix() or \
                entry.get("java_sha256") != sha256_file(source.path):
            raise ValueError(f"potion semantic source evidence is stale: {identifier}")
        variants = list(entry.get("variants") or ())
        expected_bark = [False] if identifier == "SMOKE_BOMB" else [False, True]
        if [item.get("sacred_bark") for item in variants] != expected_bark:
            raise ValueError(f"potion semantic variants are incomplete: {identifier}")
        for variant in variants:
            hashes = list(variant.get("boundary_hashes") or ())
            if not hashes or variant.get("boundaries") != len(hashes) or \
                    variant.get("effect_sha256") != canonical_digest(hashes):
                raise ValueError(f"potion semantic effect evidence is invalid: {identifier}")
            if not str(variant.get("setup_digest") or ""):
                raise ValueError(f"potion semantic setup digest is missing: {identifier}")
    return payload


def load_relic_semantic_audit() -> dict[str, Any]:
    """Validate committed callback-complete relic scenario evidence."""

    from sls.content.source_audit import java_relic_callbacks, java_sources, registry_game_ids
    from sls.rl.training_contract import canonical_digest, sha256_file

    payload = json.loads(RELIC_SEMANTIC_AUDIT_PATH.read_text(encoding="utf-8"))
    supplied = payload.get("audit_sha256")
    unsigned = dict(payload)
    unsigned.pop("audit_sha256", None)
    if payload.get("schema") != RELIC_SEMANTIC_AUDIT_SCHEMA or supplied != canonical_digest(unsigned):
        raise ValueError("invalid Ironclad relic semantic audit")
    if payload.get("scope_sha256") != ironclad_a0_scope_hash():
        raise ValueError("Ironclad relic semantic audit is stale for the content scope")
    entries = list(payload.get("entries") or ())
    scope = json.loads((ROOT / "configs" / "validation" / "ironclad_a0_content_scope.json").read_text(encoding="utf-8"))
    expected_ids = sorted(map(str, scope["relics"]["ids"]))
    if [str(entry.get("id")) for entry in entries] != expected_ids:
        raise ValueError("Ironclad relic evidence does not cover the exact scoped set")
    game_ids = registry_game_ids("relics", scope["relics"]["ids"])
    sources = java_sources("relics")
    for entry in entries:
        identifier = str(entry["id"])
        source = sources[game_ids[identifier]]
        callbacks = sorted(java_relic_callbacks(source))
        covered = list(entry.get("covered_callbacks") or ())
        remaining = list(entry.get("remaining_callbacks") or ())
        if entry.get("scenario") != "FIRST_TURN" or \
                sorted(covered + remaining) != callbacks or \
                bool(entry.get("callback_complete")) != (not remaining):
            raise ValueError(f"relic callback accounting is invalid: {identifier}")
        if entry.get("game_id") != game_ids[identifier] or entry.get("java_sha256") != sha256_file(source.path):
            raise ValueError(f"relic semantic source evidence is stale: {identifier}")
        if not entry.get("setup_digest") or not entry.get("effect_sha256"):
            raise ValueError(f"relic semantic effect evidence is missing: {identifier}")
    return payload


def load_event_semantic_audit() -> dict[str, Any]:
    """Validate exact scoped stock-event constructor evidence."""

    from sls.content.source_audit import java_sources, registry_game_ids
    from sls.rl.training_contract import canonical_digest, sha256_file

    payload = json.loads(EVENT_SEMANTIC_AUDIT_PATH.read_text(encoding="utf-8"))
    supplied = payload.get("audit_sha256")
    unsigned = dict(payload)
    unsigned.pop("audit_sha256", None)
    if payload.get("schema") != EVENT_SEMANTIC_AUDIT_SCHEMA or \
            supplied != canonical_digest(unsigned):
        raise ValueError("invalid Ironclad event semantic audit")
    if payload.get("scope_sha256") != ironclad_a0_scope_hash():
        raise ValueError("Ironclad event semantic audit is stale for the content scope")
    scope = json.loads(
        (ROOT / "configs" / "validation" / "ironclad_a0_content_scope.json").read_text(
            encoding="utf-8"
        )
    )
    expected_ids = sorted(set(map(str, scope["events"]["ids"])) - {"NEOW"})
    entries = list(payload.get("entries") or ())
    if [str(item.get("id")) for item in entries] != expected_ids:
        raise ValueError("Ironclad event evidence does not cover the exact non-Neow scope")
    game_ids = registry_game_ids("events", scope["events"]["ids"])
    sources = java_sources("events")
    for item in entries:
        identifier = str(item["id"])
        source = sources[game_ids[identifier]]
        hashes = list(item.get("boundary_hashes") or ())
        if item.get("scenario") != "CONSTRUCTOR" or len(hashes) != 1 or \
                item.get("effect_sha256") != canonical_digest(hashes):
            raise ValueError(f"event constructor evidence is invalid: {identifier}")
        if item.get("game_id") != game_ids[identifier] or \
                item.get("java_source") != source.path.relative_to(ROOT).as_posix() or \
                item.get("java_sha256") != sha256_file(source.path):
            raise ValueError(f"event semantic source evidence is stale: {identifier}")
        if not str(item.get("setup_digest") or ""):
            raise ValueError(f"event setup evidence is missing: {identifier}")
    return payload


def load_mechanism_semantic_audit() -> dict[str, Any]:
    """Validate committed controlled Original/native rule trajectories."""

    from sls.rl.training_contract import canonical_digest, sha256_file

    payload = json.loads(MECHANISM_SEMANTIC_AUDIT_PATH.read_text(encoding="utf-8"))
    supplied = payload.get("audit_sha256")
    unsigned = dict(payload)
    unsigned.pop("audit_sha256", None)
    if payload.get("schema") != MECHANISM_SEMANTIC_AUDIT_SCHEMA or \
            supplied != canonical_digest(unsigned):
        raise ValueError("invalid Ironclad mechanism semantic audit")
    if payload.get("scope_sha256") != ironclad_a0_scope_hash():
        raise ValueError("Ironclad mechanism semantic audit is stale for the content scope")
    expected = {
        "damage_buffer_intangible": "DAMAGE_PIPELINE",
        "duration_weak": "POWER_ORDER",
        "retain_ethereal": "POWER_ORDER",
        "engine_orb": "ORB_ENGINE",
        "engine_stance": "STANCE_ENGINE",
        "noncombat_potion_actions": "NONCOMBAT_POTION_ACTIONS",
        "run_and_checkpoint": "RUN_AND_CHECKPOINT",
    }
    entries = list(payload.get("entries") or ())
    if {str(item.get("id")): str(item.get("mechanism")) for item in entries} != expected:
        raise ValueError("Ironclad mechanism scenarios are incomplete")
    for item in entries:
        hashes = list(item.get("boundary_hashes") or ())
        expected_boundaries = (
            2 if str(item.get("id")) in {
                "damage_buffer_intangible", "duration_weak", "retain_ethereal",
            } else 1
        )
        if len(hashes) != expected_boundaries or item.get("effect_sha256") != canonical_digest(hashes):
            raise ValueError(f"mechanism trajectory evidence is invalid: {item.get('id')}")
        if not str(item.get("setup_digest") or ""):
            raise ValueError(f"mechanism setup digest is missing: {item.get('id')}")
    for relative, digest in dict(payload.get("source_files") or {}).items():
        path = ROOT / str(relative)
        if not path.is_file() or sha256_file(path) != digest:
            raise ValueError(f"mechanism source evidence is stale: {relative}")
    return payload


def load_encounter_semantic_audit() -> dict[str, Any]:
    """Validate exact Act 1 encounter and monster constructor/turn traces."""

    from sls.rl.training_contract import canonical_digest, sha256_file

    payload = json.loads(ENCOUNTER_SEMANTIC_AUDIT_PATH.read_text(encoding="utf-8"))
    supplied = payload.get("audit_sha256")
    unsigned = dict(payload)
    unsigned.pop("audit_sha256", None)
    if payload.get("schema") != ENCOUNTER_SEMANTIC_AUDIT_SCHEMA or \
            supplied != canonical_digest(unsigned):
        raise ValueError("invalid Ironclad encounter semantic audit")
    if payload.get("scope_sha256") != ironclad_a0_scope_hash():
        raise ValueError("Ironclad encounter semantic audit is stale for the content scope")
    scope = json.loads(
        (ROOT / "configs" / "validation" / "ironclad_a0_content_scope.json").read_text(
            encoding="utf-8"
        )
    )
    expected_encounters = list(map(str, scope["encounters"]["act1"]))
    expected_monsters = set(map(str, scope["monsters"]["act1"]))
    entries = list(payload.get("entries") or ())
    if [str(item.get("id")) for item in entries] != expected_encounters:
        raise ValueError("Ironclad encounter evidence does not cover the exact scope")
    covered_monsters: set[str] = set()
    for item in entries:
        hashes = list(item.get("boundary_hashes") or ())
        if item.get("scenario") != "CONSTRUCTOR_AND_FIRST_TURN" or \
                len(hashes) != 2 or item.get("effect_sha256") != canonical_digest(hashes):
            raise ValueError(f"encounter trajectory evidence is invalid: {item.get('id')}")
        if not str(item.get("setup_digest") or ""):
            raise ValueError(f"encounter setup evidence is missing: {item.get('id')}")
        covered_monsters.update(map(str, item.get("monster_ids") or ()))
        for variant in item.get("coverage_variants") or ():
            variant_hashes = list(variant.get("boundary_hashes") or ())
            if len(variant_hashes) != 2 or \
                    variant.get("effect_sha256") != canonical_digest(variant_hashes):
                raise ValueError(f"encounter coverage variant is invalid: {item.get('id')}")
    if covered_monsters != expected_monsters:
        raise ValueError("Ironclad encounter evidence does not cover the exact monster scope")
    for relative, digest in dict(payload.get("source_files") or {}).items():
        path = ROOT / str(relative)
        if not path.is_file() or sha256_file(path) != digest:
            raise ValueError(f"encounter source evidence is stale: {relative}")
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
