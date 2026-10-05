"""Conservative A20 Act1 audit targets, not a stock-parity certificate."""

from __future__ import annotations

import re
from collections.abc import Mapping
from pathlib import Path
from typing import Any

from sls.content.registry import load_content_registry
from sls.content.scope import ironclad_scope

SCHEMA = "sls-a20-act1-audit-targets-v1"
PROFILE_ID = "IRONCLAD_A20_ACT1"
# Stock AbstractDungeon.getShrine() rejects these one-time pool entries in
# Exordium. The native isEventEligible() branch has the same act conditions.
ACT1_EXCLUDED_ONE_TIME_EVENTS = frozenset({
    "DESIGNER_IN_SPIRE", "DUPLICATOR", "KNOWING_SKULL", "NLOTH",
    "SECRET_PORTAL", "THE_JOUST",
})


def compare_stock_ironclad_potion_pool(
    stock_source_root: Path, native_potions_header: Path,
) -> int:
    """Check the ordered Ironclad potion draw pool against stock source.

    This checks the index-to-potion mapping used after a potion RNG draw. It
    does not check potion effects, rarity or the RNG implementation itself.
    """

    stock = (stock_source_root / "helpers/PotionHelper.java").read_text(
        encoding="utf-8",
    )
    native = native_potions_header.read_text(encoding="utf-8")
    stock_method = stock.split("getPotions(AbstractPlayer.PlayerClass c, boolean getAll)", 1)
    if len(stock_method) != 2:
        raise ValueError("stock potion pool method missing")
    stock_method = stock_method[1].split("public static AbstractPotion getRandomPotion", 1)[0]
    ironclad = stock_method.split("case IRONCLAD:", 1)
    if len(ironclad) != 2:
        raise ValueError("stock Ironclad potion prefix missing")
    prefix = re.findall(r'retVal\.add\("([^"]+)"\)', ironclad[1].split("case THE_SILENT:", 1)[0])
    if "} else {" not in stock_method or 'retVal.add("Block Potion")' not in stock_method:
        raise ValueError("stock shared potion pool boundary changed")
    shared_start = stock_method.index('retVal.add("Block Potion")')
    shared_names = re.findall(
        r'retVal\.add\("([^"]+)"\)', stock_method[shared_start:],
    )
    native_pool = native.split("static constexpr Potion potionPool[4][33]", 1)
    if len(native_pool) != 2:
        raise ValueError("native potion pool declaration changed")
    first_row = re.search(r"\{\s*\{([^{}]+)\}", native_pool[1])
    if first_row is None:
        raise ValueError("native Ironclad potion row missing")
    native_names = re.findall(r"Potion::([A-Z_]+)", first_row.group(1))
    registry = load_content_registry().categories["potions"]
    by_game_id = {str(row["game_id"]): str(row["id"]) for row in registry}
    stock_ids = [by_game_id[name] for name in prefix + shared_names]
    if not stock_ids or stock_ids != native_names:
        raise ValueError(f"stock/native Ironclad potion order differs: {stock_ids} != {native_names}")
    return len(stock_ids)


def compare_stock_event_pools(
    stock_source_root: Path, ordered_pools: Mapping[str, Any],
) -> dict[str, int]:
    """Compare initial event pool order with the reviewed stock Java projection.

    The caller must first verify that the source projection's manifest hash
    matches the stock JAR. This verifies pool membership/order, not event
    eligibility, outcomes or RNG implementation.
    """

    dungeon = (stock_source_root / "dungeons/AbstractDungeon.java").read_text(
        encoding="utf-8",
    )
    exordium = (stock_source_root / "dungeons/Exordium.java").read_text(
        encoding="utf-8",
    )

    def segment(source: str, start: str, end: str) -> str:
        if source.count(start) != 1 or source.count(end) != 1:
            raise ValueError(f"stock event method boundary changed: {start}")
        return source.split(start, 1)[1].split(end, 1)[0]

    stock_lists = {
        "events": re.findall(
            r'eventList\.add\("([^"]+)"\)',
            segment(exordium, "protected void initializeEventList()",
                    "protected void initializeShrineList()"),
        ),
        "shrines": re.findall(
            r'shrineList\.add\("([^"]+)"\)',
            segment(exordium, "protected void initializeShrineList()",
                    "protected void initializeEventImg()"),
        ),
        "special_one_time_events": re.findall(
            r'specialOneTimeEventList\.add\("([^"]+)"\)',
            segment(dungeon, "public void initializeSpecialOneTimeEventList()",
                    "private boolean isNoteForYourselfAvailable()"),
        ),
    }
    eligibility = segment(
        dungeon, "private boolean isNoteForYourselfAvailable()",
        "public static ArrayList<AbstractCard> getColorlessRewardCards()",
    )
    if "ascensionLevel >= 15" not in eligibility:
        raise ValueError("stock Note For Yourself A15+ exclusion changed")
    stock_lists["special_one_time_events"] = [
        name for name in stock_lists["special_one_time_events"]
        if re.sub(r"[^A-Z0-9]", "", name.upper()) != "NOTEFORYOURSELF"
    ]

    game_id_to_content = {
        re.sub(r"[^A-Z0-9]", "", str(row["game_id"]).upper()): str(row["id"])
        for row in load_content_registry().categories["events"]
    }
    ordinal_to_content = {
        int(row["ordinal"]): str(row["id"])
        for row in load_content_registry().categories["events"]
    }
    summary: dict[str, int] = {}
    for pool_name, stock_names in stock_lists.items():
        try:
            stock_ids = [
                game_id_to_content[re.sub(r"[^A-Z0-9]", "", name.upper())]
                for name in stock_names
            ]
            native_ids = [ordinal_to_content[int(value)]
                          for value in ordered_pools[pool_name]]
        except (KeyError, TypeError) as exc:
            raise ValueError(f"unmapped stock/native event in {pool_name}") from exc
        if not stock_ids or stock_ids != native_ids:
            raise ValueError(
                f"stock/native {pool_name} order differs: {stock_ids} != {native_ids}",
            )
        summary[pool_name] = len(stock_ids)
    return summary


