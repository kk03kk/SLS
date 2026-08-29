"""Generate the source-backed Ironclad A0-A20 FullRun reachable inventory."""

from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path
import re
from typing import Any

try:
    from tools.generate_content_scope import (
        IRONCLAD_A0_CURSES,
        IRONCLAD_A0_SPECIAL_CARDS,
        IRONCLAD_A0_STATUSES,
        ROOT,
        _block,
        _brace_body,
        _card_metadata,
        _enum_ids,
        _ironclad_relics,
    )
except ModuleNotFoundError:  # Direct ``python tools/...`` execution.
    from generate_content_scope import (
        IRONCLAD_A0_CURSES,
        IRONCLAD_A0_SPECIAL_CARDS,
        IRONCLAD_A0_STATUSES,
        ROOT,
        _block,
        _brace_body,
        _card_metadata,
        _enum_ids,
        _ironclad_relics,
    )


SCHEMA = "sls-ironclad-fullrun-reachable-v1"
OUTPUT = ROOT / "configs" / "validation" / "ironclad_fullrun_inventory.json"
CONSTANTS = ROOT / "cpp" / "simulator" / "include" / "constants"
JAVA = ROOT / "reference" / "original-game" / "decompiled" / "com" / "megacrit" / "cardcrawl"


ASCENSION_MODIFIERS = (
    (1, "MORE_ELITES", "map", "More elite rooms", "dungeons/AbstractDungeon.java", "ascensionLevel >= 1", "src/game/Map.cpp", "ascensionLevel > 0"),
    (2, "DEADLIER_NORMALS", "combat", "Normal enemies deal more damage", "monsters/exordium/SpikeSlime_S.java", "ascensionLevel >= 2", "src/combat/MonsterMoveDamage.cpp", "asc2"),
    (3, "DEADLIER_ELITES", "combat", "Elite enemies deal more damage", "monsters/exordium/Sentry.java", "ascensionLevel >= 3", "src/combat/MonsterMoveDamage.cpp", "asc3"),
    (4, "DEADLIER_BOSSES", "combat", "Bosses deal more damage", "monsters/exordium/SlimeBoss.java", "ascensionLevel >= 4", "src/combat/MonsterMoveDamage.cpp", "asc4"),
    (5, "REDUCED_POST_BOSS_HEAL", "transition", "Heal 75% of missing HP after an act", "dungeons/AbstractDungeon.java", "ascensionLevel >= 5", "src/game/GameContext.cpp", "ascension >= 5"),
    (6, "DAMAGED_START", "run_start", "Start at 90% current HP", "dungeons/AbstractDungeon.java", "ascensionLevel >= 6", "src/game/GameContext.cpp", "ascension < 6"),
    (7, "TOUGHER_NORMALS", "combat", "Normal enemies have more HP", "monsters/exordium/SpikeSlime_S.java", "ascensionLevel >= 7", "src/combat/MonsterSpecific.cpp", "ascension >= 7"),
    (8, "TOUGHER_ELITES", "combat", "Elite enemies have more HP", "monsters/exordium/Sentry.java", "ascensionLevel >= 8", "src/combat/MonsterSpecific.cpp", "ascension >= 8"),
    (9, "TOUGHER_BOSSES", "combat", "Bosses have more HP", "monsters/exordium/SlimeBoss.java", "ascensionLevel >= 9", "src/combat/MonsterSpecific.cpp", "ascension >= 9"),
    (10, "ASCENDERS_BANE", "run_start", "Add Ascender's Bane to the starting deck", "dungeons/AbstractDungeon.java", "ascensionLevel >= 10", "src/game/GameContext.cpp", "ascension >= 10"),
    (11, "ONE_LESS_POTION_SLOT", "inventory", "Reduce potion capacity by one", "characters/AbstractPlayer.java", "ascensionLevel >= 11", "src/game/GameContext.cpp", "ascension >= 11"),
    (12, "FEWER_UPGRADED_CARDS", "rewards", "Reduce upgraded-card chances", "dungeons/TheCity.java", "ascensionLevel >= 12", "src/game/Game.cpp", "ascension < 12"),
    (13, "LESS_BOSS_GOLD", "rewards", "Reduce boss gold rewards", "rooms/AbstractRoom.java", "ascensionLevel >= 13", "src/game/GameContext.cpp", "ascension >= 13"),
    (14, "LOWER_MAX_HP", "run_start", "Reduce Ironclad starting max HP by five", "dungeons/AbstractDungeon.java", "ascensionLevel >= 14", "src/game/GameContext.cpp", "ascension < 14"),
    (15, "UNFAVORABLE_EVENTS", "events", "Use unfavorable event values and pools", "events/shrines/WomanInBlue.java", "ascensionLevel >= 15", "src/game/GameContext.cpp", "ascension >= 15"),
    (16, "COSTLIER_SHOPS", "economy", "Increase shop prices", "shop/ShopScreen.java", "ascensionLevel >= 16", "src/game/Shop.cpp", "ascension >= 16"),
    (17, "CHALLENGING_NORMAL_AI", "combat", "Normal enemies gain stronger moves and AI", "monsters/exordium/SpikeSlime_M.java", "ascensionLevel >= 17", "src/combat/MonsterSpecific.cpp", "ascension >= 17"),
    (18, "CHALLENGING_ELITE_AI", "combat", "Elite enemies gain stronger moves and AI", "monsters/exordium/Sentry.java", "ascensionLevel >= 18", "src/combat/MonsterSpecific.cpp", "ascension >= 18"),
    (19, "CHALLENGING_BOSS_AI", "combat", "Bosses gain stronger moves and AI", "monsters/exordium/TheGuardian.java", "ascensionLevel >= 19", "src/combat/MonsterSpecific.cpp", "ascension >= 19"),
    (20, "DOUBLE_ACT3_BOSS", "transition", "Fight a second distinct Act 3 boss", "ui/buttons/ProceedButton.java", "ascensionLevel >= 20", "src/game/GameContext.cpp", "ascension >= 20"),
)


