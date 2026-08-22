"""Curriculum-scoped parity readiness, separate from final FullRun acceptance."""

from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path
from typing import Any, Mapping

from sls.validation.truth import load_bundle


ALLOWED_EVIDENCE = {"LIVE_FULLRUN", "RESUMED_AUTOSAVE"}


@dataclass(frozen=True, slots=True)
class BundleRecord:
    path: Path
    manifest: dict[str, Any]
    boundaries: list[dict[str, Any]]


def load_records(root: Path) -> tuple[dict[str, BundleRecord], list[dict[str, str]]]:
    records: dict[str, BundleRecord] = {}
    invalid = []
    for manifest_path in sorted(root.rglob("manifest.json")):
        path = manifest_path.parent
        try:
            manifest, boundaries = load_bundle(path)
            records[path.name] = BundleRecord(path, manifest, boundaries)
        except Exception as error:
            invalid.append({"bundle": path.name, "reason": str(error)})
    return records, invalid


def _status(boundary: Mapping[str, Any]) -> str:
    comparison = boundary.get("comparison") or {}
    return str(comparison.get("status") or ("DIFFERENCE" if boundary.get("differences") else "MATCH"))


def _source(manifest: Mapping[str, Any]) -> tuple[str | None, str | None]:
    provenance = manifest.get("provenance") or {}
    return provenance.get("source_run_id"), provenance.get("source_anchor")


def _anchor(manifest: Mapping[str, Any], anchor_id: str | None) -> Mapping[str, Any] | None:
    return next(
        (
            item for item in manifest.get("anchors") or ()
            if isinstance(item, Mapping) and item.get("anchor_id") == anchor_id
        ),
        None,
    )


def evaluate_route(records: Mapping[str, BundleRecord], leaf_id: str) -> dict[str, Any]:
    chain: list[BundleRecord] = []
    seen: set[str] = set()
    current = leaf_id
    failures: list[str] = []
    while current:
        if current in seen:
            failures.append("PROVENANCE_CYCLE")
            break
        seen.add(current)
        record = records.get(current)
        if record is None:
            failures.append(f"MISSING_SOURCE_BUNDLE:{current}")
            break
        chain.append(record)
        source_id, source_anchor = _source(record.manifest)
        if source_id is None:
            break
        parent = records.get(str(source_id))
        if parent is None:
            current = str(source_id)
            continue
        anchor = _anchor(parent.manifest, source_anchor)
        if anchor is None:
            failures.append(f"MISSING_SOURCE_ANCHOR:{source_id}:{source_anchor}")
        else:
            exact = (
                record.manifest.get("start_state", {}).get("boundary_hash")
                == anchor.get("boundary_hash")
            )
            first_anchor = min(
                (
                    item for item in record.manifest.get("anchors") or ()
                    if isinstance(item, Mapping)
                ),
                key=lambda item: int(item.get("sequence", 0)),
                default=None,
            )
            resume_verified = bool(
                first_anchor
                and first_anchor.get("capability") == "RESUME_VERIFIED"
                and first_anchor.get("resume_boundary_hash")
                == anchor.get("resume_boundary_hash")
            )
            if not exact and not resume_verified:
                failures.append(f"SOURCE_BOUNDARY_HASH_MISMATCH:{record.path.name}")
        current = str(source_id)
    chain.reverse()
    if not chain or chain[0].manifest.get("evidence_class") != "LIVE_FULLRUN":
        failures.append("NO_LIVE_FULLRUN_ROOT")
    seed = chain[0].manifest.get("seed") if chain else None
    screens: set[str] = set()
    actions: set[str] = set()
    rooms: set[str] = set()
    bosses: set[str] = set()
    reached_act_two = False
    used_boundaries = 0
    for chain_index, record in enumerate(chain):
        if reached_act_two:
            break
        manifest = record.manifest
        if manifest.get("evidence_class") not in ALLOWED_EVIDENCE:
            failures.append(f"INELIGIBLE_EVIDENCE:{record.path.name}")
        if manifest.get("capture_mode", "PAIRED") != "PAIRED":
            failures.append(f"NOT_PAIRED:{record.path.name}")
        if manifest.get("seed") != seed:
            failures.append(f"SEED_MISMATCH:{record.path.name}")
        # A resumed child supersedes its parent's tail starting at the source
        # anchor.  The child re-records that boundary after stock autosave
        # normalization, so only the strict prefix is part of this route.
        exclusive_end = None
        if chain_index + 1 < len(chain):
            next_manifest = chain[chain_index + 1].manifest
            _, next_anchor_id = _source(next_manifest)
            source_anchor = _anchor(manifest, next_anchor_id)
            if source_anchor is None:
                failures.append(
                    f"MISSING_SOURCE_ANCHOR:{record.path.name}:{next_anchor_id}"
                )
            else:
                exclusive_end = int(source_anchor.get("sequence", 0))
        for boundary in record.boundaries:
            sequence = int(boundary.get("sequence", 0))
            if exclusive_end is not None and sequence >= exclusive_end:
                break
            cursor = boundary.get("cursor") or {}
            act = int(cursor.get("act", 0) or 0)
            if reached_act_two:
                break
            if _status(boundary) != "MATCH":
                failures.append(f"NON_MATCH_BOUNDARY:{record.path.name}:{sequence}")
                break
            used_boundaries += 1
            screens.add(str(cursor.get("screen")))
            rooms.add(str(cursor.get("room")))
            action = boundary.get("selected_action")
            if action:
                actions.add(str(action.get("kind")))
            run = (boundary.get("canonical_public_state") or {}).get("run") or {}
            if run.get("boss"):
                bosses.add(str(run["boss"]))
            if act >= 2:
                reached_act_two = True
                break
    if not reached_act_two:
        failures.append("ACT_TWO_NOT_REACHED")
    return {
        "leaf": leaf_id, "seed": seed, "valid": not failures,
        "chain": [record.path.name for record in chain], "failures": failures,
        "reached_act_two": reached_act_two, "used_boundaries": used_boundaries,
        "coverage": {
            "screens": sorted(screens), "selected_actions": sorted(actions),
            "rooms": sorted(rooms), "bosses": sorted(bosses),
        },
    }


