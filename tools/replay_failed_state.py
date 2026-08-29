"""Replay a fail-fast Simulator decision boundary from a worker crash dump."""

from __future__ import annotations

import argparse
import json
from pathlib import Path
import sys


ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "src"))

from sls.backends.simulator import SimulatorBackend
from sls.curriculum import (
    IRONCLAD_A0_ACT1, IRONCLAD_A0_ACT2, IRONCLAD_A0_ACT3, IRONCLAD_A0_HEART,
)
from sls.rl.workers import CRASH_DUMP_SCHEMA, _action_groups, _option_groups


PROFILES = {
    profile.profile_id: profile
    for profile in (
        IRONCLAD_A0_ACT1, IRONCLAD_A0_ACT2, IRONCLAD_A0_ACT3, IRONCLAD_A0_HEART,
    )
}


def replay_dump(path: Path) -> dict[str, object]:
    payload = json.loads(path.read_text(encoding="utf-8"))
    if payload.get("schema") != CRASH_DUMP_SCHEMA:
        raise ValueError("unsupported simulator crash dump")
    profile_id = str(payload["profile"])
    try:
        profile = PROFILES[profile_id]
    except KeyError as error:
        raise ValueError(f"unknown curriculum profile: {profile_id}") from error
    state = payload.get("raw_backend_state")
    if not isinstance(state, dict) or not state:
        raise ValueError("crash dump does not contain a replayable backend state")
    backend = SimulatorBackend(profile)
    decision = backend.load_checkpoint(state)
    restored_raw = backend.raw_state
    return {
        "schema": "sls-simulator-crash-replay-v1",
        "source_dump": str(path.resolve()),
        "source": {
            "schema": payload["schema"],
            "error": payload.get("error"),
            "profile": profile_id,
            "seed": payload.get("seed"),
            "worker_index": payload.get("worker_index"),
            "worker_episode_ordinal": payload.get("worker_episode_ordinal"),
            "last_semantic_action": payload.get("last_semantic_action"),
            "terminal_flag": payload.get("terminal_flag"),
            "screen": payload.get("screen"),
            "public_run_state": payload.get("public_run_state"),
            "public_combat": payload.get("public_combat"),
            "inventory": payload.get("inventory"),
            "rng_checkpoint": payload.get("rng_checkpoint"),
            "generated_actions": payload.get("generated_actions"),
            "available_option_groups": payload.get("available_option_groups"),
            "raw_legal_action_groups": payload.get("raw_legal_action_groups"),
        },
        "terminal": decision.terminal,
        "screen": decision.observation.screen.value,
        "actions": [action.to_dict() for action in decision.actions],
        "restored": {
            "observation": decision.observation.to_dict(),
            "terminal": decision.terminal,
            "actions": [action.to_dict() for action in decision.actions],
            "public_run_state": restored_raw.get("public_run"),
            "public_combat": restored_raw.get("public_combat"),
            "inventory": restored_raw.get("public_inventory"),
            "rng_checkpoint": (
                (restored_raw.get("combat_checkpoint") or {}).get("rng")
                if restored_raw.get("public_combat") else restored_raw.get("rng")
            ),
            "available_option_groups": _option_groups(restored_raw),
            "raw_legal_action_groups": _action_groups(restored_raw),
        },
    }


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--dump", type=Path, required=True)
    args = parser.parse_args()
    print(json.dumps(replay_dump(args.dump), indent=2, sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