def _source_hash(path: Path) -> str:
    return hashlib.sha256(path.read_bytes().replace(b"\r\n", b"\n")).hexdigest()


def _canonical_digest(payload: dict[str, Any]) -> str:
    unsigned = dict(payload)
    unsigned.pop("inventory_sha256", None)
    encoded = json.dumps(unsigned, sort_keys=True, separators=(",", ":"))
    return hashlib.sha256(encoded.encode()).hexdigest()


def _namespace_arrays(source: str, namespace: str) -> tuple[set[str], set[str]]:
    body = _block(source, rf"namespace {namespace}\s*\{{(.*?)\n\s*\}}", label=namespace)
    events = _enum_ids(_block(body, r"events\s*\{(.*?)\};", label=f"{namespace} events"), "Event")
    shrines = _enum_ids(_block(body, r"shrines\s*\{(.*?)\};", label=f"{namespace} shrines"), "Event")
    return events, shrines


def _encounter_sections(source: str) -> dict[str, set[str]]:
    enum = _block(source, r"enum class MonsterEncounter[^\{]*\{(.*?)\};", label="encounter enum")
    bounds = (
        ("act1", "// Act 1 Weak", "// Act 2"),
        ("act2", "// Act 2", "// Act 3"),
        ("act3", "// Act 3", "// Act 4"),
        ("act4", "// Act 4", "// Events"),
        ("events", "// Events", None),
    )
    result: dict[str, set[str]] = {}
    for name, start, end in bounds:
        body = enum.split(start, 1)[1]
        if end is not None:
            body = body.split(end, 1)[0]
        result[name] = {
            token for token in re.findall(r"^\s*([A-Z][A-Z0-9_]+)\s*,?\s*$", body, re.MULTILINE)
            if token != "INVALID"
        }
    return result


def _monsters_for_encounters(encounters: set[str]) -> set[str]:
    path = ROOT / "cpp" / "simulator" / "src" / "combat" / "MonsterGroup.cpp"
    source = path.read_text(encoding="utf-8")
    start = source.index("void MonsterGroup::createMonsters")
    body = _brace_body(source, source.index("{", start))
    cases = list(re.finditer(r"case MonsterEncounter::([A-Z0-9_]+)\s*:", body))
    pending: list[str] = []
    for index, match in enumerate(cases):
        if match.group(1) in encounters:
            end = cases[index + 1].start() if index + 1 < len(cases) else len(body)
            pending.append(body[match.end():end])
    visited: set[str] = set()
    result: set[str] = set()
    while pending:
        fragment = pending.pop()
        result |= _enum_ids(fragment, "MonsterId")
        for name in re.findall(r"\b([A-Za-z][A-Za-z0-9_]*)\s*\(", fragment):
            if name in visited:
                continue
            match = re.search(rf"(?:void|MonsterId)\s+MonsterGroup::{re.escape(name)}\s*\([^)]*\)\s*\{{", source)
            if match:
                visited.add(name)
                pending.append(_brace_body(source, match.end() - 1))
    result.discard("INVALID")
    # Monsters created after the encounter constructor remain reachable
    # policy-visible content and must be part of the closure.
    dynamic_summons = {
        "AUTOMATON": {"BRONZE_ORB"},
        "COLLECTOR": {"TORCH_HEAD"},
    }
    for encounter_id, monster_ids in dynamic_summons.items():
        if encounter_id in encounters:
            result.update(monster_ids)
    return result


