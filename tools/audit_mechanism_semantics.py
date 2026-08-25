"""Executable Original/native evidence for controlled shared-rule scenarios."""

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
from sls.content.scope import ironclad_a0_scope_hash  # noqa: E402
from sls.contracts import ActionKind, Decision, ScreenType  # noqa: E402
from sls.rl.training_contract import canonical_digest, sha256_file  # noqa: E402
from sls.validation.policies import deterministic_action  # noqa: E402
from sls.validation.truth import load_bundle  # noqa: E402
from audit_card_semantics import _adapt_probe_payload, _rng  # noqa: E402
from audit_relic_semantics import _effect_projection  # noqa: E402


SCENARIOS = {
    "damage_buffer_intangible": ("DAMAGE_PIPELINE", ("END_TURN",)),
    "duration_weak": ("POWER_ORDER", ("END_TURN",)),
    "retain_ethereal": ("POWER_ORDER", ("END_TURN",)),
}


def _state(decision: Decision, payload: Mapping[str, Any]) -> dict[str, Any]:
    return {"decision": _effect_projection(decision), "rng": _rng(payload)}


def _assert_match(
    scenario_id: str, boundary: int, original: AdaptedOriginalDecision,
    simulator: AdaptedOriginalDecision, original_payload: Mapping[str, Any],
    simulator_payload: Mapping[str, Any],
) -> str:
    expected = _state(original.decision, original_payload)
    actual = _state(simulator.decision, simulator_payload)
    if expected != actual:
        root = ROOT / "validation-results" / "content-audit"
        root.mkdir(parents=True, exist_ok=True)
        (root / "mechanism-mismatch-original.json").write_text(
            json.dumps(expected, indent=2, sort_keys=True) + "\n", encoding="utf-8",
        )
        (root / "mechanism-mismatch-simulator.json").write_text(
            json.dumps(actual, indent=2, sort_keys=True) + "\n", encoding="utf-8",
        )
        raise RuntimeError(f"mechanism mismatch: {scenario_id} boundary {boundary}")
    return canonical_digest(expected)


def _native_step(battle: Any, decision: Decision, kind: str) -> None:
    wanted = ActionKind[kind]
    action = next(item for item in decision.actions if item.kind is wanted)
    if wanted is ActionKind.END_TURN:
        battle.step("end_turn")
        return
    raise ValueError(f"unsupported mechanism audit action: {action.kind.value}")


