"""Portable, deterministic attestation for locally verified Act 1 evidence."""

from __future__ import annotations

from itertools import product
import json
from pathlib import Path
from typing import Any, Callable, Mapping

from sls.model.encoding import ENCODING_SCHEMA, vocabulary_hash
from sls.content.scope import IRONCLAD_A0_SCOPE_ID, ironclad_a0_scope_hash
from sls.content.semantic_audit import semantic_audit_hash, verify_semantic_audit
from sls.rl.training_contract import (
    ROOT, TRAINING_CHECKPOINT_SCHEMA, canonical_digest, git_index_digest, git_state,
    native_source_digest, sha256_file,
)
from sls.validation.readiness import evaluate_route, load_records, readiness_report
from sls.validation.truth import value_hash


READINESS_LOCK_SCHEMA = "sls-act1-readiness-lock-v1"
DEFAULT_LOCK = ROOT / "configs" / "validation" / "act1_readiness.lock.json"
TRAINING_READY_LOCK = ROOT / "configs" / "validation" / "act1_training_readiness.lock.json"
ENGINEERING_READY = "ENGINEERING_READY"
TRAINING_READY = "TRAINING_READY"
READINESS_LEVELS = {ENGINEERING_READY, TRAINING_READY}


def _contract() -> dict[str, str]:
    return {
        "encoding_schema": ENCODING_SCHEMA,
        "vocabulary_sha256": vocabulary_hash(),
        "checkpoint_schema": TRAINING_CHECKPOINT_SCHEMA,
        "content_scope_id": IRONCLAD_A0_SCOPE_ID,
        "content_scope_sha256": ironclad_a0_scope_hash(),
        "semantic_audit_sha256": semantic_audit_hash(),
        "native_source_sha256": native_source_digest(),
        "adapter_sha256": git_index_digest((
            "src/sls/backends/original/adapter.py", "src/sls/content/normalize.py",
        )),
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


def build_readiness_lock(
    root: Path,
    requirements: Mapping[str, Any],
    *,
    replay_validator: Callable[[Path, int, int], None],
    level: str = ENGINEERING_READY,
    expansion_report: Mapping[str, Any] | None = None,
) -> dict[str, Any]:
    if level not in READINESS_LEVELS:
        raise ValueError(f"unsupported Act 1 readiness level: {level}")
    report = readiness_report(root, requirements)
    if not report["ready"]:
        raise ValueError(f"Act 1 evidence is not ready: {report['failures']}")
    selected = _select_routes(report, requirements)
    records, invalid = load_records(root)
    if invalid:
        raise ValueError(f"truth corpus contains invalid bundles: {invalid}")
    for route in selected:
        for segment in route["segments"]:
            replay_validator(
                records[str(segment["bundle"])].path,
                int(segment["from_step"]),
                int(segment["to_step"]),
            )
    bundle_ids = sorted({bundle for route in selected for bundle in route["chain"]})
    expansion = None
    if level == TRAINING_READY:
        verify_semantic_audit(require_pilot_ready=True)
        expansion = _validate_expansion(
            records, selected, requirements, expansion_report,
            replay_validator=replay_validator,
        )
        bundle_ids = sorted(set(bundle_ids) | set(expansion["bundle_ids"]))
    elif expansion_report is not None:
        raise ValueError("engineering readiness does not accept expansion evidence")
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
        "level": level,
        "profile": requirements.get("profile"),
        "requirements_sha256": canonical_digest(dict(requirements)),
        "requirements": dict(requirements),
        "contract": _contract(),
        "routes": [
            {
                "leaf": route["leaf"], "seed": route["seed"],
                "boss": next(boss for boss in requirements["bosses"] if boss in route["coverage"]["bosses"]),
                "chain": route["chain"], "used_boundaries": route["used_boundaries"],
                "segments": route["segments"],
                "coverage": route["coverage"],
            }
            for route in selected
        ],
        "bundles": bundles,
        "coverage": report["coverage"],
    }
    if expansion is not None:
        lock["expansion"] = expansion["lock"]
    lock["lock_sha256"] = canonical_digest(lock)
    return lock


def verify_readiness_lock(
    path: Path = DEFAULT_LOCK, *, require_clean: bool = True,
    expected_level: str | None = None,
) -> dict[str, Any]:
    lock = json.loads(path.read_text(encoding="utf-8"))
    if lock.get("schema") != READINESS_LOCK_SCHEMA:
        raise ValueError("unsupported Act 1 readiness lock")
    level = str(lock.get("level") or ENGINEERING_READY)
    if level not in READINESS_LEVELS:
        raise ValueError("unsupported Act 1 readiness level")
    if expected_level is not None and level != expected_level:
        raise ValueError(
            f"Act 1 readiness level mismatch: expected {expected_level}, got {level}"
        )
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
    if level == TRAINING_READY:
        expansion = lock.get("expansion") or {}
        expansion_requirements = lock["requirements"].get("expansion") or {}
        expected_rounds = int(expansion_requirements.get("rounds", 2))
        seeds_per_round = int(expansion_requirements.get("seeds_per_round", 4))
        rounds = expansion.get("rounds") or ()
        if (
            expansion.get("schema") != "sls-act1-training-expansion-lock-v1"
            or len(rounds) != expected_rounds
            or any(len(item.get("evidence") or ()) != seeds_per_round for item in rounds)
            or int(expansion.get("unique_seed_count", 0))
            < len(required_bosses) + expected_rounds * seeds_per_round
        ):
            raise ValueError("training readiness lock has incomplete expansion evidence")
    state = git_state()
    if require_clean and state["dirty"]:
        raise ValueError("Act 1 readiness lock verification requires a clean Git worktree")
    return {
        "valid": True, "lock_sha256": supplied, "profile": lock["profile"],
        "level": level, "git": state,
    }


