"""Recover interrupted truth recordings without treating them as acceptance evidence."""

from __future__ import annotations

import argparse
from pathlib import Path
import sys


ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "src"))

from sls.validation.truth import recover_partial_bundle


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("root", type=Path, nargs="?", default=ROOT / "validation-results" / "truth")
    args = parser.parse_args()
    failures = 0
    for path in sorted(args.root.glob("*.partial")):
        try:
            print(recover_partial_bundle(path))
        except (OSError, ValueError, KeyError) as error:
            failures += 1
            print(f"refused {path}: {error}", file=sys.stderr)
    return 2 if failures else 0


if __name__ == "__main__":
    raise SystemExit(main())
