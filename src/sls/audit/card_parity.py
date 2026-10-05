"""Controlled card-scenario comparison against captured stock-game states."""

from __future__ import annotations

import json
from pathlib import Path
from typing import Any, Iterable, Mapping

from sls.backends.original.adapter import adapt_original

CARD_RESULT_SCHEMA = "sls-stock-card-parity-v1"
MATCH = "SCENARIO_MATCH"
DIFFERENCE = "DIFFERENCE"

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
    "choice_options",
    "public_context",
)


def last_scenario_payloads(
    log_paths: Iterable[Path], *, prefix: str,
) -> dict[str, dict[str, Any]]:
    """Load the final ready boundary emitted for every named Oracle scenario."""

    return {
        scenario_id: payloads[-1]
        for scenario_id, payloads in scenario_payload_sequences(
            log_paths, prefix=prefix,
        ).items()
    }


def scenario_payload_sequences(
    log_paths: Iterable[Path], *, prefix: str,
) -> dict[str, list[dict[str, Any]]]:
    """Load every ready boundary emitted for each named Oracle scenario."""

    result: dict[str, list[dict[str, Any]]] = {}
    for log_path in log_paths:
        with log_path.open(encoding="utf-8", errors="replace") as handle:
            for line in handle:
                marker = "Sending message: "
                if marker not in line or '"_parity_scenario"' not in line:
                    continue
                try:
                    payload = json.loads(line.split(marker, 1)[1])
                except json.JSONDecodeError:
                    continue
                scenario_id = str(
                    payload.get("_parity_scenario", {}).get("scenario_id", ""),
                )
                if scenario_id.startswith(prefix):
                    result.setdefault(scenario_id, []).append(payload)
    return result


def card_semantic_projection(
    payload: Mapping[str, Any], *, rng_baseline: Mapping[str, Any],
) -> dict[str, Any]:
    """Project fields the controlled card scenario is designed to establish.

    Run map, master deck, and Neow RNG are setup-harness state rather than card
    semantics.  Every combat-visible card zone, legal decision, stat, power,
    relic and enemy intent remains, along with every RNG stream's counter delta
    across the card action.
    """

    decision = adapt_original(payload).decision
    observation = decision.observation.to_dict()
    rng = dict(payload.get("_rng", {}))
    baseline = dict(rng_baseline.get("_rng", {}))
    rng_counter_deltas = {
        key: int(value["counter"]) - int(baseline[key]["counter"])
        for key, value in rng.items() if key in baseline
    }
    return {
        "observation": {key: observation[key] for key in _OBSERVATION_FIELDS},
        "actions": [action.to_dict() for action in decision.actions],
        "terminal": decision.terminal,
        "rng_counter_deltas": rng_counter_deltas,
    }


def structured_differences(
    original: Any, simulator: Any, *, path: str = "$",
) -> list[dict[str, Any]]:
    if type(original) is not type(simulator):
        return [{"path": path, "original": original, "simulator": simulator}]
    if isinstance(original, dict):
        differences: list[dict[str, Any]] = []
        for key in sorted(set(original) | set(simulator)):
            if key not in original or key not in simulator:
                differences.append({
                    "path": f"{path}.{key}",
                    "original": original.get(key),
                    "simulator": simulator.get(key),
                })
            else:
                differences.extend(structured_differences(
                    original[key], simulator[key], path=f"{path}.{key}",
                ))
        return differences
    if isinstance(original, list):
        if len(original) != len(simulator):
            return [{"path": path, "original": original, "simulator": simulator}]
        differences = []
        for index, (left, right) in enumerate(zip(original, simulator, strict=True)):
            differences.extend(structured_differences(
                left, right, path=f"{path}[{index}]",
            ))
        return differences
    if original != simulator:
        return [{"path": path, "original": original, "simulator": simulator}]
    return []


def _settle_native_card_choice(battle: Any) -> None:
    for _ in range(16):
        actions = battle.snapshot()["_legal_actions"]
        choice = next((item for item in actions if item["kind"] == "choose"), None)
        proceed = next((item for item in actions if item["kind"] == "proceed"), None)
        selected = choice or proceed
        if selected is None:
            return
        if selected["kind"] == "choose":
            battle.step("choose", choice_index=selected["choice_index"])
        else:
            battle.step("proceed")
    raise RuntimeError("card scenario did not settle after 16 choice transitions")


def audit_card_scenarios(log_paths: Iterable[Path]) -> dict[str, Any]:
    from sls.backends.simulator import native

    originals = scenario_payload_sequences(log_paths, prefix="card_probe:")
    rows: dict[str, dict[str, Any]] = {}
    for scenario_id, original_payloads in sorted(originals.items()):
        _, card_id, upgraded = scenario_id.split(":")
        battle = native.LightspeedBattle()
        battle.reset_card_probe(0, card_id, upgraded == "1")
        # The archived Original audit executed scenarios sequentially, so its
        # combat RNG streams intentionally carry over between cards.  Restore
        # that exact pre-action state before comparing any stochastic result.
        battle.set_rng_state(original_payloads[0]["_rng"])
        simulator_baseline = battle.snapshot()
        battle.step("play", card_index=1, target_index=0)
        retrieval_updates = int(
            original_payloads[-1].get("_timing_evidence", {}).get(
                "discovery_retrieval_updates", 0,
            ),
        )
        if card_id == "DISCOVERY" and retrieval_updates:
            battle.set_discovery_retrieval_updates(retrieval_updates)
        _settle_native_card_choice(battle)
        simulator_payload = battle.snapshot()
        original = card_semantic_projection(
            original_payloads[-1], rng_baseline=original_payloads[0],
        )
        simulator = card_semantic_projection(
            simulator_payload, rng_baseline=simulator_baseline,
        )
        differences = structured_differences(original, simulator)
        rows[scenario_id] = {
            "card_id": card_id,
            "upgraded": upgraded == "1",
            "status": DIFFERENCE if differences else MATCH,
            "differences": differences,
        }

    return {
        "schema": CARD_RESULT_SCHEMA,
        "projection": {
            "observation_fields": list(_OBSERVATION_FIELDS),
            "rng_streams": "per-stream counter deltas across the card action",
            "excluded": [
                "run map",
                "master deck",
                "absolute RNG state established by the probe harness",
            ],
        },
        "summary": {
            "scenarios": len(rows),
            "matched": sum(row["status"] == MATCH for row in rows.values()),
            "differences": sum(row["status"] == DIFFERENCE for row in rows.values()),
        },
        "scenarios": rows,
    }
