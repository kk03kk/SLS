"""Differential first-turn probes for deterministic scoped combat relics."""

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
from sls.backends.simulator import IRONCLAD_A0_ACT1, native  # noqa: E402
from sls.content.scope import ironclad_a0_scope_hash, load_ironclad_a0_scope  # noqa: E402
from sls.content.source_audit import (  # noqa: E402
    java_relic_callbacks, java_sources, registry_game_ids,
)
from sls.contracts import Decision, ScreenType  # noqa: E402
from sls.rl.training_contract import canonical_digest, sha256_file  # noqa: E402
from sls.validation.policies import deterministic_action  # noqa: E402
from audit_card_semantics import _adapt_probe_payload, _rng  # noqa: E402
from audit_potion_semantics import _projection  # noqa: E402


# Every scoped relic is installed through the same stock lifecycle boundary.
# Relics without a first-turn hook still attest constructor/onEquip neutrality,
# inventory identity and RNG non-consumption; later trigger scenarios can add
# callback-specific hashes without leaving any relic unexecutable.
FIRST_TURN_RELICS = tuple(map(str, load_ironclad_a0_scope()["relics"]["ids"]))
FIRST_TURN_CALLBACKS = {
    "onEquip", "atPreBattle", "atBattleStartPreDraw", "atBattleStart",
    "atTurnStart", "atTurnStartPostDraw",
}
INTERACTIVE_EQUIP_RELICS = {
    "ASTROLABE", "BOTTLED_FLAME", "BOTTLED_LIGHTNING", "BOTTLED_TORNADO",
    "CALLING_BELL", "CAULDRON", "DOLLYS_MIRROR", "EMPTY_CAGE", "ORRERY",
    "PANDORAS_BOX", "TINY_HOUSE",
}


def _effect_projection(decision: Decision) -> dict[str, Any]:
    value = _projection(decision)
    # The probe deliberately normalizes the actionable card piles after stock
    # lifecycle callbacks. Their instance ids differ across the long-lived Java
    # process, so semantic content and state are the relevant contract here.
    for zone in ("hand", "draw_pile", "discard_pile", "exhaust_pile"):
        for card in value[zone]:
            card.pop("instance_id", None)
    return value


def _assert_match(
    relic_id: str,
    original: AdaptedOriginalDecision,
    simulator: AdaptedOriginalDecision,
    original_payload: Mapping[str, Any],
    simulator_payload: Mapping[str, Any],
) -> str:
    expected = {"decision": _effect_projection(original.decision), "rng": _rng(original_payload)}
    actual = {"decision": _effect_projection(simulator.decision), "rng": _rng(simulator_payload)}
    if expected != actual:
        root = ROOT / "validation-results" / "content-audit"
        root.mkdir(parents=True, exist_ok=True)
        (root / "relic-mismatch-original.json").write_text(
            json.dumps(expected, indent=2, sort_keys=True) + "\n", encoding="utf-8",
        )
        (root / "relic-mismatch-simulator.json").write_text(
            json.dumps(actual, indent=2, sort_keys=True) + "\n", encoding="utf-8",
        )
        raise RuntimeError(f"first-turn relic mismatch: {relic_id}")
    return canonical_digest(expected)


def capture(seed: int) -> dict[str, Any]:
    scope = load_ironclad_a0_scope()
    game_ids = registry_game_ids("relics", scope["relics"]["ids"])
    sources = java_sources("relics")
    session = OriginalSession(StdioTransport())
    original_backend = OriginalBackend(session, IRONCLAD_A0_ACT1)
    try:
        decision = original_backend.reset(seed)
        for _ in range(40):
            if decision.observation.screen is ScreenType.COMBAT:
                break
            decision = original_backend.step(deterministic_action(decision, decision)).decision
        else:
            raise RuntimeError("Original did not reach first combat")
        entries = []
        for index, relic_id in enumerate(FIRST_TURN_RELICS, 1):
            original_payload = session.execute(f"parity_relic {relic_id}")
            scenario = dict(original_payload.get("_parity_scenario") or {})
            expected_id = f"relic_probe:{relic_id}:FIRST_TURN"
            if str(scenario.get("scenario_id") or "").upper() != expected_id.upper():
                raise RuntimeError(f"Original did not attest {expected_id}: {scenario}")
            battle = native.LightspeedBattle()
            battle.reset_relic_probe(seed, relic_id)
            simulator_payload = battle.snapshot()
            digest = _assert_match(
                relic_id, adapt_original(original_payload),
                _adapt_probe_payload(simulator_payload), original_payload, simulator_payload,
            )
            source = sources[game_ids[relic_id]]
            callbacks = set(java_relic_callbacks(source))
            invoked = callbacks & FIRST_TURN_CALLBACKS
            if relic_id in INTERACTIVE_EQUIP_RELICS:
                invoked.discard("onEquip")
            entries.append({
                "id": relic_id, "scenario": "FIRST_TURN",
                "setup_digest": scenario["setup_digest"], "effect_sha256": digest,
                "game_id": game_ids[relic_id],
                "java_source": source.path.relative_to(ROOT).as_posix(),
                "java_sha256": sha256_file(source.path),
                "covered_callbacks": sorted(invoked),
                "remaining_callbacks": sorted(callbacks - invoked),
                "callback_complete": callbacks == invoked,
            })
            print(f"RELIC_AUDIT {index}/{len(FIRST_TURN_RELICS)} {relic_id}", file=sys.stderr, flush=True)
    finally:
        original_backend.return_to_menu()
    result: dict[str, Any] = {
        "schema": "sls-ironclad-relic-semantics-v1",
        "scope_sha256": ironclad_a0_scope_hash(),
        "oracle_schema": "spirecomm-parity-v10",
        "seed": seed, "entries": entries,
    }
    result["audit_sha256"] = canonical_digest(result)
    return result


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--seed", type=int, default=0)
    parser.add_argument("--output", type=Path, default=ROOT / "configs" / "validation" / "ironclad_a0_relic_semantics.json")
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
        write_completion(2, entry="relic-audit", error=f"{type(error).__name__}: {error}", argv=sys.argv)
        raise
    else:
        write_completion(code, entry="relic-audit")
        raise SystemExit(code)
