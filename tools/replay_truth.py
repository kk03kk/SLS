"""Offline replay of an immutable Original truth bundle."""

from __future__ import annotations

import argparse
import gzip
import json
from pathlib import Path
import sys
from typing import Any


ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "src"))

from sls.backends.original.adapter import adapt_original
from sls.backends.simulator import (
    IRONCLAD_A0_ACT1, IRONCLAD_A0_ACT2, IRONCLAD_A0_ACT3, IRONCLAD_A0_HEART,
    SimulatorBackend,
)
from sls.contracts import Action, Decision
from sls.curriculum import completed_act_between, evaluate_horizon
from sls.validation.compare import canonical_original, canonical_simulator, parity_differences
from sls.validation.diff import differences
from sls.validation.policies import action_ids
from sls.contracts.continuation import continuation_original, continuation_simulator
from sls.validation.truth import continuation_differences, file_hash, load_bundle, value_hash
from sls.validation.evidence import comparison_result, original_evidence_gaps


PROFILES = {p.profile_id: p for p in (
    IRONCLAD_A0_ACT1, IRONCLAD_A0_ACT2, IRONCLAD_A0_ACT3, IRONCLAD_A0_HEART,
)}


def _apply_profile_horizon(profile: Any, previous: Decision | None, current: Decision) -> Decision:
    """Recreate the backend-level terminal wrapper around a raw Original payload."""

    if previous is None:
        return current
    horizon = evaluate_horizon(
        profile,
        current.observation,
        act_completed=completed_act_between(previous.observation, current.observation),
    )
    if horizon.terminated == current.terminal:
        return current
    return Decision(
        current.observation,
        () if horizon.terminated else current.actions,
        horizon.terminated,
    )


def _checkpoint(path: Path) -> dict[str, Any]:
    with gzip.open(path, "rt", encoding="utf-8") as stream:
        return json.load(stream)


def _rng_report(
    original: dict[str, Any], simulator: dict[str, Any],
    previous_original: dict[str, Any] | None, previous_simulator: dict[str, Any] | None,
    *, drop_dead_neow: bool = False,
) -> dict[str, Any] | None:
    left = canonical_original(original).get("rng", {})
    right = canonical_simulator(simulator).get("rng", {})
    if drop_dead_neow:
        left.pop("neow", None)
        right.pop("neow", None)
        continuation = original.get("_continuation") or (original.get("game_state") or {}).get("_continuation") or {}
        if continuation.get("post_combat"):
            for stream in ("monster_hp", "ai", "shuffle", "card_random", "misc"):
                left.pop(stream, None)
                right.pop(stream, None)
    streams = []
    for name in sorted(set(left) | set(right)):
        if left.get(name) == right.get(name):
            continue
        before_left = (previous_original or {}).get(name)
        before_right = (previous_simulator or {}).get(name)
        streams.append({
            "stream": name, "original": left.get(name), "simulator": right.get(name),
            "original_counter_delta": None if before_left is None or left.get(name) is None else (
                left[name]["counter"] - before_left["counter"]
            ),
            "simulator_counter_delta": None if before_right is None or right.get(name) is None else (
                right[name]["counter"] - before_right["counter"]
            ),
            "kind": "COUNTER" if left.get(name, {}).get("counter") != right.get(name, {}).get("counter") else "STATE",
        })
    return {"streams": streams} if streams else None


