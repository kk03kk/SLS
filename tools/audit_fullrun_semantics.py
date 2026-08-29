"""Build the conservative A0-A20 Ironclad FullRun evidence ledger.

This ledger extends, but never weakens, the locked A0 Act 1 audit.  Content
without executable Original/native parity evidence is deliberately BLOCKED.
"""

from __future__ import annotations

import argparse
import json
from pathlib import Path
import sys
from typing import Any


ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "src"))

from sls.rl.training_contract import canonical_digest  # noqa: E402


SCHEMA = "sls-ironclad-fullrun-semantic-audit-v1"
INVENTORY = ROOT / "configs" / "validation" / "ironclad_fullrun_inventory.json"
ACT1_AUDIT = ROOT / "configs" / "validation" / "ironclad_a0_semantic_audit.json"
OUTPUT = ROOT / "configs" / "validation" / "ironclad_fullrun_semantic_audit.json"


def _blocked(
    identifier: str, category: str, stage: str, *, evidence: list[str],
    remaining: list[str], details: dict[str, Any] | None = None,
) -> dict[str, Any]:
    return {
        "id": identifier,
        "category": category,
        "stage": stage,
        "status": "BLOCKED",
        "evidence_levels": evidence,
        "remaining": remaining,
        "details": details or {},
    }


def _difference(
    identifier: str, category: str, stage: str, *, differences: dict[str, Any],
    evidence: list[str], remaining: list[str],
) -> dict[str, Any]:
    return {
        "id": identifier,
        "category": category,
        "stage": stage,
        "status": "DIFFERENCE",
        "evidence_levels": evidence,
        "remaining": remaining,
        "differences": differences,
        "details": {},
    }