def capture(seed: int) -> dict[str, Any]:
    support_paths: list[Path] = []
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
        for scenario_id, (mechanism, actions) in SCENARIOS.items():
            original_payload = session.execute(f"parity_scenario {scenario_id}")
            scenario = dict(original_payload.get("_parity_scenario") or {})
            if scenario.get("scenario_id") != scenario_id:
                raise RuntimeError(f"Original did not attest {scenario_id}: {scenario}")
            if scenario.get("source") != "RULE_TEST:ISOLATED_V2":
                raise RuntimeError(f"Original loaded a stale rule-test oracle: {scenario}")
            original = adapt_original(original_payload)
            backend._adapted = original

            battle = native.LightspeedBattle()
            battle.reset(seed, "CULTIST")
            battle.apply_scenario(scenario_id)
            battle.set_rng_state(_rng(original_payload))
            simulator_payload = battle.snapshot()
            simulator = _adapt_probe_payload(simulator_payload)
            hashes = [
                _assert_match(
                    scenario_id, 0, original, simulator,
                    original_payload, simulator_payload,
                )
            ]
            for boundary, kind in enumerate(actions, 1):
                original_action = next(
                    item for item in original.decision.actions
                    if item.kind is ActionKind[kind]
                )
                original_transition = backend.step(original_action)
                original_payload = session.payload or {}
                original = AdaptedOriginalDecision(
                    original_transition.decision,
                    backend._adapted.commands if backend._adapted is not None else {},
                )
                _native_step(battle, simulator.decision, kind)
                simulator_payload = battle.snapshot()
                simulator = _adapt_probe_payload(simulator_payload)
                hashes.append(
                    _assert_match(
                        scenario_id, boundary, original, simulator,
                        original_payload, simulator_payload,
                    )
                )
            entries.append({
                "id": scenario_id,
                "mechanism": mechanism,
                "setup_digest": scenario["setup_digest"],
                "actions": list(actions),
                "boundary_hashes": hashes,
                "effect_sha256": canonical_digest(hashes),
            })
            print(f"MECHANISM_AUDIT {scenario_id}", file=sys.stderr, flush=True)

        for engine_id, mechanism in (("stance", "STANCE_ENGINE"), ("orb", "ORB_ENGINE")):
            original_payload = session.execute(f"parity_engine {engine_id}")
            scenario = dict(original_payload.get("_parity_scenario") or {})
            if scenario.get("scenario_id") != f"engine_probe:{engine_id.upper()}":
                raise RuntimeError(f"Original did not attest {engine_id} engine: {scenario}")
            if engine_id == "stance":
                native_probe = native.stance_mechanics_probe()
                expected = {
                    "calm_exit_energy": int(scenario["calm_exit_energy"]),
                    "calm_exit_stance": scenario["calm_exit_stance"],
                    "divinity_entry_energy": int(scenario["divinity_entry_energy"]),
                    "divinity_entry_stance": scenario["divinity_entry_stance"],
                }
                actual = {
                    "calm_exit_energy": native_probe["calm_exit"]["energy"],
                    "calm_exit_stance": native_probe["calm_exit"]["stance"],
                    "divinity_entry_energy": native_probe["divinity_entry"]["energy"],
                    "divinity_entry_stance": native_probe["divinity_entry"]["stance"],
                }
            else:
                native_probe = native.orb_mechanics_probe()
                expected = {
                    "plasma_evoke_energy": int(scenario["plasma_evoke_energy"]),
                    "frost_evoke_block": int(scenario["frost_evoke_block"]),
                    "slot_cap": int(scenario["slot_cap"]),
                }
                actual = {
                    "plasma_evoke_energy": native_probe["plasma"]["energy_gained"],
                    "frost_evoke_block": (
                        native_probe["frost_evoke"]["block"]
                        - native_probe["passive"]["block"]
                    ),
                    "slot_cap": native_probe["slot_cap"],
                }
            if expected != actual:
                raise RuntimeError(
                    f"{engine_id} engine mismatch: original={expected} native={actual}"
                )
            digest = canonical_digest(expected)
            entries.append({
                "id": f"engine_{engine_id}",
                "mechanism": mechanism,
                "setup_digest": scenario["setup_digest"],
                "actions": [],
                "boundary_hashes": [digest],
                "effect_sha256": canonical_digest([digest]),
            })
            print(f"MECHANISM_AUDIT engine_{engine_id}", file=sys.stderr, flush=True)

        expansion_path = ROOT / "validation-results" / "act1-validation-expansion.json"
        expansion = json.loads(expansion_path.read_text(encoding="utf-8"))
        support_paths.append(expansion_path)
        leaves = sorted({
            str(evidence["leaf"])
            for round_item in expansion["rounds"]
            for evidence in round_item["evidence"]
        })
        run_hashes: list[str] = []
        noncombat_evidence: dict[str, Any] | None = None
        for leaf in leaves:
            bundle = ROOT / "validation-results" / "truth" / leaf
            manifest, boundaries = load_bundle(bundle, verify=True)
            support_paths.append(bundle / "manifest.json")
            if not boundaries or any(
                (boundary.get("comparison") or {}).get("status") != "MATCH"
                for boundary in boundaries
            ):
                raise RuntimeError(f"expansion truth is not exact: {leaf}")
            run_hashes.append(canonical_digest({
                "leaf": leaf,
                "manifest": manifest,
                "original": [boundary["original_boundary_hash"] for boundary in boundaries],
                "simulator": [boundary["simulator_boundary_hash"] for boundary in boundaries],
            }))
            if noncombat_evidence is None:
                for boundary in boundaries:
                    decision = boundary["canonical_original_decision"]
                    observation = decision["observation"]
                    potion_ids = {
                        str(potion["content_id"]) for potion in observation.get("potions") or ()
                    }
                    action_kinds = {str(action["kind"]) for action in decision["actions"]}
                    if observation["screen"] != "COMBAT" and "FRUIT_JUICE" in potion_ids \
                            and {"USE_POTION", "DISCARD_POTION"} <= action_kinds:
                        noncombat_evidence = {
                            "leaf": leaf,
                            "sequence": boundary["sequence"],
                            "screen": observation["screen"],
                            "original_boundary_hash": boundary["original_boundary_hash"],
                            "simulator_boundary_hash": boundary["simulator_boundary_hash"],
                        }
                        break
        if noncombat_evidence is None:
            raise RuntimeError("expansion truth lacks a matched noncombat potion boundary")

        run = native.LightspeedRunState()
        run.reset(seed)
        checkpoint = run.snapshot()
        replay = native.LightspeedRunState()
        replay.load_state(checkpoint)
        if replay.snapshot() != checkpoint:
            raise RuntimeError("native full-run checkpoint did not restore exactly")
        run_digest = canonical_digest({
            "paired_truth": run_hashes,
            "native_checkpoint": checkpoint,
        })
        entries.append({
            "id": "run_and_checkpoint",
            "mechanism": "RUN_AND_CHECKPOINT",
            "setup_digest": canonical_digest(leaves),
            "actions": [],
            "boundary_hashes": [run_digest],
            "effect_sha256": canonical_digest([run_digest]),
            "truth_leaves": leaves,
        })
        noncombat_digest = canonical_digest(noncombat_evidence)
        entries.append({
            "id": "noncombat_potion_actions",
            "mechanism": "NONCOMBAT_POTION_ACTIONS",
            "setup_digest": canonical_digest(noncombat_evidence),
            "actions": ["USE_POTION", "DISCARD_POTION"],
            "boundary_hashes": [noncombat_digest],
            "effect_sha256": canonical_digest([noncombat_digest]),
            "truth_evidence": noncombat_evidence,
        })
    finally:
        backend.return_to_menu()

    source_paths = (
        ROOT / "java" / "oracle-mod" / "src" / "main" / "java" / "spirecomm" / "parity" / "OracleScenarioPatch.java",
        ROOT / "cpp" / "simulator" / "python" / "module.cpp",
        ROOT / "reference" / "original-game" / "decompiled" / "com" / "megacrit" / "cardcrawl" / "random" / "Random.java",
        ROOT / "reference" / "original-game" / "decompiled" / "com" / "megacrit" / "cardcrawl" / "powers" / "BufferPower.java",
        ROOT / "reference" / "original-game" / "decompiled" / "com" / "megacrit" / "cardcrawl" / "powers" / "IntangiblePlayerPower.java",
        ROOT / "reference" / "original-game" / "decompiled" / "com" / "megacrit" / "cardcrawl" / "powers" / "WeakPower.java",
        ROOT / "reference" / "original-game" / "decompiled" / "com" / "megacrit" / "cardcrawl" / "powers" / "EquilibriumPower.java",
        ROOT / "reference" / "original-game" / "decompiled" / "com" / "megacrit" / "cardcrawl" / "powers" / "watcher" / "EstablishmentPower.java",
        ROOT / "reference" / "original-game" / "decompiled" / "com" / "megacrit" / "cardcrawl" / "stances" / "CalmStance.java",
        ROOT / "reference" / "original-game" / "decompiled" / "com" / "megacrit" / "cardcrawl" / "stances" / "DivinityStance.java",
        ROOT / "reference" / "original-game" / "decompiled" / "com" / "megacrit" / "cardcrawl" / "orbs" / "Frost.java",
        ROOT / "reference" / "original-game" / "decompiled" / "com" / "megacrit" / "cardcrawl" / "orbs" / "Plasma.java",
        ROOT / "reference" / "original-game" / "decompiled" / "com" / "megacrit" / "cardcrawl" / "potions" / "BloodPotion.java",
        ROOT / "reference" / "original-game" / "decompiled" / "com" / "megacrit" / "cardcrawl" / "potions" / "EntropicBrew.java",
        ROOT / "reference" / "original-game" / "decompiled" / "com" / "megacrit" / "cardcrawl" / "potions" / "FruitJuice.java",
    )
    result: dict[str, Any] = {
        "schema": "sls-ironclad-mechanism-semantics-v1",
        "scope_sha256": ironclad_a0_scope_hash(),
        "oracle_schema": "spirecomm-parity-v10",
        "seed": seed,
        "source_files": {
            path.relative_to(ROOT).as_posix(): sha256_file(path)
            for path in (*source_paths, *support_paths)
        },
        "entries": entries,
    }
    result["audit_sha256"] = canonical_digest(result)
    return result


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--seed", type=int, default=0)
    parser.add_argument(
        "--output", type=Path,
        default=ROOT / "configs" / "validation" / "ironclad_a0_mechanism_semantics.json",
    )
    args = parser.parse_args()
    payload = capture(args.seed)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(
        json.dumps(payload, indent=2, sort_keys=True) + "\n", encoding="utf-8",
    )
    return 0


if __name__ == "__main__":
    from sls.validation.runtime import write_completion
    try:
        code = main()
    except BaseException as error:
        write_completion(
            2, entry="mechanism-audit",
            error=f"{type(error).__name__}: {error}", argv=sys.argv,
        )
        raise
    else:
        write_completion(code, entry="mechanism-audit")
        raise SystemExit(code)
