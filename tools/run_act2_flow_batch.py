"""Run one source-bound stock flow batch, with recovery and a 30-minute bound."""

from __future__ import annotations

import argparse
import sys
from pathlib import Path

from tools.verify_oracle import inspect, runtime_smoke


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("mode", choices=("validation", "production"))
    parser.add_argument("--oracle", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--scripts", type=Path)
    parser.add_argument("--artifact", type=Path)
    parser.add_argument("--seeds", type=int, nargs="+", required=True)
    parser.add_argument("--timeout", type=float, default=1800)
    args = parser.parse_args()
    if not 0 < args.timeout <= 1800:
        raise ValueError("flow batch timeout must be <=30 minutes")
    inspect(args.oracle)
    if args.mode == "validation":
        if (args.scripts is None) == (args.artifact is None):
            parser.error("validation requires exactly one of --scripts or --artifact")
        tool = "capture_act2_system_batch.py" if args.scripts else "capture_act2_stock_policy_system_batch.py"
        extra = (["--scripts", args.scripts.resolve().as_posix()] if args.scripts else
                 ["--artifact", args.artifact.resolve().as_posix()])
    else:
        if args.artifact is None or args.scripts is not None:
            parser.error("production requires --artifact only")
        tool = "capture_act2_production_batch.py"
        extra = ["--artifact", args.artifact.resolve().as_posix()]
    command = [Path(sys.executable).as_posix(), Path(__file__).with_name(tool).as_posix(),
               "--output", args.output.resolve().as_posix(), *extra,
               "--seeds", *(str(seed) for seed in args.seeds)]
    print(runtime_smoke(args.oracle.resolve(), args.output.resolve(), args.mode, None,
                        args.timeout, capture_command=command))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
