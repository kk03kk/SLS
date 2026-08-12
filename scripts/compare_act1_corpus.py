"""Replay every recorded Act 1 original-game trace and report coverage."""

from __future__ import annotations

import argparse
import sys
from collections import Counter
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from spirecomm.differential import load_trace, replay_trace
from spirecomm.envs import SimulatorSTSEnv
from spirecomm.simulator.catalog import ACT1_ENCOUNTERS


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("corpus", nargs="?", default="logs/act1_corpus")
    parser.add_argument("--include", action="append", default=[])
    args = parser.parse_args()
    paths = sorted(Path(args.corpus).glob("**/*.json"))
    paths.extend(Path(value) for value in args.include)
    passed = Counter()
    failures = []
    for path in paths:
        trace = load_trace(path)
        env = SimulatorSTSEnv()
        try:
            differences = replay_trace(env, trace)
        finally:
            env.close()
        encounter = trace.get("options", {}).get("encounter", "UNKNOWN")
        if differences:
            failures.append((path, differences[0]))
        else:
            passed[encounter] += 1

    for encounter in ACT1_ENCOUNTERS:
        print(f"{encounter:20} PASS={passed[encounter]:3}")
    for path, difference in failures:
        print(
            f"FAIL {path}: {difference.path}: "
            f"original={difference.expected!r} simulator={difference.actual!r}"
        )
    covered = sum(passed[encounter] > 0 for encounter in ACT1_ENCOUNTERS)
    print(f"coverage={covered}/{len(ACT1_ENCOUNTERS)} traces={len(paths)} failures={len(failures)}")
    return 1 if failures else 0


if __name__ == "__main__":
    raise SystemExit(main())
