"""Replay a directory of original-game traces and write a parity report."""

from __future__ import annotations

import argparse
import copy
import json
import sys
from collections import Counter
from datetime import datetime, timezone
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from spirecomm.differential import load_trace, replay_trace
from spirecomm.envs import SimulatorSTSEnv


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("trace_dir", type=Path)
    parser.add_argument("--output", type=Path)
    parser.add_argument("--ignore-rng", action="store_true")
    args = parser.parse_args()
    output = args.output or args.trace_dir / "report.json"
    rows = []
    for path in sorted(args.trace_dir.glob("seed-*.json")):
        trace = load_trace(path)
        if args.ignore_rng:
            trace = copy.deepcopy(trace)
            trace.pop("initial_rng", None)
            for step in trace.get("steps") or []:
                step.pop("rng", None)
        env = SimulatorSTSEnv()
        try:
            differences = replay_trace(env, trace)
            error = None
        except Exception as exc:  # Keep all ten seeds in the aggregate report.
            differences = []
            error = f"{type(exc).__name__}: {exc}"
        finally:
            env.close()
        first = differences[0] if differences else None
        rows.append(
            {
                "seed": trace.get("seed"),
                "encounter": (trace.get("options") or {}).get("encounter"),
                "steps": len(trace.get("steps") or []),
                "outcome": trace.get("outcome"),
                "passed": first is None and error is None,
                "first_difference": None
                if first is None
                else {
                    "path": first.path,
                    "original": first.expected,
                    "simulator": first.actual,
                },
                "error": error,
                "trace": str(path.resolve()),
            }
        )
    difference_paths = Counter(
        row["first_difference"]["path"]
        for row in rows
        if row["first_difference"] is not None
    )
    report = {
        "generated_at": datetime.now(timezone.utc).isoformat(),
        "scope": "Ironclad A0, first combat of each independent game seed",
        "rng_checked": not args.ignore_rng,
        "trace_count": len(rows),
        "passed": sum(row["passed"] for row in rows),
        "failed": sum(not row["passed"] for row in rows),
        "encounters": dict(Counter(row["encounter"] for row in rows)),
        "first_difference_paths": dict(difference_paths),
        "results": rows,
    }
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps(report, ensure_ascii=False, indent=2))
    return 0 if rows and all(row["passed"] for row in rows) else 1


if __name__ == "__main__":
    raise SystemExit(main())
