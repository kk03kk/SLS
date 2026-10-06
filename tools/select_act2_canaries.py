"""Freeze eight coverage canaries in seed order, without checkpoint selection."""

from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path

from sls.rl.training_contract import native_source_digest

START = 8000011000000
BOSSES = {"CHAMP", "COLLECTOR", "AUTOMATON"}
BOSS_MONSTERS = BOSSES | {"THE_CHAMP", "THE_COLLECTOR", "BRONZE_AUTOMATON"}
ELITES = {"BOOK_OF_STABBING", "GREMLIN_LEADER", "TASKMASTER"}


def select(data: dict) -> list[dict]:
    if data["profile"] != "IRONCLAD_A20_ACT2":
        raise ValueError("wrong evaluation horizon")
    rows = sorted(data["result"]["seed_results"], key=lambda r: r["seed"])
    if len(rows) not in {128, 512} or [r["seed"] for r in rows] != list(range(START, START + len(rows))):
        raise ValueError("requires the fixed diagnostic range of 128 or 512 seeds")
    selected = []
    used = set()
    if any('act2_elite_entries' not in row
           or row.get('act2_entry_diagnostics_contract') != 'sls-act2-map-room-coverage-v1'
           for row in rows):
        raise ValueError('missing witnessed Act2 elite entry diagnostics; rescan')
    for role, count in (("ACT2_ORDINARY_FAILURE", 3), ("ACT2_ELITE_ENTRY", 2)):
        candidates = []
        for row in rows:
            if row["seed"] in used or "2" not in row["act_entries"]:
                continue
            enemies = set(row.get("last_context", {}).get("enemy_ids", []))
            is_elite = bool(enemies & ELITES)
            ordinary = row["reason"] == "DEATH" and enemies and not is_elite and not enemies & BOSS_MONSTERS
            if (role == "ACT2_ORDINARY_FAILURE" and ordinary) or (
                    role == "ACT2_ELITE_ENTRY" and row['act2_elite_entries']):
                candidates.append(row)
        for row in candidates[:count]:
            selected.append({"seed": row["seed"], "role": role,
                             "native_evidence": (row['act2_elite_entries'] if role == 'ACT2_ELITE_ENTRY'
                                                 else row["last_context"])})
            used.add(row["seed"])
        if len(candidates) < count:
            raise ValueError(f"missing coverage for {role}; extend deterministically to 512, never fabricate")
    for boss in sorted(BOSSES):
        candidates = [r for r in rows if r["seed"] not in used and f"ACT_2:{boss}" in r["entered_bosses"]]
        if not candidates:
            raise ValueError(f"missing actual entry for {boss}; extend deterministically to 512")
        row = candidates[0]
        selected.append({"seed": row["seed"], "role": f"ACT2_BOSS_ENTRY:{boss}",
                         "native_evidence": row["entered_bosses"]})
        used.add(row["seed"])
    return sorted(selected, key=lambda r: r["seed"])


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("scout", type=Path)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    if args.output.exists():
        raise FileExistsError("refuse to overwrite frozen canary selection")
    data = json.loads(args.scout.read_text(encoding="utf-8"))
    if data["simulator"]["native_source_sha256"] != native_source_digest():
        raise ValueError("diagnostic scout uses stale simulator sources")
    args.output.write_text(json.dumps({"schema": "sls-act2-canary-selection-v1",
        "purpose": "COVERAGE_ONLY_NOT_WIN_RATE_OR_CHECKPOINT_SELECTION",
        "scout_sha256": hashlib.sha256(args.scout.read_bytes()).hexdigest(),
        "model_sha256": data["model_sha256"], "native_source_sha256": native_source_digest(),
        "selection_rule": "first eligible seeds; 3 ordinary deaths, 2 witnessed elite entries, 3 distinct bosses",
        "runs": select(data)}, indent=2) + "\n", encoding="utf-8")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
