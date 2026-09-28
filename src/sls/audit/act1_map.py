"""Bounded checks for the A20 Act1 map's stock-visible structure."""

from __future__ import annotations

import re
from collections import Counter
from collections.abc import Mapping, Sequence
from pathlib import Path
from typing import Any


def compare_stock_act1_map_constants(stock_exordium: Path, native_map: Path) -> dict[str, float]:
    """Compare Exordium's map probabilities with the native source constants."""

    stock = stock_exordium.read_text(encoding="utf-8")
    stock_dungeon = stock_exordium.with_name("AbstractDungeon.java").read_text(
        encoding="utf-8",
    )
    native = native_map.read_text(encoding="utf-8")
    pairs = {
        "shop": ("shopRoomChance", "SHOP_ROOM_CHANCE"),
        "rest": ("restRoomChance", "REST_ROOM_CHANCE"),
        "treasure": ("treasureRoomChance", "TREASURE_ROOM_CHANCE"),
        "event": ("eventRoomChance", "EVENT_ROOM_CHANCE"),
        "elite": ("eliteRoomChance", "ELITE_ROOM_CHANCE_A0"),
    }
    result: dict[str, float] = {}
    for name, (stock_name, native_name) in pairs.items():
        stock_values = re.findall(rf"\b{stock_name}\s*=\s*([0-9.]+)f\s*;", stock)
        native_values = re.findall(rf"\b{native_name}\s*=\s*([0-9.]+)[fF]\s*;", native)
        if len(stock_values) != 1 or len(native_values) != 1:
            raise ValueError(f"map chance declaration changed: {name}")
        if float(stock_values[0]) != float(native_values[0]):
            raise ValueError(f"stock/native map chance differs: {name}")
        result[name] = float(stock_values[0])
    if "eliteRoomChance * 1.6f" not in stock_dungeon or "ELITE_ROOM_CHANCE_A0 * 1.6f" not in native:
        raise ValueError("A1+ elite map multiplier changed")
    return result


def check_act1_map_structure(nodes: Sequence[Mapping[str, Any]]) -> dict[str, int]:
    """Check stock map rules that are visible without entering a room."""

    by_id = {str(node["node_id"]): node for node in nodes}
    if len(by_id) != len(nodes) or not nodes:
        raise ValueError("duplicate or empty map nodes")
    counts: Counter[str] = Counter()
    burning = 0
    for node in nodes:
        x, y = int(node["x"]), int(node["y"])
        room = str(node["room_type"])
        if not (0 <= x < 7 and 0 <= y < 15):
            raise ValueError("map node outside Act1 dimensions")
        if y == 0 and room != "MONSTER":
            raise ValueError("first map row must be monster rooms")
        if y == 8 and room != "TREASURE":
            raise ValueError("ninth map row must be treasure rooms")
        if y == 14 and room != "REST":
            raise ValueError("last map row must be rest rooms")
        if y <= 4 and room in {"ELITE", "REST"}:
            raise ValueError("early Act1 elite/rest room")
        if y == 13 and room == "REST":
            raise ValueError("rest room immediately before fixed rest row")
        if node["burning"]:
            burning += 1
            if room != "ELITE":
                raise ValueError("burning map node is not an elite")
        for edge in node["outgoing_node_ids"]:
            if edge == "map:3:15":
                if y != 14:
                    raise ValueError("boss edge starts before final rest row")
                continue
            dest = by_id.get(str(edge))
            if dest is None or int(dest["y"]) != y + 1:
                raise ValueError("map edge does not reach the next row")
        counts[room] += 1
    if burning != 1:
        raise ValueError(f"expected one burning elite, found {burning}")
    if {int(node["y"]) for node in nodes} != set(range(15)):
        raise ValueError("Act1 map is missing a row")
    return dict(counts)