def _ascension_registry() -> list[dict[str, Any]]:
    result = []
    for level, identifier, category, summary, java_rel, java_pred, native_rel, native_pred in ASCENSION_MODIFIERS:
        java_path = JAVA / java_rel
        native_path = ROOT / "cpp" / "simulator" / native_rel
        if java_pred not in java_path.read_text(encoding="utf-8", errors="replace"):
            raise RuntimeError(f"Original ascension predicate is missing: {java_rel}: {java_pred}")
        if native_pred not in native_path.read_text(encoding="utf-8"):
            raise RuntimeError(f"native ascension predicate is missing: {native_rel}: {native_pred}")
        result.append({
            "level": level, "id": identifier, "category": category,
            "summary": summary, "activated_for": list(range(level, 21)),
            "original_source": {"path": str(java_path.relative_to(ROOT)).replace("\\", "/"), "predicate": java_pred},
            "native_source": {"path": str(native_path.relative_to(ROOT)).replace("\\", "/"), "predicate": native_pred},
        })
    return result


def build_inventory() -> dict[str, Any]:
    card_source = (CONSTANTS / "CardPools.h").read_text(encoding="utf-8")
    metadata = _card_metadata()
    red = {card_id for card_id, value in metadata.items() if value["color"] == "RED"}
    colorless = _enum_ids(_block(
        card_source, r"namespace ColorlessRarityCardPool\s*\{.*?colorlessCardBlob\[\]\s*\{(.*?)\};",
        label="colorless reward pool",
    ), "CardId")
    base_cards = red | colorless | IRONCLAD_A0_SPECIAL_CARDS | IRONCLAD_A0_STATUSES | IRONCLAD_A0_CURSES
    # Stock Prismatic Shard switches ordinary rewards to CardLibrary's
    # getAnyColorCard(rarity).  That routine accepts every unlocked COMMON,
    # UNCOMMON, or RARE card, regardless of color.  Keep this Original closure
    # separate from the native closure because the current simulator explicitly
    # disables Prismatic Shard.
    prismatic_rewards = {
        card_id for card_id, value in metadata.items()
        if value["rarity"] in {"COMMON", "UNCOMMON", "RARE"}
    }
    original_a0_cards = base_cards | prismatic_rewards
    original_a20_cards = original_a0_cards | {"ASCENDERS_BANE"}

    potion_source = (CONSTANTS / "Potions.h").read_text(encoding="utf-8")
    potions = _enum_ids(_block(
        potion_source, r"potionPool\[4\]\[33\]\s*\{\s*\{(.*?)\}\s*,",
        label="Ironclad potion pool",
    ), "Potion")

    event_source = (CONSTANTS / "Events.h").read_text(encoding="utf-8")
    events_by_act: dict[str, list[str]] = {}
    shrines: set[str] = set()
    for number in (1, 2, 3):
        events, act_shrines = _namespace_arrays(event_source, f"Act{number}")
        events_by_act[f"act{number}"] = sorted(events)
        shrines |= act_shrines
    one_time_a0 = _enum_ids(_block(event_source, r"oneTimeEventsAsc0\s*\{(.*?)\};", label="A0 one-time events"), "Event")
    one_time_a15 = _enum_ids(_block(event_source, r"oneTimeEventsAsc15\s*\{(.*?)\};", label="A15 one-time events"), "Event")

    encounter_source = (CONSTANTS / "MonsterEncounters.h").read_text(encoding="utf-8")
    encounters = _encounter_sections(encounter_source)
    monsters = {name: sorted(_monsters_for_encounters(ids)) for name, ids in encounters.items()}

    room_source = (CONSTANTS / "Rooms.h").read_text(encoding="utf-8")
    room_enum = _block(room_source, r"enum class Room[^\{]*\{(.*?)\};", label="rooms")
    rooms = sorted(set(re.findall(
        r"^\s*([A-Z][A-Z0-9_]+)(?:\s*=\s*\d+)?\s*,?\s*$",
        room_enum, re.MULTILINE,
    )) - {"INVALID", "NONE"})

    source_paths = {
        CONSTANTS / name for name in (
            "Cards.h", "CardPools.h", "Potions.h", "Relics.h", "RelicPools.h",
            "Events.h", "MonsterEncounters.h", "MonsterIds.h", "Rooms.h",
        )
    }
    source_paths.add(ROOT / "cpp" / "simulator" / "src" / "combat" / "MonsterGroup.cpp")
    source_paths.add(ROOT / "cpp" / "simulator" / "include" / "game" / "GameContext.h")
    source_paths.add(JAVA / "helpers" / "CardLibrary.java")
    source_paths.add(JAVA / "dungeons" / "AbstractDungeon.java")
    for item in ASCENSION_MODIFIERS:
        source_paths.add(JAVA / item[4])
        source_paths.add(ROOT / "cpp" / "simulator" / item[6])

    a0_events = set().union(*map(set, events_by_act.values()), shrines, one_time_a0, {"NEOW"})
    a20_events = set().union(*map(set, events_by_act.values()), shrines, one_time_a15, {"NEOW"})
    all_encounters = set().union(*encounters.values())
    all_monsters = set().union(*map(set, monsters.values()))
    relics = _ironclad_relics()
    payload: dict[str, Any] = {
        "schema": SCHEMA,
        "character": "IRONCLAD",
        "ascension_range": [0, 20],
        "acts": [1, 2, 3, 4],
        "profiles": [
            *[f"IRONCLAD_A{level}_FULLRUN" for level in range(21)],
            *[f"IRONCLAD_A{level}_HEART" for level in range(21)],
        ],
        "cards": {
            "native_current_a0": sorted(base_cards),
            "native_current_a20": sorted(base_cards | {"ASCENDERS_BANE"}),
            "original_theoretical_a0": sorted(original_a0_cards),
            "original_theoretical_a20": sorted(original_a20_cards),
            "prismatic_any_color_rewards": sorted(prismatic_rewards),
            "red": sorted(red), "colorless_reward": sorted(colorless),
            "special_event": sorted(IRONCLAD_A0_SPECIAL_CARDS),
            "statuses": sorted(IRONCLAD_A0_STATUSES),
            "curses_a0": sorted(IRONCLAD_A0_CURSES),
            "curses_a20": sorted(IRONCLAD_A0_CURSES | {"ASCENDERS_BANE"}),
        },
        "relics": {
            "original_theoretical": sorted(relics),
            "native_current": sorted(relics - {"PRISMATIC_SHARD"}),
            "native_disabled_original_reachable": ["PRISMATIC_SHARD"],
        },
        "potions": {"ids": sorted(potions)},
        "events": {
            "by_act": events_by_act, "shrines": sorted(shrines),
            "one_time_a0": sorted(one_time_a0), "one_time_a15_plus": sorted(one_time_a15),
            "a0_reachable": sorted(a0_events), "a20_reachable": sorted(a20_events),
        },
        "encounters": {key: sorted(value) for key, value in encounters.items()},
        "monsters": monsters,
        "rooms": rooms,
        "keys": ["RUBY_KEY", "EMERALD_KEY", "SAPPHIRE_KEY"],
        "map": {"acts": [1, 2, 3, 4], "act4_sequence": ["REST", "SHOP", "SHIELD_AND_SPEAR", "THE_HEART"]},
        "ascension_modifiers": _ascension_registry(),
        "counts": {
            "original_theoretical_a0_heart": {"cards": len(original_a0_cards), "relics": len(relics), "potions": len(potions), "events": len(a0_events), "encounters": len(all_encounters), "monsters": len(all_monsters)},
            "original_theoretical_a20_heart": {"cards": len(original_a20_cards), "relics": len(relics), "potions": len(potions), "events": len(a20_events), "encounters": len(all_encounters), "monsters": len(all_monsters), "ascension_modifiers": 20},
            "native_current_a0_heart": {"cards": len(base_cards), "relics": len(relics - {"PRISMATIC_SHARD"}), "potions": len(potions), "events": len(a0_events), "encounters": len(all_encounters), "monsters": len(all_monsters)},
            "native_current_a20_heart": {"cards": len(base_cards | {"ASCENDERS_BANE"}), "relics": len(relics - {"PRISMATIC_SHARD"}), "potions": len(potions), "events": len(a20_events), "encounters": len(all_encounters), "monsters": len(all_monsters), "ascension_modifiers": 20},
        },
        "source_sha256": {
            str(path.relative_to(ROOT)).replace("\\", "/"): _source_hash(path)
            for path in sorted(source_paths)
        },
    }
    payload["inventory_sha256"] = _canonical_digest(payload)
    return payload


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true")
    parser.add_argument("--output", type=Path, default=OUTPUT)
    args = parser.parse_args()
    rendered = json.dumps(build_inventory(), indent=2, sort_keys=True) + "\n"
    if args.check:
        if not args.output.is_file() or args.output.read_text(encoding="utf-8") != rendered:
            print(f"stale FullRun inventory: {args.output}")
            return 1
        return 0
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(rendered, encoding="utf-8")
    print(args.output)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
