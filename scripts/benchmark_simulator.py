from __future__ import annotations

import argparse
import sys
import time
from pathlib import Path

import numpy as np

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from spirecomm.envs import SimulatorSTSEnv


def main() -> int:
    parser = argparse.ArgumentParser(description="Benchmark random headless battles")
    parser.add_argument("--episodes", type=int, default=1000)
    parser.add_argument("--seed", type=int, default=0)
    parser.add_argument("--encounter", default="JAW_WORM")
    args = parser.parse_args()

    env = SimulatorSTSEnv(encounter=args.encounter)
    rng = np.random.default_rng(args.seed)
    steps = 0
    outcomes: dict[str, int] = {}
    started = time.perf_counter()
    for offset in range(args.episodes):
        _, info = env.reset(seed=args.seed + offset)
        for _ in range(1000):
            action = int(rng.choice(np.flatnonzero(env.action_masks())))
            _, _, terminated, truncated, info = env.step(action)
            steps += 1
            if terminated or truncated:
                outcome = str(info.get("outcome"))
                outcomes[outcome] = outcomes.get(outcome, 0) + 1
                break
        else:
            raise RuntimeError(f"episode {offset} did not terminate")
    elapsed = time.perf_counter() - started
    print(
        {
            "episodes": args.episodes,
            "steps": steps,
            "seconds": round(elapsed, 6),
            "episodes_per_second": round(args.episodes / elapsed, 2),
            "steps_per_second": round(steps / elapsed, 2),
            "outcomes": outcomes,
        }
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
