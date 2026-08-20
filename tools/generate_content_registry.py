"""Regenerate canonical content IDs from the committed simulator headers."""

from __future__ import annotations

import hashlib
import json
import re
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
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


def main() -> int:
    categories = {}
    hashes = {}
    for category, (filename, enum_name) in SOURCES.items():
        path = HEADERS / filename
        categories[category] = enum_items(path, enum_name)
        hashes[filename] = hashlib.sha256(path.read_bytes()).hexdigest()
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
