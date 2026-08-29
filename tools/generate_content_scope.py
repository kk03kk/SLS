"""Generate the deterministic Ironclad A0 reachable-content contract."""

from __future__ import annotations

import argparse
import json
from pathlib import Path
import re
import sys
from typing import Any


ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "src"))

from sls.content.scope import (  # noqa: E402
    CONTENT_SCOPE_SCHEMA,
    IRONCLAD_A0_SCOPE_ID,
    IRONCLAD_A0_SCOPE_PATH,
    canonical_scope_digest,
)
from sls.rl.training_contract import source_sha256  # noqa: E402


CONSTANTS = ROOT / "cpp" / "simulator" / "include" / "constants"
JAVA = ROOT / "reference" / "original-game" / "decompiled"
REGISTRY = ROOT / "src" / "sls" / "content" / "registry.json"

IRONCLAD_A0_SPECIAL_CARDS = {"APPARITION", "BITE", "JAX", "RITUAL_DAGGER"}
IRONCLAD_A0_STATUSES = {"BURN", "DAZED", "SLIMED", "WOUND"}
IRONCLAD_A0_CURSES = {
    "CLUMSY", "CURSE_OF_THE_BELL", "DECAY", "DOUBT", "INJURY",
    "NECRONOMICURSE", "NORMALITY", "PAIN", "PARASITE", "REGRET",
    "SHAME", "WRITHE",
}


def _sha256(path: Path) -> str:
    return source_sha256(path)


def _block(source: str, pattern: str, *, label: str) -> str:
    match = re.search(pattern, source, re.DOTALL)
    if match is None:
        raise RuntimeError(f"cannot locate {label}")
    return match.group(1)


def _enum_ids(block: str, kind: str) -> set[str]:
    return set(re.findall(rf"{re.escape(kind)}::([A-Z0-9_]+)", block))


def _brace_body(source: str, opening_brace: int) -> str:
    depth = 0
    for index in range(opening_brace, len(source)):
        if source[index] == "{":
            depth += 1
        elif source[index] == "}":
            depth -= 1
            if depth == 0:
                return source[opening_brace + 1:index]
    raise RuntimeError("unterminated C++ function body")


def _act1_monsters(encounters: set[str]) -> set[str]:
    source = (ROOT / "cpp" / "simulator" / "src" / "combat" / "MonsterGroup.cpp").read_text(
        encoding="utf-8"
    )
    create_start = source.index("void MonsterGroup::createMonsters")
    create_body = _brace_body(source, source.index("{", create_start))
    cases = list(re.finditer(r"case MonsterEncounter::([A-Z0-9_]+)\s*:", create_body))
    selected = []
    for index, match in enumerate(cases):
        if match.group(1) not in encounters:
            continue
        end = cases[index + 1].start() if index + 1 < len(cases) else len(create_body)
        selected.append(create_body[match.end():end])

    function_cache: dict[str, str] = {}
    def function_body(name: str) -> str | None:
        if name in function_cache:
            return function_cache[name]
        match = re.search(
            rf"(?:void|MonsterId)\s+MonsterGroup::{re.escape(name)}\s*\([^)]*\)\s*\{{",
            source,
        )
        if match is None:
            return None
        body = _brace_body(source, match.end() - 1)
        function_cache[name] = body
        return body

    pending = list(selected)
    visited: set[str] = set()
    monsters: set[str] = set()
    while pending:
        body = pending.pop()
        monsters |= _enum_ids(body, "MonsterId")
        for name in re.findall(r"\b([A-Za-z][A-Za-z0-9_]*)\s*\(", body):
            if name in visited:
                continue
            nested = function_body(name)
            if nested is not None:
                visited.add(name)
                pending.append(nested)
    monsters.discard("INVALID")
    return monsters