def readiness_report(root: Path, requirements: Mapping[str, Any]) -> dict[str, Any]:
    records, invalid = load_records(root)
    routes = []
    for bundle_id, record in records.items():
        if any(int((boundary.get("cursor") or {}).get("act", 0) or 0) >= 2 for boundary in record.boundaries):
            routes.append(evaluate_route(records, bundle_id))
    valid = [route for route in routes if route["valid"]]
    bosses = {value for route in valid for value in route["coverage"]["bosses"]}
    screens = {value for route in valid for value in route["coverage"]["screens"]}
    actions = {value for route in valid for value in route["coverage"]["selected_actions"]}
    rooms = {value for route in valid for value in route["coverage"]["rooms"]}
    required_bosses = set(map(str, requirements.get("bosses", ())))
    required_screens = set(map(str, requirements.get("screens", ())))
    required_actions = set(map(str, requirements.get("selected_actions", ())))
    room_suffixes = set(map(str, requirements.get("room_suffixes", ())))
    failures = []
    if len({route["seed"] for route in valid}) < int(requirements.get("routes", 3)):
        failures.append("INSUFFICIENT_COMPLETED_ROUTES")
    if not required_bosses.issubset(bosses): failures.append("MISSING_BOSSES")
    if not required_screens.issubset(screens): failures.append("MISSING_SCREENS")
    if not required_actions.issubset(actions): failures.append("MISSING_SELECTED_ACTIONS")
    if any(not any(room.endswith(suffix) for room in rooms) for suffix in room_suffixes):
        failures.append("MISSING_ROOM_TYPES")
    return {
        "schema": "sls-act1-training-readiness-v1", "ready": not failures,
        "failures": failures, "valid_routes": valid, "candidate_routes": routes,
        "invalid_bundles": invalid,
        "coverage": {
            "bosses": sorted(bosses), "screens": sorted(screens),
            "selected_actions": sorted(actions), "rooms": sorted(rooms),
        },
    }
