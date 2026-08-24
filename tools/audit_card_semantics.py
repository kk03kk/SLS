"""Capture deterministic Original/native effect evidence for scoped cards."""

from __future__ import annotations

import argparse
import json
import os
from pathlib import Path
import sys
from typing import Any, Mapping


ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "src"))

from sls.backends.original import OriginalBackend, OriginalSession, StdioTransport  # noqa: E402
from sls.backends.original.adapter import AdaptedOriginalDecision, adapt_original  # noqa: E402
from sls.backends.simulator import IRONCLAD_A0_ACT1, native  # noqa: E402
from sls.content.scope import ironclad_a0_scope_hash, load_ironclad_a0_scope  # noqa: E402
from sls.content.source_audit import java_sources, registry_game_ids  # noqa: E402
from sls.contracts import Action, ActionKind, Decision, ScreenType  # noqa: E402
from sls.rl.training_contract import canonical_digest, sha256_file  # noqa: E402
from sls.validation.policies import PRIORITY, deterministic_action  # noqa: E402


SCHEMA = "sls-ironclad-card-semantics-v1"
DEFAULT_OUTPUT = ROOT / "configs" / "validation" / "ironclad_a0_card_semantics.json"


def _projection(decision: Decision) -> dict[str, Any]:
    value = decision.observation.to_dict()
    return {
        "screen": value["screen"],
        "player": value["player"],
        "hand": value["hand"],
        "draw_pile": value["draw_pile"],
        "discard_pile": value["discard_pile"],
        "exhaust_pile": value["exhaust_pile"],
        "enemies": [
            {
                key: item[key]
                for key in ("instance_id", "monster_id", "current_hp", "max_hp", "block", "intent")
            }
            for item in value["enemies"]
        ],
        "powers": value["powers"],
        "relics": value["relics"],
        "choice_options": value["choice_options"],
        "public_context": value["public_context"],
        "actions": sorted(
            (action.to_dict() for action in decision.actions),
            key=lambda item: json.dumps(item, sort_keys=True, separators=(",", ":")),
        ),
        "terminal": decision.terminal,
    }


def _rng(payload: Mapping[str, Any]) -> dict[str, Any]:
    value = payload.get("_rng") or (payload.get("game_state") or {}).get("_rng") or {}
    return {str(key): dict(item) for key, item in sorted(value.items())}


def _adapt_probe_payload(payload: Mapping[str, Any]) -> AdaptedOriginalDecision:
    """Expose the LightspeedBattle choice record through the wire adapter shape."""

    value = dict(payload)
    game = dict(value.get("game_state") or {})
    combat = dict(game.get("combat_state") or {})
    choice = dict(combat.get("choice") or {})
    if choice.get("options") and not combat.get("card_select"):
        options = list(choice["options"])
        combat["card_select"] = {
            "cards": options,
            "source": str(choice.get("source") or "GENERATED"),
        }
        game["combat_state"] = combat
        game["choice_list"] = [str(item.get("id") or "") for item in options]
        game["screen_state"] = {"cards": options}
        value["game_state"] = game
        if any(str(item.get("kind")) == "proceed" for item in value.get("_legal_actions") or ()):
            available = list(value.get("available_commands") or ())
            if "confirm" not in available:
                available.append("confirm")
            value["available_commands"] = available
    return adapt_original(value)


def _assert_match(
    card_id: str,
    upgrades: int,
    boundary: int,
    original: AdaptedOriginalDecision,
    simulator: AdaptedOriginalDecision,
    original_payload: Mapping[str, Any],
    simulator_payload: Mapping[str, Any],
) -> str:
    expected = {"decision": _projection(original.decision), "rng": _rng(original_payload)}
    actual = {"decision": _projection(simulator.decision), "rng": _rng(simulator_payload)}
    if expected != actual:
        expected_path = ROOT / "validation-results" / "content-audit" / "card-mismatch-original.json"
        actual_path = ROOT / "validation-results" / "content-audit" / "card-mismatch-simulator.json"
        expected_path.parent.mkdir(parents=True, exist_ok=True)
        expected_path.write_text(json.dumps(expected, indent=2, sort_keys=True) + "\n", encoding="utf-8")
        actual_path.write_text(json.dumps(actual, indent=2, sort_keys=True) + "\n", encoding="utf-8")
        raise RuntimeError(
            f"card semantic mismatch card={card_id} upgrades={upgrades} boundary={boundary}; "
            f"details={expected_path},{actual_path}"
        )
    return canonical_digest(expected)


def _execute_original(session: OriginalSession, adapted: AdaptedOriginalDecision, action: Action) -> dict[str, Any]:
    payload: dict[str, Any] = session.payload or {}
    for command in adapted.commands[action.candidate_id]:
        payload = session.execute(command)
    return payload


def _execute_native(
    battle: Any,
    adapted: AdaptedOriginalDecision,
    action: Action,
) -> dict[str, Any]:
    payload: dict[str, Any] = battle.snapshot()
    for command in adapted.commands[action.candidate_id]:
        parts = command.split()
        kind = parts[0].lower()
        if kind in {"wait", "state"}:
            continue
        if kind == "play":
            battle.step(
                "play", card_index=int(parts[1]),
                target_index=int(parts[2]) if len(parts) > 2 else 0,
            )
        elif kind == "choose":
            battle.step("choose", choice_index=int(parts[1]))
        elif kind in {"confirm", "proceed"}:
            battle.step("proceed")
        elif kind in {"end", "end_turn"}:
            battle.step("end_turn")
        else:
            raise RuntimeError(f"unsupported native card-probe command: {command}")
        payload = battle.snapshot()
    return payload


