"""Regenerate canonical content IDs from the committed simulator headers."""

from __future__ import annotations

import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "src"))

from sls.rl.training_contract import source_sha256  # noqa: E402

HEADERS = ROOT / "cpp" / "simulator" / "include" / "constants"
OUTPUT = ROOT / "src" / "sls" / "content" / "registry.json"
SOURCES = {
    "characters": ("CharacterClasses.h", "CharacterClass"),
    "cards": ("Cards.h", "CardId"),
    "relics": ("Relics.h", "RelicId"),
    "potions": ("Potions.h", "Potion"),
    "monsters": ("MonsterIds.h", "MonsterId"),
    "encounters": ("MonsterEncounters.h", "MonsterEncounter"),
    "events": ("Events.h", "Event"),
}

GAME_ID_ARRAYS = {
    "cards": ("Cards.h", "cardStringIds"),
    "relics": ("Relics.h", "relicIds"),
    "potions": ("Potions.h", "potionIds"),
    "monsters": ("MonsterIds.h", "monsterIdStrings"),
    "encounters": ("MonsterEncounters.h", "monsterEncounterStrings"),
    "events": ("Events.h", "eventIdStrings"),
}


def enum_items(path: Path, enum_name: str) -> list[dict[str, int | str]]:
    source = path.read_text(encoding="utf-8")
    match = re.search(
        rf"enum\s+class\s+{re.escape(enum_name)}\b[^{{]*\{{(.*?)\}};",
        source,
        re.DOTALL,
    )
    if match is None:
        raise RuntimeError(f"cannot find enum {enum_name} in {path}")
    body = re.sub(r"//.*", "", match.group(1))
    result: list[dict[str, int | str]] = []
    ordinal = 0
    for raw in body.split(","):
        token = raw.strip()
        if not token:
            continue
        name, separator, assigned = token.partition("=")
        name = name.strip()
        if separator:
            ordinal = int(assigned.strip(), 0)
        if name != "INVALID":
            result.append({"id": name, "ordinal": ordinal})
        ordinal += 1
    return result


def string_array(path: Path, array_name: str) -> list[str]:
    source = path.read_text(encoding="utf-8")
    match = re.search(
        rf"\b{re.escape(array_name)}\s*\[\s*\]\s*(?:=\s*)?\{{(.*?)\}};",
        source,
        re.DOTALL,
    )
    if match is None:
        raise RuntimeError(f"cannot find string array {array_name} in {path}")
    return [
        bytes(value, "utf-8").decode("unicode_escape")
        for value in re.findall(r'"((?:\\.|[^"\\])*)"', match.group(1))
    ]


def main() -> int:
    categories = {}
    hashes = {}
    for category, (filename, enum_name) in SOURCES.items():
        path = HEADERS / filename
        categories[category] = enum_items(path, enum_name)
        hashes[filename] = source_sha256(path)
    for category, (filename, array_name) in GAME_ID_ARRAYS.items():
        path = HEADERS / filename
        values = string_array(path, array_name)
        for item in categories[category]:
            ordinal = int(item["ordinal"])
            if ordinal >= len(values):
                raise RuntimeError(
                    f"{array_name} has no entry for {category} ordinal {ordinal}"
                )
            item["game_id"] = values[ordinal]
    payload = {
        "schema_version": 1,
        "source": {
            "simulator_manifest": "cpp/simulator/SLS_VENDOR.json",
            "header_sha256": hashes,
        },
        "categories": categories,
    }
    OUTPUT.write_text(
        json.dumps(payload, indent=2, sort_keys=True) + "\n", encoding="utf-8"
    )
    print(OUTPUT)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
