"""Build the versioned base-game content registry from pinned sts_lightspeed headers."""

from __future__ import annotations

import argparse
import hashlib
import json
import re
import subprocess
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
DEFAULT_HEADERS = ROOT / ".native-build" / "sts_lightspeed" / "include" / "constants"
DEFAULT_OUTPUT = ROOT / "spirecomm" / "content" / "registry.json"
REFERENCE_BUILD = ROOT / "reference_build.json"

SOURCES = {
    "characters": ("CharacterClasses.h", "CharacterClass"),
    "cards": ("Cards.h", "CardId"),
    "relics": ("Relics.h", "RelicId"),
    "potions": ("Potions.h", "Potion"),
    "monsters": ("MonsterIds.h", "MonsterId"),
    "encounters": ("MonsterEncounters.h", "MonsterEncounter"),
    "events": ("Events.h", "Event"),
}

IRONCLAD_CARDS = {
    "ANGER", "CLEAVE", "WARCRY", "FLEX", "IRON_WAVE", "BODY_SLAM",
    "TRUE_GRIT", "SHRUG_IT_OFF", "CLASH", "THUNDERCLAP", "POMMEL_STRIKE",
    "TWIN_STRIKE", "CLOTHESLINE", "ARMAMENTS", "HAVOC", "HEADBUTT",
    "WILD_STRIKE", "HEAVY_BLADE", "PERFECTED_STRIKE", "SWORD_BOOMERANG",
    "EVOLVE", "UPPERCUT", "GHOSTLY_ARMOR", "FIRE_BREATHING", "DROPKICK",
    "CARNAGE", "BLOODLETTING", "RUPTURE", "SECOND_WIND", "SEARING_BLOW",
    "BATTLE_TRANCE", "SENTINEL", "ENTRENCH", "RAGE", "FEEL_NO_PAIN",
    "DISARM", "SEEING_RED", "DARK_EMBRACE", "COMBUST", "WHIRLWIND",
    "SEVER_SOUL", "RAMPAGE", "SHOCKWAVE", "METALLICIZE", "BURNING_PACT",
    "PUMMEL", "FLAME_BARRIER", "BLOOD_FOR_BLOOD", "INTIMIDATE",
    "HEMOKINESIS", "RECKLESS_CHARGE", "INFERNAL_BLADE", "DUAL_WIELD",
    "POWER_THROUGH", "INFLAME", "SPOT_WEAKNESS", "DOUBLE_TAP",
    "DEMON_FORM", "BLUDGEON", "FEED", "LIMIT_BREAK", "CORRUPTION",
    "BARRICADE", "FIEND_FIRE", "BERSERK", "IMPERVIOUS", "JUGGERNAUT",
    "BRUTALITY", "REAPER", "EXHUME", "OFFERING", "IMMOLATE",
    "STRIKE_RED", "DEFEND_RED", "BASH",
}

ACT1_ENCOUNTERS = {
    "CULTIST", "JAW_WORM", "TWO_LOUSE", "SMALL_SLIMES", "BLUE_SLAVER",
    "GREMLIN_GANG", "LOOTER", "LARGE_SLIME", "LOTS_OF_SLIMES",
    "EXORDIUM_THUGS", "EXORDIUM_WILDLIFE", "RED_SLAVER", "THREE_LOUSE",
    "TWO_FUNGI_BEASTS", "GREMLIN_NOB", "LAGAVULIN", "THREE_SENTRIES",
    "SLIME_BOSS", "THE_GUARDIAN", "HEXAGHOST",
}

ACT1_MONSTERS = {
    "ACID_SLIME_L", "ACID_SLIME_M", "ACID_SLIME_S", "BLUE_SLAVER",
    "CULTIST", "FAT_GREMLIN", "FUNGI_BEAST", "GREEN_LOUSE", "GREMLIN_NOB",
    "GREMLIN_WIZARD", "HEXAGHOST", "JAW_WORM", "LAGAVULIN", "LOOTER",
    "MAD_GREMLIN", "RED_LOUSE", "RED_SLAVER", "SENTRY", "SHIELD_GREMLIN",
    "SLIME_BOSS", "SNEAKY_GREMLIN", "SPIKE_SLIME_L", "SPIKE_SLIME_M",
    "SPIKE_SLIME_S", "THE_GUARDIAN",
}

IRONCLAD_POOL_POTIONS = {
    "ANCIENT_POTION", "ATTACK_POTION", "BLESSING_OF_THE_FORGE",
    "BLOCK_POTION", "BLOOD_POTION", "COLORLESS_POTION", "CULTIST_POTION",
    "DEXTERITY_POTION", "DISTILLED_CHAOS", "DUPLICATION_POTION",
    "ELIXIR_POTION", "ENERGY_POTION", "ENTROPIC_BREW", "ESSENCE_OF_STEEL",
    "EXPLOSIVE_POTION", "FAIRY_POTION", "FEAR_POTION", "FIRE_POTION",
    "FLEX_POTION", "FRUIT_JUICE", "GAMBLERS_BREW", "HEART_OF_IRON",
    "LIQUID_BRONZE", "LIQUID_MEMORIES", "POWER_POTION", "REGEN_POTION",
    "SKILL_POTION", "SMOKE_BOMB", "SNECKO_OIL", "SPEED_POTION",
    "STRENGTH_POTION", "SWIFT_POTION", "WEAK_POTION",
}

