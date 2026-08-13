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
    requested_seeds = os.environ.get("STS_CORPUS_SEEDS")
    if requested_seeds:
        seeds = [int(value.strip()) for value in requested_seeds.split(",") if value.strip()]
    else:
        battle_count = int(os.environ.get("STS_CORPUS_BATTLES", "1"))
        requested_seed = os.environ.get("STS_CORPUS_SEED")
        seed = int(requested_seed) if requested_seed not in {None, ""} else None
        seeds = [seed] * battle_count
    exact_output = os.environ.get("STS_CORPUS_OUTPUT")
    output_dir = os.environ.get("STS_CORPUS_OUTPUT_DIR")
    if exact_output and len(seeds) != 1:
        raise ValueError("STS_CORPUS_OUTPUT requires exactly one seed")
    env = OriginalSTSEnv(transport=StdioTransport(log_path=PROTOCOL_LOG))
    try:
        for sequence, seed in enumerate(seeds):
            temporary = CORPUS / ".recording.json"
            trace = record_episode(
                env,
                lambda _observation, info: int(
                    env.np_random.integers(len(info["legal_actions"]))
                ),
                temporary,
                seed=seed,
            )
            encounter = trace["options"]["encounter"]
            floor = trace["initial"].get("floor", 0)
            seed = trace.get("seed", 0)
            stamp = datetime.now().strftime("%Y%m%d-%H%M%S-%f")
            if exact_output:
                destination = Path(exact_output)
            elif output_dir:
                destination = Path(output_dir) / f"seed-{seed}.json"
            else:
                destination = CORPUS / encounter / f"{seed}-f{floor}-{sequence}-{stamp}.json"
            destination.parent.mkdir(parents=True, exist_ok=True)
            temporary.replace(destination)
            if sequence + 1 < len(seeds):
                env.return_to_menu()
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
