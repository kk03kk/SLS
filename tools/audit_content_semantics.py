"""Build a deterministic, evidence-levelled Ironclad semantic audit ledger."""

from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path
import re
import sys
from typing import Any, Callable


ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "src"))

from sls.content.scope import ironclad_a0_scope_hash, load_ironclad_a0_scope  # noqa: E402
from sls.content.semantic_audit import SEMANTIC_AUDIT_PATH, SEMANTIC_AUDIT_SCHEMA  # noqa: E402
from sls.content.source_audit import (  # noqa: E402
    JavaSource, java_card_metadata, java_potion_metadata, java_relic_metadata,
    java_sources, registry_game_ids,
)
from sls.rl.training_contract import canonical_digest  # noqa: E402


CPP_ROOTS = (
    ROOT / "cpp" / "simulator" / "src",
    ROOT / "cpp" / "simulator" / "include",
    ROOT / "src" / "sls",
)
BEHAVIOR_TEST_ROOTS = (
    ROOT / "tests" / "simulator",
    ROOT / "tests" / "original",
    ROOT / "tests" / "fixtures" / "regressions",
)
STATUSES = {"VERIFIED", "DIFFERENCE", "BLOCKED", "OUT_OF_SCOPE"}


def _sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def _references(identifier: str, roots: tuple[Path, ...], suffixes: set[str]) -> list[str]:
    pattern = re.compile(rf"(?<![A-Z0-9_]){re.escape(identifier)}(?![A-Z0-9_])")
    result = []
    for root in roots:
        for path in root.rglob("*"):
            if not path.is_file() or path.suffix not in suffixes:
                continue
            if "constants" in path.parts and root.name == "include":
                continue
            for line_number, line in enumerate(
                path.read_text(encoding="utf-8", errors="replace").splitlines(), 1,
            ):
                if pattern.search(line):
                    result.append(
                        f"{path.relative_to(ROOT).as_posix()}:{line_number}"
                    )
    return sorted(result)


def _callbacks(source: JavaSource, category: str) -> list[str]:
    names = {
        "cards": ("use", "upgrade", "canUse", "triggerWhenDrawn", "triggerOnEndOfTurnForPlayingCard"),
        "potions": ("use", "canUse", "getPotency"),
        "relics": (
            "onEquip", "onUnequip", "atBattleStart", "atTurnStart", "atTurnStartPostDraw",
            "onPlayerEndTurn", "onUseCard", "onPlayCard", "onAttack", "onAttacked",
            "onLoseHp", "onVictory", "onEnterRoom", "onChestOpen", "onObtainCard",
        ),
        "events": ("buttonEffect", "onEnterRoom", "update"),
    }[category]
    return sorted({
        name for name in names
        if re.search(rf"\b{re.escape(name)}\s*\(", source.text)
    })


def _native_metadata() -> dict[str, dict[str, dict[str, Any]]]:
    from sls.backends.simulator import native

    return {
        "cards": {str(item["enum_id"]): dict(item) for item in native.card_metadata_probe()},
        "potions": {str(item["enum_id"]): dict(item) for item in native.potion_metadata_probe()},
        "relics": {str(item["enum_id"]): dict(item) for item in native.relic_metadata_probe()},
    }


def _expected_metadata(category: str, source: JavaSource) -> dict[str, Any]:
    extractors: dict[str, Callable[[JavaSource], dict[str, object]]] = {
        "cards": java_card_metadata,
        "potions": java_potion_metadata,
        "relics": java_relic_metadata,
    }
    if category not in extractors:
        return {}
    value = dict(extractors[category](source))
    if category == "potions":
        value["rarity"] = {"COMMON": 0, "UNCOMMON": 1, "RARE": 2}[str(value["rarity"])]
    if category == "relics":
        value["tier"] = str(value["tier"]).upper()
    return value


