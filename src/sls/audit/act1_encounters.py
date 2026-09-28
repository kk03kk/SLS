"""Compare stock Exordium encounter tables with native Act1 tables."""

from __future__ import annotations

import re
from pathlib import Path

_NUMBER_WORDS = {"2": "TWO", "3": "THREE"}
_STOCK_ENTRY = re.compile(r'new MonsterInfo\("([^"]+)", ([0-9.]+)f\)')
_NATIVE_ENTRY = re.compile(r"ME::([A-Z][A-Z0-9_]*)")
_NATIVE_WEIGHT = re.compile(r"([0-9.]+)f/([0-9.]+)")


def _method(source: str, start: str, end: str) -> str:
    if source.count(start) != 1 or source.count(end) != 1:
        raise ValueError(f"stock encounter method boundary changed: {start}")
    return source.split(start, 1)[1].split(end, 1)[0]


def _stock_id(name: str) -> str:
    words = re.findall(r"[A-Z0-9]+", name.upper())
    return "_".join(_NUMBER_WORDS.get(word, word) for word in words)


def _first_act_array(source: str, name: str) -> str:
    match = re.search(rf"\b{name}\[3\]\[\d+\]\s*=\s*\{{\s*\{{([^}}]+)\}}", source)
    if match is None:
        raise ValueError(f"native encounter table missing: {name}")
    return match.group(1)


def compare_stock_act1_encounters(
    exordium_java: Path, native_header: Path,
) -> dict[str, int]:
    """Check sorted pool order and weights, not generated seed trajectories."""

    stock = exordium_java.read_text(encoding="utf-8")
    native = native_header.read_text(encoding="utf-8")
    methods = {
        "weak": _method(stock, "protected void generateWeakEnemies(int count)",
                        "protected void generateStrongEnemies(int count)"),
        "strong": _method(stock, "protected void generateStrongEnemies(int count)",
                          "protected void generateElites(int count)"),
        "elite": _method(stock, "protected void generateElites(int count)",
                         "protected ArrayList<String> generateExclusions()"),
    }
    tables = {
        "weak": "weakEnemies", "strong": "strongEnemies", "elite": "elites",
    }
    summary: dict[str, int] = {}
    for category, table in tables.items():
        stock_rows = [(_stock_id(name), float(weight))
                      for name, weight in _STOCK_ENTRY.findall(methods[category])]
        if not stock_rows:
            raise ValueError(f"stock {category} encounter table is empty")
        # MonsterInfo.normalizeWeights calls Collections.sort; Java's stable
        # sort preserves original order for equal weights.
        stock_rows.sort(key=lambda row: row[1])
        native_ids = _NATIVE_ENTRY.findall(_first_act_array(native, table))
        stock_ids = [row[0] for row in stock_rows]
        if stock_ids != native_ids:
            raise ValueError(
                f"stock/native {category} encounter order differs: "
                f"{stock_ids} != {native_ids}",
            )
        if category != "elite":
            weights = _NATIVE_WEIGHT.findall(
                _first_act_array(native, f"{category}Weights"),
            )
            native_weights = [float(numerator) / float(denominator)
                              for numerator, denominator in weights]
            total = sum(weight for _, weight in stock_rows)
            stock_weights = [weight / total for _, weight in stock_rows]
            if len(native_weights) != len(stock_weights) or any(
                abs(left - right) > 1e-6
                for left, right in zip(native_weights, stock_weights)
            ):
                raise ValueError(f"stock/native {category} encounter weights differ")
        summary[category] = len(stock_ids)
    return summary
