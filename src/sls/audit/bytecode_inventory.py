"""Reproducible per-content bytecode evidence from the local stock JAR."""

from __future__ import annotations

import concurrent.futures
import hashlib
import json
import re
import subprocess
import zipfile
from pathlib import Path
from typing import Any, Iterable

BYTECODE_INVENTORY_SCHEMA = "sls-stock-bytecode-inventory-v1"

_PACKAGES = {
    "cards": "com/megacrit/cardcrawl/cards/",
    "monsters": "com/megacrit/cardcrawl/monsters/",
    "potions": "com/megacrit/cardcrawl/potions/",
    "relics": "com/megacrit/cardcrawl/relics/",
    "events": "com/megacrit/cardcrawl/events/",
}
_ENUMS = {
    "cards": "CardId",
    "monsters": "MonsterId",
    "potions": "Potion",
    "relics": "RelicId",
    "events": "Event",
}
_EXPLICIT_STOCK_CLASSES = {
    ("monsters", "BEAR"): "com.megacrit.cardcrawl.monsters.city.BanditBear",
    ("monsters", "BLUE_SLAVER"): "com.megacrit.cardcrawl.monsters.exordium.SlaverBlue",
    ("monsters", "FAT_GREMLIN"): "com.megacrit.cardcrawl.monsters.exordium.GremlinFat",
    ("monsters", "GREEN_LOUSE"): "com.megacrit.cardcrawl.monsters.exordium.LouseDefensive",
    ("monsters", "MAD_GREMLIN"): "com.megacrit.cardcrawl.monsters.exordium.GremlinWarrior",
    ("monsters", "MYSTIC"): "com.megacrit.cardcrawl.monsters.city.Healer",
    ("monsters", "POINTY"): "com.megacrit.cardcrawl.monsters.city.BanditPointy",
    ("monsters", "RED_LOUSE"): "com.megacrit.cardcrawl.monsters.exordium.LouseNormal",
    ("monsters", "RED_SLAVER"): "com.megacrit.cardcrawl.monsters.exordium.SlaverRed",
    ("monsters", "ROMEO"): "com.megacrit.cardcrawl.monsters.city.BanditLeader",
    ("monsters", "SHIELD_GREMLIN"): "com.megacrit.cardcrawl.monsters.exordium.GremlinTsundere",
    ("monsters", "SNEAKY_GREMLIN"): "com.megacrit.cardcrawl.monsters.exordium.GremlinThief",
    ("potions", "EMPTY_POTION_SLOT"): "com.megacrit.cardcrawl.potions.PotionSlot",
    ("events", "NEOW"): "com.megacrit.cardcrawl.neow.NeowEvent",
}
_STRUCTURAL_CONTENT = {
    ("events", "MONSTER"),
    ("events", "REST"),
    ("events", "SHOP"),
    ("events", "TREASURE"),
}
_ID_PATTERN = re.compile(
    r"public static final java\.lang\.String (?:ID|POTION_ID);\s+"
    r"descriptor: Ljava/lang/String;\s+"
    r"flags: .*?\s+ConstantValue: String (?P<id>[^\r\n]+)",
)
_METHOD_PATTERN = re.compile(
    r"^  (?:public|protected|private) .*\([^;]*\);$",
    re.MULTILINE,
)
_COMPILED_PATTERN = re.compile(r'^Compiled from "(?P<source>[^"]+)"', re.MULTILINE)
_ENUM_REFERENCE_PATTERN = re.compile(
    r"\b(?P<enum>CardId|MonsterId|Potion|RelicId|Event)::"
    r"(?P<content_id>[A-Z][A-Z0-9_]*)\b",
)


def _sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def _canonical_symbol(value: str) -> str:
    return re.sub(r"[^A-Z0-9]", "", value.upper())


def _symbol_variants(value: str) -> set[str]:
    symbol = _canonical_symbol(value)
    variants = {symbol}
    if symbol.startswith("THE"):
        variants.add(symbol[3:])
    return variants


def _class_names(archive: zipfile.ZipFile, prefix: str) -> list[str]:
    return sorted(
        name[:-6].replace("/", ".")
        for name in archive.namelist()
        if name.startswith(prefix) and name.endswith(".class") and "$" not in name
    )


def _disassemble(javap: Path, stock_jar: Path, class_name: str) -> dict[str, Any]:
    completed = subprocess.run(
        [
            str(javap), "-classpath", str(stock_jar),
            "-c", "-p", "-verbose", class_name,
        ],
        check=True,
        capture_output=True,
    )
    text = completed.stdout.decode("utf-8", errors="replace").replace("\r\n", "\n")
    identifier = _ID_PATTERN.search(text)
    compiled = _COMPILED_PATTERN.search(text)
    return {
        "class_name": class_name,
        "stock_id": (
            identifier.group("id").strip().replace(r"\'", "'")
            if identifier else None
        ),
        "compiled_from": compiled.group("source") if compiled else None,
        "javap_sha256": _sha256(text.encode("utf-8")),
        "methods": _METHOD_PATTERN.findall(text),
        "javap_command": (
            f'javap -classpath "{stock_jar}" -c -p -verbose {class_name}'
        ),
    }


