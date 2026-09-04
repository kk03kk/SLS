"""Expand stock bytecode inventory into fail-closed method obligations."""

from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "src"))

from sls.audit.semantic_coverage import (  # noqa: E402
    COVERAGE_SCHEMA,
    REQUIRED_SYSTEM_OBLIGATIONS,
)
from sls.content.scope import load_ironclad_a0_scope  # noqa: E402


def _scope_ids(category: str) -> set[str]:
    section = load_ironclad_a0_scope().get(category, {})
    result: set[str] = set()
    if isinstance(section, dict):
        for values in section.values():
            if isinstance(values, list):
                result.update(map(str, values))
    return result


def build_obligations(inventory: dict[str, object]) -> dict[str, object]:
    obligations: list[dict[str, object]] = []
    for category, raw_rows in dict(inventory.get("categories") or {}).items():
        scoped = _scope_ids(str(category))
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
    for encounter_id in sorted(_scope_ids("encounters")):
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
        "scope_id": load_ironclad_a0_scope()["scope_id"],
        "scope_sha256": load_ironclad_a0_scope()["scope_sha256"],
        "obligations": obligations,
    }


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("inventory", type=Path)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    result = build_obligations(json.loads(args.inventory.read_text(encoding="utf-8")))
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
