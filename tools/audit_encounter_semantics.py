"""Differential constructor and first-turn traces for scoped encounters."""

from __future__ import annotations

import argparse
import json
from pathlib import Path
import re
import sys
from typing import Any, Mapping

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "src"))
sys.path.insert(0, str(ROOT / "tools"))

from sls.backends.original import OriginalBackend, OriginalSession, StdioTransport  # noqa: E402
from sls.backends.original.adapter import AdaptedOriginalDecision, adapt_original  # noqa: E402
from sls.backends.simulator import IRONCLAD_A0_ACT1, IRONCLAD_A0_HEART, native  # noqa: E402
from sls.content.normalize import normalize_content_id  # noqa: E402
from sls.content.registry import load_content_registry  # noqa: E402
from sls.content.scope import ironclad_a0_scope_hash, load_ironclad_a0_scope  # noqa: E402
from sls.contracts import ActionKind, ScreenType  # noqa: E402
from sls.rl.training_contract import canonical_digest, source_sha256  # noqa: E402
from sls.validation.policies import deterministic_action  # noqa: E402
from audit_card_semantics import _adapt_probe_payload, _rng  # noqa: E402
from audit_relic_semantics import _effect_projection  # noqa: E402


def _monster_sources() -> dict[str, Path]:
    result: dict[str, Path] = {}
    registry = load_content_registry()
    game_ids = {
        str(item["game_id"]): str(item["id"])
        for item in registry.items("monsters")
        if item.get("game_id")
    }
    original_aliases = {
        "BanditBear": "BEAR",
        "BanditChild": "POINTY",
        "BanditLeader": "ROMEO",
        "Champ": "THE_CHAMP",
        "Healer": "MYSTIC",
        "Maw": "THE_MAW",
        "SlaverBoss": "TASKMASTER",
    }
    root = ROOT / "reference" / "original-game" / "decompiled" / "com" / "megacrit" / "cardcrawl" / "monsters"
    for path in root.rglob("*.java"):
        text = path.read_text(encoding="utf-8", errors="replace")
        match = re.search(r'public\s+static\s+final\s+String\s+ID\s*=\s*"([^"]+)"', text)
        if match is None:
            continue
        game_id = match.group(1)
        result[original_aliases.get(
            game_id, game_ids.get(game_id, normalize_content_id(game_id)),
        )] = path
    return result


def _state(adapted: AdaptedOriginalDecision, payload: Mapping[str, Any]) -> dict[str, Any]:
    decision = _effect_projection(adapted.decision)
    # Native models Lagavulin's private ``isOut`` flag as an internal ASLEEP
    # status so damage code can wake it. Stock exposes the same fact through
    # its SLEEP intent, not an AbstractPower. Compare the public mechanism and
    # omit this one representation-only implementation marker.
    decision["powers"] = [
        power for power in decision["powers"]
        if power["content_id"] not in {"ASLEEP", "MINION_LEADER"}
    ]
    for index, power in enumerate(decision["powers"]):
        power["instance_id"] = f"POWER:{index}"
    return {"decision": decision, "rng": _rng(payload)}


def _assert_match(
    encounter_id: str, boundary: int,
    original: AdaptedOriginalDecision, simulator: AdaptedOriginalDecision,
    original_payload: Mapping[str, Any], simulator_payload: Mapping[str, Any],
) -> str:
    expected = _state(original, original_payload)
    actual = _state(simulator, simulator_payload)
    if expected != actual:
        root = ROOT / "validation-results" / "content-audit"
        root.mkdir(parents=True, exist_ok=True)
        (root / "encounter-mismatch-original.json").write_text(
            json.dumps(expected, indent=2, sort_keys=True) + "\n", encoding="utf-8",
        )
        (root / "encounter-mismatch-simulator.json").write_text(
            json.dumps(actual, indent=2, sort_keys=True) + "\n", encoding="utf-8",
        )
        raise RuntimeError(f"encounter mismatch: {encounter_id} boundary {boundary}")
    return canonical_digest(expected)