def _build_source_reference_index(root: Path) -> dict[tuple[str, str], list[str]]:
    """Scan simulator sources once and index every content-enum reference."""
    references: dict[tuple[str, str], list[str]] = {}
    for base in (
        root / "native" / "simulator" / "src",
        root / "native" / "simulator" / "include",
    ):
        for path in base.rglob("*"):
            if path.suffix not in {".cpp", ".h"}:
                continue
            try:
                lines = path.read_text(encoding="utf-8", errors="replace").splitlines()
            except OSError:
                continue
            for line_number, line in enumerate(lines, 1):
                location = f"{path.relative_to(root).as_posix()}:{line_number}"
                for match in _ENUM_REFERENCE_PATTERN.finditer(line):
                    key = (match.group("enum"), match.group("content_id"))
                    references.setdefault(key, []).append(location)
    return references


def build_bytecode_inventory(
    *,
    root: Path,
    stock_jar: Path,
    javap: Path,
    categories: Iterable[str] = _PACKAGES,
    workers: int = 8,
) -> dict[str, Any]:
    registry = json.loads(
        (root / "src" / "sls" / "content" / "registry.json").read_text(
            encoding="utf-8",
        ),
    )["categories"]
    selected = tuple(categories)
    with zipfile.ZipFile(stock_jar) as archive:
        raw_classes = {
            category: {
                class_name: archive.read(class_name.replace(".", "/") + ".class")
                for class_name in _class_names(archive, _PACKAGES[category])
            }
            for category in selected
        }
        for (category, _), class_name in _EXPLICIT_STOCK_CLASSES.items():
            if category not in raw_classes:
                continue
            member = class_name.replace(".", "/") + ".class"
            raw_classes[category].setdefault(class_name, archive.read(member))
    jobs = [
        (category, class_name)
        for category, classes in raw_classes.items()
        for class_name in classes
    ]
    disassembled: dict[tuple[str, str], dict[str, Any]] = {}
    with concurrent.futures.ThreadPoolExecutor(max_workers=workers) as executor:
        futures = {
            executor.submit(_disassemble, javap, stock_jar, class_name): (
                category, class_name,
            )
            for category, class_name in jobs
        }
        for future in concurrent.futures.as_completed(futures):
            disassembled[futures[future]] = future.result()

    source_references = _build_source_reference_index(root)
    result_categories: dict[str, list[dict[str, Any]]] = {}
    for category in selected:
        by_stock_id: dict[str, list[dict[str, Any]]] = {}
        for class_name, raw in raw_classes[category].items():
            evidence = disassembled[(category, class_name)]
            evidence["class_sha256"] = _sha256(raw)
            stock_id = evidence["stock_id"]
            if stock_id is not None:
                by_stock_id.setdefault(stock_id, []).append(evidence)
        rows: list[dict[str, Any]] = []
        for content in registry[category]:
            content_id = str(content["id"])
            game_id = str(content["game_id"])
            explicit_class = _EXPLICIT_STOCK_CLASSES.get((category, content_id))
            matches = (
                [disassembled[(category, explicit_class)]]
                if explicit_class is not None else [
                    evidence for evidence in by_stock_id.get(game_id, [])
                    if ".deprecated." not in evidence["class_name"]
                ]
            )
            match_basis = "EXPLICIT_CLASS" if explicit_class else "STOCK_ID"
            if not matches:
                wanted = _symbol_variants(content_id)
                matches = [
                    evidence
                    for evidence in disassembled.values()
                    if evidence["class_name"] in raw_classes[category]
                    and ".deprecated." not in evidence["class_name"]
                    and (
                        wanted & _symbol_variants(
                            evidence["class_name"].rsplit(".", 1)[-1],
                        )
                        or (
                            evidence["stock_id"] is not None
                            and wanted & _symbol_variants(evidence["stock_id"])
                        )
                    )
                ]
                match_basis = "CANONICAL_SYMBOL"
            rows.append({
                "content_id": content_id,
                "game_id": game_id,
                "stock_classes": matches,
                "stock_class_status": (
                    "EXACT_ID_MATCH" if len(matches) == 1 and match_basis == "STOCK_ID"
                    else "EXACT_EXPLICIT_MATCH" if len(matches) == 1 and match_basis == "EXPLICIT_CLASS"
                    else "EXACT_SYMBOL_MATCH" if len(matches) == 1
                    else "AMBIGUOUS_MATCH" if matches
                    else "STRUCTURAL_NODE_TYPE" if (category, content_id) in _STRUCTURAL_CONTENT
                    else "UNRESOLVED"
                ),
                "stock_match_basis": match_basis if matches else None,
                "simulator_references": source_references.get(
                    (_ENUMS[category], content_id), [],
                ),
            })
        result_categories[category] = rows

    return {
        "schema": BYTECODE_INVENTORY_SCHEMA,
        "stock_jar": str(stock_jar.resolve()),
        "stock_jar_sha256": _sha256(stock_jar.read_bytes()),
        "javap": str(javap.resolve()),
        "categories": result_categories,
        "summary": {
            category: {
                "registry": len(rows),
                "exact_stock_class": sum(
                    row["stock_class_status"] in {
                        "EXACT_ID_MATCH", "EXACT_SYMBOL_MATCH",
                        "EXACT_EXPLICIT_MATCH",
                    }
                    for row in rows
                ),
                "unresolved_stock_class": sum(
                    row["stock_class_status"] == "UNRESOLVED" for row in rows
                ),
                "structural_node_type": sum(
                    row["stock_class_status"] == "STRUCTURAL_NODE_TYPE"
                    for row in rows
                ),
                "with_simulator_reference": sum(
                    bool(row["simulator_references"]) for row in rows
                ),
            }
            for category, rows in result_categories.items()
        },
    }