def _card_metadata() -> dict[str, dict[str, str]]:
    source = (CONSTANTS / "Cards.h").read_text(encoding="utf-8")
    registry = json.loads(REGISTRY.read_text(encoding="utf-8"))["categories"]["cards"]
    by_ordinal = {int(item["ordinal"]): str(item["id"]) for item in registry}
    arrays: dict[str, list[str]] = {}
    for name, array_name, enum in (
        ("color", "cardColors", "CardColor"),
        ("rarity", "cardRarities", "CardRarity"),
        ("type", "cardTypes", "CardType"),
    ):
        raw = _block(
            source, rf"{array_name}\[\]\s*=\s*\{{(.*?)\}};",
            label=f"card {name} table",
        )
        arrays[name] = re.findall(rf"{enum}::([A-Z_]+)", raw)
    if not all(len(values) == len(by_ordinal) + 1 for values in arrays.values()):
        raise RuntimeError("card metadata tables do not match the content registry")
    metadata = {
        by_ordinal[ordinal]: {name: arrays[name][ordinal] for name in arrays}
        for ordinal in sorted(by_ordinal)
    }
    # Match the public accessor in Cards.h.  The inherited generated table has
    # two rotated color triplets and must not be used directly for reachability.
    for card_id in ("BRUTALITY", "COMBUST"):
        metadata[card_id]["color"] = "RED"
    for card_id in ("BUFFER", "COMPILE_DRIVER"):
        metadata[card_id]["color"] = "BLUE"
    for card_id in ("BULLET_TIME", "CONCENTRATE"):
        metadata[card_id]["color"] = "GREEN"
    for card_id in ("BRILLIANCE", "COLLECT"):
        metadata[card_id]["color"] = "PURPLE"
    return metadata


def _ironclad_relics() -> set[str]:
    source = (CONSTANTS / "RelicPools.h").read_text(encoding="utf-8")
    ironclad = _block(
        source, r"namespace Ironclad\s*\{(.*?)\n\s*\};\s*\n\s*namespace Silent",
        label="Ironclad relic pools",
    )
    pooled = _enum_ids(ironclad, "RelicId")
    # Stock special/event relics are shared by all characters.  RED_CIRCLET is
    # the stock fallback when a relic pool is exhausted and is therefore part
    # of the full-run reachable closure as well.
    java_special: set[str] = set()
    registry = json.loads(REGISTRY.read_text(encoding="utf-8"))["categories"]["relics"]
    game_to_id = {str(item["game_id"]): str(item["id"]) for item in registry}
    for path in (JAVA / "com" / "megacrit" / "cardcrawl" / "relics").glob("*.java"):
        text = path.read_text(encoding="utf-8", errors="replace")
        identifier = re.search(r'public static final String ID\s*=\s*"([^"]+)"', text)
        tier = re.search(r"AbstractRelic\.RelicTier\.([A-Z_]+)", text)
        if identifier and tier and tier.group(1) == "SPECIAL":
            canonical = game_to_id.get(identifier.group(1))
            if canonical is None:
                raise RuntimeError(f"special relic is absent from registry: {identifier.group(1)}")
            java_special.add(canonical)
    return pooled | java_special | {"BURNING_BLOOD"}