def capture(
    seed: int, *, fullrun: bool = False, only_encounter: str | None = None,
) -> dict[str, Any]:
    scope = load_ironclad_a0_scope()
    if fullrun:
        inventory = json.loads(
            (ROOT / "configs" / "validation" / "ironclad_fullrun_inventory.json").read_text(
                encoding="utf-8"
            )
        )
        encounter_ids = tuple(sorted(set().union(*(
            set(map(str, values)) for values in inventory["encounters"].values()
        ))))
        expected_monsters = set().union(*(
            set(map(str, values)) for values in inventory["monsters"].values()
        ))
        profile = IRONCLAD_A0_HEART
        scope_digest = str(inventory["inventory_sha256"])
        schema = "sls-ironclad-fullrun-encounter-semantics-v1"
    else:
        encounter_ids = tuple(map(str, scope["encounters"]["act1"]))
        expected_monsters = set(map(str, scope["monsters"]["act1"]))
        profile = IRONCLAD_A0_ACT1
        scope_digest = ironclad_a0_scope_hash()
        schema = "sls-ironclad-encounter-semantics-v1"
    if only_encounter is not None:
        wanted = tuple(
            item.strip().upper() for item in only_encounter.split(",") if item.strip()
        )
        unknown = sorted(set(wanted) - set(encounter_ids))
        if not wanted or unknown:
            raise ValueError(f"unknown scoped encounter(s): {unknown or only_encounter}")
        encounter_ids = tuple(item for item in encounter_ids if item in set(wanted))
    monster_sources = _monster_sources()
    session = OriginalSession(StdioTransport())
    backend = OriginalBackend(session, profile)
    try:
        decision = backend.reset(seed)
        for _ in range(40):
            if decision.observation.screen is ScreenType.COMBAT:
                break
            decision = backend.step(deterministic_action(decision, decision)).decision
        else:
            raise RuntimeError("Original did not reach first combat")

        entries = []
        covered_monsters: set[str] = set()
        for index, encounter_id in enumerate(encounter_ids, 1):
            pre_rng = _rng(session.payload or {})
            original_payload = session.execute(f"parity_encounter {encounter_id}")
            scenario = dict(original_payload.get("_parity_scenario") or {})
            if scenario.get("scenario_id") != f"encounter_probe:{encounter_id}":
                raise RuntimeError(f"Original did not attest encounter {encounter_id}: {scenario}")
            original = adapt_original(original_payload)
            backend._adapted = original
            initial_monster_ids = {
                enemy.monster_id for enemy in original.decision.observation.enemies
            }

            battle = native.LightspeedBattle()
            battle.reset_encounter_probe(seed, encounter_id, pre_rng)
            simulator_payload = battle.snapshot()
            simulator = _adapt_probe_payload(simulator_payload)
            hashes = [
                _assert_match(
                    encounter_id, 0, original, simulator,
                    original_payload, simulator_payload,
                )
            ]

            original_action = next(
                action for action in original.decision.actions
                if action.kind is ActionKind.END_TURN
            )
            transition = backend.step(original_action)
            original_payload = session.payload or {}
            original = backend._adapted
            if original is None:
                raise RuntimeError("Original encounter transition lost its adapted boundary")
            battle.step("end_turn")
            simulator_payload = battle.snapshot()
            simulator = _adapt_probe_payload(simulator_payload)
            hashes.append(
                _assert_match(
                    encounter_id, 1, original, simulator,
                    original_payload, simulator_payload,
                )
            )

            monster_ids = sorted({
                enemy.monster_id for enemy in transition.decision.observation.enemies
            } | initial_monster_ids)
            covered_monsters.update(monster_ids)
            entries.append({
                "id": encounter_id,
                "scenario": "CONSTRUCTOR_AND_FIRST_TURN",
                "setup_digest": scenario["setup_digest"],
                "monster_ids": monster_ids,
                "boundary_hashes": hashes,
                "effect_sha256": canonical_digest(hashes),
            })
            print(f"ENCOUNTER_AUDIT {index}/{len(encounter_ids)} {encounter_id}", file=sys.stderr, flush=True)

        # Variable-composition encounters need additional RNG variants before
        # every scoped monster can carry direct constructor/turn evidence.
        # Repeat only the two relevant stock encounters and stop as soon as the
        # exact monster closure is covered.
        entry_by_id = {entry["id"]: entry for entry in entries}
        coverage_encounters = () if only_encounter is not None else (
            ("LARGE_SLIME", "LOTS_OF_SLIMES", "GREMLIN_GANG", "GREMLIN_LEADER")
            if fullrun else ("LARGE_SLIME", "GREMLIN_GANG")
        )
        for encounter_id in coverage_encounters:
            for variant in range(1, 13):
                if covered_monsters == expected_monsters:
                    break
                target_missing = expected_monsters - covered_monsters if fullrun else (
                    {"ACID_SLIME_L", "SPIKE_SLIME_L"}
                    if encounter_id == "LARGE_SLIME" else
                    {"FAT_GREMLIN", "GREMLIN_WIZARD", "MAD_GREMLIN", "SHIELD_GREMLIN", "SNEAKY_GREMLIN"}
                ) - covered_monsters
                if not target_missing:
                    break
                pre_rng = _rng(session.payload or {})
                original_payload = session.execute(f"parity_encounter {encounter_id}")
                scenario = dict(original_payload.get("_parity_scenario") or {})
                original = adapt_original(original_payload)
                backend._adapted = original
                initial_monster_ids = {
                    enemy.monster_id for enemy in original.decision.observation.enemies
                }
                battle = native.LightspeedBattle()
                battle.reset_encounter_probe(seed, encounter_id, pre_rng)
                simulator_payload = battle.snapshot()
                simulator = _adapt_probe_payload(simulator_payload)
                hashes = [
                    _assert_match(
                        encounter_id, variant * 2, original, simulator,
                        original_payload, simulator_payload,
                    )
                ]
                original_action = next(
                    action for action in original.decision.actions
                    if action.kind is ActionKind.END_TURN
                )
                transition = backend.step(original_action)
                original_payload = session.payload or {}
                original = backend._adapted
                if original is None:
                    raise RuntimeError("Original supplemental encounter lost its boundary")
                battle.step("end_turn")
                simulator_payload = battle.snapshot()
                simulator = _adapt_probe_payload(simulator_payload)
                hashes.append(
                    _assert_match(
                        encounter_id, variant * 2 + 1, original, simulator,
                        original_payload, simulator_payload,
                    )
                )
                monster_ids = sorted(initial_monster_ids | {
                    enemy.monster_id for enemy in transition.decision.observation.enemies
                })
                covered_monsters.update(monster_ids)
                entry = entry_by_id[encounter_id]
                entry["monster_ids"] = sorted(set(entry["monster_ids"]) | set(monster_ids))
                entry.setdefault("coverage_variants", []).append({
                    "variant": variant,
                    "setup_digest": scenario["setup_digest"],
                    "monster_ids": monster_ids,
                    "boundary_hashes": hashes,
                    "effect_sha256": canonical_digest(hashes),
                })
                print(
                    f"ENCOUNTER_COVERAGE {encounter_id} variant={variant} "
                    f"remaining={sorted(expected_monsters - covered_monsters)}",
                    file=sys.stderr, flush=True,
                )
    finally:
        backend.return_to_menu()

    if only_encounter is None and covered_monsters != expected_monsters:
        raise RuntimeError(
            f"encounter probes do not cover exact monster scope: "
            f"missing={sorted(expected_monsters - covered_monsters)} "
            f"extra={sorted(covered_monsters - expected_monsters)}"
        )
    registry = ROOT / "src" / "sls" / "content" / "registry.json"
    helper = ROOT / "reference" / "original-game" / "decompiled" / "com" / "megacrit" / "cardcrawl" / "helpers" / "MonsterHelper.java"
    source_files = [registry, helper]
    for monster_id in sorted(covered_monsters if only_encounter is not None else expected_monsters):
        if monster_id not in monster_sources:
            raise RuntimeError(f"missing stock monster source: {monster_id}")
        source_files.append(monster_sources[monster_id])
    result: dict[str, Any] = {
        "schema": schema,
        "scope_sha256": scope_digest,
        "oracle_schema": "spirecomm-parity-v10",
        "seed": seed,
        "source_files": {
            path.relative_to(ROOT).as_posix(): source_sha256(path)
            for path in source_files
        },
        "entries": entries,
    }
    result["audit_sha256"] = canonical_digest(result)
    return result


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--seed", type=int, default=0)
    parser.add_argument("--fullrun", action="store_true")
    parser.add_argument("--only-encounter")
    parser.add_argument(
        "--output", type=Path,
        default=ROOT / "configs" / "validation" / "ironclad_a0_encounter_semantics.json",
    )
    args = parser.parse_args()
    payload = capture(
        args.seed, fullrun=args.fullrun, only_encounter=args.only_encounter,
    )
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(payload, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    return 0


if __name__ == "__main__":
    from sls.validation.runtime import write_completion
    try:
        code = main()
    except BaseException as error:
        write_completion(2, entry="encounter-audit", error=f"{type(error).__name__}: {error}", argv=sys.argv)
        raise
    else:
        write_completion(code, entry="encounter-audit")
        raise SystemExit(code)
