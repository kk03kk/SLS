"""Fail-closed semantic coverage obligations for stock/native parity."""

from __future__ import annotations

import re
from collections import defaultdict
from typing import Any, Mapping

from sls.audit.act1_targets import PROFILE_ID, target_ids
from sls.audit.stock_parity import (
    BLOCKING_PARITY_STATUSES,
    BRANCH_PARTIAL,
    PARITY_STATUSES,
    PRESENTATION_ONLY,
    SEMANTIC_DIFFERENCE,
    SEMANTIC_MATCH,
    SEMANTIC_UI_FOLD,
    UNREVIEWED,
)
from sls.content.scope import ironclad_scope, load_ironclad_a0_scope

COVERAGE_SCHEMA = "sls-semantic-coverage-v1"
REQUIRED_SYSTEM_OBLIGATIONS = frozenset({
    "NEOW", "MAP_GENERATION", "ROOM_POOLS", "REWARD_POOLS",
    "SHOP_PRICING", "CARD_POOLS", "RELIC_POOLS", "POTION_POOLS",
    "ACT_TRANSITION", "BOSS_SELECTION", "RNG_STREAMS",
    "CHECKPOINT_ROUND_TRIP", "SMOKE_BOMB", "KEY_UI_FOLDS",
})


def validate_semantic_coverage(payload: Mapping[str, Any]) -> dict[str, Any]:
    """Validate evidence and summarize content without inventing parity.

    Each obligation corresponds to a stock Java class/method/branch and must
    name distinct stock and simulator evidence.  A content item is a semantic
    match only when every one of its declared obligations is a match.
    """

    if payload.get("schema") != COVERAGE_SCHEMA:
        raise ValueError("unsupported semantic coverage schema")
    obligations = payload.get("obligations")
    if not isinstance(obligations, list) or not obligations:
        raise ValueError("semantic coverage must declare at least one obligation")

    by_content: dict[tuple[str, str], list[dict[str, Any]]] = defaultdict(list)
    seen_ids: set[str] = set()
    for raw in obligations:
        if not isinstance(raw, Mapping):
            raise ValueError("semantic coverage obligation must be an object")
        row = dict(raw)
        obligation_id = str(row.get("obligation_id") or "")
        category = str(row.get("category") or "")
        content_id = str(row.get("content_id") or "")
        status = str(row.get("status") or UNREVIEWED)
        if not obligation_id or obligation_id in seen_ids:
            raise ValueError("semantic obligation IDs must be unique and non-empty")
        if not category or not content_id:
            raise ValueError(f"{obligation_id}: category/content_id is missing")
        if status not in PARITY_STATUSES:
            raise ValueError(f"{obligation_id}: unsupported parity status {status}")
        if status in {SEMANTIC_MATCH, SEMANTIC_UI_FOLD, PRESENTATION_ONLY}:
            stock = row.get("stock_evidence")
            simulator = row.get("simulator_evidence")
            comparisons = row.get("comparisons")
            if not isinstance(stock, Mapping) or not _is_sha256(stock.get("artifact_sha256")):
                raise ValueError(f"{obligation_id}: stock evidence is missing")
            if not isinstance(simulator, Mapping) or not _is_sha256(simulator.get("source_sha256")):
                raise ValueError(f"{obligation_id}: simulator evidence is missing")
            if stock is simulator or stock == simulator:
                raise ValueError(f"{obligation_id}: evidence must be independent")
            for field, evidence, key in (
                ("stock_jar_sha256", stock, "artifact_sha256"),
                ("native_source_sha256", simulator, "source_sha256"),
            ):
                if payload.get(field) is not None and evidence[key] != payload[field]:
                    raise ValueError(f"{obligation_id}: evidence does not match {field}")
            if status != SEMANTIC_MATCH and not str(row.get("rationale") or "").strip():
                raise ValueError(f"{obligation_id}: a non-semantic classification needs a rationale")
            required = {"before", "actions", "after", "rng"}
            if not isinstance(comparisons, Mapping) or not required <= set(comparisons):
                raise ValueError(f"{obligation_id}: required comparisons are missing")
            if any(comparisons[name] is not True for name in required):
                raise ValueError(f"{obligation_id}: a required comparison did not pass")
        seen_ids.add(obligation_id)
        by_content[(category, content_id)].append(row)

    content: list[dict[str, Any]] = []
    for (category, content_id), rows in sorted(by_content.items()):
        statuses = {str(row.get("status") or UNREVIEWED) for row in rows}
        if SEMANTIC_DIFFERENCE in statuses:
            status = SEMANTIC_DIFFERENCE
        elif UNREVIEWED in statuses:
            status = UNREVIEWED
        elif BRANCH_PARTIAL in statuses:
            status = BRANCH_PARTIAL
        elif SEMANTIC_UI_FOLD in statuses:
            status = SEMANTIC_UI_FOLD
        elif SEMANTIC_MATCH in statuses:
            status = SEMANTIC_MATCH
        else:
            status = PRESENTATION_ONLY
        content.append({
            "category": category,
            "content_id": content_id,
            "status": status,
            "obligations": len(rows),
        })
    blocking = [row for row in content if row["status"] in BLOCKING_PARITY_STATUSES]
    return {
        "schema": COVERAGE_SCHEMA,
        "content": content,
        "blocking": blocking,
        "ready_for_training": not blocking,
    }


