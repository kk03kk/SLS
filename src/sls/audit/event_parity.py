"""Stock event-constructor parity at the first policy decision boundary."""

from __future__ import annotations

import json
from pathlib import Path
from typing import Any, Iterable, Mapping

from sls.audit.card_parity import DIFFERENCE, MATCH, structured_differences
from sls.backends.original.adapter import adapt_original
from sls.backends.simulator.environment import SimulatorBackend
from sls.curriculum import IRONCLAD_A0_HEART

EVENT_RESULT_SCHEMA = "sls-stock-event-parity-v1"

_OBSERVATION_FIELDS = (
    "player", "run", "screen", "deck", "relics", "potions",
    "choice_options", "reward_options", "shop_items", "event_options",
    "rest_options", "boss_relic_options",
)


def event_scenario_runs(log_paths: Iterable[Path]) -> list[dict[str, Any]]:
    runs: list[dict[str, Any]] = []
    by_key: dict[tuple[Path, str], dict[str, Any]] = {}
    for log_path in log_paths:
        previous: Mapping[str, Any] | None = None
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
                if scenario_id.startswith("event_probe:"):
                    key = (log_path, scenario_id)
                    if key not in by_key:
                        if previous is None or "_rng" not in previous:
                            raise ValueError(
                                f"missing pre-constructor RNG for {scenario_id}",
                            )
                        run = {
                            "scenario_id": scenario_id,
                            "before": previous,
                            "payloads": [],
                        }
                        by_key[key] = run
                        runs.append(run)
                    by_key[key]["payloads"].append(payload)
                previous = payload
    return runs


def _decision_projection(decision: Any) -> dict[str, Any]:
    observation = decision.observation.to_dict()
    return {
        "observation": {
            key: observation[key] for key in _OBSERVATION_FIELDS
        },
        "actions": [action.to_dict() for action in decision.actions],
        "terminal": decision.terminal,
    }


def _last_adaptable_payload(payloads: Iterable[Mapping[str, Any]]) -> Mapping[str, Any]:
    result: Mapping[str, Any] | None = None
    for payload in payloads:
        try:
            adapt_original(payload)
        except ValueError:
            continue
        result = payload
    if result is None:
        raise ValueError("event scenario has no adaptable decision boundary")
    return result


def audit_event_scenarios(log_paths: Iterable[Path]) -> dict[str, Any]:
    rows: dict[str, dict[str, Any]] = {}
    for run in event_scenario_runs(log_paths):
        scenario_id = run["scenario_id"]
        _, event_id = scenario_id.split(":", 1)
        original_payload = _last_adaptable_payload(run["payloads"])
        original = {
            **_decision_projection(adapt_original(original_payload).decision),
            "rng": original_payload["_rng"],
        }

        backend = SimulatorBackend(IRONCLAD_A0_HEART)
        backend._native.reset_event_probe(0, event_id, run["before"]["_rng"])
        raw = backend._native.snapshot()
        simulator = {
            **_decision_projection(backend._adapt(raw)),
            "rng": raw["rng"],
        }
        differences = structured_differences(original, simulator)
        rows[scenario_id] = {
            "event_id": event_id,
            "status": DIFFERENCE if differences else MATCH,
            "boundary": "INITIAL_EVENT_DECISION",
            "differences": differences,
        }

    return {
        "schema": EVENT_RESULT_SCHEMA,
        "projection": {
            "boundary": "event construction through first stable policy decision",
            "observation_fields": list(_OBSERVATION_FIELDS),
            "rng": "exact absolute state for every stock-compatible stream",
            "not_established": "outcomes of every event option",
        },
        "summary": {
            "events": len(rows),
            "matched": sum(row["status"] == MATCH for row in rows.values()),
            "differences": sum(
                row["status"] == DIFFERENCE for row in rows.values()
            ),
        },
        "scenarios": rows,
    }
