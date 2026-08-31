"""Controlled stock encounter construction and first-turn comparisons."""

from __future__ import annotations

import json
from pathlib import Path
from typing import Any, Iterable, Mapping

from sls.audit.card_parity import DIFFERENCE, MATCH, structured_differences
from sls.audit.relic_parity import _combat_projection
from sls.content.normalize import normalize_monster_id

ENCOUNTER_RESULT_SCHEMA = "sls-stock-encounter-parity-v1"


def encounter_scenario_runs(
    log_paths: Iterable[Path],
) -> list[dict[str, Any]]:
    """Recover each Oracle invocation and its pre-constructor RNG boundary."""

    runs: list[dict[str, Any]] = []
    by_key: dict[tuple[Path, str, str], dict[str, Any]] = {}
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
                scenario = payload.get("_parity_scenario", {})
                scenario_id = str(scenario.get("scenario_id", ""))
                digest = str(scenario.get("setup_digest", ""))
                if scenario_id.startswith("encounter_probe:"):
                    key = (log_path, scenario_id, digest)
                    if key not in by_key:
                        if previous is None or "_rng" not in previous:
                            raise ValueError(
                                f"missing pre-constructor RNG for {scenario_id}",
                            )
                        run = {
                            "scenario_id": scenario_id,
                            "setup_digest": digest,
                            "before": previous,
                            "payloads": [],
                        }
                        by_key[key] = run
                        runs.append(run)
                    by_key[key]["payloads"].append(payload)
                previous = payload
    return runs


def _waiting_boundaries(run: Mapping[str, Any]) -> tuple[Mapping[str, Any], ...]:
    return tuple(
        payload for payload in run["payloads"]
        if payload.get("game_state", {}).get("action_phase") == "WAITING_ON_USER"
    )


def _comparison_projection(payload: Mapping[str, Any]) -> dict[str, Any]:
    return {
        **_combat_projection(payload),
        "rng": payload.get("_rng", {}),
    }


def audit_encounter_scenarios(log_paths: Iterable[Path]) -> dict[str, Any]:
    from sls.backends.simulator import native

    rows: dict[str, dict[str, Any]] = {}
    for run in encounter_scenario_runs(log_paths):
        scenario_id = run["scenario_id"]
        _, encounter_id = scenario_id.split(":", 1)
        boundaries = _waiting_boundaries(run)
        if not boundaries:
            raise ValueError(f"no stable player boundary for {scenario_id}")
        initial = boundaries[0]
        initial_turn = int(initial["game_state"]["combat_state"]["turn"])
        after_turn = next(
            (
                payload for payload in boundaries
                if int(payload["game_state"]["combat_state"]["turn"])
                > initial_turn
            ),
            None,
        )
        if after_turn is None:
            raise ValueError(f"no post-monster-turn boundary for {scenario_id}")

        battle = native.LightspeedBattle()
        battle.reset_encounter_probe(0, encounter_id, run["before"]["_rng"])
        simulator_initial = battle.snapshot()
        initial_differences = structured_differences(
            _comparison_projection(initial),
            _comparison_projection(simulator_initial),
        )
        battle.step("end_turn")
        simulator_after_turn = battle.snapshot()
        turn_differences = structured_differences(
            _comparison_projection(after_turn),
            _comparison_projection(simulator_after_turn),
        )
        differences = [
            {**difference, "boundary": "INITIAL"}
            for difference in initial_differences
        ] + [
            {**difference, "boundary": "AFTER_FIRST_MONSTER_TURN"}
            for difference in turn_differences
        ]
        monsters = {
            normalize_monster_id(monster["id"])
            for payload in (initial, after_turn)
            for monster in payload["game_state"]["combat_state"]["monsters"]
        }
        row_id = f"{scenario_id}:{run['setup_digest']}"
        rows[row_id] = {
            "encounter_id": encounter_id,
            "setup_digest": run["setup_digest"],
            "monster_ids": sorted(monsters),
            "status": DIFFERENCE if differences else MATCH,
            "boundaries": ["INITIAL", "AFTER_FIRST_MONSTER_TURN"],
            "differences": differences,
        }

    return {
        "schema": ENCOUNTER_RESULT_SCHEMA,
        "projection": {
            "combat": "all model-visible combat fields and legal actions",
            "rng": "exact absolute state for every stock-compatible stream",
        },
        "summary": {
            "scenario_runs": len(rows),
            "encounters": len({row["encounter_id"] for row in rows.values()}),
            "monsters": len({
                monster
                for row in rows.values()
                for monster in row["monster_ids"]
            }),
            "matched": sum(row["status"] == MATCH for row in rows.values()),
            "differences": sum(
                row["status"] == DIFFERENCE for row in rows.values()
            ),
        },
        "scenarios": rows,
    }
