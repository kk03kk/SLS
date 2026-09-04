"""Fail-closed semantic coverage obligations for stock/native parity."""

from __future__ import annotations

from collections import defaultdict
from typing import Any, Mapping

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
from sls.content.scope import load_ironclad_a0_scope

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
        if status == SEMANTIC_MATCH:
            stock = row.get("stock_evidence")
            simulator = row.get("simulator_evidence")
            comparisons = row.get("comparisons")
            if not isinstance(stock, Mapping) or not stock.get("artifact_sha256"):
                raise ValueError(f"{obligation_id}: stock evidence is missing")
            if not isinstance(simulator, Mapping) or not simulator.get("source_sha256"):
                raise ValueError(f"{obligation_id}: simulator evidence is missing")
            if stock is simulator or stock == simulator:
                raise ValueError(f"{obligation_id}: evidence must be independent")
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
        elif statuses == {SEMANTIC_MATCH}:
            status = SEMANTIC_MATCH
        elif SEMANTIC_UI_FOLD in statuses:
            status = SEMANTIC_UI_FOLD
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


def require_semantic_training_gate(
    payload: Mapping[str, Any], *, require_scope_complete: bool = False,
) -> None:
    result = validate_semantic_coverage(payload)
    missing: list[str] = []
    if require_scope_complete:
        scope = load_ironclad_a0_scope()
        expected: set[tuple[str, str]] = set()
        for category in (
            "cards", "potions", "relics", "events", "encounters", "monsters",
        ):
            for values in scope[category].values():
                expected.update((category, str(item)) for item in values)
        expected.update(("systems", item) for item in REQUIRED_SYSTEM_OBLIGATIONS)
        actual = {
            (str(row["category"]), str(row["content_id"]))
            for row in result["content"]
        }
        missing = [f"{category}:{content_id}" for category, content_id in sorted(expected - actual)]
    if not result["ready_for_training"] or missing:
        labels = [
            f"{row['category']}:{row['content_id']}={row['status']}"
            for row in result["blocking"]
        ]
        labels.extend(f"{item}=MISSING" for item in missing)
        raise ValueError("semantic parity gate failed: " + ", ".join(labels[:20]))
