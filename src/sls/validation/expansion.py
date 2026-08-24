"""Deterministic assembly of coverage-expansion evidence reports."""

from __future__ import annotations

from pathlib import Path
from typing import Any, Mapping

from sls.validation.readiness import evaluate_route, load_records
from sls.validation.truth import value_hash


EXPANSION_REPORT_SCHEMA = "sls-act1-validation-expansion-v1"
SELECTION_REPORT_SCHEMA = "sls-act1-validation-selection-v1"


def _selection_pairs(selection: Mapping[str, Any]) -> list[tuple[int, int]]:
    if selection.get("schema") != SELECTION_REPORT_SCHEMA:
        raise ValueError("unsupported Act 1 validation selection report")
    supplied = selection.get("selection_sha256")
    unsigned = dict(selection)
    unsigned.pop("selection_sha256", None)
    if supplied != value_hash(unsigned):
        raise ValueError("Act 1 validation selection digest mismatch")
    pairs = [
        (int(item["seed"]), int(item["variant"]))
        for item in selection.get("selections") or ()
    ]
    if not pairs or len(set(pairs)) != len(pairs):
        raise ValueError("Act 1 validation selection must contain unique seed/variant pairs")
    return pairs


def _route_endpoint(records: Mapping[str, Any], route: Mapping[str, Any]) -> tuple[int, bool]:
    segment = route["segments"][-1]
    boundary = records[str(segment["bundle"])].boundaries[int(segment["to_step"])]
    cursor = boundary.get("cursor") or {}
    return int(cursor.get("floor", 0) or 0), bool(boundary.get("terminal_kind"))


def assemble_expansion_round(
    truth_root: Path,
    selection: Mapping[str, Any],
    *,
    round_number: int,
    previous: Mapping[str, Any] | None = None,
    min_floor: int = 8,
    min_boundaries: int = 50,
    oracle_schema: str = "spirecomm-parity-v10",
) -> dict[str, Any]:
    """Select the strongest valid truth leaf for every selected seed/variant.

    This only assembles provenance.  Training-lock generation independently
    validates and offline-replays every selected segment.
    """

    if round_number <= 0:
        raise ValueError("validation expansion round must be positive")
    pairs = _selection_pairs(selection)
    report = dict(previous or {"schema": EXPANSION_REPORT_SCHEMA, "rounds": []})
    if report.get("schema") != EXPANSION_REPORT_SCHEMA:
        raise ValueError("unsupported Act 1 validation expansion report")
    rounds = list(report.get("rounds") or ())
    if round_number != len(rounds) + 1:
        raise ValueError("validation expansion rounds must be appended consecutively")
    used_seeds = {
        int(item["seed"])
        for old_round in rounds
        for item in old_round.get("evidence") or ()
    }
    records, invalid = load_records(truth_root)
    if invalid:
        raise ValueError(f"truth corpus contains invalid bundles: {invalid}")

    evidence = []
    for seed, variant in pairs:
        if seed in used_seeds:
            raise ValueError(f"validation expansion seed was already used: {seed}")
        policy_id = f"deterministic-action-v1:variant-{variant}"
        candidates = []
        for leaf, record in records.items():
            if int(record.manifest.get("seed", -1)) != seed:
                continue
            route = evaluate_route(records, leaf)
            failures = [value for value in route["failures"] if value != "ACT_TWO_NOT_REACHED"]
            if failures or not route["segments"]:
                continue
            chain = [records[bundle_id] for bundle_id in route["chain"]]
            if any(item.manifest.get("policy_id") != policy_id for item in chain):
                continue
            if any(
                (item.manifest.get("instrumentation") or {}).get("schema") != oracle_schema
                for item in chain
            ):
                continue
            floor, terminal = _route_endpoint(records, route)
            if int(route["used_boundaries"]) < min_boundaries:
                continue
            if floor < min_floor and not terminal:
                continue
            candidates.append((
                int(route["used_boundaries"]), floor, str(leaf), route,
            ))
        if not candidates:
            raise ValueError(
                f"no clean current-schema evidence for selected seed {seed} variant {variant}"
            )
        # Run IDs are UTC timestamp-prefixed, so the final key selects the
        # newest equally strong recapture after a contract-changing repair.
        route = max(candidates, key=lambda item: item[:3])[3]
        evidence.append({"seed": seed, "variant": variant, "leaf": route["leaf"]})

    rounds.append({
        "round": round_number,
        "selection": dict(selection),
        "evidence": sorted(evidence, key=lambda item: (item["seed"], item["variant"])),
    })
    return {"schema": EXPANSION_REPORT_SCHEMA, "rounds": rounds}
