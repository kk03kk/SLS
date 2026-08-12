from __future__ import annotations

import argparse
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from spirecomm.differential import load_trace, replay_trace
from spirecomm.envs import SimulatorSTSEnv


def main() -> int:
    parser = argparse.ArgumentParser(description="Replay an original-game trace in SimulatorSTSEnv")
    parser.add_argument("trace")
    args = parser.parse_args()
    differences = replay_trace(SimulatorSTSEnv(), load_trace(args.trace))
    if not differences:
        print("PASS: simulator trace matches")
        return 0
    first = differences[0]
    print(f"FAIL: first divergence at {first.path}")
    print(f"  original:  {first.expected!r}")
    print(f"  simulator: {first.actual!r}")
    return 1


if __name__ == "__main__":
    raise SystemExit(main())
