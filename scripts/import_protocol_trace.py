from __future__ import annotations

import argparse
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from spirecomm.differential import import_protocol_log


def main() -> int:
    parser = argparse.ArgumentParser(description="Convert a protocol JSONL log to a golden trace")
    parser.add_argument("source")
    parser.add_argument("target")
    args = parser.parse_args()
    trace = import_protocol_log(args.source, args.target)
    print(f"wrote {len(trace['steps'])} battle steps to {args.target}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
