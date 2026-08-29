"""Attach a production policy to the currently running Ironclad game."""

from __future__ import annotations

import argparse
import signal
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "src"))

from sls.backends.original import LiveGameBackend  # noqa: E402
from sls.runtime import AgentRuntime, load_policy_artifact  # noqa: E402


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("artifact", type=Path)
    parser.add_argument("--device", default="cpu")
    parser.add_argument("--log", type=Path, default=ROOT / "logs" / "live-agent.jsonl")
    parser.add_argument("--low-confidence", type=float, default=0.55)
    parser.add_argument("--max-actions", type=int)
    parser.add_argument("--wait-for-neow", action="store_true")
    parser.add_argument("--wait-timeout", type=float, default=600.0)
    args = parser.parse_args()
    stopped = False

    def stop(_number: int, _frame: object) -> None:
        nonlocal stopped
        stopped = True

    signal.signal(signal.SIGINT, stop)
    signal.signal(signal.SIGTERM, stop)
    loaded = load_policy_artifact(args.artifact, device=args.device)
    runtime = AgentRuntime(
        LiveGameBackend(
            wait_for_neow=args.wait_for_neow,
            wait_timeout_seconds=args.wait_timeout,
        ), loaded, device=args.device,
        log_path=args.log, low_confidence=args.low_confidence,
    )
    final = runtime.run(max_actions=args.max_actions, stop_requested=lambda: stopped)
    print(
        f"live agent stopped at {final.observation.screen.value} "
        f"act={final.observation.run.act} floor={final.observation.run.floor}",
        file=sys.stderr,
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
