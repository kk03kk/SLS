"""Inventory content evidence without turning presence into whole-content parity."""
from __future__ import annotations

import argparse
import hashlib
import json
from collections import Counter, defaultdict
from pathlib import Path

from sls.content.normalize import (
    normalize_card_id,
    normalize_content_id,
    normalize_monster_id,
    normalize_potion_id,
)
from sls.content.scope import ironclad_scope
from sls.rl.training_contract import native_source_digest


def build(root: Path) -> dict:
    scope = ironclad_scope(20)
    inventory = {category: set(scope[category]["ids"]) for category in ("cards", "relics", "potions", "events")}
    inventory["monsters"] = set(scope["monsters"]["act1"] + scope["monsters"]["act2"] + scope["monsters"]["act2_event"])
    inventory["encounters"] = set(scope["encounters"]["act1"] + scope["encounters"]["act1_event"]
                                  + scope["encounters"]["act2"] + scope["encounters"]["act2_event"])
    evidence: dict = defaultdict(lambda: defaultdict(list))
    source = native_source_digest()
    inputs = {}
    capture, replay = root / "controlled-stock.json", root / "controlled-final.json"
    if capture.is_file() and replay.is_file():
        data, report = json.loads(capture.read_text()), json.loads(replay.read_text())
        if report["native_source_sha256"] != source:
            raise ValueError("cannot reuse stale controlled differential")
        inputs[capture.name] = hashlib.sha256(capture.read_bytes()).hexdigest()
        inputs[replay.name] = hashlib.sha256(replay.read_bytes()).hexdigest()
        passed = {r["seed"] for r in report["runs"] if r["status"] == "HARNESS_MATCH"
                  and r["first_divergence"] is None}
        for row in data["runs"]:
            if row["seed"] not in passed:
                continue
            witness = {"seed": row["seed"], "scene": row.get("scene", {}).get("id", "encounter-probe"),
                       "level": "MATCHED_BOUNDED_SCENARIO; NOT_ALL_MECHANISMS"}
            evidence["encounters"][row["encounter"]].append(witness)
            seen = defaultdict(set)
            for boundary in row["boundaries"]:
                direct = boundary["_stock_direct"]
                seen["cards"].update(normalize_card_id(c["id"]) for zone in
                                     ("hand", "draw_pile", "discard_pile", "exhaust_pile")
                                     for c in direct.get(zone, ()))
                seen["monsters"].update(normalize_monster_id(c["id"]) for c in direct["monsters"])
                seen["relics"].update(normalize_content_id(c["id"]) for c in direct.get("relics", ()))
                seen["potions"].update(normalize_potion_id(c["id"]) for c in direct.get("potions", ())
                                       if normalize_potion_id(c["id"]) != "EMPTY_POTION_SLOT")
            for category, ids in seen.items():
                for content in ids:
                    evidence[category][content].append({**witness, "level": "MATCHED_BOUNDARY_PRESENCE_ONLY"})

    # Full system replays add actual inventory presence, including conditional
    # runs only when the independently measured timing reaches the terminal.
    # This never upgrades a content item to complete mechanism qualification.
    for report_path in sorted(root.glob("systems-conditional-*.json")) + sorted(root.glob("systems-policy-conditional-*.json")):
        report = json.loads(report_path.read_text())
        if report.get("native_source_sha256") != source:
            continue
        inputs[report_path.name] = hashlib.sha256(report_path.read_bytes()).hexdigest()
        capture_path = root / report_path.name.replace("systems-conditional-", "systems-").replace("systems-policy-conditional-", "systems-policy-")
        capture = json.loads(capture_path.read_text())
        if hashlib.sha256(capture_path.read_bytes()).hexdigest() != report["stock_capture_sha256"]:
            raise ValueError("system content evidence capture changed")
        rows = {row["seed"]: row for row in capture["runs"]}
        for replay in report["runs"]:
            if (replay.get("status") not in {"SYSTEM_BRANCH_MATCH", "CONDITIONAL_SYSTEM_BRANCH_MATCH"}
                    or replay.get("first_divergence") is not None or not replay.get("terminal_reason_verified")):
                continue
            row = rows[replay["seed"]]
            raw_path = Path(row["stock_capture"])
            if hashlib.sha256(raw_path.read_bytes()).hexdigest() != row["sha256"]:
                raise ValueError("system content raw trajectory changed")
            inputs[raw_path.name] = row["sha256"]
            seen = defaultdict(set)
            for line in raw_path.read_text().splitlines():
                record = json.loads(line)
                obs = record["observation"]
                seen["cards"].update(c["card_id"] for zone in
                                     ("deck", "hand", "draw_pile", "discard_pile", "exhaust_pile")
                                     for c in obs[zone])
                seen["monsters"].update(e["monster_id"] for e in obs["enemies"])
                for category in ("relics", "potions"):
                    seen[category].update(c["content_id"] for c in obs[category])
                event = record["stock_raw"].get("_continuation", {}).get("event_id")
                if obs["screen"] == "EVENT" and event:
                    seen["events"].add(normalize_content_id(event))
            for category, ids in seen.items():
                for content in ids & inventory[category]:
                    evidence[category][content].append({"seed": row["seed"],
                        "report": report_path.name, "level": "MATCHED_BOUNDARY_PRESENCE_ONLY",
                        "timing_conditioned": replay["status"].startswith("CONDITIONAL")})
    history_path = Path("local/audits/a20-act1-semantic-coverage-final.json")
    historical = defaultdict(lambda: defaultdict(list))
    if history_path.is_file():
        history = json.loads(history_path.read_text())
        inputs[history_path.as_posix()] = hashlib.sha256(history_path.read_bytes()).hexdigest()
        for row in history["obligations"]:
            historical[row["category"]][row["content_id"]].append({
                "obligation_id": row["obligation_id"], "method": row.get("java_method"),
                "status": row["status"], "current_qualification": False})
    categories = {}
    for category, ids in inventory.items():
        rows = []
        for content in sorted(ids):
            witnessed = evidence[category].get(content, [])
            rows.append({"id": content, "status": "CURRENT_BOUNDED_EVIDENCE" if witnessed else
                         "HISTORICAL_OBLIGATIONS_NEED_CURRENT_REPLAY" if historical[category].get(content)
                         else "NO_CURRENT_BOUNDED_EVIDENCE",
                         "whole_content_qualified": False, "current_witnesses": witnessed,
                         "historical_method_obligations": historical[category].get(content, [])})
        categories[category] = {"registered": len(ids), "status_counts": dict(Counter(r["status"] for r in rows)),
                                "whole_content_qualified": 0, "items": rows}
    return {"schema": "sls-act12-content-evidence-inventory-v1", "native_source_sha256": source,
            "scope_sha256": scope["scope_sha256"], "input_sha256": inputs, "categories": categories,
            "limits": ["cards/relics/potions use conservative global Ironclad inventory",
                       "events use global registry inventory; late-only events not claimed Act1-2 reachable",
                       "encounters and monsters restrict registered inventory to Acts1-2",
                       "Presence is not effect execution or complete mechanism/interaction qualification",
                       "This inventory is separate from the frozen 72-obligation qualification"]}


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    if args.output.exists():
        raise FileExistsError("immutable inventory exists")
    report = build(args.root)
    args.output.write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({k: {n: v[n] for n in ("registered", "status_counts", "whole_content_qualified")}
                      for k, v in report["categories"].items()}))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
