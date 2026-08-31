"""Controlled potion-scenario comparison against captured stock-game states."""

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

POTION_RESULT_SCHEMA = "sls-stock-potion-parity-v1"
TRANSIENT_BOUNDARY = "TRANSIENT_BOUNDARY"

_OBSERVATION_FIELDS = (
    "player",
    "screen",
    "hand",
    "draw_pile",
    "discard_pile",
    "exhaust_pile",
    "enemies",
    "powers",
    "relics",
    "potions",
    "choice_options",
    "public_context",
)
_DISCOVERY_POTIONS = {
    "ATTACK_POTION", "SKILL_POTION", "POWER_POTION", "COLORLESS_POTION",
}


def potion_semantic_projection(
    payload: Mapping[str, Any], *, rng_baseline: Mapping[str, Any],
) -> dict[str, Any]:
    decision = adapt_original(payload).decision
    observation = decision.observation.to_dict()
    rng = dict(payload.get("_rng", {}))
    baseline = dict(rng_baseline.get("_rng", {}))
    return {
        "observation": {key: observation[key] for key in _OBSERVATION_FIELDS},
        "actions": [action.to_dict() for action in decision.actions],
        "terminal": decision.terminal,
        "rng_counter_deltas": {
            key: int(value["counter"]) - int(baseline[key]["counter"])
            for key, value in rng.items() if key in baseline
        },
    }


def _settle_choices(battle: Any) -> None:
    for _ in range(16):
        actions = battle.snapshot()["_legal_actions"]
        choice = next((item for item in actions if item["kind"] == "choose"), None)
        proceed = next((item for item in actions if item["kind"] == "proceed"), None)
        selected = choice or proceed
        if selected is None:
            return
        kwargs = (
            {"choice_index": selected["choice_index"]}
            if selected["kind"] == "choose" else {}
        )
        battle.step(selected["kind"], **kwargs)
    raise RuntimeError("potion scenario did not settle after 16 choice transitions")


def audit_potion_scenarios(log_paths: Iterable[Path]) -> dict[str, Any]:
    from sls.backends.simulator import native

    originals = scenario_payload_sequences(log_paths, prefix="potion_probe:")
    rows: dict[str, dict[str, Any]] = {}
    for scenario_id, original_payloads in sorted(originals.items()):
        _, potion_id, sacred_bark = scenario_id.split(":")
        if potion_id == "SMOKE_BOMB":
            rows[scenario_id] = {
                "potion_id": potion_id,
                "sacred_bark": False,
                "status": TRANSIENT_BOUNDARY,
                "differences": [],
                "note": (
                    "Stock exposes the 2.5-second escape animation boundary after "
                    "consuming the potion; native resolves the same escape immediately."
                ),
            }
            continue

        battle = native.LightspeedBattle()
        battle.reset_potion_probe(0, potion_id, sacred_bark == "true")
        battle.set_rng_state(original_payloads[0]["_rng"])
        simulator_baseline = battle.snapshot()
        if potion_id == "FAIRY_POTION":
            battle.step("end_turn")
        else:
            battle.step("potion", potion_index=0, target_index=0)
        retrieval_updates = int(
            original_payloads[-1].get("_timing_evidence", {}).get(
                "discovery_retrieval_updates", 0,
            ),
        )
        if potion_id in _DISCOVERY_POTIONS and retrieval_updates:
            battle.set_discovery_retrieval_updates(retrieval_updates)
        _settle_choices(battle)
        simulator_payload = battle.snapshot()
        original = potion_semantic_projection(
            original_payloads[-1], rng_baseline=original_payloads[0],
        )
        simulator = potion_semantic_projection(
            simulator_payload, rng_baseline=simulator_baseline,
        )
        differences = structured_differences(original, simulator)
        rows[scenario_id] = {
            "potion_id": potion_id,
            "sacred_bark": sacred_bark == "true",
            "status": DIFFERENCE if differences else MATCH,
            "differences": differences,
        }

    return {
        "schema": POTION_RESULT_SCHEMA,
        "summary": {
            "scenarios": len(rows),
            "matched": sum(row["status"] == MATCH for row in rows.values()),
            "differences": sum(row["status"] == DIFFERENCE for row in rows.values()),
            "transient_boundaries": sum(
                row["status"] == TRANSIENT_BOUNDARY for row in rows.values()
            ),
        },
        "scenarios": rows,
    }