def _validate_expansion(
    records: Mapping[str, Any], selected_routes: tuple[Mapping[str, Any], ...],
    requirements: Mapping[str, Any], report: Mapping[str, Any] | None, *,
    replay_validator: Callable[[Path, int, int], None],
) -> dict[str, Any]:
    if report is None or report.get("schema") != "sls-act1-validation-expansion-v1":
        raise ValueError("training readiness requires a validation expansion report")
    expansion_requirements = requirements.get("expansion") or {}
    required_rounds = int(expansion_requirements.get("rounds", 2))
    per_round = int(expansion_requirements.get("seeds_per_round", 4))
    min_floor = int(expansion_requirements.get("min_floor", 8))
    min_boundaries = int(expansion_requirements.get("min_boundaries", 50))
    oracle_schema = str(expansion_requirements.get("oracle_schema", "spirecomm-parity-v10"))
    rounds = list(report.get("rounds") or ())
    if len(rounds) != required_rounds:
        raise ValueError(f"training readiness requires exactly {required_rounds} validation rounds")
    base_seeds = {int(route["seed"]) for route in selected_routes}
    used_seeds = set(base_seeds)
    bundle_ids: set[str] = set()
    locked_rounds = []
    for expected_number, round_value in enumerate(rounds, 1):
        if int(round_value.get("round", -1)) != expected_number:
            raise ValueError("validation expansion rounds must be consecutive and one-based")
        selection = round_value.get("selection") or {}
        if selection.get("schema") != "sls-act1-validation-selection-v1":
            raise ValueError("validation round has an unsupported selection report")
        supplied = selection.get("selection_sha256")
        unsigned = dict(selection)
        unsigned.pop("selection_sha256", None)
        if supplied != value_hash(unsigned):
            raise ValueError("validation seed selection digest mismatch")
        selected_items = list(selection.get("selections") or ())
        evidence = list(round_value.get("evidence") or ())
        if len(selected_items) != per_round or len(evidence) != per_round:
            raise ValueError(f"each validation round requires exactly {per_round} seeds")
        expected_pairs = {(int(item["seed"]), int(item["variant"])) for item in selected_items}
        evidence_pairs = {(int(item["seed"]), int(item["variant"])) for item in evidence}
        if len(expected_pairs) != per_round or evidence_pairs != expected_pairs:
            raise ValueError("validation evidence does not match the selected seed/variant pairs")
        locked_evidence = []
        for item in sorted(evidence, key=lambda value: (int(value["seed"]), int(value["variant"]))):
            seed, variant, leaf = int(item["seed"]), int(item["variant"]), str(item["leaf"])
            if seed in used_seeds:
                raise ValueError(f"validation expansion seed is not unique: {seed}")
            route = evaluate_route(records, leaf)
            failures = [value for value in route["failures"] if value != "ACT_TWO_NOT_REACHED"]
            if failures:
                raise ValueError(f"invalid validation expansion evidence {leaf}: {failures}")
            if int(route["seed"]) != seed:
                raise ValueError(f"validation expansion seed mismatch for {leaf}")
            policy_id = f"deterministic-action-v1:variant-{variant}"
            chain = [records[bundle_id] for bundle_id in route["chain"]]
            if any(record.manifest.get("policy_id") != policy_id for record in chain):
                raise ValueError(f"validation expansion policy mismatch for {leaf}")
            if any((record.manifest.get("instrumentation") or {}).get("schema") != oracle_schema for record in chain):
                raise ValueError(f"validation expansion Oracle schema mismatch for {leaf}")
            if int(route["used_boundaries"]) < min_boundaries:
                raise ValueError(f"validation expansion evidence is too short for {leaf}")
            last_segment = route["segments"][-1]
            last_boundary = records[str(last_segment["bundle"])].boundaries[int(last_segment["to_step"])]
            cursor = last_boundary.get("cursor") or {}
            terminal = bool(last_boundary.get("terminal_kind"))
            if int(cursor.get("floor", 0) or 0) < min_floor and not terminal:
                raise ValueError(f"validation expansion evidence does not reach floor {min_floor}: {leaf}")
            for segment in route["segments"]:
                replay_validator(
                    records[str(segment["bundle"])].path,
                    int(segment["from_step"]), int(segment["to_step"]),
                )
            bundle_ids.update(route["chain"])
            used_seeds.add(seed)
            locked_evidence.append({
                "seed": seed, "variant": variant, "leaf": leaf,
                "used_boundaries": route["used_boundaries"],
                "segments": route["segments"], "chain": route["chain"],
            })
        locked_rounds.append({
            "round": expected_number, "selection_sha256": supplied,
            "evidence": locked_evidence,
        })
    if len(used_seeds) < len(base_seeds) + required_rounds * per_round:
        raise ValueError("training readiness does not contain enough unique seeds")
    return {
        "bundle_ids": sorted(bundle_ids),
        "lock": {
            "schema": "sls-act1-training-expansion-lock-v1",
            "requirements": dict(expansion_requirements), "rounds": locked_rounds,
            "unique_seed_count": len(used_seeds),
        },
    }