def _is_sha256(value: Any) -> bool:
    return isinstance(value, str) and re.fullmatch(r"[0-9a-f]{64}", value) is not None


def require_semantic_training_gate(
    payload: Mapping[str, Any], *, require_scope_complete: bool = False,
    targets: Mapping[str, Any] | None = None,
    baseline: Mapping[str, Any] | None = None,
) -> None:
    result = validate_semantic_coverage(payload)
    missing: list[str] = []
    if require_scope_complete:
        expected: set[tuple[str, str]] = set()
        categories = ("cards", "potions", "relics", "events", "encounters", "monsters")
        if payload.get("scope_id") == PROFILE_ID:
            if targets is None:
                raise ValueError("A20 Act1 completeness requires its target inventory")
            if payload.get("scope_sha256") != ironclad_scope(20)["scope_sha256"]:
                raise ValueError("A20 Act1 coverage scope hash is stale")
            if targets.get("scope_sha256") != payload.get("scope_sha256"):
                raise ValueError("coverage and target scope hashes differ")
            authority = targets.get("authority")
            if not isinstance(authority, Mapping):
                raise ValueError("A20 Act1 target authority is missing")
            for field in ("stock_jar_sha256", "native_source_sha256"):
                if not _is_sha256(payload.get(field)) or payload[field] != authority.get(field):
                    raise ValueError(f"coverage and target {field} differ")
            for category in categories:
                expected.update((category, item) for item in target_ids(targets, category))
            if baseline is None:
                raise ValueError("A20 Act1 completeness requires its method-obligation baseline")
            validate_semantic_coverage(baseline)
            for field in ("scope_id", "scope_sha256", "stock_jar_sha256", "native_source_sha256"):
                if baseline.get(field) != payload.get(field):
                    raise ValueError(f"coverage and baseline {field} differ")
            reference_rows = {row["obligation_id"]: row for row in baseline["obligations"]}
            reviewed_rows = {row["obligation_id"]: row for row in payload["obligations"]}
            missing.extend(f"obligation:{item}" for item in sorted(reference_rows.keys() - reviewed_rows.keys()))
            for obligation_id in reference_rows.keys() & reviewed_rows.keys():
                for field in ("category", "content_id", "java_class", "java_method",
                              "stock_class_sha256", "stock_javap_sha256"):
                    if reference_rows[obligation_id].get(field) != reviewed_rows[obligation_id].get(field):
                        raise ValueError(f"{obligation_id}: reviewed obligation differs from baseline {field}")
        else:
            scope = load_ironclad_a0_scope()
            if payload.get("scope_id") not in (None, scope["scope_id"]):
                raise ValueError("unsupported semantic coverage scope")
            for category in categories:
                for values in scope[category].values():
                    expected.update((category, str(item)) for item in values)
        expected.update(("systems", item) for item in REQUIRED_SYSTEM_OBLIGATIONS)
        actual = {
            (str(row["category"]), str(row["content_id"]))
            for row in result["content"]
        }
        missing.extend(f"{category}:{content_id}" for category, content_id in sorted(expected - actual))
    if not result["ready_for_training"] or missing:
        labels = [
            f"{row['category']}:{row['content_id']}={row['status']}"
            for row in result["blocking"]
        ]
        labels.extend(f"{item}=MISSING" for item in missing)
        raise ValueError("semantic parity gate failed: " + ", ".join(labels[:20]))
