"""Machine-checkable static audit of the canonical simulator boundary."""

from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path
import re
import sys
from typing import Any


ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "src"))
REGISTRY = ROOT / "src" / "sls" / "content" / "registry.json"
JAVA = ROOT / "reference" / "original-game" / "decompiled"
CONSTANTS = ROOT / "cpp" / "simulator" / "include" / "constants"
BATTLE = ROOT / "cpp" / "simulator" / "src" / "combat" / "BattleContext.cpp"
CARD_POOLS = CONSTANTS / "CardPools.h"
RELIC_POOLS = CONSTANTS / "RelicPools.h"
POTIONS = CONSTANTS / "Potions.h"
EVENTS = CONSTANTS / "Events.h"
GAME_CONTEXT = ROOT / "cpp" / "simulator" / "src" / "game" / "GameContext.cpp"
POLICY_VOCABULARY = ROOT / "configs" / "model" / "policy_vocabulary_v2.json"
NATIVE_MODULE = ROOT / "cpp" / "simulator" / "python" / "module.cpp"

IRONCLAD_REACHABLE_STATUSES = {"BURN", "DAZED", "SLIMED", "VOID", "WOUND"}
IRONCLAD_REACHABLE_CURSES = {
    "ASCENDERS_BANE", "CLUMSY", "CURSE_OF_THE_BELL", "DECAY", "DOUBT",
    "INJURY", "NECRONOMICURSE", "NORMALITY", "PAIN", "PARASITE", "PRIDE",
    "REGRET", "SHAME", "WRITHE",
}


def _java_ids(relative: str, field: str = "ID") -> set[str]:
    pattern = re.compile(
        rf"public static final String {re.escape(field)}\s*=\s*\"([^\"]+)\""
    )
    result: set[str] = set()
    for path in (JAVA / "com" / "megacrit" / "cardcrawl" / relative).rglob("*.java"):
        match = pattern.search(path.read_text(encoding="utf-8"))
        if match:
            result.add(match.group(1))
    return result


def _pool_ids(source: str, pattern: str) -> set[str]:
    match = re.search(pattern, source, re.DOTALL)
    if match is None:
        raise RuntimeError(f"cannot locate canonical card pool: {pattern}")
    return set(re.findall(r"CardId::([A-Z0-9_]+)", match.group(1)))


