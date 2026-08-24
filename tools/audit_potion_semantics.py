"""Capture deterministic Original/native effect evidence for scoped potions."""

from __future__ import annotations

import argparse
import json
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

from audit_card_semantics import _adapt_probe_payload, _rng  # noqa: E402


SCHEMA = "sls-ironclad-potion-semantics-v1"
DEFAULT_OUTPUT = ROOT / "configs" / "validation" / "ironclad_a0_potion_semantics.json"


def _projection(decision: Decision) -> dict[str, Any]:
    value = decision.observation.to_dict()
    return {
        key: value[key] for key in (
            "screen", "player", "hand", "draw_pile", "discard_pile",
            "exhaust_pile", "enemies", "powers", "relics", "potions",
            "choice_options", "public_context",
        )
    } | {
        "actions": sorted(
            (action.to_dict() for action in decision.actions),
            key=lambda item: json.dumps(item, sort_keys=True, separators=(",", ":")),
        ),
        "terminal": decision.terminal,
    }


def _assert_match(
    potion_id: str, sacred_bark: bool, boundary: int,
    original: AdaptedOriginalDecision, simulator: AdaptedOriginalDecision,
    original_payload: Mapping[str, Any], simulator_payload: Mapping[str, Any],
) -> str:
    expected = {"decision": _projection(original.decision), "rng": _rng(original_payload)}
    actual = {"decision": _projection(simulator.decision), "rng": _rng(simulator_payload)}
    if expected != actual:
        root = ROOT / "validation-results" / "content-audit"
        root.mkdir(parents=True, exist_ok=True)
        expected_path = root / "potion-mismatch-original.json"
        actual_path = root / "potion-mismatch-simulator.json"
        expected_path.write_text(json.dumps(expected, indent=2, sort_keys=True) + "\n", encoding="utf-8")
        actual_path.write_text(json.dumps(actual, indent=2, sort_keys=True) + "\n", encoding="utf-8")
        raise RuntimeError(
            f"potion semantic mismatch potion={potion_id} sacred_bark={sacred_bark} "
            f"boundary={boundary}; details={expected_path},{actual_path}"
        )
    return canonical_digest(expected)


def _execute_original(
    session: OriginalSession, adapted: AdaptedOriginalDecision, action: Action,
) -> dict[str, Any]:
    payload: dict[str, Any] = session.payload or {}
    for command in adapted.commands[action.candidate_id]:
        payload = session.execute(command)
    return payload


def _execute_native(
    battle: Any, adapted: AdaptedOriginalDecision, action: Action,
) -> dict[str, Any]:
    payload: dict[str, Any] = battle.snapshot()
    for command in adapted.commands[action.candidate_id]:
        parts = command.split()
        kind = parts[0].lower()
        if kind == "potion" and parts[1].lower() == "use":
            battle.step(
                "potion", potion_index=int(parts[2]),
                target_index=int(parts[3]) if len(parts) > 3 else 0,
            )
        elif kind == "choose":
            battle.step("choose", choice_index=int(parts[1]))
        elif kind in {"confirm", "proceed"}:
            battle.step("proceed")
        elif kind in {"end", "end_turn"}:
            battle.step("end_turn")
        elif kind in {"wait", "state"}:
            continue
        else:
            raise RuntimeError(f"unsupported native potion-probe command: {command}")
        payload = battle.snapshot()
    return payload


def _common_action(original: Decision, simulator: Decision) -> Action:
    simulator_ids = {action.candidate_id for action in simulator.actions}
    common = [action for action in original.actions if action.candidate_id in simulator_ids]
    if not common:
        raise RuntimeError("potion probe continuation exposes no common semantic action")
    return sorted(common, key=lambda action: (PRIORITY.get(action.kind, 1000), action.candidate_id))[0]