TESTED_RELICS = {
    "ANCHOR", "BAG_OF_PREPARATION", "HAPPY_FLOWER", "SACRED_BARK", "VAJRA",
}

# Only these inherited slices have project tests. "partial" deliberately does
# not claim original-game parity; that stronger evidence is recorded later.
PARTIAL = {
    "characters": {"IRONCLAD"},
    "cards": IRONCLAD_CARDS,
    "relics": TESTED_RELICS,
    "potions": IRONCLAD_POOL_POTIONS,
    "monsters": ACT1_MONSTERS,
    "encounters": ACT1_ENCOUNTERS,
}

EVIDENCE_FILES = {
    "characters": ["tests/test_act1_coverage.py"],
    "cards": [
        "tests/test_act1_coverage.py",
        "tests/test_ironclad_attacks.py",
        "tests/test_ironclad_skills.py",
        "tests/test_ironclad_powers.py",
        "tests/test_ironclad_choices.py",
    ],
    "relics": ["tests/test_relics.py"],
    "potions": ["tests/test_potions.py"],
    "monsters": [
        "tests/test_act1_normal_state_machines.py",
        "tests/test_act1_elite_state_machines.py",
        "tests/test_act1_boss_state_machines.py",
    ],
    "encounters": [
        "tests/test_act1_coverage.py",
        "tests/test_act1_normal_state_machines.py",
        "tests/test_act1_elite_state_machines.py",
        "tests/test_act1_boss_state_machines.py",
    ],
}


def _enum_body(text: str, enum_name: str) -> str:
    pattern = rf"enum(?:\s+class)?\s+{re.escape(enum_name)}(?:\s*:\s*[^{{]+)?\s*{{(.*?)}};"
    match = re.search(pattern, text, flags=re.DOTALL)
    if match is None:
        raise ValueError(f"enum {enum_name!r} not found")
    body = re.sub(r"//.*?$|/\*.*?\*/", "", match.group(1), flags=re.MULTILINE | re.DOTALL)
    return body


def parse_enum(path: Path, enum_name: str) -> list[tuple[str, int]]:
    """Parse a simple C++ integral enum without evaluating arbitrary code."""
    body = _enum_body(path.read_text(encoding="utf-8"), enum_name)
    result: list[tuple[str, int]] = []
    value = -1
    for raw in body.split(","):
        entry = raw.strip()
        if not entry:
            continue
        match = re.fullmatch(r"([A-Za-z_]\w*)\s*(?:=\s*(-?\d+))?", entry)
        if match is None:
            raise ValueError(f"unsupported enum entry in {path.name}: {entry!r}")
        name, explicit = match.groups()
        value = int(explicit) if explicit is not None else value + 1
        if name != "INVALID":
            result.append((name, value))
    return result


def _source_commit(headers: Path) -> str:
    checkout = headers.parents[1]
    try:
        return subprocess.check_output(
            ["git", "-C", str(checkout), "rev-parse", "HEAD"], text=True
        ).strip()
    except (OSError, subprocess.CalledProcessError) as exc:
        raise RuntimeError(f"cannot identify source checkout at {checkout}") from exc


def build_registry(headers: Path) -> dict:
    reference = json.loads(REFERENCE_BUILD.read_text(encoding="utf-8"))
    expected_commit = reference["simulator_base"]["commit"]
    actual_commit = _source_commit(headers)
    if actual_commit != expected_commit:
        raise ValueError(
            f"sts_lightspeed commit mismatch: expected {expected_commit}, got {actual_commit}"
        )

    categories = {}
    source_hashes = {}
    for category, (filename, enum_name) in SOURCES.items():
        path = headers / filename
        source_hashes[filename] = hashlib.sha256(path.read_bytes()).hexdigest()
        items = []
        for content_id, ordinal in parse_enum(path, enum_name):
            implementation = "partial" if content_id in PARTIAL.get(category, set()) else "declared"
            evidence = "unit" if implementation == "partial" else "upstream"
            items.append({
                "id": content_id,
                "ordinal": ordinal,
                "implementation": implementation,
                "evidence": evidence,
                "evidence_files": EVIDENCE_FILES.get(category, []) if implementation == "partial" else [],
            })
        categories[category] = items

    return {
        "schema_version": 1,
        "target": {
            "game_version": reference["game"]["reported_version"],
            "game_sha256": reference["game"]["sha256"],
            "source_project": reference["simulator_base"]["project"],
            "source_commit": actual_commit,
            "source_sha256": source_hashes,
        },
        "status_vocabulary": {
            "implementation": ["declared", "partial", "implemented"],
            "evidence": ["upstream", "unit", "oracle_trace"],
        },
        "categories": categories,
    }


def render(registry: dict) -> str:
    return json.dumps(registry, indent=2, ensure_ascii=False) + "\n"


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--headers", type=Path, default=DEFAULT_HEADERS)
    parser.add_argument("--output", type=Path, default=DEFAULT_OUTPUT)
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    rendered = render(build_registry(args.headers.resolve()))
    if args.check:
        if not args.output.exists() or args.output.read_text(encoding="utf-8") != rendered:
            print(f"content registry is stale: {args.output}")
            return 1
        print(f"content registry is current: {args.output}")
        return 0
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(rendered, encoding="utf-8")
    print(f"wrote {args.output}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
