"""Assemble a strict Act 1 validation round from local truth bundles."""

from __future__ import annotations

import argparse
import json
from pathlib import Path
import sys


ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "src"))

from sls.validation.expansion import assemble_expansion_round


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--truth-root", type=Path, default=ROOT / "validation-results" / "truth")
    parser.add_argument("--selection", type=Path, required=True)
    parser.add_argument("--round", type=int, required=True)
    parser.add_argument(
        "--output", type=Path,
        default=ROOT / "validation-results" / "act1-validation-expansion.json",
    )
    args = parser.parse_args()
    selection = json.loads(args.selection.read_text(encoding="utf-8"))
    previous = (
        json.loads(args.output.read_text(encoding="utf-8"))
        if args.output.exists() else None
    )
    report = assemble_expansion_round(
        args.truth_root, selection, round_number=args.round, previous=previous,
    )
    text = json.dumps(report, ensure_ascii=False, indent=2, sort_keys=True) + "\n"
    args.output.parent.mkdir(parents=True, exist_ok=True)
    temporary = args.output.with_suffix(args.output.suffix + ".tmp")
    temporary.write_text(text, encoding="utf-8")
    temporary.replace(args.output)
    print(json.dumps({"output": str(args.output), "round": args.round}, sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
