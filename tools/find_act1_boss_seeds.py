"""Find the lowest non-negative native seeds for each Act 1 boss."""

from __future__ import annotations

import argparse
import json
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "src"))

from sls.backends.simulator import SimulatorBackend
from sls.curriculum import IRONCLAD_A0_ACT1


TARGETS = {"SLIME_BOSS", "THE_GUARDIAN", "HEXAGHOST"}


def find_lowest(max_seed: int) -> dict[str, int]:
    found: dict[str, int] = {}
    backend = SimulatorBackend(IRONCLAD_A0_ACT1)
    for seed in range(max_seed + 1):
        boss = backend.reset(seed).observation.run.visible_boss_id
        if boss in TARGETS and boss not in found:
            found[boss] = seed
            if set(found) == TARGETS:
                break
    missing = TARGETS - set(found)
    if missing:
        raise RuntimeError(
            f"bosses not found through seed {max_seed}: {', '.join(sorted(missing))}"
        )
    return dict(sorted(found.items(), key=lambda item: item[1]))


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--max-seed", type=int, default=10_000)
    args = parser.parse_args()
    if args.max_seed < 0:
        parser.error("--max-seed must be non-negative")
    print(json.dumps(find_lowest(args.max_seed), indent=2, sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
