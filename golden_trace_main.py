"""CommunicationMod entry point that records one original-game golden trace."""

from __future__ import annotations

import os
from pathlib import Path

from spirecomm.differential import record_episode
from spirecomm.envs import OriginalSTSEnv
from spirecomm.envs.original_sts_env import StdioTransport


ROOT = Path(__file__).resolve().parent
TRACE_PATH = Path(os.environ.get("STS_GOLDEN_TRACE", ROOT / "logs" / "golden_original.json"))


def main() -> int:
    env = OriginalSTSEnv(
        transport=StdioTransport(log_path=ROOT / "logs" / "golden_protocol.jsonl")
    )
    record_episode(
        env,
        lambda _observation, info: int(env.np_random.integers(len(info["legal_actions"]))),
        TRACE_PATH,
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
