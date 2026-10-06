"""Capture local, identified stock bytecode; never publish disassemblies."""

from __future__ import annotations

import argparse
import hashlib
import json
import subprocess
import zipfile
from pathlib import Path

CLASSES = (
    "monsters.city.BookOfStabbing", "monsters.city.Byrd",
    "monsters.city.ShelledParasite", "monsters.exordium.FungiBeast",
    "monsters.exordium.SlaverBlue", "monsters.exordium.SlaverRed",
    "monsters.city.Taskmaster", "monsters.city.SnakePlant",
    "monsters.city.Champ", "monsters.city.TheCollector",
    "monsters.city.BronzeAutomaton", "monsters.city.BronzeOrb",
    "powers.FlightPower", "powers.MalleablePower", "powers.PlatedArmorPower",
    "powers.SporeCloudPower", "powers.PainfulStabsPower", "powers.EntanglePower",
    "actions.GameActionManager", "dungeons.AbstractDungeon",
    "rooms.AbstractRoom", "potions.SmokeBomb",
)


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--stock-jar", type=Path, required=True)
    parser.add_argument("--javap", type=Path, required=True)
    parser.add_argument("--output-dir", type=Path, required=True)
    parser.add_argument("--extra-classes", nargs="*", default=[],
                        help="additional stock class suffixes for flow source review")
    args = parser.parse_args()
    args.output_dir.mkdir(parents=True, exist_ok=False)
    rows = {}
    with zipfile.ZipFile(args.stock_jar) as jar:
        for short in dict.fromkeys((*CLASSES, *args.extra_classes)):
            name = "com.megacrit.cardcrawl." + short
            command = [str(args.javap), "-classpath", str(args.stock_jar), "-c", "-p", name]
            payload = subprocess.check_output(command, stderr=subprocess.STDOUT)
            target = args.output_dir / (short + ".txt")
            target.write_bytes(payload)
            rows[name] = {
                "class_sha256": hashlib.sha256(jar.read(name.replace(".", "/") + ".class")).hexdigest(),
                "bytecode_sha256": hashlib.sha256(payload).hexdigest(),
                "command": command,
            }
    (args.output_dir / "identity.json").write_text(json.dumps({
        "schema": "sls-act2-stock-bytecode-v1",
        "stock_jar_sha256": hashlib.sha256(args.stock_jar.read_bytes()).hexdigest(),
        "classes": rows,
    }, indent=2) + "\n", encoding="utf-8")
    print(f"Captured {len(rows)} stock classes locally at {args.output_dir}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
