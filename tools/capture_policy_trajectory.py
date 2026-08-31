"""Capture one simulator or CommunicationMod policy trajectory."""

from __future__ import annotations

import argparse
import json
import os
import signal
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "src"))

from sls.backends.original import OriginalBackend  # noqa: E402
from sls.backends.simulator import SimulatorBackend  # noqa: E402
from sls.curriculum import (  # noqa: E402
    IRONCLAD_A0_ACT1,
    IRONCLAD_A0_ACT2,
    IRONCLAD_A0_ACT3,
    IRONCLAD_A0_FULLRUN,
    IRONCLAD_A0_HEART,
    CurriculumProfile,
)
from sls.diagnostics import capture_policy_trajectory  # noqa: E402
from sls.runtime import load_policy_artifact  # noqa: E402

_PROFILES_BY_GOAL = {
    "ACT1": IRONCLAD_A0_ACT1,
    "ACT2": IRONCLAD_A0_ACT2,
    "ACT3": IRONCLAD_A0_ACT3,
    "FULLRUN": IRONCLAD_A0_FULLRUN,
    "HEART": IRONCLAD_A0_HEART,
}


def _profile_for_goal(goal: str) -> CurriculumProfile:
    try:
        return _PROFILES_BY_GOAL[goal]
    except KeyError as error:
        raise ValueError(f"canary artifact goal is unsupported: {goal}") from error


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("backend", choices=("simulator", "original"))
    parser.add_argument("artifact", type=Path)
    parser.add_argument("--seed", type=int, default=0)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--journal", type=Path)
    parser.add_argument("--max-actions", type=int)
    args = parser.parse_args()
    stopped = False

    def stop(_number: int, _frame: object) -> None:
        nonlocal stopped
        stopped = True

    signal.signal(signal.SIGINT, stop)
    signal.signal(signal.SIGTERM, stop)
    artifact = load_policy_artifact(args.artifact, device="cpu")
    profile = _profile_for_goal(artifact.metadata.goal)
    backend = (
        SimulatorBackend(profile)
        if args.backend == "simulator"
        else OriginalBackend(profile=profile)
    )
    result = capture_policy_trajectory(
        backend, artifact, backend_name=args.backend, seed=args.seed,
        output=args.output, journal=args.journal,
        max_actions=args.max_actions, stop_requested=lambda: stopped,
    )
    completion = os.environ.get("SLS_RUN_COMPLETION")
    if completion:
        target = Path(completion)
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(json.dumps({"exit_code": 0, **result}, indent=2) + "\n", encoding="utf-8")
    else:
        print(json.dumps(result, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
