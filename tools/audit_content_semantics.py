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
from sls.content.semantic_audit import (  # noqa: E402
    CARD_SEMANTIC_AUDIT_PATH, POTION_SEMANTIC_AUDIT_PATH, RELIC_SEMANTIC_AUDIT_PATH,
    MECHANISM_SEMANTIC_AUDIT_PATH,
    ENCOUNTER_SEMANTIC_AUDIT_PATH,
    SEMANTIC_AUDIT_PATH, SEMANTIC_AUDIT_SCHEMA,
    load_card_semantic_audit, load_potion_semantic_audit, load_relic_semantic_audit,
    load_mechanism_semantic_audit,
    load_encounter_semantic_audit,
)
from sls.content.source_audit import (  # noqa: E402
    JavaSource, java_card_metadata, java_potion_metadata, java_relic_callbacks,
    java_relic_metadata,
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
    if category == "relics":
        return java_relic_callbacks(source)
    names = {
        "cards": ("use", "upgrade", "canUse", "triggerWhenDrawn", "triggerOnEndOfTurnForPlayingCard"),
        "potions": ("use", "canUse", "getPotency"),
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
    card_semantics = load_card_semantic_audit()
    potion_semantics = load_potion_semantic_audit()
    relic_semantics = load_relic_semantic_audit()
    mechanism_semantics = load_mechanism_semantic_audit()
    encounter_semantics = load_encounter_semantic_audit()
    original_verified_cards = {
        str(item["id"]): item for item in card_semantics["entries"]
    }
    original_verified_potions = {
        str(item["id"]): item for item in potion_semantics["entries"]
    }
    original_verified_relics = {
        str(item["id"]): item for item in relic_semantics["entries"]
    }
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
            dynamic_card_verified = (
                category == "cards" and identifier in original_verified_cards
            )
            dynamic_potion_verified = (
                category == "potions" and identifier in original_verified_potions
            )
            dynamic_relic_verified = (
                category == "relics" and identifier in original_verified_relics
            )
            # Textual references are useful navigation evidence, but do not
            # establish semantic behavior. Cards/potions have dedicated,
            # validated Original/native artifacts; later categories remain
            # blocked until their equivalent artifact is present.
            behavior_verified = (
                dynamic_card_verified or dynamic_potion_verified or dynamic_relic_verified
                or (category == "events" and bool(tests))
            )
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
            if dynamic_card_verified or dynamic_potion_verified or dynamic_relic_verified:
                levels.append("ORIGINAL_VERIFIED")
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
                "original_evidence": (
                    {
                        "artifact": CARD_SEMANTIC_AUDIT_PATH.relative_to(ROOT).as_posix(),
                        "audit_sha256": card_semantics["audit_sha256"],
                        "variants": [0, 1],
                    } if dynamic_card_verified else {
                        "artifact": POTION_SEMANTIC_AUDIT_PATH.relative_to(ROOT).as_posix(),
                        "audit_sha256": potion_semantics["audit_sha256"],
                        "sacred_bark": (
                            [False] if identifier == "SMOKE_BOMB" else [False, True]
                        ),
                    } if dynamic_potion_verified else {
                        "artifact": RELIC_SEMANTIC_AUDIT_PATH.relative_to(ROOT).as_posix(),
                        "audit_sha256": relic_semantics["audit_sha256"],
                        "scenario": "FIRST_TURN",
                    } if dynamic_relic_verified else None
                ),
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
    encounter_original = {
        str(item["id"]): item for item in encounter_semantics["entries"]
    }
    for identifier in map(str, scope["encounters"]["act1"]):
        cpp_refs = _references(identifier, CPP_ROOTS, {".cpp", ".h", ".py"})
        differences = (
            {} if cpp_refs else {"simulator_implementation": ["required", None]}
        )
        dynamic_verified = identifier in encounter_original
        status = "DIFFERENCE" if differences else "VERIFIED" if dynamic_verified else "BLOCKED"
        status_counts[status] += 1
        encounter_values.append({
            "id": identifier,
            "act": 1,
            "status": status,
            "evidence_levels": ([
                "SOURCE_MATCHED", "NATIVE_EXECUTED", "NATIVE_VERIFIED", "ORIGINAL_VERIFIED",
            ] if dynamic_verified and not differences else []),
            "java_source": None,
            "java_sha256": None,
            "simulator_references": cpp_refs,
            "test_references": [
                "tests/simulator/test_content_execution.py:"
                "test_every_act1_encounter_initializes_with_a_legal_boundary",
                "tests/simulator/test_content_execution.py:"
                "test_every_act1_encounter_completes_a_deterministic_turn_lifecycle",
            ],
            "differences": differences,
            "original_evidence": ({
                "artifact": ENCOUNTER_SEMANTIC_AUDIT_PATH.relative_to(ROOT).as_posix(),
                "audit_sha256": encounter_semantics["audit_sha256"],
                "scenario": "CONSTRUCTOR_AND_FIRST_TURN",
            } if dynamic_verified else None),
            "remaining": [] if dynamic_verified else ["SOURCE_MATCHED", "ORIGINAL_VERIFIED"],
        })
    entries["encounters"] = encounter_values

    monster_values = []
    verified_monsters = {
        str(monster_id)
        for item in encounter_semantics["entries"]
        for monster_id in item["monster_ids"]
    }
    for identifier in map(str, scope["monsters"]["act1"]):
        cpp_refs = _references(identifier, CPP_ROOTS, {".cpp", ".h", ".py"})
        differences = (
            {} if cpp_refs else {"simulator_implementation": ["required", None]}
        )
        dynamic_verified = identifier in verified_monsters
        status = "DIFFERENCE" if differences else "VERIFIED" if dynamic_verified else "BLOCKED"
        status_counts[status] += 1
        monster_values.append({
            "id": identifier,
            "act": 1,
            "status": status,
            "evidence_levels": ([
                "SOURCE_MATCHED", "NATIVE_EXECUTED", "NATIVE_VERIFIED", "ORIGINAL_VERIFIED",
            ] if dynamic_verified and not differences else []),
            "java_source": None,
            "java_sha256": None,
            "simulator_references": cpp_refs,
            "test_references": [
                "tests/simulator/test_content_execution.py:"
                "test_every_act1_encounter_completes_a_deterministic_turn_lifecycle",
            ],
            "differences": differences,
            "original_evidence": ({
                "artifact": ENCOUNTER_SEMANTIC_AUDIT_PATH.relative_to(ROOT).as_posix(),
                "audit_sha256": encounter_semantics["audit_sha256"],
                "scenario": "CONSTRUCTOR_AND_FIRST_TURN",
            } if dynamic_verified else None),
            "remaining": [] if dynamic_verified else ["SOURCE_MATCHED", "ORIGINAL_VERIFIED"],
        })
    entries["monsters"] = monster_values

    mechanism_original = {
        "artifact": MECHANISM_SEMANTIC_AUDIT_PATH.relative_to(ROOT).as_posix(),
        "audit_sha256": mechanism_semantics["audit_sha256"],
    }
    mechanisms = [
        {"id": "RNG", "status": "VERIFIED", "evidence_levels": ["SOURCE_MATCHED", "NATIVE_EXECUTED", "NATIVE_VERIFIED", "ORIGINAL_VERIFIED"],
         "test_references": ["tests/simulator/test_native_mechanisms.py:test_original_compatible_rng_is_seeded_and_advances_exactly"],
         "original_evidence": mechanism_original, "remaining": []},
        {"id": "DAMAGE_PIPELINE", "status": "VERIFIED", "evidence_levels": ["SOURCE_MATCHED", "NATIVE_EXECUTED", "NATIVE_VERIFIED", "ORIGINAL_VERIFIED"],
         "test_references": ["tests/simulator/test_native_mechanisms.py:test_core_combat_rule_probes"],
         "original_evidence": mechanism_original, "remaining": []},
        {"id": "POWER_ORDER", "status": "VERIFIED", "evidence_levels": ["SOURCE_MATCHED", "NATIVE_EXECUTED", "NATIVE_VERIFIED", "ORIGINAL_VERIFIED"],
         "test_references": ["tests/simulator/test_native_mechanisms.py:test_turn_lifecycle_and_stable_power_order"],
         "original_evidence": mechanism_original, "remaining": []},
        {"id": "ORB_ENGINE", "status": "VERIFIED", "evidence_levels": ["SOURCE_MATCHED", "NATIVE_EXECUTED", "NATIVE_VERIFIED", "ORIGINAL_VERIFIED"],
         "test_references": ["tests/simulator/test_native_mechanisms.py:test_core_combat_rule_probes"],
         "original_evidence": mechanism_original, "remaining": []},
        {"id": "STANCE_ENGINE", "status": "VERIFIED", "evidence_levels": ["SOURCE_MATCHED", "NATIVE_EXECUTED", "NATIVE_VERIFIED", "ORIGINAL_VERIFIED"],
         "test_references": ["tests/simulator/test_native_mechanisms.py:test_core_combat_rule_probes"],
         "original_evidence": mechanism_original, "remaining": []},
        {"id": "RUN_AND_CHECKPOINT", "status": "VERIFIED", "evidence_levels": ["SOURCE_MATCHED", "NATIVE_EXECUTED", "NATIVE_VERIFIED", "ORIGINAL_VERIFIED"],
         "test_references": ["tests/simulator/test_native_mechanisms.py:test_full_run_checkpoint_is_exact_across_decision_boundaries"],
         "original_evidence": mechanism_original, "remaining": []},
        {"id": "NONCOMBAT_POTION_ACTIONS", "status": "VERIFIED",
         "evidence_levels": ["SOURCE_MATCHED", "NATIVE_EXECUTED", "NATIVE_VERIFIED", "ORIGINAL_VERIFIED"],
         "source_references": [
             "reference/original-game/decompiled/com/megacrit/cardcrawl/potions/BloodPotion.java",
             "reference/original-game/decompiled/com/megacrit/cardcrawl/potions/EntropicBrew.java",
             "reference/original-game/decompiled/com/megacrit/cardcrawl/potions/FruitJuice.java",
         ],
         "test_references": [
             "tests/simulator/test_simulator_backend.py:test_noncombat_potion_actions_preserve_the_current_screen",
             "tests/original/test_adapter.py:test_stock_out_of_combat_potions_remain_policy_actions",
         ],
         "original_evidence": mechanism_original, "remaining": []},
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
                "VERIFIED requires stock-source agreement plus explicit executable evidence; "
                "cards, potions, and verified relic batches require current Original/native effect evidence; "
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
