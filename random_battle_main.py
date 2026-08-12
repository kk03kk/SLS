"""CommunicationMod entry point for a minimal random battle agent."""

from __future__ import annotations

import json
from datetime import datetime
from pathlib import Path
from typing import Any

from spirecomm.envs.base_sts_env import BaseSTSEnv
from spirecomm.envs.original_sts_env import OriginalSTSEnv, StdioTransport


LOG_PATH = Path(__file__).resolve().parent / "logs" / "random_battle_agent.jsonl"


def log(event: str, **fields: Any) -> None:
    LOG_PATH.parent.mkdir(parents=True, exist_ok=True)
    record = {
        "time": datetime.now().astimezone().isoformat(timespec="seconds"),
        "event": event,
        **fields,
    }
    with LOG_PATH.open("a", encoding="utf-8") as stream:
        stream.write(json.dumps(record, ensure_ascii=False) + "\n")


class RandomBattleAgent:
    def __init__(self, env: BaseSTSEnv, seed: int | None = None) -> None:
        self.env = env
        self.seed = seed

    def run_one_battle(self, max_steps: int = 1000) -> None:
        observation, info = self.env.reset(seed=self.seed)
        log("battle_started", battle=info["battle"])

        for step_number in range(1, max_steps + 1):
            legal_count = len(info["legal_actions"])
            action_index = int(self.env.np_random.integers(legal_count))
            selected = info["legal_actions"][action_index]
            observation, reward, terminated, truncated, info = self.env.step(action_index)
            log(
                "step",
                step=step_number,
                selected=selected,
                reward=reward,
                terminated=terminated,
                battle=info["battle"],
            )
            if terminated or truncated:
                log("battle_finished", steps=step_number)
                return

        raise RuntimeError(f"Battle did not finish within {max_steps} actions")


def main() -> int:
    protocol_log = LOG_PATH.with_name("random_battle_protocol.jsonl")
    env = OriginalSTSEnv(transport=StdioTransport(log_path=protocol_log))
    try:
        RandomBattleAgent(env).run_one_battle()
    except Exception as exc:
        # Never print diagnostics to stdout: every stdout line is a game command.
        log("fatal_error", error=repr(exc))
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