def build_audit() -> dict[str, Any]:
    inventory = json.loads(INVENTORY.read_text(encoding="utf-8"))
    act1 = json.loads(ACT1_AUDIT.read_text(encoding="utf-8"))
    inherited = dict(act1["summary"]["status_counts"])
    if inherited != {"BLOCKED": 0, "DIFFERENCE": 0, "OUT_OF_SCOPE": 0, "VERIFIED": 418}:
        raise RuntimeError("the inherited A0 Act 1 audit is not the expected 418/0 baseline")

    act1_encounters = {str(item["id"]) for item in act1["entries"]["encounters"]}
    act1_monsters = {str(item["id"]) for item in act1["entries"]["monsters"]}
    entries: list[dict[str, Any]] = []
    entries.append(_difference(
        "PRISMATIC_SHARD", "relic", "FULLRUN_CLOSURE",
        differences={
            "original": "ordinary rewards use CardLibrary.getAnyColorCard(rarity)",
            "native": "GameContext::disablePrismaticShard is true",
            "impact": "214 Original-reachable reward cards are absent from native runs",
        },
        evidence=["SOURCE_MATCHED"],
        remaining=["NATIVE_VERIFIED", "ORIGINAL_VERIFIED", "ROUTE_VERIFIED"],
    ))
    for category, inherited_ids in (
        ("encounter", act1_encounters), ("monster", act1_monsters),
    ):
        source = inventory[f"{category}s"]
        reachable: dict[str, list[str]] = {}
        for stage in ("act2", "act3", "act4", "events"):
            for identifier in source[stage]:
                if identifier not in inherited_ids:
                    reachable.setdefault(identifier, []).append(stage.upper())
        for identifier, stages in reachable.items():
            entries.append(_blocked(
                identifier, category, stages[0],
                evidence=["NATIVE_ENUMERATED"],
                remaining=["ORIGINAL_VERIFIED", "NATIVE_VERIFIED", "ROUTE_VERIFIED"],
                details={"reachable_stages": stages},
            ))

    entries.append(_blocked(
        "ASCENDERS_BANE", "card", "A10_PLUS",
        evidence=["SOURCE_ENUMERATED", "NATIVE_METADATA_PRESENT"],
        remaining=["ORIGINAL_VERIFIED", "NATIVE_VERIFIED"],
    ))
    for modifier in inventory["ascension_modifiers"]:
        entries.append(_blocked(
            str(modifier["id"]), "ascension_modifier", f"A{modifier['level']}_PLUS",
            evidence=["SOURCE_MATCHED"],
            remaining=["ORIGINAL_VERIFIED", "NATIVE_VERIFIED", "ROUTE_VERIFIED"],
            details={
                "level": modifier["level"],
                "original_source": modifier["original_source"],
                "native_source": modifier["native_source"],
            },
        ))
    entries.extend((
        _blocked(
            "ACT_TRANSITIONS", "run_system", "ACT2_ACT3",
            evidence=["NATIVE_EXECUTED"],
            remaining=["ORIGINAL_VERIFIED", "ROUTE_VERIFIED"],
            details={"test": "tests/simulator/test_fullrun_structure.py"},
        ),
        _blocked(
            "THREE_KEYS", "run_system", "HEART",
            evidence=["NATIVE_EXECUTED", "POLICY_VISIBLE"],
            remaining=["ORIGINAL_VERIFIED", "ROUTE_VERIFIED"],
            details={"test": "tests/simulator/test_fullrun_structure.py"},
        ),
        _blocked(
            "ACT4_HEART_ROUTE", "run_system", "HEART",
            evidence=["NATIVE_EXECUTED"],
            remaining=["ORIGINAL_VERIFIED", "ROUTE_VERIFIED"],
            details={"test": "tests/simulator/test_fullrun_structure.py"},
        ),
        _blocked(
            "A20_SECOND_ACT3_BOSS", "run_system", "A20",
            evidence=["NATIVE_EXECUTED", "SOURCE_MATCHED"],
            remaining=["ORIGINAL_VERIFIED", "ROUTE_VERIFIED"],
            details={"test": "tests/simulator/test_fullrun_structure.py"},
        ),
    ))
    entries.sort(key=lambda item: (item["category"], item["stage"], item["id"]))
    counts = {
        "VERIFIED": inherited["VERIFIED"],
        "DIFFERENCE": inherited["DIFFERENCE"] + sum(
            item["status"] == "DIFFERENCE" for item in entries
        ),
        "BLOCKED": inherited["BLOCKED"] + sum(
            item["status"] == "BLOCKED" for item in entries
        ),
        "OUT_OF_SCOPE": inherited["OUT_OF_SCOPE"],
    }
    payload: dict[str, Any] = {
        "schema": SCHEMA,
        "inventory_sha256": inventory["inventory_sha256"],
        "inherited_act1_audit": {
            "path": ACT1_AUDIT.relative_to(ROOT).as_posix(),
            "audit_sha256": act1["audit_sha256"],
            "status_counts": inherited,
        },
        "entries": entries,
        "stages": {
            "A0_ACT1": {"status": "TRAINING_READY", "inherited": True},
            "A0_ACT2": {"status": "BLOCKED", "reason": "later encounter/monster and route parity is incomplete"},
            "A0_ACT3": {"status": "BLOCKED", "reason": "later encounter/monster and route parity is incomplete"},
            "A0_HEART": {"status": "BLOCKED", "reason": "key/Act 4 route parity is incomplete"},
            "A1_A20": {"status": "BLOCKED", "reason": "ascension modifier parity is incomplete"},
        },
        "summary": {
            "status_counts": counts,
            "fullrun_training_ready": False,
            "claim": (
                "The native route is structurally executable; no later-stage or ascension "
                "entry is promoted without executable Original/native parity evidence."
            ),
        },
    }
    payload["audit_sha256"] = canonical_digest(payload)
    return payload


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true")
    parser.add_argument("--output", type=Path, default=OUTPUT)
    args = parser.parse_args()
    rendered = json.dumps(build_audit(), indent=2, sort_keys=True) + "\n"
    if args.check:
        if not args.output.is_file() or args.output.read_text(encoding="utf-8") != rendered:
            print(f"stale FullRun semantic audit: {args.output}")
            return 1
        return 0
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(rendered, encoding="utf-8")
    print(args.output)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