def audit() -> dict[str, Any]:
    registry = json.loads(REGISTRY.read_text(encoding="utf-8"))
    categories = registry["categories"]
    failures: list[str] = []

    from sls.model.encoding import build_policy_vocabulary
    expected_vocabulary = build_policy_vocabulary()
    actual_vocabulary = (
        json.loads(POLICY_VOCABULARY.read_text(encoding="utf-8"))
        if POLICY_VOCABULARY.exists() else None
    )
    if actual_vocabulary != expected_vocabulary:
        failures.append("policy vocabulary is stale or missing")
    vocabulary_unique = bool(actual_vocabulary) and all(
        len(values) == len(set(values))
        for key in ("content", "categorical")
        if isinstance((values := actual_vocabulary.get(key)), list)
    )
    if not vocabulary_unique:
        failures.append("policy vocabulary contains token collisions")
    native_intents = set(re.findall(
        r'return\s+"([A-Z_]+)"',
        NATIVE_MODULE.read_text(encoding="utf-8"),
    ))
    missing_native_intents = sorted(
        native_intents - set((actual_vocabulary or {}).get("categorical", []))
    )
    if missing_native_intents:
        failures.append("native monster intents are missing from policy vocabulary")

    hash_mismatches: list[str] = []
    for filename, expected in registry["source"]["header_sha256"].items():
        actual = hashlib.sha256((CONSTANTS / filename).read_bytes()).hexdigest()
        if actual != expected:
            hash_mismatches.append(filename)
    if hash_mismatches:
        failures.append("content registry is stale")

    java_cards = _java_ids("cards")
    java_relics = _java_ids("relics")
    java_potions = _java_ids("potions", "POTION_ID")
    missing_java = {
        "cards": sorted(
            item["game_id"] for item in categories["cards"]
            if item.get("game_id") not in java_cards
        ),
        "relics": sorted(
            item["game_id"] for item in categories["relics"]
            if item.get("game_id") not in java_relics
        ),
        "potions": sorted(
            item["game_id"] for item in categories["potions"]
            if item["id"] != "EMPTY_POTION_SLOT"
            and item.get("game_id") not in java_potions
        ),
    }
    if any(missing_java.values()):
        failures.append("C++ content IDs are missing from decompiled Java")

    pool_source = CARD_POOLS.read_text(encoding="utf-8")
    ironclad = _pool_ids(
        pool_source,
        r"colorCardPool\[4\]\[72\]\s*\{\s*\{(.*?)\}\s*,",
    ) | {"STRIKE_RED", "DEFEND_RED", "BASH"}
    colorless = _pool_ids(
        pool_source,
        r"ColorlessRarityCardPool.*?colorlessCardBlob\[\]\s*\{(.*?)\};",
    )
    battle_source = BATTLE.read_text(encoding="utf-8")
    explicit_cases = set(re.findall(r"case\s+CardId::([A-Z0-9_]+)", battle_source))
    missing_ironclad = sorted(ironclad - explicit_cases)
    missing_colorless = sorted(colorless - explicit_cases)
    if missing_ironclad:
        failures.append("playable Ironclad cards lack an implementation case")
    if missing_colorless:
        failures.append("obtainable colorless cards lack an implementation case")

    relic_source = RELIC_POOLS.read_text(encoding="utf-8")
    ironclad_relic_source = relic_source.split("namespace Ironclad", 1)[1].split(
        "namespace Silent", 1
    )[0]
    ironclad_relic_pool = set(re.findall(
        r"RelicId::([A-Z0-9_]+)", ironclad_relic_source
    ))

    potion_source = POTIONS.read_text(encoding="utf-8")
    ironclad_potion_source = potion_source.split(
        "static constexpr Potion potionPool", 1
    )[1].split("},", 1)[0]
    ironclad_potions = set(re.findall(
        r"Potion::([A-Z0-9_]+)", ironclad_potion_source
    ))
    potion_cases = set(re.findall(
        r"case\s+Potion::([A-Z0-9_]+)", battle_source
    ))
    # Fairy in a Bottle triggers from the lethal-damage pipeline, not usePotion.
    missing_potion_use_paths = sorted(
        ironclad_potions - potion_cases - {"FAIRY_POTION"}
    )
    if missing_potion_use_paths:
        failures.append("Ironclad potion pool lacks an execution path")

    event_source = EVENTS.read_text(encoding="utf-8")
    pooled_events = set(re.findall(r"Event::([A-Z0-9_]+)", event_source.split(
        "namespace EventPools", 1
    )[1]))
    game_source = GAME_CONTEXT.read_text(encoding="utf-8")
    event_choice_cases = set(re.findall(
        r"case\s+Event::([A-Z0-9_]+)",
        game_source.split("void GameContext::chooseEventOption", 1)[1],
    ))
    # Bonfire immediately enters its card-selection continuation and therefore
    # intentionally has no chooseEventOption switch arm.
    missing_event_choice_paths = sorted(
        pooled_events - event_choice_cases - {"BONFIRE_SPIRITS"}
    )
    if missing_event_choice_paths:
        failures.append("pooled events lack a choice execution path")

    first_party_roots = [ROOT / value for value in ("src", "tools", "tests", "java", "docs")]
    historical_markers: list[str] = []
    marker = re.compile(r"five.?fight|mini.?run", re.IGNORECASE)
    for directory in first_party_roots:
        for path in directory.rglob("*"):
            if path.is_file() and path.suffix.lower() in {".py", ".java", ".md"}:
                if path.resolve() == Path(__file__).resolve():
                    continue
                if marker.search(path.read_text(encoding="utf-8", errors="replace")):
                    historical_markers.append(str(path.relative_to(ROOT)))
    if historical_markers:
        failures.append("historical FiveFight/mini-run code remains")

    all_cards = {str(item["id"]) for item in categories["cards"]}
    no_explicit_case = sorted(all_cards - explicit_cases)
    game_header = (ROOT / "cpp" / "simulator" / "include" / "game" / "GameContext.h")
    prismatic_disabled = "disablePrismaticShard = true" in game_header.read_text(
        encoding="utf-8"
    )

    return {
        "status": "pass" if not failures else "fail",
        "failures": failures,
        "evidence": {
            "reference_java_files": sum(1 for _ in JAVA.rglob("*.java")),
            "registry_header_hash_mismatches": hash_mismatches,
            "registry_counts": {
                category: len(items) for category, items in categories.items()
            },
            "policy_vocabulary_sha256": expected_vocabulary["sha256"],
            "policy_vocabulary_content_tokens": len(expected_vocabulary["content"]),
        "policy_vocabulary_unique": vocabulary_unique,
        "native_intents_missing_from_policy_vocabulary": missing_native_intents,
            "java_id_missing": missing_java,
            "ironclad_playable_cards": len(ironclad),
            "ironclad_missing_use_cases": missing_ironclad,
            "colorless_reward_cards": len(colorless),
            "colorless_missing_use_cases": missing_colorless,
            "ironclad_relic_pool": len(ironclad_relic_pool),
            "ironclad_potion_pool": len(ironclad_potions),
            "ironclad_potions_missing_use_paths": missing_potion_use_paths,
            "ironclad_reachable_statuses": sorted(IRONCLAD_REACHABLE_STATUSES),
            "ironclad_reachable_curses": sorted(IRONCLAD_REACHABLE_CURSES),
            "pooled_events": len(pooled_events),
            "pooled_events_missing_choice_paths": missing_event_choice_paths,
            "historical_markers": historical_markers,
            "assert_false_sites": len(re.findall(r"assert\s*\(\s*false\s*\)", (
                "\n".join(
                    path.read_text(encoding="utf-8", errors="replace")
                    for path in (ROOT / "cpp" / "simulator").rglob("*.cpp")
                )
            ))),
        },
        "known_static_gaps": {
            "prismatic_shard_card_pool_disabled": prismatic_disabled,
            "cards_without_explicit_use_switch_case": len(no_explicit_case),
            "note": (
                "Status/curse lifecycle cards do not all require a use switch case. "
                "Non-Ironclad colored cards are outside the current executable contract."
            ),
        },
        "dynamic_gate": (
            "FullRun equivalence still requires Original Game + CommunicationMod."
        ),
        "claim_scope": (
            "A pass means only that local static and regression gates succeeded; "
            "it is not evidence of Original Game parity or complete behavioral fidelity."
        ),
    }


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--output", type=Path)
    args = parser.parse_args()
    report = audit()
    rendered = json.dumps(report, indent=2, sort_keys=True) + "\n"
    if args.output:
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(rendered, encoding="utf-8")
    print(rendered, end="")
    return 0 if report["status"] == "pass" else 1


if __name__ == "__main__":
    raise SystemExit(main())