def build_scope() -> dict[str, Any]:
    card_source = (CONSTANTS / "CardPools.h").read_text(encoding="utf-8")
    metadata = _card_metadata()
    red = {card_id for card_id, item in metadata.items() if item["color"] == "RED"}
    colorless = _enum_ids(_block(
        card_source,
        r"namespace ColorlessRarityCardPool\s*\{.*?colorlessCardBlob\[\]\s*\{(.*?)\};",
        label="colorless reward pool",
    ), "CardId")
    cards = red | colorless | IRONCLAD_A0_SPECIAL_CARDS | IRONCLAD_A0_STATUSES | IRONCLAD_A0_CURSES

    potion_source = (CONSTANTS / "Potions.h").read_text(encoding="utf-8")
    potions = _enum_ids(_block(
        potion_source,
        r"potionPool\[4\]\[33\]\s*\{\s*\{(.*?)\}\s*,",
        label="Ironclad potion pool",
    ), "Potion")

    event_source = (CONSTANTS / "Events.h").read_text(encoding="utf-8")
    events = _enum_ids(
        event_source.split("namespace EventPools", 1)[1], "Event",
    ) | {"NEOW"}
    one_time_events = _enum_ids(_block(
        event_source,
        r"oneTimeEventsAsc0\s*\{(.*?)\};",
        label="A0 one-time event pool",
    ), "Event")
    act1_event_block = _block(
        event_source,
        r"namespace Act1\s*\{(.*?)\n\s*\}",
        label="Act 1 event pools",
    )
    act1_events = _enum_ids(_block(
        act1_event_block, r"events\s*\{(.*?)\};", label="Act 1 events",
    ), "Event")
    act1_shrines = _enum_ids(_block(
        act1_event_block, r"shrines\s*\{(.*?)\};", label="Act 1 shrines",
    ), "Event")

    encounter_source = (CONSTANTS / "MonsterEncounters.h").read_text(encoding="utf-8")
    act1_encounters = set(re.findall(
        r"^\s*([A-Z][A-Z0-9_]+)\s*,?\s*$",
        _block(
            encounter_source,
            r"// Act 1 Weak\s*(.*?)\s*// Act 2",
            label="Act 1 encounters",
        ),
        re.MULTILINE,
    ))

    act1_monsters = _act1_monsters(act1_encounters)
    source_files = (
        CONSTANTS / "Cards.h", CONSTANTS / "CardPools.h",
        CONSTANTS / "Potions.h", CONSTANTS / "Relics.h",
        CONSTANTS / "RelicPools.h", CONSTANTS / "Events.h", REGISTRY,
        CONSTANTS / "MonsterEncounters.h", CONSTANTS / "MonsterIds.h",
        ROOT / "cpp" / "simulator" / "src" / "combat" / "MonsterGroup.cpp",
    )
    payload: dict[str, Any] = {
        "schema": CONTENT_SCOPE_SCHEMA,
        "scope_id": IRONCLAD_A0_SCOPE_ID,
        "character": "IRONCLAD",
        "ascension": 0,
        "policy_excluded_content_ids": ["PRISMATIC_SHARD"],
        "cards": {
            "ids": sorted(cards),
            "red": sorted(red),
            "colorless_reward": sorted(colorless),
            "special_event": sorted(IRONCLAD_A0_SPECIAL_CARDS),
            "statuses": sorted(IRONCLAD_A0_STATUSES),
            "curses": sorted(IRONCLAD_A0_CURSES),
        },
        "potions": {"ids": sorted(potions)},
        "relics": {"ids": sorted(_ironclad_relics())},
        "events": {
            "ids": sorted(events),
            "act1_base": sorted(act1_events),
            "act1_shrines": sorted(act1_shrines),
            "a0_one_time_candidates": sorted(one_time_events),
        },
        "encounters": {"act1": sorted(act1_encounters)},
        "monsters": {"act1": sorted(act1_monsters)},
        "shared_engine_primitives": ["DAMAGE_PIPELINE", "ORBS", "POWERS", "RNG", "STANCES"],
        "source_sha256": {
            str(path.relative_to(ROOT)).replace("\\", "/"): _sha256(path)
            for path in source_files
        },
    }
    payload["scope_sha256"] = canonical_scope_digest(payload)
    return payload


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true")
    parser.add_argument("--output", type=Path, default=IRONCLAD_A0_SCOPE_PATH)
    args = parser.parse_args()
    rendered = json.dumps(build_scope(), indent=2, sort_keys=True) + "\n"
    if args.check:
        if not args.output.exists() or args.output.read_text(encoding="utf-8") != rendered:
            print(f"stale content scope: {args.output}", file=sys.stderr)
            return 1
        return 0
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(rendered, encoding="utf-8")
    print(args.output)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
