"""Versioned, JSON-safe checkpoints used by lockstep differential tests."""

from __future__ import annotations

import json
from copy import deepcopy
from pathlib import Path
from typing import Any


CHECKPOINT_SCHEMA_VERSION = 1
FULL_RUN_CHECKPOINT_SCHEMA_VERSION = 2
FULL_RUN_CHECKPOINT_KIND = "sts1_full_run"

FULL_RUN_RNG_STREAMS = (
    "ai",
    "card_random",
    "card",
    "event",
    "math_util",
    "merchant",
    "misc",
    "monster_hp",
    "monster",
    "neow",
    "potion",
    "relic",
    "shuffle",
    "treasure",
)

FULL_RUN_ORDERED_POOLS = (
    "events",
    "shrines",
    "special_one_time_events",
    "common_relics",
    "uncommon_relics",
    "rare_relics",
    "shop_relics",
    "boss_relics",
    "colorless_cards",
    "normal_encounters",
    "elite_encounters",
)

FULL_RUN_DERIVED_RNG = ("map",)


def export_combat_checkpoint(payload: dict[str, Any]) -> dict[str, Any]:
    game_state = payload.get("game_state")
    if not isinstance(game_state, dict) or not game_state.get("combat_state"):
        raise ValueError("A live combat payload is required")
    return {
        "schema_version": CHECKPOINT_SCHEMA_VERSION,
        "game_state": deepcopy(game_state),
        "rng": deepcopy(payload.get("_rng")),
        "legal_actions": deepcopy(payload.get("_legal_actions", [])),
    }


def save_combat_checkpoint(payload: dict[str, Any], path: str | Path) -> None:
    Path(path).write_text(
        json.dumps(export_combat_checkpoint(payload), ensure_ascii=False, indent=2),
        encoding="utf-8",
    )


def load_combat_checkpoint(path: str | Path) -> dict[str, Any]:
    checkpoint = json.loads(Path(path).read_text(encoding="utf-8"))
    if checkpoint.get("schema_version") != CHECKPOINT_SCHEMA_VERSION:
        raise ValueError(f"Unsupported combat checkpoint: {checkpoint.get('schema_version')}")
    return checkpoint


def _validate_rng_state(name: str, state: Any) -> None:
    if not isinstance(state, dict):
        raise ValueError(f"RNG stream {name!r} must be an object")
    if set(state) != {"counter", "seed0", "seed1"}:
        raise ValueError(
            f"RNG stream {name!r} must contain counter, seed0 and seed1 only"
        )
    counter = state["counter"]
    if isinstance(counter, bool) or not isinstance(counter, int) or counter < 0:
        raise ValueError(f"RNG stream {name!r} has an invalid counter")
    for key in ("seed0", "seed1"):
        value = state[key]
        if isinstance(value, bool) or not isinstance(value, int) or not 0 <= value < 2**64:
            raise ValueError(f"RNG stream {name!r} has an invalid {key}")


