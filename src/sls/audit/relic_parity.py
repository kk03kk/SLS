"""Controlled relic callback comparisons against stock-game Oracle captures."""

from __future__ import annotations

from pathlib import Path
from typing import Any, Iterable, Mapping

from sls.audit.card_parity import (
    DIFFERENCE,
    MATCH,
    scenario_payload_sequences,
    structured_differences,
)
from sls.backends.original.adapter import adapt_original

RELIC_RESULT_SCHEMA = "sls-stock-relic-parity-v1"
ADAPTER_BOUNDARY = "ADAPTER_BOUNDARY"
TRANSIENT_BOUNDARY = "TRANSIENT_BOUNDARY"
BRANCH_PARTIAL = "BRANCH_PARTIAL"

_OBSERVATION_FIELDS = (
    "player", "screen", "hand", "draw_pile", "discard_pile",
    "exhaust_pile", "enemies", "powers", "relics", "potions",
    "choice_options", "public_context",
)


def _combat_projection(payload: Mapping[str, Any]) -> dict[str, Any]:
    decision = adapt_original(payload).decision
    observation = decision.observation.to_dict()
    return {
        "observation": {key: observation[key] for key in _OBSERVATION_FIELDS},
        "actions": [action.to_dict() for action in decision.actions],
        "terminal": decision.terminal,
    }


def _typed(value: Any) -> Any:
    if not isinstance(value, str):
        return value
    if value == "true":
        return True
    if value == "false":
        return False
    try:
        return int(value)
    except ValueError:
        return value


def _oracle_result(payload: Mapping[str, Any]) -> dict[str, Any]:
    scenario = dict(payload["_parity_scenario"])
    for key in ("scenario_id", "source", "setup_digest"):
        scenario.pop(key, None)
    return {key: _typed(value) for key, value in scenario.items()}


def _native_specialized_result(battle: Any, scenario_id: str) -> dict[str, Any]:
    parts = scenario_id.split(":")
    prefix, relic_id = parts[0], parts[1]
    if prefix == "relic_campfire_probe":
        return dict(battle.relic_campfire_probe(relic_id, parts[2]))
    if prefix == "relic_card_use_probe":
        return dict(battle.relic_card_use_probe(0, relic_id))
    if prefix == "relic_counter_probe":
        return {"counter": battle.relic_counter_probe(relic_id, int(parts[2]))}
    if prefix == "relic_damage_probe":
        return {"damage": battle.relic_damage_probe(0, relic_id)}
    if prefix == "relic_end_turn_probe":
        return dict(battle.relic_end_turn_probe(0, relic_id))
    if prefix == "relic_equip_probe":
        return dict(battle.relic_equip_probe(0, relic_id))
    if prefix == "relic_heal_probe":
        return {"result": battle.relic_heal_probe(0, relic_id, int(parts[2]))}
    if prefix == "relic_hp_loss_probe":
        return dict(battle.relic_hp_loss_probe(0, relic_id))
    if prefix == "relic_neutral_probe":
        return {
            "policy_state_unchanged": battle.relic_policy_neutral_probe(
                relic_id, parts[2],
            ),
        }
    if prefix == "relic_obtain_card_probe":
        return dict(battle.relic_obtain_card_probe(0, relic_id))
    if prefix == "relic_resource_probe":
        return dict(battle.relic_resource_probe(0, relic_id))
    if prefix == "relic_reward_probe":
        return {
            "result": battle.relic_reward_scalar_probe(relic_id, int(parts[2])),
        }
    if prefix == "relic_shuffle_probe":
        return dict(battle.relic_shuffle_probe(0, relic_id))
    if prefix == "relic_spawn_probe":
        return {
            "spawn_result": battle.relic_can_spawn_probe(
                0, relic_id, int(parts[2]), parts[3] == "true", parts[4],
            ),
        }
    if prefix == "relic_special_resource_probe":
        return dict(battle.relic_special_resource_probe(0, relic_id))
    if prefix == "relic_trigger_probe":
        return dict(battle.relic_trigger_probe(0, relic_id))
    if prefix == "relic_turn_state_probe":
        return dict(battle.relic_turn_state_probe(0, relic_id))
    if prefix == "relic_unequip_probe":
        return dict(battle.relic_unequip_probe(0, relic_id))
    if prefix == "relic_victory_probe":
        return {
            "counter": battle.relic_victory_counter_probe(
                relic_id, int(parts[2]),
            ),
        }
    if prefix == "relic_victory_resource_probe":
        return dict(battle.relic_victory_resource_probe(0, relic_id))
    if prefix == "relic_world_probe":
        return dict(battle.relic_world_probe(0, relic_id))
    raise ValueError(f"unsupported specialized relic scenario: {scenario_id}")


