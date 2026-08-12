"""Print deterministic enemy move traces without needing the graphical game."""

from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from spirecomm.checkpoints import export_combat_checkpoint
from spirecomm.envs import SimulatorSTSEnv
from spirecomm.simulator.catalog import ACT1_ENCOUNTERS


def durable_checkpoint(payload):
    checkpoint = export_combat_checkpoint(payload)
    game = checkpoint["game_state"]
    player = game["combat_state"]["player"]
    player["current_hp"] = 999
    player["max_hp"] = 999
    game["current_hp"] = 999
    game["max_hp"] = 999
    return checkpoint


def enemy_frame(payload):
    combat = payload["game_state"]["combat_state"]
    return {
        "turn": combat["turn"],
        "player_hp": combat["player"]["current_hp"],
        "player_powers": combat["player"]["powers"],
        "monsters": [
            {
                "id": monster["monster_id"],
                "hp": monster["current_hp"],
                "max_hp": monster["max_hp"],
                "block": monster["block"],
                "move": monster["move_id"],
                "intent": monster["intent"],
                "damage": monster["move_adjusted_damage"],
                "hits": monster["move_hits"],
                "powers": monster["powers"],
            }
            for monster in combat["monsters"]
        ],
    }


def trace(encounter: str, seed: int, turns: int):
    source = SimulatorSTSEnv(encounter=encounter)
    env = SimulatorSTSEnv()
    try:
        source.reset(seed=seed)
        env.reset(options={"checkpoint": durable_checkpoint(source.payload)})
        frames = []
        for _ in range(turns):
            frames.append(enemy_frame(env.payload))
            end_turn = next(
                index for index, action in enumerate(env.legal_actions)
                if action.kind == "end_turn"
            )
            _, _, terminated, _, _ = env.step(end_turn)
            if terminated:
                break
        return {"encounter": encounter, "seed": seed, "frames": frames}
    finally:
        source.close()
        env.close()


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--encounter", choices=("ALL",) + ACT1_ENCOUNTERS, default="ALL")
    parser.add_argument("--seed", type=int, default=0)
    parser.add_argument("--seeds", type=int, default=1)
    parser.add_argument("--turns", type=int, default=8)
    args = parser.parse_args()
    encounters = ACT1_ENCOUNTERS if args.encounter == "ALL" else (args.encounter,)
    for encounter in encounters:
        for seed in range(args.seed, args.seed + args.seeds):
            print(json.dumps(trace(encounter, seed, args.turns), separators=(",", ":")))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
