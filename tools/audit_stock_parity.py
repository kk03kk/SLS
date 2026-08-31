from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "src"
if str(SRC) not in sys.path:
    sys.path.insert(0, str(SRC))

from sls.audit.card_parity import audit_card_scenarios
from sls.audit.encounter_parity import audit_encounter_scenarios
from sls.audit.event_parity import audit_event_scenarios
from sls.audit.mechanism_parity import audit_mechanism_scenarios
from sls.audit.potion_parity import audit_potion_scenarios
from sls.audit.relic_parity import audit_relic_scenarios
from sls.audit.stock_parity import build_stock_parity_manifest


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Build an evidence-status inventory for stock-game parity.",
    )
    parser.add_argument("--stock-jar", type=Path, required=True)
    parser.add_argument("--oracle-jar", type=Path, required=True)
    parser.add_argument("--original-log", type=Path, action="append", default=[])
    parser.add_argument("--card-results", type=Path)
    parser.add_argument("--potion-results", type=Path)
    parser.add_argument("--relic-results", type=Path)
    parser.add_argument("--encounter-results", type=Path)
    parser.add_argument("--event-results", type=Path)
    parser.add_argument("--mechanism-results", type=Path)
    parser.add_argument("--output", type=Path, required=True)
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    manifest = build_stock_parity_manifest(
        stock_jar=args.stock_jar,
        oracle_jar=args.oracle_jar,
        original_logs=args.original_log,
    )
    if args.card_results is not None:
        card_results = audit_card_scenarios(args.original_log)
        args.card_results.parent.mkdir(parents=True, exist_ok=True)
        args.card_results.write_text(
            json.dumps(card_results, indent=2, ensure_ascii=False) + "\n",
            encoding="utf-8",
        )
        by_card: dict[str, list[str]] = {}
        for row in card_results["scenarios"].values():
            by_card.setdefault(row["card_id"], []).append(row["status"])
        passed = 0
        for row in manifest["categories"]["cards"]:
            statuses = by_card.get(row["content_id"], [])
            if statuses and all(status == "SCENARIO_MATCH" for status in statuses):
                row["simulator_parity"] = "SCENARIO_MATCH"
                passed += 1
            elif statuses:
                row["simulator_parity"] = "DIFFERENCE"
        manifest["summary"]["cards"]["simulator_parity_passed"] = passed
    if args.potion_results is not None:
        potion_results = audit_potion_scenarios(args.original_log)
        args.potion_results.parent.mkdir(parents=True, exist_ok=True)
        args.potion_results.write_text(
            json.dumps(potion_results, indent=2, ensure_ascii=False) + "\n",
            encoding="utf-8",
        )
        by_potion: dict[str, list[str]] = {}
        for row in potion_results["scenarios"].values():
            by_potion.setdefault(row["potion_id"], []).append(row["status"])
        passed = 0
        for row in manifest["categories"]["potions"]:
            statuses = by_potion.get(row["content_id"], [])
            if statuses and all(status == "SCENARIO_MATCH" for status in statuses):
                row["simulator_parity"] = "SCENARIO_MATCH"
                passed += 1
            elif "DIFFERENCE" in statuses:
                row["simulator_parity"] = "DIFFERENCE"
            elif statuses:
                row["simulator_parity"] = "TRANSIENT_BOUNDARY"
        manifest["summary"]["potions"]["simulator_parity_passed"] = passed
    if args.relic_results is not None:
        relic_results = audit_relic_scenarios(args.original_log)
        args.relic_results.parent.mkdir(parents=True, exist_ok=True)
        args.relic_results.write_text(
            json.dumps(relic_results, indent=2, ensure_ascii=False) + "\n",
            encoding="utf-8",
        )
        by_relic: dict[str, list[str]] = {}
        for row in relic_results["scenarios"].values():
            by_relic.setdefault(row["relic_id"], []).append(row["status"])
        passed = 0
        for row in manifest["categories"]["relics"]:
            statuses = by_relic.get(row["content_id"], [])
            if statuses and all(status == "SCENARIO_MATCH" for status in statuses):
                row["simulator_parity"] = "SCENARIO_MATCH"
                passed += 1
            elif "DIFFERENCE" in statuses:
                row["simulator_parity"] = "DIFFERENCE"
            elif statuses:
                row["simulator_parity"] = "ADAPTER_BOUNDARY"
        manifest["summary"]["relics"]["simulator_parity_passed"] = passed
    if args.encounter_results is not None:
        encounter_results = audit_encounter_scenarios(args.original_log)
        args.encounter_results.parent.mkdir(parents=True, exist_ok=True)
        args.encounter_results.write_text(
            json.dumps(encounter_results, indent=2, ensure_ascii=False) + "\n",
            encoding="utf-8",
        )
        by_encounter: dict[str, list[str]] = {}
        by_monster: dict[str, list[str]] = {}
        monster_scenarios: dict[str, list[str]] = {}
        for scenario_id, row in encounter_results["scenarios"].items():
            by_encounter.setdefault(row["encounter_id"], []).append(row["status"])
            for monster_id in row["monster_ids"]:
                by_monster.setdefault(monster_id, []).append(row["status"])
                monster_scenarios.setdefault(monster_id, []).append(scenario_id)
        encounter_passed = 0
        for row in manifest["categories"]["encounters"]:
            statuses = by_encounter.get(row["content_id"], [])
            if statuses and all(status == "SCENARIO_MATCH" for status in statuses):
                row["simulator_parity"] = "SCENARIO_MATCH"
                encounter_passed += 1
            elif statuses:
                row["simulator_parity"] = "DIFFERENCE"
        manifest["summary"]["encounters"][
            "simulator_parity_passed"
        ] = encounter_passed

        monster_captured = 0
        monster_passed = 0
        for row in manifest["categories"]["monsters"]:
            statuses = by_monster.get(row["content_id"], [])
            if not statuses:
                continue
            row["original_evidence"] = "ORIGINAL_SCENARIO_CAPTURED"
            row["original_scenarios"] = monster_scenarios[row["content_id"]]
            monster_captured += 1
            if all(status == "SCENARIO_MATCH" for status in statuses):
                row["simulator_parity"] = "SCENARIO_MATCH"
                monster_passed += 1
            else:
                row["simulator_parity"] = "DIFFERENCE"
        manifest["summary"]["monsters"]["original_captured"] = monster_captured
        manifest["summary"]["monsters"][
            "simulator_parity_passed"
        ] = monster_passed
    if args.event_results is not None:
        event_results = audit_event_scenarios(args.original_log)
        args.event_results.parent.mkdir(parents=True, exist_ok=True)
        args.event_results.write_text(
            json.dumps(event_results, indent=2, ensure_ascii=False) + "\n",
            encoding="utf-8",
        )
        by_event = {
            row["event_id"]: row["status"]
            for row in event_results["scenarios"].values()
        }
        passed = 0
        for row in manifest["categories"]["events"]:
            status = by_event.get(row["content_id"])
            if status == "SCENARIO_MATCH":
                row["simulator_parity"] = "SCENARIO_MATCH"
                passed += 1
            elif status is not None:
                row["simulator_parity"] = "DIFFERENCE"
        manifest["summary"]["events"]["simulator_parity_passed"] = passed
    if args.mechanism_results is not None:
        mechanism_results = audit_mechanism_scenarios(args.original_log)
        args.mechanism_results.parent.mkdir(parents=True, exist_ok=True)
        args.mechanism_results.write_text(
            json.dumps(mechanism_results, indent=2, ensure_ascii=False) + "\n",
            encoding="utf-8",
        )
        manifest["mechanisms"] = mechanism_results["summary"]
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(
        json.dumps(manifest, indent=2, ensure_ascii=False) + "\n",
        encoding="utf-8",
    )
    print(json.dumps(manifest["summary"], indent=2, ensure_ascii=False))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
