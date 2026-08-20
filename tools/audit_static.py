"""Machine-checkable static audit of the canonical simulator boundary."""

from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path
import re
from typing import Any


ROOT = Path(__file__).resolve().parents[1]
REGISTRY = ROOT / "src" / "sls" / "content" / "registry.json"
JAVA = ROOT / "reference" / "original-game" / "decompiled"
CONSTANTS = ROOT / "cpp" / "simulator" / "include" / "constants"
BATTLE = ROOT / "cpp" / "simulator" / "src" / "combat" / "BattleContext.cpp"
CARD_POOLS = CONSTANTS / "CardPools.h"


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
            "java_id_missing": missing_java,
            "ironclad_playable_cards": len(ironclad),
            "ironclad_missing_use_cases": missing_ironclad,
            "colorless_reward_cards": len(colorless),
            "colorless_missing_use_cases": missing_colorless,
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
