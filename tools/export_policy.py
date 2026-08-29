"""Export a strict standalone live-game policy artifact."""

from __future__ import annotations

import argparse
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "src"))

from sls.runtime import export_policy_artifact  # noqa: E402


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("checkpoint", type=Path)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--ascension-min", type=int, required=True)
    parser.add_argument("--ascension-max", type=int, required=True)
    parser.add_argument(
        "--goal", choices=("ACT1", "ACT2", "ACT3", "FULLRUN", "HEART"),
        required=True,
    )
    args = parser.parse_args()
    print(export_policy_artifact(
        args.checkpoint, args.output,
        ascension_min=args.ascension_min, ascension_max=args.ascension_max,
        goal=args.goal,
    ))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