def _probe_variant(
    session: OriginalSession, potion_id: str, sacred_bark: bool, seed: int,
) -> dict[str, Any]:
    original_payload = session.execute(f"parity_potion {potion_id} {str(sacred_bark).lower()}")
    scenario = original_payload.get("_parity_scenario") or {}
    expected_scenario = f"potion_probe:{potion_id}:{str(sacred_bark).lower()}"
    if str(scenario.get("scenario_id")).lower() != expected_scenario.lower():
        raise RuntimeError(f"Original did not attest {expected_scenario}: {scenario}")

    battle = native.LightspeedBattle()
    battle.reset_potion_probe(seed, potion_id, sacred_bark)
    battle.set_rng_state(_rng(original_payload))
    simulator_payload = battle.snapshot()
    original = adapt_original(original_payload)
    simulator = _adapt_probe_payload(simulator_payload)
    hashes = [_assert_match(
        potion_id, sacred_bark, 0, original, simulator,
        original_payload, simulator_payload,
    )]
    wanted_kind = ActionKind.END_TURN if potion_id == "FAIRY_POTION" else ActionKind.USE_POTION
    candidates = [action for action in original.decision.actions if action.kind is wanted_kind]
    common_ids = {action.candidate_id for action in simulator.decision.actions}
    candidates = [action for action in candidates if action.candidate_id in common_ids]
    if not candidates:
        raise RuntimeError(f"potion probe has no common trigger: {potion_id}/{sacred_bark}")
    action = candidates[0]

    for boundary in range(1, 40):
        original_payload = _execute_original(session, original, action)
        simulator_payload = _execute_native(battle, simulator, action)
        original = adapt_original(original_payload)
        simulator = _adapt_probe_payload(simulator_payload)
        hashes.append(_assert_match(
            potion_id, sacred_bark, boundary, original, simulator,
            original_payload, simulator_payload,
        ))
        if original.decision.terminal or "parity_potion" in {
            str(item).lower() for item in original_payload.get("available_commands") or ()
        }:
            return {
                "sacred_bark": sacred_bark,
                "setup_digest": str(scenario["setup_digest"]),
                "boundaries": boundary + 1,
                "boundary_hashes": hashes,
                "effect_sha256": canonical_digest(hashes),
            }
        action = _common_action(original.decision, simulator.decision)
    raise RuntimeError(f"potion probe continuation did not settle: {potion_id}/{sacred_bark}")


def capture(seed: int) -> dict[str, Any]:
    scope = load_ironclad_a0_scope()
    ids = sorted(map(str, scope["potions"]["ids"]), key=lambda item: (item == "SMOKE_BOMB", item))
    sources = java_sources("potions")
    game_ids = registry_game_ids("potions", ids)
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
        entries = []
        for index, potion_id in enumerate(ids, 1):
            source = sources[game_ids[potion_id]]
            variants = [_probe_variant(session, potion_id, False, seed)]
            if potion_id != "SMOKE_BOMB":
                variants.append(_probe_variant(session, potion_id, True, seed))
            entries.append({
                "id": potion_id,
                "game_id": game_ids[potion_id],
                "java_source": source.path.relative_to(ROOT).as_posix(),
                "java_sha256": sha256_file(source.path),
                "variants": variants,
            })
            print(f"POTION_AUDIT {index}/{len(ids)} {potion_id}", file=sys.stderr, flush=True)
    finally:
        original.return_to_menu()
    result: dict[str, Any] = {
        "schema": SCHEMA,
        "scope_sha256": ironclad_a0_scope_hash(),
        "oracle_schema": "spirecomm-parity-v10",
        "seed": seed,
        "entries": sorted(entries, key=lambda item: item["id"]),
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
    print(f"POTION_AUDIT_COMPLETE {args.output} {payload['audit_sha256']}", file=sys.stderr)
    return 0


if __name__ == "__main__":
    from sls.validation.runtime import write_completion
    try:
        result = main()
    except BaseException as error:
        write_completion(2, entry="potion-audit", error=f"{type(error).__name__}: {error}", argv=sys.argv)
        raise
    else:
        write_completion(result, entry="potion-audit")
        raise SystemExit(result)