def validate_full_run_checkpoint(checkpoint: dict[str, Any]) -> None:
    """Reject incomplete state before it can silently produce a divergent run."""

    if not isinstance(checkpoint, dict):
        raise ValueError("Full-run checkpoint must be an object")
    if checkpoint.get("schema_version") != FULL_RUN_CHECKPOINT_SCHEMA_VERSION:
        raise ValueError(
            f"Unsupported full-run checkpoint: {checkpoint.get('schema_version')}"
        )
    if checkpoint.get("checkpoint_kind") != FULL_RUN_CHECKPOINT_KIND:
        raise ValueError(f"Unsupported checkpoint kind: {checkpoint.get('checkpoint_kind')}")
    if not isinstance(checkpoint.get("reference_build"), dict):
        raise ValueError("Full-run checkpoint requires reference_build identity")
    if not isinstance(checkpoint.get("run_state"), dict):
        raise ValueError("Full-run checkpoint requires run_state")
    run_state = checkpoint["run_state"]
    for key in ("seed", "math_seed"):
        value = run_state.get(key)
        if isinstance(value, bool) or not isinstance(value, int) or not 0 <= value < 2**64:
            raise ValueError(f"Full-run checkpoint run_state requires a valid {key}")
    if not isinstance(checkpoint.get("legal_actions"), list):
        raise ValueError("Full-run checkpoint legal_actions must be a list")
    for key in ("player_state", "progress_state", "screen_info"):
        if not isinstance(checkpoint.get(key), dict):
            raise ValueError(f"Full-run checkpoint requires {key}")
    progress = checkpoint["progress_state"]
    screen = checkpoint["screen_info"]
    if progress.get("screen_continuation_serialized") is not True:
        raise ValueError("Full-run checkpoint requires a serialized screen continuation")
    if screen.get("complete") is not True:
        raise ValueError("Full-run checkpoint screen_info is explicitly incomplete")
    if screen.get("screen_state") != progress.get("screen_state"):
        raise ValueError("Full-run checkpoint screen state fields do not agree")

    rng = checkpoint.get("rng")
    if not isinstance(rng, dict) or set(rng) != set(FULL_RUN_RNG_STREAMS):
        raise ValueError("Full-run checkpoint must contain exactly all 14 RNG streams")
    for name in FULL_RUN_RNG_STREAMS:
        _validate_rng_state(name, rng[name])

    pools = checkpoint.get("ordered_pools")
    if not isinstance(pools, dict) or set(pools) != set(FULL_RUN_ORDERED_POOLS):
        raise ValueError("Full-run checkpoint must contain every ordered content pool")
    for name in FULL_RUN_ORDERED_POOLS:
        if not isinstance(pools[name], list):
            raise ValueError(f"Ordered pool {name!r} must be a list")

    derived_rng = checkpoint.get("derived_rng")
    if not isinstance(derived_rng, dict) or set(derived_rng) != set(FULL_RUN_DERIVED_RNG):
        raise ValueError("Full-run checkpoint must contain the map RNG derivation")
    map_rng = derived_rng["map"]
    required_map_fields = {
        "algorithm", "base_seed", "derived_seed", "act", "ascension",
        "assign_burning_elite",
    }
    if not isinstance(map_rng, dict) or set(map_rng) != required_map_fields:
        raise ValueError("Map RNG derivation has an invalid shape")
    if map_rng["algorithm"] != "sts.RandomXS128/Map.fromSeed:v1":
        raise ValueError("Map RNG derivation has an unsupported algorithm")
    for key in ("base_seed", "derived_seed"):
        value = map_rng[key]
        if isinstance(value, bool) or not isinstance(value, int) or not 0 <= value < 2**64:
            raise ValueError(f"Map RNG derivation has an invalid {key}")
    if not isinstance(map_rng["assign_burning_elite"], bool):
        raise ValueError("Map RNG derivation has an invalid burning-elite flag")


def export_full_run_checkpoint(
    *,
    reference_build: dict[str, Any],
    run_state: dict[str, Any],
    rng: dict[str, Any],
    derived_rng: dict[str, Any],
    ordered_pools: dict[str, Any],
    player_state: dict[str, Any],
    progress_state: dict[str, Any],
    screen_info: dict[str, Any],
    legal_actions: list[Any],
) -> dict[str, Any]:
    checkpoint = {
        "schema_version": FULL_RUN_CHECKPOINT_SCHEMA_VERSION,
        "checkpoint_kind": FULL_RUN_CHECKPOINT_KIND,
        "reference_build": deepcopy(reference_build),
        "run_state": deepcopy(run_state),
        "rng": deepcopy(rng),
        "derived_rng": deepcopy(derived_rng),
        "ordered_pools": deepcopy(ordered_pools),
        "player_state": deepcopy(player_state),
        "progress_state": deepcopy(progress_state),
        "screen_info": deepcopy(screen_info),
        "legal_actions": deepcopy(legal_actions),
    }
    validate_full_run_checkpoint(checkpoint)
    return checkpoint


def save_full_run_checkpoint(checkpoint: dict[str, Any], path: str | Path) -> None:
    validate_full_run_checkpoint(checkpoint)
    Path(path).write_text(
        json.dumps(checkpoint, ensure_ascii=False, indent=2),
        encoding="utf-8",
    )


def load_full_run_checkpoint(path: str | Path) -> dict[str, Any]:
    checkpoint = json.loads(Path(path).read_text(encoding="utf-8"))
    validate_full_run_checkpoint(checkpoint)
    return checkpoint
