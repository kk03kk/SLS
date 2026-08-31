"""Parity checks for shared combat rules and engine mechanisms."""

from __future__ import annotations

import json
from pathlib import Path
from typing import Any, Iterable, Mapping

from sls.audit.card_parity import DIFFERENCE, MATCH, structured_differences
from sls.backends.original.adapter import adapt_original

MECHANISM_RESULT_SCHEMA = "sls-stock-mechanism-parity-v1"

_RULE_SCENARIOS = (
    "damage_buffer_intangible",
    "duration_weak",
    "retain_ethereal",
)
_OBSERVATION_FIELDS = (
    "player", "hand", "draw_pile", "discard_pile", "exhaust_pile",
    "enemies", "powers", "public_context",
)


def mechanism_payloads(
    log_paths: Iterable[Path],
) -> dict[str, list[Mapping[str, Any]]]:
    rows: dict[str, list[Mapping[str, Any]]] = {}
    for log_path in log_paths:
        with log_path.open(encoding="utf-8", errors="replace") as handle:
            for line in handle:
                marker = "Sending message: "
                if marker not in line:
                    continue
                try:
                    payload = json.loads(line.split(marker, 1)[1])
                except json.JSONDecodeError:
                    continue
                scenario_id = str(
                    payload.get("_parity_scenario", {}).get("scenario_id", ""),
                )
                if scenario_id in _RULE_SCENARIOS or scenario_id.startswith(
                    "engine_probe:",
                ):
                    rows.setdefault(scenario_id, []).append(payload)
    return rows


def _decision_projection(payload: Mapping[str, Any]) -> dict[str, Any]:
    decision = adapt_original(payload).decision
    observation = decision.observation.to_dict()
    return {
        "observation": {
            key: observation[key] for key in _OBSERVATION_FIELDS
        },
        "actions": [action.to_dict() for action in decision.actions],
        "terminal": decision.terminal,
    }


def audit_mechanism_scenarios(log_paths: Iterable[Path]) -> dict[str, Any]:
    from sls.backends.simulator import native

    captured = mechanism_payloads(log_paths)
    rows: dict[str, dict[str, Any]] = {}
    for scenario_id in _RULE_SCENARIOS:
        payloads = captured.get(scenario_id, [])
        if not payloads:
            continue
        original = _decision_projection(payloads[-1])
        battle = native.LightspeedBattle()
        battle.reset(0, "CULTIST")
        battle.apply_scenario(scenario_id)
        battle.step("end_turn")
        simulator = _decision_projection(battle.snapshot())
        differences = structured_differences(original, simulator)
        rows[scenario_id] = {
            "status": DIFFERENCE if differences else MATCH,
            "boundary": "AFTER_ONE_FULL_MONSTER_TURN",
            "differences": differences,
        }

    stance_payloads = captured.get("engine_probe:STANCE", [])
    if stance_payloads:
        expected = stance_payloads[-1]["_parity_scenario"]
        actual = native.stance_mechanics_probe()
        original = {
            "calm_exit": {
                "energy": int(expected["calm_exit_energy"]),
                "stance": expected["calm_exit_stance"],
            },
            "divinity_entry": {
                "energy": int(expected["divinity_entry_energy"]),
                "stance": expected["divinity_entry_stance"],
            },
        }
        simulator = {
            "calm_exit": actual["calm_exit"],
            "divinity_entry": {
                "energy": actual["divinity_entry"]["energy"],
                "stance": actual["divinity_entry"]["stance"],
            },
        }
        differences = structured_differences(original, simulator)
        rows["engine_probe:STANCE"] = {
            "status": DIFFERENCE if differences else MATCH,
            "boundary": "STANCE_TRANSITIONS",
            "differences": differences,
        }

    orb_payloads = captured.get("engine_probe:ORB", [])
    if orb_payloads:
        expected = orb_payloads[-1]["_parity_scenario"]
        actual = native.orb_mechanics_probe()
        original = {
            "plasma_evoke_energy": int(expected["plasma_evoke_energy"]),
            "frost_evoke_block": int(expected["frost_evoke_block"]),
            "slot_cap": int(expected["slot_cap"]),
        }
        simulator = {
            "plasma_evoke_energy": actual["plasma"]["energy_gained"],
            "frost_evoke_block": (
                actual["frost_evoke"]["block"] - actual["passive"]["block"]
            ),
            "slot_cap": actual["slot_cap"],
        }
        differences = structured_differences(original, simulator)
        rows["engine_probe:ORB"] = {
            "status": DIFFERENCE if differences else MATCH,
            "boundary": "ORB_EVOKE_AND_SLOT_RULES",
            "differences": differences,
        }

    return {
        "schema": MECHANISM_RESULT_SCHEMA,
        "projection": {
            "rule_scenarios": list(_RULE_SCENARIOS),
            "observation_fields": list(_OBSERVATION_FIELDS),
            "engine_probes": ["STANCE", "ORB"],
        },
        "summary": {
            "scenarios": len(rows),
            "matched": sum(row["status"] == MATCH for row in rows.values()),
            "differences": sum(
                row["status"] == DIFFERENCE for row in rows.values()
            ),
        },
        "scenarios": rows,
    }
