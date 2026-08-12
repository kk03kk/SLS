"""CommunicationMod entry point for collecting strict Act 1 battle traces."""

from __future__ import annotations

import json
import os
from datetime import datetime
from pathlib import Path

from spirecomm.differential import record_episode
from spirecomm.envs import OriginalSTSEnv
from spirecomm.envs.original_sts_env import StdioTransport


ROOT = Path(__file__).resolve().parent
CORPUS = ROOT / "logs" / "act1_corpus"
PROTOCOL_LOG = ROOT / "logs" / "act1_corpus_protocol.jsonl"
ERROR_LOG = ROOT / "logs" / "act1_corpus_errors.jsonl"


def main() -> int:
    battle_count = int(os.environ.get("STS_CORPUS_BATTLES", "1"))
    env = OriginalSTSEnv(transport=StdioTransport(log_path=PROTOCOL_LOG))
    try:
        for sequence in range(battle_count):
            temporary = CORPUS / ".recording.json"
            trace = record_episode(
                env,
                lambda _observation, info: int(
                    env.np_random.integers(len(info["legal_actions"]))
                ),
                temporary,
            )
            encounter = trace["options"]["encounter"]
            floor = trace["initial"].get("floor", 0)
            seed = trace.get("seed", 0)
            stamp = datetime.now().strftime("%Y%m%d-%H%M%S-%f")
            destination = CORPUS / encounter / f"{seed}-f{floor}-{sequence}-{stamp}.json"
            destination.parent.mkdir(parents=True, exist_ok=True)
            temporary.replace(destination)
    except Exception as exc:
        ERROR_LOG.parent.mkdir(parents=True, exist_ok=True)
        with ERROR_LOG.open("a", encoding="utf-8") as stream:
            stream.write(json.dumps({"error": repr(exc)}, ensure_ascii=True) + "\n")
        return 1
    finally:
        env.close()
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
