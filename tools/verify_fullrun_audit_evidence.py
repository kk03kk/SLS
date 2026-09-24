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


def verify_evidence(comparisons: list[Path], seed_audit_path: Path) -> dict[str, object]:
    bosses: Counter[str] = Counter()
    failures: list[str] = []
    seen_seeds: set[int] = set()
    seen_sources: set[str] = set()
    accepted = 0
    for path in comparisons:
        row = json.loads(path.read_text(encoding="utf-8"))
        if (
            row.get("schema") != "sls-policy-trajectory-comparison-v2"
            or any(row.get(field) is not True for field in (
                "passed", "contract_match", "seed_match", "backend_match", "trajectory_complete",
            ))
            or row.get("first_divergence") is not None
            or type(row.get("matched_boundaries")) is not int
            or row["matched_boundaries"] <= 0
            or row["matched_boundaries"] != row.get("simulator_boundaries")
            or row["matched_boundaries"] != row.get("original_boundaries")
        ):
            failures.append(f"incomplete or mismatched trajectory evidence: {path}")
            continue
        sources = [row.get("simulator_sha256"), row.get("original_sha256")]
        seed = row.get("seed")
        if type(seed) is not int or any(
            not isinstance(digest, str) or len(digest) != 64
            or any(character not in "0123456789abcdef" for character in digest)
            for digest in sources
        ):
            failures.append(f"missing trajectory identity; regenerate comparison: {path}")
            continue
        seed %= 2**64
        if seed in seen_seeds or any(digest in seen_sources for digest in sources):
            failures.append(f"duplicate trajectory evidence: {path}")
            continue
        seen_seeds.add(seed)
        seen_sources.update(sources)
        accepted += 1
        for boss in set(row.get("bosses") or ()):
            bosses[str(boss)] += 1
    if accepted < 30:
        failures.append("fewer than 30 independent trajectory comparisons")
    for boss in sorted(BOSSES):
        if bosses[boss] < 3:
            failures.append(f"{boss} appears {bosses[boss]} times; require 3")
    seed_audit = json.loads(seed_audit_path.read_text(encoding="utf-8"))
    count = seed_audit.get("requested_seeds")
    seed_range = seed_audit.get("seed_range")
    if (
        seed_audit.get("schema") != "sls-simulator-seed-invariants-v1"
        or type(count) is not int or count < 10_000
        or seed_audit.get("completed_seeds") != count
        or seed_audit.get("passed") is not True
        or seed_audit.get("failures") != []
        or not isinstance(seed_range, list) or len(seed_range) != 2
        or any(type(value) is not int for value in seed_range)
        or seed_range[1] - seed_range[0] != count
    ):
        failures.append("10,000-seed simulator invariant audit did not pass")
    return {
        "schema": "sls-fullrun-audit-evidence-gate-v1",
        "passed": not failures,
        "trajectory_comparisons": accepted,
        "boss_counts": dict(sorted(bosses.items())),
        "seed_audit": str(seed_audit_path.resolve()),
        "failures": failures,
    }


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--comparison", type=Path, action="append", default=[])
    parser.add_argument("--seed-audit", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    result = verify_evidence(args.comparison, args.seed_audit)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    temporary = args.output.with_suffix(args.output.suffix + ".tmp")
    temporary.write_text(json.dumps(result, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    temporary.replace(args.output)
    print(json.dumps(result, sort_keys=True))
    return 0 if result["passed"] else 1


if __name__ == "__main__":
    raise SystemExit(main())
