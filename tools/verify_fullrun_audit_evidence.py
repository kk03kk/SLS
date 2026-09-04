"""Gate the 30-trajectory/9-boss and 10k-seed acceptance evidence."""

from __future__ import annotations

import argparse
import json
from collections import Counter
from pathlib import Path

BOSSES = {
    "ACT_1:HEXAGHOST", "ACT_1:SLIME_BOSS", "ACT_1:THE_GUARDIAN",
    "ACT_2:AUTOMATON", "ACT_2:COLLECTOR", "ACT_2:CHAMP",
    "ACT_3:AWAKENED_ONE", "ACT_3:TIME_EATER", "ACT_3:DONU_AND_DECA",
}


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--comparison", type=Path, action="append", default=[])
    parser.add_argument("--seed-audit", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    bosses: Counter[str] = Counter()
    failures: list[str] = []
    for path in args.comparison:
        row = json.loads(path.read_text(encoding="utf-8"))
        if row.get("matched") is not True and row.get("passed") is not True:
            failures.append(f"trajectory mismatch: {path}")
        for boss in row.get("bosses") or ():
            bosses[str(boss)] += 1
    if len(args.comparison) < 30:
        failures.append("fewer than 30 independent trajectory comparisons")
    for boss in sorted(BOSSES):
        if bosses[boss] < 3:
            failures.append(f"{boss} appears {bosses[boss]} times; require 3")
    seed_audit = json.loads(args.seed_audit.read_text(encoding="utf-8"))
    if int(seed_audit.get("requested_seeds", 0)) < 10_000 or seed_audit.get("passed") is not True:
        failures.append("10,000-seed simulator invariant audit did not pass")
    result = {
        "schema": "sls-fullrun-audit-evidence-gate-v1",
        "passed": not failures,
        "trajectory_comparisons": len(args.comparison),
        "boss_counts": dict(sorted(bosses.items())),
        "seed_audit": str(args.seed_audit.resolve()),
        "failures": failures,
    }
    args.output.parent.mkdir(parents=True, exist_ok=True)
    temporary = args.output.with_suffix(args.output.suffix + ".tmp")
    temporary.write_text(json.dumps(result, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    temporary.replace(args.output)
    print(json.dumps(result, sort_keys=True))
    return 0 if result["passed"] else 1


if __name__ == "__main__":
    raise SystemExit(main())