def _common_action(original: Decision, simulator: Decision) -> Action:
    simulator_ids = {action.candidate_id for action in simulator.actions}
    common = [action for action in original.actions if action.candidate_id in simulator_ids]
    if not common:
        raise RuntimeError("card probe continuation exposes no common semantic action")
    return sorted(common, key=lambda action: (PRIORITY.get(action.kind, 1000), action.candidate_id))[0]


def _probe_variant(session: OriginalSession, card_id: str, upgrades: int, seed: int) -> dict[str, Any]:
    original_payload = session.execute(f"parity_card {card_id} {upgrades}")
    scenario = original_payload.get("_parity_scenario") or {}
    expected_scenario = f"card_probe:{card_id}:{upgrades}"
    if str(scenario.get("scenario_id")) != expected_scenario:
        raise RuntimeError(f"Original did not attest {expected_scenario}: {scenario}")

    battle = native.LightspeedBattle()
    battle.reset_card_probe(seed, card_id, bool(upgrades))
    battle.set_rng_state(_rng(original_payload))
    simulator_payload = battle.snapshot()
    original = adapt_original(original_payload)
    simulator = _adapt_probe_payload(simulator_payload)
    hashes = [
        _assert_match(
            card_id, upgrades, 0, original, simulator,
            original_payload, simulator_payload,
        )
    ]

    plays = [
        action for action in original.decision.actions
        if action.kind is ActionKind.PLAY_CARD and action.subject_id == "HAND:0"
    ]
    if len(plays) != 1 or plays[0].candidate_id not in {
        action.candidate_id for action in simulator.decision.actions
    }:
        raise RuntimeError(f"card probe is not jointly playable: {card_id}+{upgrades}")
    action = plays[0]

    for boundary in range(1, 33):
        original_payload = _execute_original(session, original, action)
        timing = dict(original_payload.get("_timing_evidence") or {})
        native_choice = dict(
            ((simulator_payload.get("game_state") or {}).get("combat_state") or {}).get("choice")
            or {}
        )
        if (
            action.kind is ActionKind.SELECT_CARD
            and str(native_choice.get("task") or "").upper() == "DISCOVERY"
            and timing.get("discovery_retrieval_updates") is not None
        ):
            battle.set_discovery_retrieval_updates(
                int(timing["discovery_retrieval_updates"])
            )
        simulator_payload = _execute_native(battle, simulator, action)
        original = adapt_original(original_payload)
        simulator = _adapt_probe_payload(simulator_payload)
        hashes.append(
            _assert_match(
                card_id, upgrades, boundary, original, simulator,
                original_payload, simulator_payload,
            )
        )
        if "parity_card" in {
            str(item).lower() for item in original_payload.get("available_commands") or ()
        }:
            return {
                "upgrades": upgrades,
                "setup_digest": str(scenario["setup_digest"]),
                "boundaries": boundary + 1,
                "boundary_hashes": hashes,
                "effect_sha256": canonical_digest(hashes),
            }
        action = _common_action(original.decision, simulator.decision)
    raise RuntimeError(f"card probe continuation did not settle: {card_id}+{upgrades}")


def capture(seed: int) -> dict[str, Any]:
    scope = load_ironclad_a0_scope()
    ids = sorted(map(str, scope["cards"]["ids"]))
    sources = java_sources("cards")
    game_ids = registry_game_ids("cards", ids)
    session = OriginalSession(StdioTransport())
    original = OriginalBackend(session, IRONCLAD_A0_ACT1)
    try:
        decision = original.reset(seed)
        for _ in range(40):
            if decision.observation.screen is ScreenType.COMBAT:
                break
            decision = original.step(deterministic_action(decision, decision)).decision
        else:
            raise RuntimeError("Original did not reach the first combat")
        if len(decision.observation.enemies) != 1:
            raise RuntimeError("card audit seed must reach a one-monster first combat")

        entries = []
        for index, card_id in enumerate(ids, 1):
            source = sources[game_ids[card_id]]
            variants = [_probe_variant(session, card_id, upgrades, seed) for upgrades in (0, 1)]
            entries.append({
                "id": card_id,
                "game_id": game_ids[card_id],
                "java_source": source.path.relative_to(ROOT).as_posix(),
                "java_sha256": sha256_file(source.path),
                "variants": variants,
            })
            print(f"CARD_AUDIT {index}/{len(ids)} {card_id}", file=sys.stderr, flush=True)
    finally:
        original.return_to_menu()
    result: dict[str, Any] = {
        "schema": SCHEMA,
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
    parser.add_argument("--output", type=Path, default=DEFAULT_OUTPUT)
    args = parser.parse_args()
    payload = capture(args.seed)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(payload, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    print(f"CARD_AUDIT_COMPLETE {args.output} {payload['audit_sha256']}", file=sys.stderr)
    return 0


if __name__ == "__main__":
    from sls.validation.runtime import write_completion
    try:
        result = main()
    except BaseException as error:
        write_completion(2, entry="card-audit", error=f"{type(error).__name__}: {error}", argv=sys.argv)
        raise
    else:
        write_completion(result, entry="card-audit")
        raise SystemExit(result)
