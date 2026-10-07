"""Build a conservative source-bound Ironclad A20 audit ledger, not a certificate."""

from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path

from sls.content.registry import load_content_registry
from sls.content.scope import ironclad_scope
from sls.rl.training_contract import native_source_digest

ROOT = Path(__file__).resolve().parents[1]
SCHEMA = "sls-a20-fullrun-audit-ledger-v1"
STATES = ("UNREVIEWED", "STATIC_REVIEWED", "CONTROLLED_MATCH",
          "PRODUCTION_WITNESSED", "KNOWN_DIFFERENCE", "FIXED_REGRESSION")
ACT4 = ("SHIELD_AND_SPEAR", "THE_HEART")
FLOW = ("NORMAL_NEOW", "MAP_AND_ENCOUNTER_POOLS", "SHOP", "REWARDS",
        "ACT1_TO_ACT2", "ACT2_TO_ACT3", "A20_ORDERED_DOUBLE_BOSSES",
        "RUBY_KEY_COST", "SAPPHIRE_KEY_COST", "EMERALD_KEY_AND_BURNING_ELITE",
        "ACT3_TO_ACT4", "ACT4_ROUTE", "TERMINAL_AND_HEART_SUCCESS", "CHECKPOINT_RNG")
SHARED = ("MULTIHIT", "DEATH_THORNS_QUEUE", "ARTIFACT_ORDER", "DEBUFF_DURATION",
          "DRAW_SHUFFLE", "GENERATED_CARDS", "EXHAUST_COPY", "POTIONS",
          "VICTORY_HEAL", "REWARD_RNG", "DAMAGE_ROUNDING", "PASSIVE_CLOCK_RNG")


def build_inventory() -> dict:
    scope = ironclad_scope(20)
    registry = load_content_registry().categories
    rows = []
    candidates = {name: set(scope[name]["ids"]) for name in ("cards", "potions", "relics", "events")}
    encounters = {identifier: [act] for act in range(1, 4)
                  for identifier in scope["encounters"][f"act{act}"]}
    for act in range(1, 4):
        for identifier in scope["encounters"].get(f"act{act}_event", []):
            encounters.setdefault(identifier, []).append(act)
    for identifier in ACT4:
        encounters[identifier] = [4]
    candidates["encounters"] = set(encounters)
    for category in ("cards", "potions", "relics", "events", "encounters", "monsters"):
        for entry in registry[category]:
            identifier = entry["id"]
            rows.append({
                "id": f"{category}:{identifier}", "category": category,
                "content_id": identifier, "game_id": entry.get("game_id"),
                "candidate_basis": ("DECLARED_SCOPE_OR_ACT4" if identifier in candidates.get(category, set())
                                    else "REGISTRY_REACHABILITY_REQUIRES_REVIEW"),
                "acts": sorted(set(encounters.get(identifier, []))) if category == "encounters" else [],
                "policy_excluded": identifier in scope["policy_excluded_content_ids"],
                "reachability": "UNVERIFIED", "status": "UNREVIEWED",
                "stock_methods": [], "native_paths": [], "trigger": None,
                "scenes": [], "evidence": [], "missing_branches": ["BASIC_BEHAVIOR_AND_ACQUISITION"],
            })
    for category, identifiers in (("flow", FLOW), ("shared", SHARED)):
        for identifier in identifiers:
            rows.append({"id": f"{category}:{identifier}", "category": category,
                         "content_id": identifier, "status": "UNREVIEWED",
                         "stock_methods": [], "native_paths": [], "trigger": None,
                         "scenes": [], "evidence": [], "missing_branches": ["INDEPENDENT_STOCK_EVIDENCE"]})
    if not set(encounters) <= {row["id"] for row in registry["encounters"]}:
        raise ValueError("declared encounter is absent from native registry")
    sources = {path: hashlib.sha256((ROOT / path).read_bytes().replace(b"\r\n", b"\n")).hexdigest()
               for path in (*scope["source_sha256"], "src/sls/content/scope.json",
                            "native/simulator/src/game/GameContext.cpp")}
    return {"schema": SCHEMA, "profile": "IRONCLAD_A20_HEART", "ascension": 20,
            "purpose": "COVERAGE_LEDGER_NOT_PARITY_OR_WIN_RATE_CERTIFICATE",
            "native_source_sha256": native_source_digest(), "source_sha256": sources,
            "allowed_states": list(STATES), "seed_start": 131200000,
            "final_holdout_used": False, "production_behavior_changed": False,
            "inherited_passes": False, "obligations": rows}


def allocate_seeds(count: int, occupied: set[int], *, start: int = 131200000) -> list[list[int]]:
    """Reject collisions instead of silently skipping an earlier allocation."""
    if count < 1 or start < 131200000:
        raise ValueError("invalid controlled allocation")
    values = list(range(start, start + 3 * count))
    if occupied.intersection(values):
        raise ValueError("controlled seed collision")
    return [values[index:index + 3] for index in range(0, len(values), 3)]


def recorded_seeds(paths: list[Path]) -> set[int]:
    result = set()
    def visit(value):
        if isinstance(value, dict):
            for key, child in value.items():
                if key == "seed" and isinstance(child, int):
                    result.add(child)
                elif key == "seeds" and isinstance(child, list):
                    result.update(item for item in child if isinstance(item, int))
                else:
                    visit(child)
        elif isinstance(value, list):
            for child in value:
                visit(child)
    for path in paths:
        visit(json.loads(path.read_text(encoding="utf-8")))
    return result


def inventory_text(data: dict) -> str:
    """Keep the large candidate ledger one obligation per line for review."""
    header = {key: value for key, value in data.items() if key != "obligations"}
    prefix = json.dumps(header, indent=2).rstrip()[:-1].rstrip()
    rows = ",\n".join("    " + json.dumps(row, separators=(",", ":")) for row in data["obligations"])
    return prefix + ',\n  "obligations": [\n' + rows + "\n  ]\n}\n"


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output", required=True, type=Path)
    args = parser.parse_args()
    data = build_inventory()
    args.output.parent.mkdir(parents=True, exist_ok=True)
    with args.output.open("x", encoding="utf-8") as stream:
        stream.write(inventory_text(data))
    print(f"{len(data['obligations'])} unreviewed obligations; no parity passes inferred")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