def build_a20_act1_targets(ordered_pools: Mapping[str, Any]) -> dict[str, Any]:
    """Select audit candidates without claiming every candidate is reachable.

    Native event pools are seed-independent at a fresh A20 Act1 start. Some
    candidates have additional stock eligibility predicates, so pool membership
    is deliberately weaker than reachability. Global cards/relics/potions are
    retained conservatively until their acquisition routes are audited.
    """

    scope = ironclad_scope(20)
    registry = load_content_registry().categories
    event_by_ordinal = {
        int(row["ordinal"]): str(row["id"]) for row in registry["events"]
    }
    event_ids: set[str] = set()
    for pool_name in ("events", "shrines", "special_one_time_events"):
        values = ordered_pools.get(pool_name)
        if not isinstance(values, list):
            raise ValueError(f"missing native event pool: {pool_name}")
        unknown = set(map(int, values)) - event_by_ordinal.keys()
        if unknown:
            raise ValueError(f"unknown native event ordinals: {sorted(unknown)}")
        event_ids.update(event_by_ordinal[int(value)] for value in values)

    if "NOTE_FOR_YOURSELF" in event_ids:
        raise ValueError("A20 native event pool contains Note For Yourself")
    outside_act1 = event_ids & ACT1_EXCLUDED_ONE_TIME_EVENTS
    event_ids -= outside_act1

    def rows(ids: set[str], basis: str) -> list[dict[str, str]]:
        return [{"content_id": content_id, "basis": basis}
                for content_id in sorted(ids)]

    categories = {
        "cards": rows(set(scope["cards"]["ids"]), "GLOBAL_CONSERVATIVE"),
        "potions": rows(set(scope["potions"]["ids"]), "GLOBAL_CONSERVATIVE"),
        "relics": rows(set(scope["relics"]["ids"]), "GLOBAL_CONSERVATIVE"),
        "events": rows(event_ids, "NATIVE_POOL_CANDIDATE"),
        "encounters": rows(
            set(scope["encounters"]["act1"])
            | set(scope["encounters"]["act1_event"]),
            "DECLARED_ACT1",
        ),
        "monsters": rows(set(scope["monsters"]["act1"]), "DECLARED_ACT1"),
    }
    excluded = set(scope["policy_excluded_content_ids"])
    for row in categories["relics"]:
        if row["content_id"] in excluded:
            row["policy_acquisition"] = "EXCLUDED"

    return {
        "schema": SCHEMA,
        "profile_id": PROFILE_ID,
        "scope_sha256": scope["scope_sha256"],
        "status": "AUDIT_CANDIDATES_NOT_PROVEN_STOCK_REACHABLE",
        "categories": categories,
        "outside_act1_events": sorted(outside_act1),
        "outside_act1_evidence": {
            "stock": "AbstractDungeon.getShrine (desktop-1.0.jar)",
            "native": "GameContext::isEventEligible",
        },
        "summary": {name: len(items) for name, items in categories.items()},
    }


def target_ids(payload: Mapping[str, Any], category: str) -> set[str]:
    if payload.get("schema") != SCHEMA or payload.get("profile_id") != PROFILE_ID:
        raise ValueError("unsupported A20 Act1 target inventory")
    categories = payload.get("categories")
    if not isinstance(categories, Mapping):
        raise ValueError("target inventory has no categories")
    rows = categories.get(category)
    if not isinstance(rows, list) or not rows:
        raise ValueError(f"target inventory has no {category} entries")
    result = {str(row["content_id"]) for row in rows}
    if len(result) != len(rows):
        raise ValueError(f"target inventory has duplicate {category} IDs")
    return result
