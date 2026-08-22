"""Evaluate curriculum parity evidence without weakening final acceptance."""

from __future__ import annotations

import argparse
import json
from pathlib import Path
import sys
import tomllib

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "src"))

from sls.validation.readiness import readiness_report


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("root", type=Path, nargs="?", default=ROOT / "validation-results" / "truth")
    parser.add_argument("--config", type=Path, default=ROOT / "configs" / "validation" / "act1_training.toml")
    parser.add_argument("--output", type=Path)
    args = parser.parse_args()
    with args.config.open("rb") as stream:
        config = tomllib.load(stream)
    report = readiness_report(args.root, config["requirements"])
    text = json.dumps(report, ensure_ascii=False, indent=2, sort_keys=True) + "\n"
    if args.output:
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(text, encoding="utf-8")
    else:
        print(text, end="")
    return 0 if report["ready"] else 1


if __name__ == "__main__":
    raise SystemExit(main())