def build_audit() -> dict[str, Any]:
    scope = load_ironclad_a0_scope()
    native = _native_metadata()
    entries: dict[str, list[dict[str, Any]]] = {}
    status_counts = {status: 0 for status in sorted(STATUSES)}
    for category in ("cards", "potions", "relics", "events"):
        ids = list(map(str, scope[category]["ids"]))
        game_ids = registry_game_ids(category, ids)
        sources = java_sources(category)
        values = []
        for identifier in ids:
            game_id = game_ids[identifier]
            source = sources.get(game_id)
            cpp_refs = _references(identifier, CPP_ROOTS, {".cpp", ".h", ".py"})
            tests = _references(identifier, BEHAVIOR_TEST_ROOTS, {".py", ".json"})
            differences: dict[str, Any] = {}
            expected: dict[str, Any] = {}
            if source is None:
                differences["java_source"] = [game_id, None]
            else:
                expected = _expected_metadata(category, source)
                actual = native.get(category, {}).get(identifier, {})
                for key, expected_value in expected.items():
                    actual_value = actual.get(key)
                    if category == "relics" and key == "tier":
                        actual_value = str(actual_value).upper()
                    if actual_value != expected_value:
                        differences[f"metadata.{key}"] = [expected_value, actual_value]
            if not cpp_refs:
                differences["simulator_implementation"] = ["required", None]
            source_matched = source is not None and not differences
            behavior_verified = bool(tests)
            status = (
                "DIFFERENCE" if differences else
                "VERIFIED" if source_matched and behavior_verified else
                "BLOCKED"
            )
            status_counts[status] += 1
            levels = []
            if source_matched:
                levels.append("SOURCE_MATCHED")
            if expected and source_matched:
                levels.append("NATIVE_METADATA_VERIFIED")
            if behavior_verified:
                levels.append("NATIVE_VERIFIED")
            if category in {"cards", "potions", "relics"}:
                levels.append("NATIVE_EXECUTED")
            values.append({
                "id": identifier,
                "game_id": game_id,
                "status": status,
                "evidence_levels": levels,
                "java_source": (
                    None if source is None else source.path.relative_to(ROOT).as_posix()
                ),
                "java_sha256": None if source is None else _sha256(source.path),
                "java_callbacks": [] if source is None else _callbacks(source, category),
                "simulator_references": cpp_refs,
                "test_references": tests,
                "category_execution_test": (
                    "tests/simulator/test_content_execution.py" if category in {
                        "cards", "potions", "relics",
                    } else None
                ),
                "metadata": expected,
                "scope_groups": (
                    sorted(
                        key for key in (
                            "act1_base", "act1_shrines", "a0_one_time_candidates",
                        )
                        if identifier in set(scope["events"].get(key, ()))
                    ) if category == "events" else []
                ),
                "differences": differences,
            })
        entries[category] = values

    encounter_values = []
    for identifier in map(str, scope["encounters"]["act1"]):
        cpp_refs = _references(identifier, CPP_ROOTS, {".cpp", ".h", ".py"})
        differences = (
            {} if cpp_refs else {"simulator_implementation": ["required", None]}
        )
        status = "DIFFERENCE" if differences else "BLOCKED"
        status_counts[status] += 1
        encounter_values.append({
            "id": identifier,
            "act": 1,
            "status": status,
            "evidence_levels": ["NATIVE_EXECUTED"] if not differences else [],
            "java_source": None,
            "java_sha256": None,
            "simulator_references": cpp_refs,
            "test_references": [
                "tests/simulator/test_content_execution.py:"
                "test_every_act1_encounter_initializes_with_a_legal_boundary",
            ],
            "differences": differences,
            "remaining": ["SOURCE_MATCHED", "NATIVE_VERIFIED", "ORIGINAL_VERIFIED"],
        })
    entries["encounters"] = encounter_values

    mechanisms = [
        {"id": "RNG", "status": "BLOCKED", "evidence_levels": ["NATIVE_VERIFIED"],
         "test_references": ["tests/simulator/test_native_mechanisms.py:test_original_compatible_rng_is_seeded_and_advances_exactly"],
         "remaining": ["SOURCE_MATCHED", "ORIGINAL_VERIFIED"]},
        {"id": "DAMAGE_PIPELINE", "status": "BLOCKED", "evidence_levels": ["NATIVE_VERIFIED"],
         "test_references": ["tests/simulator/test_native_mechanisms.py:test_core_combat_rule_probes"],
         "remaining": ["SOURCE_MATCHED", "ORIGINAL_VERIFIED"]},
        {"id": "POWER_ORDER", "status": "BLOCKED", "evidence_levels": ["NATIVE_VERIFIED"],
         "test_references": ["tests/simulator/test_native_mechanisms.py:test_turn_lifecycle_and_stable_power_order"],
         "remaining": ["SOURCE_MATCHED", "ORIGINAL_VERIFIED"]},
        {"id": "ORB_ENGINE", "status": "BLOCKED", "evidence_levels": ["NATIVE_VERIFIED"],
         "test_references": ["tests/simulator/test_native_mechanisms.py:test_core_combat_rule_probes"],
         "remaining": ["SOURCE_MATCHED"]},
        {"id": "STANCE_ENGINE", "status": "BLOCKED", "evidence_levels": ["NATIVE_VERIFIED"],
         "test_references": ["tests/simulator/test_native_mechanisms.py:test_core_combat_rule_probes"],
         "remaining": ["SOURCE_MATCHED"]},
        {"id": "RUN_AND_CHECKPOINT", "status": "BLOCKED", "evidence_levels": ["NATIVE_VERIFIED"],
         "test_references": ["tests/simulator/test_native_mechanisms.py:test_full_run_checkpoint_is_exact_across_decision_boundaries"],
         "remaining": ["ACT1_SOURCE_AUDIT", "ORIGINAL_VERIFIED"]},
        {"id": "NONCOMBAT_POTION_ACTIONS", "status": "BLOCKED",
         "evidence_levels": ["SOURCE_MATCHED", "NATIVE_VERIFIED"],
         "source_references": [
             "reference/original-game/decompiled/com/megacrit/cardcrawl/potions/BloodPotion.java",
             "reference/original-game/decompiled/com/megacrit/cardcrawl/potions/EntropicBrew.java",
             "reference/original-game/decompiled/com/megacrit/cardcrawl/potions/FruitJuice.java",
         ],
         "test_references": [
             "tests/simulator/test_simulator_backend.py:test_noncombat_potion_actions_preserve_the_current_screen",
             "tests/original/test_adapter.py:test_stock_out_of_combat_potions_remain_policy_actions",
         ],
         "remaining": ["ORIGINAL_VERIFIED"]},
    ]
    for item in mechanisms:
        status_counts[item["status"]] += 1
    payload: dict[str, Any] = {
        "schema": SEMANTIC_AUDIT_SCHEMA,
        "content_scope_sha256": ironclad_a0_scope_hash(),
        "entries": entries,
        "mechanisms": mechanisms,
        "summary": {
            "status_counts": status_counts,
            "act1_pilot_ready": status_counts["DIFFERENCE"] == 0 and status_counts["BLOCKED"] == 0,
            "claim": (
                "VERIFIED requires stock-source metadata agreement plus explicit executable evidence; "
                "BLOCKED entries are not parity claims."
            ),
        },
    }
    payload["audit_sha256"] = canonical_digest(payload)
    return payload


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true")
    parser.add_argument("--require-pilot-ready", action="store_true")
    parser.add_argument("--output", type=Path, default=SEMANTIC_AUDIT_PATH)
    args = parser.parse_args()
    payload = build_audit()
    rendered = json.dumps(payload, indent=2, sort_keys=True) + "\n"
    if args.check:
        if not args.output.exists() or args.output.read_text(encoding="utf-8") != rendered:
            print(f"stale semantic audit: {args.output}", file=sys.stderr)
            return 2
    else:
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(rendered, encoding="utf-8")
    print(json.dumps(payload["summary"], indent=2, sort_keys=True))
    if args.require_pilot_ready and not payload["summary"]["act1_pilot_ready"]:
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
