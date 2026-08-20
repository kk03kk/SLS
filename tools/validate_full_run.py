"""CommunicationMod entry point for one canonical paired FullRun validation."""

from __future__ import annotations

import argparse
import os
from pathlib import Path
import sys


ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "src"))

from sls.backends.original import OriginalBackend, OriginalSession, StdioTransport
from sls.backends.simulator import (
    IRONCLAD_A0_ACT1,
    IRONCLAD_A0_ACT2,
    IRONCLAD_A0_ACT3,
    IRONCLAD_A0_HEART,
    SimulatorBackend,
)
from sls.validation import run_paired


PROFILES = {
    profile.profile_id: profile
    for profile in (
        IRONCLAD_A0_ACT1,
        IRONCLAD_A0_ACT2,
        IRONCLAD_A0_ACT3,
        IRONCLAD_A0_HEART,
    )
}


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--seed", type=int, default=int(os.environ.get("SLS_PARITY_SEED", "0")))
    parser.add_argument(
        "--profile",
        choices=sorted(PROFILES),
        default=os.environ.get("SLS_PARITY_PROFILE", "IRONCLAD_A0_HEART"),
    )
    parser.add_argument("--max-steps", type=int, default=int(os.environ.get("SLS_PARITY_MAX_STEPS", "10000")))
    parser.add_argument(
        "--output",
        type=Path,
        default=Path(os.environ.get("SLS_PARITY_OUTPUT", "validation-results/full-run.json")),
    )
    parser.add_argument("--without-rng", action="store_true")
    args = parser.parse_args()
    protocol_log = os.environ.get("SLS_PROTOCOL_LOG")
    transport = StdioTransport(
        log_path=Path(protocol_log).resolve() if protocol_log else None,
    )
    profile = PROFILES[args.profile]
    trace = run_paired(
        OriginalBackend(OriginalSession(transport), profile),
        SimulatorBackend(profile),
        seed=args.seed,
        max_steps=args.max_steps,
        include_rng=not args.without_rng,
    )
    path = trace.write(args.output)
    print(
        f"PARITY seed={args.seed} steps={len(trace.steps)} "
        f"complete={trace.complete} matches={trace.matches} output={path}",
        file=sys.stderr,
        flush=True,
    )
    return 0 if trace.matches and trace.complete else 1


if __name__ == "__main__":
    raise SystemExit(main())
