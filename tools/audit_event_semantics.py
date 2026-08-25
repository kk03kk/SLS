"""Targeted Original/native constructor evidence for scoped stock events."""

from __future__ import annotations

import argparse
import json
from pathlib import Path
import sys
from typing import Any, Mapping

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "src"))
sys.path.insert(0, str(ROOT / "tools"))

from sls.backends.original import OriginalBackend, OriginalSession, StdioTransport  # noqa: E402
from sls.backends.original.adapter import AdaptedOriginalDecision, adapt_original  # noqa: E402
from sls.backends.simulator import IRONCLAD_A0_ACT1, SimulatorBackend  # noqa: E402
from sls.content.scope import ironclad_a0_scope_hash, load_ironclad_a0_scope  # noqa: E402
from sls.content.source_audit import java_sources, registry_game_ids  # noqa: E402
from sls.contracts import Decision, ScreenType  # noqa: E402
from sls.rl.training_contract import canonical_digest, sha256_file  # noqa: E402
from sls.validation.policies import deterministic_action  # noqa: E402
from audit_card_semantics import _rng  # noqa: E402


def _settle_original_event(
    session: OriginalSession, payload: Mapping[str, Any], *, limit: int = 120,
) -> dict[str, Any]:
    """Advance presentation-only frames until an event exposes a decision.

    Some stock events (notably Dead Adventurer) install their dialog over
    several render frames. CommunicationMod can expose an otherwise valid
    command boundary in between, with only WAIT available and no semantic
    action. Such a boundary is not a game decision and must not enter parity
    evidence.
    """
    current = dict(payload)
    for _ in range(limit):
        available = {str(item).lower() for item in current.get("available_commands") or ()}
        game = current.get("game_state") or {}
        interactive = bool(
            {"choose", "play", "end", "proceed"} & available
            or str(game.get("screen_name") or "NONE").upper() != "NONE"
        )
        if interactive or game.get("victory") or game.get("defeat"):
            return current
        if "wait" not in available:
            return current
        current = session.execute("wait 1")
    raise RuntimeError("event presentation did not reach a semantic decision")


def _state(decision: Decision, payload: Mapping[str, Any]) -> dict[str, Any]:
    observation = decision.observation.to_dict()
    # A targeted EventRoom is deliberately not inserted into the generated map
    # graph. Map reachability is outside the event constructor contract and is
    # validated by full-run truth; all event/player/deck/action state remains.
    observation["map_nodes"] = []
    rng_payload = dict(payload)
    if "_rng" not in rng_payload and "rng" in rng_payload:
        rng_payload["_rng"] = rng_payload["rng"]
    return {
        "decision": {
            "observation": observation,
            "actions": sorted(
                (action.to_dict() for action in decision.actions),
                key=lambda item: json.dumps(item, sort_keys=True, separators=(",", ":")),
            ),
            "terminal": decision.terminal,
        },
        "rng": _rng(rng_payload),
    }


def _assert_match(
    event_id: str, original: AdaptedOriginalDecision, simulator: Decision,
    original_payload: Mapping[str, Any], simulator_payload: Mapping[str, Any],
) -> str:
    expected = _state(original.decision, original_payload)
    actual = _state(simulator, simulator_payload)
    if expected != actual:
        root = ROOT / "validation-results" / "content-audit"
        root.mkdir(parents=True, exist_ok=True)
        (root / "event-mismatch-original.json").write_text(
            json.dumps(expected, indent=2, sort_keys=True) + "\n", encoding="utf-8",
        )
        (root / "event-mismatch-simulator.json").write_text(
            json.dumps(actual, indent=2, sort_keys=True) + "\n", encoding="utf-8",
        )
        raise RuntimeError(f"event constructor mismatch: {event_id}")
    return canonical_digest(expected)


def capture(seed: int) -> dict[str, Any]:
    scope = load_ironclad_a0_scope()
    event_ids = tuple(map(str, scope["events"]["ids"]))
    game_ids = registry_game_ids("events", event_ids)
    sources = java_sources("events")
    session = OriginalSession(StdioTransport())
    backend = OriginalBackend(session, IRONCLAD_A0_ACT1)
    try:
        decision = backend.reset(seed)
        for _ in range(40):
            if decision.observation.screen is ScreenType.COMBAT:
                break
            decision = backend.step(deterministic_action(decision, decision)).decision
        else:
            raise RuntimeError("Original did not reach first combat")

        entries = []
        for index, event_id in enumerate(event_ids, 1):
            if event_id == "NEOW":
                continue
            pre_rng = _rng(session.payload or {})
            original_payload = session.execute(f"parity_event {event_id}")
            scenario = dict(original_payload.get("_parity_scenario") or {})
            if scenario.get("scenario_id") != f"event_probe:{event_id}":
                raise RuntimeError(f"Original did not attest event {event_id}: {scenario}")
            original_payload = _settle_original_event(session, original_payload)
            original = adapt_original(original_payload)

            simulator_backend = SimulatorBackend(IRONCLAD_A0_ACT1)
            simulator_backend._native.reset_event_probe(seed, game_ids[event_id], pre_rng)
            simulator_payload = simulator_backend._native.snapshot()
            simulator = simulator_backend._adapt(simulator_payload)
            # Native collapses stock event dialogs that expose exactly one
            # possible Continue/offer action. Fold only those deterministic UI
            # boundaries until both sides expose the same semantic state. A
            # stock Continue can remain on EVENT, so screen equality alone is
            # not a sufficient stopping condition.
            for _ in range(8):
                if _state(original.decision, original_payload) == _state(
                    simulator, simulator_payload,
                ):
                    break
                if len(original.decision.actions) != 1:
                    break
                action = original.decision.actions[0]
                commands = original.commands.get(action.candidate_id)
                if action.kind.value != "CHOOSE_EVENT_OPTION" or not commands:
                    break
                for command in commands:
                    original_payload = session.execute(command)
                original_payload = _settle_original_event(session, original_payload)
                original = adapt_original(original_payload)
            digest = _assert_match(
                event_id, original, simulator, original_payload, simulator_payload,
            )
            source = sources[game_ids[event_id]]
            entries.append({
                "id": event_id,
                "game_id": game_ids[event_id],
                "scenario": "CONSTRUCTOR",
                "setup_digest": scenario["setup_digest"],
                "boundary_hashes": [digest],
                "effect_sha256": canonical_digest([digest]),
                "java_source": source.path.relative_to(ROOT).as_posix(),
                "java_sha256": sha256_file(source.path),
            })
            print(f"EVENT_AUDIT {index}/{len(event_ids)} {event_id}", file=sys.stderr, flush=True)
    finally:
        backend.return_to_menu()

    result: dict[str, Any] = {
        "schema": "sls-ironclad-event-semantics-v1",
        "scope_sha256": ironclad_a0_scope_hash(),
        "oracle_schema": "spirecomm-parity-v10",
        "seed": seed,
        "entries": entries,
    }
    result["audit_sha256"] = canonical_digest(result)
    return result


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--seed", type=int, default=0)
    parser.add_argument(
        "--output", type=Path,
        default=ROOT / "configs" / "validation" / "ironclad_a0_event_semantics.json",
    )
    args = parser.parse_args()
    payload = capture(args.seed)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(payload, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    return 0


if __name__ == "__main__":
    from sls.validation.runtime import write_completion
    try:
        code = main()
    except BaseException as error:
        write_completion(2, entry="event-audit", error=f"{type(error).__name__}: {error}", argv=sys.argv)
        raise
    else:
        write_completion(code, entry="event-audit")
        raise SystemExit(code)