def replay(
    bundle: Path, *, from_anchor: str | None = None, from_step: int = 0,
    to_step: int | None = None, window: int = 0,
) -> tuple[bool, dict[str, Any] | None]:
    manifest, boundaries = load_bundle(bundle)
    if not boundaries:
        raise ValueError("truth bundle has no boundaries")
    profile_id = manifest["profile_id"]
    if profile_id not in PROFILES:
        raise ValueError(f"unknown profile: {profile_id}")
    anchors = manifest.get("anchors") or []
    requested_anchor = None
    if from_anchor:
        requested_anchor = next((a for a in anchors if a["anchor_id"] == from_anchor), None)
        if requested_anchor is None:
            raise ValueError(f"unknown anchor: {from_anchor}")
        from_step = max(from_step, int(requested_anchor["sequence"]))
    eligible = sorted(
        (a for a in anchors if int(a["sequence"]) <= from_step),
        key=lambda a: int(a["sequence"]), reverse=True,
    )
    if requested_anchor is not None:
        requested_sequence = int(requested_anchor["sequence"])
        eligible = [a for a in eligible if int(a["sequence"]) <= requested_sequence]
    restore_mode = "ACTION_HISTORY"
    restore_anchor = None
    restore_failures: list[str] = []
    start = 0
    decision = None
    simulator = None
    for selected in eligible:
        try:
            metadata_path = bundle / selected["path"] / "metadata.json"
            metadata = json.loads(metadata_path.read_text(encoding="utf-8"))
            producer = metadata.get("checkpoint_producer") or {}
            producer_abi = producer.get("abi") or producer.get("python_abi")
            if producer_abi and producer_abi != sys.implementation.cache_tag:
                raise ValueError("native checkpoint ABI is incompatible")
            checkpoint = _checkpoint(bundle / selected["path"] / "simulator-checkpoint.json.gz")
            expected_checkpoint_hash = metadata.get("checkpoint_state_hash")
            if expected_checkpoint_hash and value_hash(checkpoint) != expected_checkpoint_hash:
                raise ValueError("native checkpoint state hash mismatch")
            candidate = SimulatorBackend(PROFILES[profile_id])
            decision = candidate.load_checkpoint(checkpoint)
            simulator = candidate
            start = int(selected["sequence"])
            restore_mode = "EXACT_CHECKPOINT"
            restore_anchor = selected["anchor_id"]
            break
        except (OSError, ValueError, KeyError, RuntimeError, json.JSONDecodeError) as error:
            restore_failures.append(f"{selected['anchor_id']}: {error}")
    if simulator is None:
        if manifest.get("evidence_class") == "RESUMED_AUTOSAVE":
            detail = "; ".join(restore_failures) or "bundle contains no native anchor"
            raise ValueError(
                "derived resume bundle has no compatible checkpoint and cannot be rebuilt "
                f"from the seed-local action history: {detail}"
            )
        simulator = SimulatorBackend(PROFILES[profile_id])
        decision = simulator.reset(int(manifest["seed"]))
    upper = len(boundaries) - 1 if to_step is None else min(to_step, len(boundaries) - 1)
    if window:
        upper = min(upper, from_step + window - 1)
    previous_original_rng = None
    previous_simulator_rng = None
    previous_original_decision = (
        adapt_original(boundaries[start - 1]["raw_original_payload"]).decision
        if start > 0 else None
    )
    for sequence in range(start, upper + 1):
        boundary = boundaries[sequence]
        if int(boundary["sequence"]) != sequence:
            raise ValueError(f"missing or unordered boundary at {sequence}")
        original = _apply_profile_horizon(
            PROFILES[profile_id], previous_original_decision,
            adapt_original(boundary["raw_original_payload"]).decision,
        )
        observation_diff = differences(original.observation.to_dict(), decision.observation.to_dict())
        action_diff = differences(action_ids(original.actions), action_ids(decision.actions))
        state_diff = parity_differences(
            boundary["raw_original_payload"], simulator.raw_state,
            drop_dead_neow=manifest.get("evidence_class") == "RESUMED_AUTOSAVE",
        )
        continuation_diff = continuation_differences(
            continuation_original(boundary["raw_original_payload"]),
            continuation_simulator(simulator.raw_state),
        )
        combined = {
            **{f"observation:{k}": v for k, v in observation_diff.items()},
            **{f"actions:{k}": v for k, v in action_diff.items()},
            **{f"state:{k}": v for k, v in state_diff.items()},
            **{f"continuation:{k}": v for k, v in continuation_diff.items()},
        }
        gaps = original_evidence_gaps(
            boundary["raw_original_payload"], canonical_screen=boundary["cursor"]["screen"],
        )
        cursor = boundary["cursor"]
        comparison = comparison_result(
            evidence_class=manifest["evidence_class"], profile=profile_id,
            screen=cursor["screen"], act=int(cursor["act"]), floor=int(cursor["floor"]),
            differences=combined, evidence_gaps=gaps,
            preceding_action=None if sequence == 0 else (
                (boundaries[sequence - 1].get("selected_action") or {}).get("kind")
            ), occurrence_signature=None,
        )
        if sequence >= from_step and comparison["status"] != "MATCH":
            report_values = (
                {f"evidence:{item['path']}": [None, item["code"]] for item in gaps}
                if gaps else combined
            )
            first = sorted(report_values)[0]
            rng_paths = [path for path in sorted(report_values) if "rng" in path.lower()]
            nearest = max(
                (a for a in anchors if int(a["sequence"]) <= sequence),
                key=lambda a: int(a["sequence"]), default=None,
            )
            return False, {
                "step": sequence, "status": comparison["status"],
                "category": comparison["category"],
                "signature": comparison["occurrence_signature"],
                "cluster_key": comparison["cluster_key"], "first_field_path": first,
                "values": report_values[first], "evidence_gaps": gaps,
                "first_rng_path": rng_paths[0] if rng_paths else None,
                "nearest_anchor": nearest["anchor_id"] if nearest else None,
                "restore_mode": restore_mode, "restore_anchor": restore_anchor,
                "restore_failures": restore_failures,
                "difference_count": len(report_values),
                "rng_divergence": _rng_report(
                    boundary["raw_original_payload"], simulator.raw_state,
                    previous_original_rng, previous_simulator_rng,
                    drop_dead_neow=manifest.get("evidence_class") == "RESUMED_AUTOSAVE",
                ),
            }
        current_original_rng = canonical_original(boundary["raw_original_payload"]).get("rng", {})
        current_simulator_rng = canonical_simulator(simulator.raw_state).get("rng", {})
        previous_original_rng = current_original_rng
        previous_simulator_rng = current_simulator_rng
        previous_original_decision = original
        selected_action = boundary.get("selected_action")
        if sequence < upper:
            if not selected_action:
                raise ValueError(f"missing action before boundary {sequence + 1}")
            decision = simulator.step(
                Action.from_dict(selected_action),
                validation_evidence=boundary.get("action_evidence") or {},
            ).decision
    return True, None


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("bundle", type=Path)
    parser.add_argument("--from-anchor")
    parser.add_argument("--from-step", type=int, default=0)
    parser.add_argument("--to-step", type=int)
    parser.add_argument("--window", type=int, default=0)
    args = parser.parse_args()
    try:
        matched, detail = replay(
            args.bundle, from_anchor=args.from_anchor, from_step=args.from_step,
            to_step=args.to_step, window=args.window,
        )
    except (OSError, ValueError, KeyError, json.JSONDecodeError) as error:
        print(json.dumps({"valid": False, "error": str(error)}, ensure_ascii=False), file=sys.stderr)
        return 2
    print(json.dumps({"valid": True, "matches": matched, "difference": detail}, ensure_ascii=False, indent=2))
    return 0 if matched else 1


if __name__ == "__main__":
    raise SystemExit(main())
