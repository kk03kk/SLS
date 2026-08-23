"""Verify portable training readiness without Original artifacts."""

from __future__ import annotations

import argparse
import json
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "src"))

from sls.validation.readiness_lock import DEFAULT_LOCK, verify_readiness_lock


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("lock", type=Path, nargs="?", default=DEFAULT_LOCK)
    parser.add_argument("--allow-dirty", action="store_true", help="development/test only")
    args = parser.parse_args()
    try:
        result = verify_readiness_lock(args.lock, require_clean=not args.allow_dirty)
    except Exception as error:
        print(json.dumps({"valid": False, "error": str(error)}, sort_keys=True), file=sys.stderr)
        return 1
    print(json.dumps(result, indent=2, sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
