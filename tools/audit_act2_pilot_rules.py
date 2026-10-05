"""Bounded native checks for high-impact Act2 mechanisms; not stock execution."""

from __future__ import annotations

import argparse
import hashlib
import json
import subprocess
import sys
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))
sys.path.insert(0, str(ROOT / "src"))

from sls.rl.training_contract import native_source_digest
from tools.audit_act2_rule_scenarios import STOCK_SHA, battle


def state(current) -> dict:
    snapshot = current.snapshot()
    return {"combat": snapshot["game_state"]["combat_state"], "rng": snapshot["_rng"]}


def scenarios() -> dict:
    cases = {}
    for encounter, monster, move in (
        ("SLAVERS", "TASKMASTER", "TASKMASTER_SCOURING_WHIP"),
        ("BOOK_OF_STABBING", "BOOK_OF_STABBING", "BOOK_OF_STABBING_MULTI_STAB"),
        ("BOOK_OF_STABBING", "BOOK_OF_STABBING", "BOOK_OF_STABBING_SINGLE_STAB"),
        ("GREMLIN_LEADER", "GREMLIN_LEADER", "GREMLIN_LEADER_ENCOURAGE"),
        ("CHOSEN", "CHOSEN", "CHOSEN_HEX"),
    ):
        current = battle(encounter, move=(monster, move))
        before = state(current)
        current.step("end_turn")
        cases[move] = [before, state(current)]
    current = battle("THREE_BYRDS")
    current.set_card_piles(["Anger"] * 4, [], [], [])
    trace = [state(current)]
    for _ in range(4):
        current.step("play", card_index=1, target_index=0)
        trace.append(state(current))
    cases["BYRD_FOUR_ATTACKS"] = trace
    return cases


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--stock-jar", type=Path, required=True)
    parser.add_argument("--javap", type=Path, required=True)
    parser.add_argument("--disassembly-dir", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    if hashlib.sha256(args.stock_jar.read_bytes()).hexdigest() != STOCK_SHA:
        raise ValueError("unreviewed stock JAR")
    if args.output.exists() or args.disassembly_dir.exists():
        raise FileExistsError("preserve prior audit evidence")
    args.disassembly_dir.mkdir(parents=True)
    classes = {}
    with zipfile.ZipFile(args.stock_jar) as archive:
        for name in ("BookOfStabbing", "GremlinLeader", "Taskmaster", "Byrd", "Chosen",
                     "SlaverRed", "SlaverBlue"):
            qualified = "com.megacrit.cardcrawl.monsters." + (
                "exordium." if name.startswith("Slaver") else "city.") + name
            data = subprocess.check_output([str(args.javap), "-classpath", str(args.stock_jar),
                                            "-c", "-p", qualified])
            (args.disassembly_dir / f"{name}.txt").write_bytes(data)
            classes[name] = {"class_sha256": hashlib.sha256(archive.read(
                qualified.replace(".", "/") + ".class")).hexdigest(),
                "disassembly_sha256": hashlib.sha256(data).hexdigest()}
    result = {"schema": "sls-act2-pilot-rule-evidence-v1", "stock_jar_sha256": STOCK_SHA,
              "native_source_sha256": native_source_digest(), "stock_classes": classes,
              "cases": scenarios(),
              "limitations": "Bytecode reference and controlled native states; no stock-game runtime, complete parity or policy win-rate proof."}
    args.output.parent.mkdir(parents=True, exist_ok=True)
    with args.output.open("x", encoding="utf-8") as stream:
        stream.write(json.dumps(result, indent=2) + "\n")
    print(f"Recorded {len(result['cases'])} bounded cases")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
