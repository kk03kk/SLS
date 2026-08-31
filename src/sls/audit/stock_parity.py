"""Build a traceable stock-game parity inventory.

The manifest deliberately distinguishes inventory coverage, captured Original
scenarios, and actual simulator parity.  Merely finding an implementation or a
successful game process must never be reported as semantic parity.
"""

from __future__ import annotations

import hashlib
import json
import re
import zipfile
from collections import defaultdict
from pathlib import Path
from typing import Any, Iterable

from sls.content.registry import load_content_registry
from sls.content.scope import load_ironclad_a0_scope

MANIFEST_SCHEMA = "sls-stock-parity-audit-v1"
ORIGINAL_CAPTURED = "ORIGINAL_SCENARIO_CAPTURED"
UNREVIEWED = "UNREVIEWED"

_ALLOWLIST_MEMBERS = {
    "cards": "spirecomm/parity/scenario-card-allowlist.tsv",
    "potions": "spirecomm/parity/scenario-potion-allowlist.tsv",
    "relics": "spirecomm/parity/scenario-relic-allowlist.tsv",
    "encounters": "spirecomm/parity/scenario-encounter-allowlist.tsv",
    "events": "spirecomm/parity/scenario-event-allowlist.tsv",
}
_LOG_PAYLOAD = re.compile(r"Sending message: (\{.*\})$")


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def load_oracle_allowlists(oracle_jar: Path) -> dict[str, dict[str, str]]:
    result: dict[str, dict[str, str]] = {}
    with zipfile.ZipFile(oracle_jar) as archive:
        for category, member in _ALLOWLIST_MEMBERS.items():
            rows: dict[str, str] = {}
            text = archive.read(member).decode("utf-8")
            for line in text.splitlines():
                if not line.strip():
                    continue
                content_id, game_id = line.split("\t", 1)
                rows[content_id] = game_id
            result[category] = rows
    return result


def captured_original_scenarios(
    log_paths: Iterable[Path],
) -> dict[str, dict[str, set[str]]]:
    """Return captured scenario IDs grouped by category and exercised content ID."""

    captures: dict[str, dict[str, set[str]]] = defaultdict(lambda: defaultdict(set))
    for log_path in log_paths:
        with log_path.open(encoding="utf-8", errors="replace") as handle:
            for line in handle:
                match = _LOG_PAYLOAD.search(line.rstrip("\n"))
                if match is None or '"_parity_scenario"' not in match.group(1):
                    continue
                try:
                    payload = json.loads(match.group(1))
                except json.JSONDecodeError:
                    continue
                scenario = payload.get("_parity_scenario", {})
                scenario_id = str(scenario.get("scenario_id", ""))
                if not scenario_id:
                    continue
                parts = scenario_id.split(":")
                if len(parts) < 2:
                    continue
                prefix = parts[0]
                if prefix == "card_probe":
                    category = "cards"
                elif prefix == "potion_probe":
                    category = "potions"
                elif prefix == "encounter_probe":
                    category = "encounters"
                elif prefix == "event_probe":
                    category = "events"
                elif prefix.startswith("relic_") or prefix == "relic_probe":
                    category = "relics"
                else:
                    continue
                captures[category][parts[1]].add(scenario_id)
    return {
        category: dict(by_id) for category, by_id in captures.items()
    }


def _scope_ids(scope: dict[str, Any], category: str) -> set[str]:
    section = scope.get(category, {})
    if not isinstance(section, dict):
        return set()
    if "ids" in section:
        return set(map(str, section["ids"]))
    result: set[str] = set()
    for value in section.values():
        if isinstance(value, list):
            result.update(map(str, value))
    return result


def build_stock_parity_manifest(
    *,
    stock_jar: Path,
    oracle_jar: Path,
    original_logs: Iterable[Path] = (),
) -> dict[str, Any]:
    registry = load_content_registry()
    scope = load_ironclad_a0_scope()
    allowlists = load_oracle_allowlists(oracle_jar)
    captures = captured_original_scenarios(original_logs)

    categories: dict[str, list[dict[str, Any]]] = {}
    for category, registry_items in registry.categories.items():
        scoped = _scope_ids(scope, category)
        oracle_ids = set(allowlists.get(category, {}))
        rows: list[dict[str, Any]] = []
        for item in registry_items:
            content_id = str(item["id"])
            scenarios = sorted(captures.get(category, {}).get(content_id, ()))
            rows.append({
                "content_id": content_id,
                "game_id": item.get("game_id"),
                "ordinal": int(item["ordinal"]),
                "ironclad_a0_scope": content_id in scoped,
                "oracle_allowlisted": content_id in oracle_ids,
                "original_scenarios": scenarios,
                "original_evidence": ORIGINAL_CAPTURED if scenarios else UNREVIEWED,
                "simulator_parity": UNREVIEWED,
            })
        categories[category] = rows

    return {
        "schema": MANIFEST_SCHEMA,
        "authority": {
            "stock_jar": str(stock_jar.resolve()),
            "stock_jar_sha256": sha256_file(stock_jar),
            "oracle_jar": str(oracle_jar.resolve()),
            "oracle_jar_sha256": sha256_file(oracle_jar),
            "scope_id": scope["scope_id"],
            "scope_sha256": scope["scope_sha256"],
        },
        "status_semantics": {
            UNREVIEWED: "No semantic parity claim has been established.",
            ORIGINAL_CAPTURED: (
                "A controlled scenario was captured from stock desktop-1.0.jar; "
                "this alone is not a simulator parity result."
            ),
        },
        "categories": categories,
        "summary": {
            category: {
                "registry": len(rows),
                "ironclad_a0_scope": sum(row["ironclad_a0_scope"] for row in rows),
                "oracle_allowlisted": sum(row["oracle_allowlisted"] for row in rows),
                "original_captured": sum(bool(row["original_scenarios"]) for row in rows),
                "simulator_parity_passed": 0,
            }
            for category, rows in categories.items()
        },
    }
