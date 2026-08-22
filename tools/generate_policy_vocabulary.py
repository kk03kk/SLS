"""Generate or verify the committed collision-free policy vocabulary."""

from __future__ import annotations

import argparse
import json
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "src"))

from sls.model.encoding import VOCABULARY_PATH, build_policy_vocabulary


def encoded() -> str:
    return json.dumps(
        build_policy_vocabulary(), ensure_ascii=False, indent=2, sort_keys=True,
    ) + "\n"


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    expected = encoded()
    if args.check:
        if not VOCABULARY_PATH.exists() or VOCABULARY_PATH.read_text(encoding="utf-8") != expected:
            print(f"stale policy vocabulary: {VOCABULARY_PATH}", file=sys.stderr)
            return 1
        print(f"policy vocabulary: OK ({build_policy_vocabulary()['sha256']})")
        return 0
    VOCABULARY_PATH.parent.mkdir(parents=True, exist_ok=True)
    temporary = VOCABULARY_PATH.with_suffix(".json.tmp")
    temporary.write_text(expected, encoding="utf-8")
    temporary.replace(VOCABULARY_PATH)
    print(VOCABULARY_PATH)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
