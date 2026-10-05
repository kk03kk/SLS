"""Expand stock bytecode inventory into fail-closed method obligations."""

from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "src"))

from sls.audit.act1_targets import target_ids  # noqa: E402
from sls.audit.semantic_coverage import (  # noqa: E402
    COVERAGE_SCHEMA,
    REQUIRED_SYSTEM_OBLIGATIONS,
)
from sls.content.scope import ironclad_scope, load_ironclad_a0_scope  # noqa: E402
from sls.rl.training_contract import native_source_digest  # noqa: E402


def _scope_ids(category: str) -> set[str]:
    section = load_ironclad_a0_scope().get(category, {})
    result: set[str] = set()
    if isinstance(section, dict):
        for values in section.values():
            if isinstance(values, list):
                result.update(map(str, values))
    return result


def build_obligations(
    inventory: dict[str, object], *, targets: dict[str, object] | None = None,
) -> dict[str, object]:
    if targets is not None:
        authority = dict(targets.get("authority") or {})
        if authority.get("stock_jar_sha256") != inventory.get("stock_jar_sha256"):
            raise ValueError("target and bytecode inventory stock JAR hashes differ")
        if authority.get("native_source_sha256") != native_source_digest():
            raise ValueError("A20 target native source hash is stale")
        if targets.get("scope_sha256") != ironclad_scope(20)["scope_sha256"]:
            raise ValueError("A20 target scope hash is stale")
        scoped_ids = {
            category: target_ids(targets, category)
            for category in ("cards", "potions", "relics", "events", "encounters", "monsters")
        }
    else:
        scoped_ids = {}
    obligations: list[dict[str, object]] = []
    inventory_categories = dict(inventory.get("categories") or {})
    if targets is not None:
        missing = (set(scoped_ids) - {"encounters"}) - inventory_categories.keys()
        if missing:
            raise ValueError(f"bytecode inventory lacks target categories: {sorted(missing)}")
    for category, raw_rows in inventory_categories.items():
        scoped = scoped_ids.get(str(category), _scope_ids(str(category)))
        available = {str(dict(row)["content_id"]) for row in raw_rows}
        if targets is not None and scoped - available:
            raise ValueError(
                f"bytecode inventory lacks {category} targets: {sorted(scoped - available)[:10]}"
            )
        for raw in raw_rows:
            row = dict(raw)
            content_id = str(row["content_id"])
            if content_id not in scoped:
                continue
            classes = list(row.get("stock_classes") or ())
            if not classes:
                obligations.append({
                    "obligation_id": f"{category}:{content_id}:stock-class",
                    "category": category,
                    "content_id": content_id,
                    "java_class": None,
                    "java_method": None,
                    "branch": "UNRESOLVED_CLASS",
                    "simulator_references": row.get("simulator_references") or [],
                    "status": "UNREVIEWED",
                })
                continue
            for class_index, stock_class in enumerate(classes):
                class_name = str(stock_class["class_name"])
                methods = list(stock_class.get("methods") or ()) or ["<no-method-index>"]
                for index, method in enumerate(methods):
                    obligations.append({
                        "obligation_id": (
                            f"{category}:{content_id}:{class_index}:{index}"
                        ),
                        "category": category,
                        "content_id": content_id,
                        "java_class": class_name,
                        "java_method": method,
                        # Method-level rows are the minimum. Auditors split
                        # conditional methods into explicit branch rows before
                        # they can be marked SEMANTIC_MATCH.
                        "branch": "BRANCH_ENUMERATION_REQUIRED",
                        "stock_class_sha256": stock_class.get("class_sha256"),
                        "stock_javap_sha256": stock_class.get("javap_sha256"),
                        "simulator_references": row.get("simulator_references") or [],
                        "status": "UNREVIEWED",
                    })
    for encounter_id in sorted(scoped_ids.get("encounters", _scope_ids("encounters"))):
        obligations.append({
            "obligation_id": f"encounters:{encounter_id}:full-state-machine",
            "category": "encounters",
            "content_id": encounter_id,
            "java_class": "AbstractDungeon encounter construction",
            "java_method": "getMonsterForRoomCreation/takeTurn/getMove",
            "branch": "ALL_MOVES_PHASES_SUMMONS_DEATH",
            "simulator_references": ["native/simulator/src/combat/MonsterGroup.cpp"],
            "status": "UNREVIEWED",
        })
    for system in sorted(REQUIRED_SYSTEM_OBLIGATIONS):
        obligations.append({
            "obligation_id": f"systems:{system}:all-branches",
            "category": "systems",
            "content_id": system,
            "java_class": "stock-system",
            "java_method": "ALL_REACHABLE_METHODS",
            "branch": "ALL_REACHABLE_BRANCHES",
            "simulator_references": [],
            "status": "UNREVIEWED",
        })
    return {
        "schema": COVERAGE_SCHEMA,
        "stock_jar_sha256": inventory.get("stock_jar_sha256"),
        "native_source_sha256": native_source_digest(),
        "scope_id": (
            str(targets["profile_id"]) if targets is not None
            else load_ironclad_a0_scope()["scope_id"]
        ),
        "scope_sha256": (
            str(targets["scope_sha256"]) if targets is not None
            else load_ironclad_a0_scope()["scope_sha256"]
        ),
        "target_inventory_status": targets.get("status") if targets is not None else None,
        "obligations": obligations,
    }


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("inventory", type=Path)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--targets", type=Path, help="A20 Act1 candidate inventory")
    args = parser.parse_args()
    targets = json.loads(args.targets.read_text(encoding="utf-8")) if args.targets else None
    result = build_obligations(
        json.loads(args.inventory.read_text(encoding="utf-8")), targets=targets,
    )
    args.output.parent.mkdir(parents=True, exist_ok=True)
    temporary = args.output.with_suffix(args.output.suffix + ".tmp")
    temporary.write_text(
        json.dumps(result, ensure_ascii=False, indent=2, sort_keys=True) + "\n",
        encoding="utf-8",
    )
    temporary.replace(args.output)
    print(json.dumps({"obligations": len(result["obligations"])}, sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
