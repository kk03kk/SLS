"""Portable, deterministic attestation for locally verified Act 1 evidence."""

from __future__ import annotations

from itertools import product
import json
from pathlib import Path
from typing import Any, Mapping

from sls.model.encoding import ENCODING_SCHEMA, vocabulary_hash
from sls.rl.checkpoint import CHECKPOINT_SCHEMA
from sls.rl.training_contract import (
    ROOT, canonical_digest, git_index_digest, git_state, native_source_digest, sha256_file,
)
from sls.validation.readiness import load_records, readiness_report


READINESS_LOCK_SCHEMA = "sls-act1-readiness-lock-v1"
DEFAULT_LOCK = ROOT / "configs" / "validation" / "act1_readiness.lock.json"


def _contract() -> dict[str, str]:
    return {
        "encoding_schema": ENCODING_SCHEMA,
        "vocabulary_sha256": vocabulary_hash(),
        "checkpoint_schema": CHECKPOINT_SCHEMA,
        "native_source_sha256": native_source_digest(),
        "adapter_sha256": git_index_digest(("src/sls/backends/original/adapter.py",)),
        "canonicalizer_sha256": git_index_digest(("src/sls/validation/compare.py",)),
        "policy_contract_sha256": git_index_digest(("src/sls/contracts", "src/sls/model")),
    }


def _coverage_score(routes: tuple[Mapping[str, Any], ...], requirements: Mapping[str, Any]) -> tuple[int, int, str]:
    coverage = {key: set() for key in ("bosses", "screens", "selected_actions", "rooms")}
    for route in routes:
        for key in coverage:
            coverage[key].update(map(str, route["coverage"][key]))
    required = (
        set(map(str, requirements.get("bosses", ()))),
        set(map(str, requirements.get("screens", ()))),
        set(map(str, requirements.get("selected_actions", ()))),
    )
    covered = sum(len(expectation & coverage[key]) for expectation, key in zip(required, ("bosses", "screens", "selected_actions")))
    extras = sum(len(coverage[key]) for key in coverage)
    leaves = "|".join(sorted(str(route["leaf"]) for route in routes))
    return covered, extras, leaves


def _select_routes(report: Mapping[str, Any], requirements: Mapping[str, Any]) -> tuple[Mapping[str, Any], ...]:
    required_bosses = tuple(map(str, requirements.get("bosses", ())))
    by_boss = {
        boss: [route for route in report["valid_routes"] if boss in route["coverage"]["bosses"]]
        for boss in required_bosses
    }
    if any(not routes for routes in by_boss.values()):
        raise ValueError("readiness report has no complete route for every required Act 1 boss")
    candidates = [
        tuple(items) for items in product(*(by_boss[boss] for boss in required_bosses))
        if len({item["seed"] for item in items}) == len(items)
    ]
    if not candidates:
        raise ValueError("required Act 1 boss routes do not have unique seeds")
    # Maximize required/extra mechanism coverage, then choose lexically earliest leaves.
    return sorted(candidates, key=lambda items: (-_coverage_score(items, requirements)[0], -_coverage_score(items, requirements)[1], _coverage_score(items, requirements)[2]))[0]


def build_readiness_lock(root: Path, requirements: Mapping[str, Any]) -> dict[str, Any]:
    report = readiness_report(root, requirements)
    if not report["ready"]:
        raise ValueError(f"Act 1 evidence is not ready: {report['failures']}")
    selected = _select_routes(report, requirements)
    records, invalid = load_records(root)
    if invalid:
        raise ValueError(f"truth corpus contains invalid bundles: {invalid}")
    bundle_ids = sorted({bundle for route in selected for bundle in route["chain"]})
    bundles = []
    for bundle_id in bundle_ids:
        record = records[bundle_id]
        manifest = record.manifest
        bundles.append({
            "bundle_id": bundle_id,
            "manifest_sha256": sha256_file(record.path / "manifest.json"),
            "boundaries_sha256": str((manifest.get("artifacts") or {}).get("boundaries.jsonl.gz", "")),
            "evidence_class": manifest.get("evidence_class"),
            "source": manifest.get("provenance") or None,
        })
    lock: dict[str, Any] = {
        "schema": READINESS_LOCK_SCHEMA,
        "profile": requirements.get("profile"),
        "requirements_sha256": canonical_digest(dict(requirements)),
        "requirements": dict(requirements),
        "contract": _contract(),
        "routes": [
            {
                "leaf": route["leaf"], "seed": route["seed"],
                "boss": next(boss for boss in requirements["bosses"] if boss in route["coverage"]["bosses"]),
                "chain": route["chain"], "used_boundaries": route["used_boundaries"],
                "coverage": route["coverage"],
            }
            for route in selected
        ],
        "bundles": bundles,
        "coverage": report["coverage"],
    }
    lock["lock_sha256"] = canonical_digest(lock)
    return lock


def verify_readiness_lock(path: Path = DEFAULT_LOCK, *, require_clean: bool = True) -> dict[str, Any]:
    lock = json.loads(path.read_text(encoding="utf-8"))
    if lock.get("schema") != READINESS_LOCK_SCHEMA:
        raise ValueError("unsupported Act 1 readiness lock")
    supplied = lock.get("lock_sha256")
    unsigned = dict(lock)
    unsigned.pop("lock_sha256", None)
    if supplied != canonical_digest(unsigned):
        raise ValueError("Act 1 readiness lock digest mismatch")
    if lock.get("requirements_sha256") != canonical_digest(lock.get("requirements")):
        raise ValueError("Act 1 readiness requirements digest mismatch")
    if lock.get("contract") != _contract():
        raise ValueError("Act 1 readiness lock is stale for the current source contract")
    required_bosses = set(map(str, lock["requirements"].get("bosses", ())))
    route_bosses = {str(route["boss"]) for route in lock.get("routes", ())}
    route_seeds = {int(route["seed"]) for route in lock.get("routes", ())}
    if route_bosses != required_bosses or len(route_seeds) != len(required_bosses):
        raise ValueError("Act 1 readiness lock does not contain unique routes for all bosses")
    state = git_state()
    if require_clean and state["dirty"]:
        raise ValueError("Act 1 readiness lock verification requires a clean Git worktree")
    return {"valid": True, "lock_sha256": supplied, "profile": lock["profile"], "git": state}