_TRIGGER_FIELDS = {
    "CHAMPION_BELT": ("weak", "vulnerable"),
    "CHARONS_ASHES": ("monster_hp_delta",),
    "DEAD_BRANCH": ("hand_delta",),
    "GREMLIN_HORN": ("hand_delta", "energy_delta"),
    "HAND_DRILL": ("vulnerable",),
    "LIZARD_TAIL": ("hp_delta", "counter"),
    "RED_SKULL": ("strength_on", "strength_after"),
    "UNCEASING_TOP": ("hand_delta",),
}


def _specialized_projection(
    scenario_id: str,
    result: Mapping[str, Any],
) -> dict[str, Any]:
    """Keep only fields that belong to the isolated callback contract.

    The stock trigger Oracle reused one live player across its batch.  Its generic
    ``hp_after`` field therefore leaked health from earlier scenarios, and the
    Gremlin Horn case reported HP for the restored Cultist rather than the
    temporary dead Louse.  Those fields are not evidence about the named relic.
    """

    prefix, relic_id = scenario_id.split(":", 2)[:2]
    if prefix != "relic_trigger_probe":
        return dict(result)
    fields = _TRIGGER_FIELDS[relic_id]
    return {key: result[key] for key in fields}


def _is_known_equip_boundary(
    scenario_id: str,
    differences: list[dict[str, Any]],
) -> bool:
    if not scenario_id.startswith("relic_equip_probe:"):
        return False
    allowed = {
        "relic_equip_probe:ASTROLABE": {"$.upgraded_delta"},
        "relic_equip_probe:CALLING_BELL": {"$.option_count"},
        "relic_equip_probe:CAULDRON": {"$.option_count"},
        "relic_equip_probe:ORRERY": {"$.option_count"},
        "relic_equip_probe:PANDORAS_BOX": {"$.option_count"},
        "relic_equip_probe:TINY_HOUSE": {"$.option_count"},
    }.get(scenario_id)
    return allowed is not None and {item["path"] for item in differences} <= allowed


def audit_relic_scenarios(log_paths: Iterable[Path]) -> dict[str, Any]:
    from sls.backends.simulator import native

    first_turn = scenario_payload_sequences(log_paths, prefix="relic_probe:")
    specialized = scenario_payload_sequences(log_paths, prefix="relic_")
    rows: dict[str, dict[str, Any]] = {}
    for scenario_id, payloads in sorted(first_turn.items()):
        _, relic_id, _ = scenario_id.split(":")
        if relic_id in {"GAMBLING_CHIP", "TOOLBOX"}:
            rows[scenario_id] = {
                "relic_id": relic_id,
                "status": ADAPTER_BOUNDARY,
                "differences": [],
                "note": "Native direct probe requires a dedicated choice-screen adapter.",
            }
            continue
        battle = native.LightspeedBattle()
        battle.reset_relic_probe(0, relic_id)
        differences = structured_differences(
            _combat_projection(payloads[-1]), _combat_projection(battle.snapshot()),
        )
        rows[scenario_id] = {
            "relic_id": relic_id,
            # Both legacy payloads pass through adapt_original.  Preserve the
            # diagnostic diff, but never claim independent semantic parity.
            "status": DIFFERENCE if differences else BRANCH_PARTIAL,
            "differences": differences,
            "projection_independence": "COMMON_ADAPTER_LEGACY",
        }

    for scenario_id, payloads in sorted(specialized.items()):
        if scenario_id.startswith("relic_probe:"):
            continue
        relic_id = scenario_id.split(":")[1]
        battle = native.LightspeedBattle()
        original = _oracle_result(payloads[-1])
        simulator = _native_specialized_result(battle, scenario_id)
        differences = structured_differences(
            _specialized_projection(scenario_id, original),
            _specialized_projection(scenario_id, simulator),
        )
        boundary = _is_known_equip_boundary(scenario_id, differences)
        rows[scenario_id] = {
            "relic_id": relic_id,
            "status": (
                TRANSIENT_BOUNDARY if boundary
                else DIFFERENCE if differences
                else MATCH
            ),
            "differences": differences,
        }
        if boundary:
            rows[scenario_id]["note"] = (
                "Stock capture stops on or retains an acquisition UI boundary; "
                "native reports the same resolved reward/effect immediately."
            )

    return {
        "schema": RELIC_RESULT_SCHEMA,
        "summary": {
            "scenarios": len(rows),
            "matched": sum(row["status"] == MATCH for row in rows.values()),
            "differences": sum(row["status"] == DIFFERENCE for row in rows.values()),
            "adapter_boundaries": sum(
                row["status"] == ADAPTER_BOUNDARY for row in rows.values()
            ),
            "transient_boundaries": sum(
                row["status"] == TRANSIENT_BOUNDARY for row in rows.values()
            ),
            "branch_partial": sum(
                row["status"] == BRANCH_PARTIAL for row in rows.values()
            ),
        },
        "scenarios": rows,
    }
