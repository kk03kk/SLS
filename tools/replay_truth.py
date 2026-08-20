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
from sls.contracts import Action
from sls.validation.compare import parity_differences
from sls.validation.diff import differences
from sls.validation.policies import action_ids
from sls.contracts.continuation import continuation_original, continuation_simulator
from sls.validation.truth import continuation_differences, difference_signature, load_bundle


PROFILES = {p.profile_id: p for p in (
    IRONCLAD_A0_ACT1, IRONCLAD_A0_ACT2, IRONCLAD_A0_ACT3, IRONCLAD_A0_HEART,
)}


def _checkpoint(path: Path) -> dict[str, Any]:
    with gzip.open(path, "rt", encoding="utf-8") as stream:
        return json.load(stream)


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
    if from_anchor:
        selected = next((a for a in anchors if a["anchor_id"] == from_anchor), None)
        if selected is None:
            raise ValueError(f"unknown anchor: {from_anchor}")
        from_step = max(from_step, int(selected["sequence"]))
    else:
        eligible = [a for a in anchors if int(a["sequence"]) <= from_step]
        selected = max(eligible, key=lambda a: int(a["sequence"]), default=None)
    simulator = SimulatorBackend(PROFILES[profile_id])
    start = 0
    if selected is not None:
        start = int(selected["sequence"])
        try:
            decision = simulator.load_checkpoint(_checkpoint(
                bundle / selected["path"] / "simulator-checkpoint.json.gz"
            ))
        except Exception:
            decision = simulator.reset(int(manifest["seed"]))
            start = 0
    else:
        decision = simulator.reset(int(manifest["seed"]))
    upper = len(boundaries) - 1 if to_step is None else min(to_step, len(boundaries) - 1)
    if window:
        upper = min(upper, from_step + window - 1)
    for sequence in range(start, upper + 1):
        boundary = boundaries[sequence]
        if int(boundary["sequence"]) != sequence:
            raise ValueError(f"missing or unordered boundary at {sequence}")
        original = adapt_original(boundary["raw_original_payload"]).decision
        observation_diff = differences(original.observation.to_dict(), decision.observation.to_dict())
        action_diff = differences(action_ids(original.actions), action_ids(decision.actions))
        state_diff = parity_differences(boundary["raw_original_payload"], simulator.raw_state)
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
        if sequence >= from_step and combined:
            cursor = boundary["cursor"]
            signature = difference_signature(
                evidence_class=manifest["evidence_class"], profile=profile_id,
                screen=cursor["screen"], act=int(cursor["act"]), floor=int(cursor["floor"]),
                category="paired-boundary", values=combined,
                preceding_action=None if sequence == 0 else (
                    (boundaries[sequence - 1].get("selected_action") or {}).get("kind")
                ),
            )
            first = sorted(combined)[0]
            rng_paths = [path for path in sorted(combined) if "rng" in path.lower()]
            return False, {
                "step": sequence, "signature": signature, "first_field_path": first,
                "values": combined[first], "first_rng_path": rng_paths[0] if rng_paths else None,
                "nearest_anchor": selected["anchor_id"] if selected else None,
                "difference_count": len(combined),
            }
        selected_action = boundary.get("selected_action")
        if sequence < upper:
            if not selected_action:
                raise ValueError(f"missing action before boundary {sequence + 1}")
            decision = simulator.step(Action.from_dict(selected_action)).decision
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
