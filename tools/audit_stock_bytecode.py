from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "src"
if str(SRC) not in sys.path:
    sys.path.insert(0, str(SRC))

from sls.audit.bytecode_inventory import build_bytecode_inventory


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description=(
            "Index stock-game content classes and link their bytecode evidence "
            "to simulator source references."
        ),
    )
    parser.add_argument("--root", type=Path, default=ROOT)
    parser.add_argument("--stock-jar", type=Path, required=True)
    parser.add_argument("--javap", type=Path, required=True)
    parser.add_argument(
        "--category",
        action="append",
        choices=("cards", "monsters", "potions", "relics", "events"),
        dest="categories",
    )
    parser.add_argument("--workers", type=int, default=8)
    parser.add_argument("--output", type=Path, required=True)
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    inventory = build_bytecode_inventory(
        root=args.root.resolve(),
        stock_jar=args.stock_jar.resolve(),
        javap=args.javap.resolve(),
        categories=args.categories or (
            "cards", "monsters", "potions", "relics", "events",
        ),
        workers=args.workers,
    )
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(
        json.dumps(inventory, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )
    print(json.dumps(inventory["summary"], ensure_ascii=False, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
