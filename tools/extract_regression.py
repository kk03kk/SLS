"""Extract a stable minimal regression fixture from a truth bundle."""

from __future__ import annotations

import argparse
import gzip
import json
from pathlib import Path
import re
import sys


ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "src"))

from sls.backends.original.adapter import adapt_original
from sls.contracts.continuation import continuation_original
from sls.validation.compare import canonical_original, canonical_simulator
from sls.validation.evidence import original_evidence_gaps
from sls.validation.truth import load_bundle, value_hash, write_json_gz


def _simulator_at_step(
    bundle: Path, manifest: dict, boundaries: list[dict], step: int,
):
    """Restore the nearest compatible anchor, falling back to an earlier one."""

    from sls.backends.simulator import (
        IRONCLAD_A0_ACT1, IRONCLAD_A0_ACT2, IRONCLAD_A0_ACT3,
        IRONCLAD_A0_HEART, SimulatorBackend,
    )
    from sls.contracts import Action
    profiles = {p.profile_id: p for p in (
        IRONCLAD_A0_ACT1, IRONCLAD_A0_ACT2, IRONCLAD_A0_ACT3, IRONCLAD_A0_HEART,
    )}
    failures: list[str] = []
    anchors = sorted(
        (a for a in manifest.get("anchors", []) if int(a["sequence"]) <= step),
        key=lambda a: int(a["sequence"]), reverse=True,
    )
    for anchor in anchors:
        try:
            with gzip.open(
                bundle / anchor["path"] / "simulator-checkpoint.json.gz", "rt", encoding="utf-8",
            ) as stream:
                checkpoint = json.load(stream)
            simulator = SimulatorBackend(profiles[manifest["profile_id"]])
            decision = simulator.load_checkpoint(checkpoint)
            action_suffix = []
            for sequence in range(int(anchor["sequence"]), step):
                action = boundaries[sequence].get("selected_action")
                if not action:
                    raise ValueError(f"missing action at step {sequence}")
                action_suffix.append(action)
                decision = simulator.step(
                    Action.from_dict(action),
                    validation_evidence=boundaries[sequence].get("action_evidence") or {},
                ).decision
            return simulator, decision, anchor, checkpoint, action_suffix
        except (OSError, ValueError, KeyError, RuntimeError) as error:
            failures.append(f"{anchor['anchor_id']}: {error}")
    raise ValueError("no compatible native anchor: " + "; ".join(failures))


def extract(
    bundle: Path, *, step: int, issue: str, output_root: Path,
    target_backend: str = "auto",
) -> Path:
    manifest, boundaries = load_bundle(bundle)
    if step < 0 or step >= len(boundaries):
        raise ValueError(f"step out of range: {step}")
    if not re.fullmatch(r"[A-Za-z0-9_.-]+", issue):
        raise ValueError("issue must contain only letters, digits, dot, dash or underscore")
    boundary = boundaries[step]
    keys = sorted(boundary.get("differences") or {})
    game = boundary["raw_original_payload"].get("game_state") or {}
    expected_screen = (
        (boundary.get("canonical_simulator_decision") or {}).get("observation") or {}
    ).get("screen")
    continuation_evidence = continuation_original(boundary["raw_original_payload"])
    folded_ui_boundary = bool(continuation_evidence.get("ui_boundary_folded")) or (
        len(game.get("choice_list") or ()) == 1
        and str(game.get("screen_type") or "").upper() == "EVENT"
        and expected_screen == "MAP"
    )
    evidence_gaps = original_evidence_gaps(
        boundary["raw_original_payload"], canonical_screen=boundary["cursor"]["screen"],
    )
    category = (
        "evidence_gap" if evidence_gaps
        else "continuation" if folded_ui_boundary
        else "adapter" if any(key.startswith("observation:") or key.startswith("actions:") for key in keys)
        else "rng" if any("rng" in key.lower() for key in keys)
        else "continuation" if any("continuation" in key for key in keys)
        else "transition"
    )
    if category == "adapter" and target_backend == "simulator-adapter":
        category = "simulator_adapter"
    elif target_backend == "simulator-transition":
        category = "transition"
    elif target_backend == "original-adapter":
        category = "adapter"
    fixture: dict[str, object] = {
        "schema": "sls-minimal-regression-v1", "issue": issue, "category": category,
        "provenance": {
            "source_run_id": bundle.name, "source_step": step,
            "source_manifest_hash": value_hash(manifest),
            "evidence_class": manifest["evidence_class"],
        },
        "difference_signature": boundary.get("difference_signature"),
    }
    adapted = adapt_original(boundary["raw_original_payload"]).decision
    if category == "evidence_gap":
        fixture["instrumentation_request"] = {
            "source_run_id": bundle.name, "source_step": step,
            "oracle_schema": manifest.get("instrumentation", {}).get("schema"),
            "required_evidence": evidence_gaps,
            "nearest_anchor": max(
                (a for a in manifest.get("anchors", []) if int(a["sequence"]) <= step),
                key=lambda a: int(a["sequence"]), default=None,
            ),
        }
    elif category == "simulator_adapter":
        _, _, anchor, checkpoint, action_suffix = _simulator_at_step(
            bundle, manifest, boundaries, step,
        )
        fixture["simulator_checkpoint"] = checkpoint
        fixture["action_suffix"] = action_suffix
        fixture["action_evidence_suffix"] = [
            boundaries[index].get("action_evidence") or {}
            for index in range(int(anchor["sequence"]), step)
        ]
        fixture["restore_anchor"] = anchor["anchor_id"]
        fixture["profile_id"] = manifest["profile_id"]
        fixture["expected"] = {
            "observation": adapted.observation.to_dict(),
            "actions": [item.to_dict() for item in adapted.actions],
            "terminal": adapted.terminal,
        }
    elif category == "adapter":
        fixture["raw_original_payload"] = boundary["raw_original_payload"]
        expected = boundary.get("canonical_simulator_decision")
        if expected is None:
            _, decision, _, _, _ = _simulator_at_step(bundle, manifest, boundaries, step)
            expected = {
                "observation": decision.observation.to_dict(),
                "actions": [action.to_dict() for action in decision.actions],
                "terminal": decision.terminal,
            }
        fixture["expected"] = expected
        expected_paths = {}
        unresolved = []
        parity_run = (
            boundary["raw_original_payload"].get("_parity_run")
            or (boundary["raw_original_payload"].get("game_state") or {}).get("_parity_run")
            or {}
        )
        for path, pair in (boundary.get("differences") or {}).items():
            if not path.startswith("observation:") and not path.startswith("actions:"):
                continue
            if pair[1] == "BURNING_ELITE" and "burning_elite_x" not in parity_run:
                unresolved.append(path)
                continue
            if (
                path.endswith(".reachable") and pair[1] is True
                and int((boundary["raw_original_payload"].get("game_state") or {}).get("floor", 0) or 0) > 0
                and "current_map_x" not in parity_run
            ):
                unresolved.append(path)
                continue
            expected_paths[path] = pair[1]
        fixture["expected_paths"] = expected_paths
        fixture["unresolved_evidence_paths"] = unresolved
    elif category == "rng":
        if step == 0:
            raise ValueError("RNG fixture requires the preceding action boundary")
        previous = boundaries[step - 1]
        action = previous.get("selected_action")
        if not action:
            raise ValueError("RNG fixture requires a preceding selected action")
        before_simulator, _, anchor, checkpoint, action_suffix = _simulator_at_step(
            bundle, manifest, boundaries, step - 1,
        )
        before_simulator_rng = canonical_simulator(before_simulator.raw_state).get("rng", {})
        action_evidence = previous.get("action_evidence") or {}
        choice = (before_simulator.raw_state.get("public_combat") or {}).get("choice") or {}
        if not action_evidence and choice.get("task") == "DISCOVERY":
            internal_choice = (
                (before_simulator.raw_state.get("combat_checkpoint") or {})
                .get("game_state", {}).get("combat_state", {})
                .get("_internal", {}).get("choice", {})
            )
            updates = internal_choice.get("discovery_retrieval_updates")
            if updates is not None:
                action_evidence = {"discovery_retrieval_updates": int(updates)}
        after_simulator, _, _, _, _ = _simulator_at_step(
            bundle, manifest, boundaries, step,
        )
        after_simulator_rng = canonical_simulator(after_simulator.raw_state).get("rng", {})
        before_original_rng = previous.get("rng") or {}
        after_original_rng = boundary.get("rng") or {}
        deltas = {}
        for stream in sorted(
            set(before_original_rng) | set(after_original_rng)
            | set(before_simulator_rng) | set(after_simulator_rng)
        ):
            def delta(before: dict, after: dict):
                if stream not in before or stream not in after:
                    return None
                return int(after[stream]["counter"]) - int(before[stream]["counter"])
            deltas[stream] = {
                "original": delta(before_original_rng, after_original_rng),
                "simulator": delta(before_simulator_rng, after_simulator_rng),
            }
        fixture.update({
            "profile_id": manifest["profile_id"],
            "restore_anchor": anchor["anchor_id"],
            "simulator_checkpoint": checkpoint,
            "action_suffix": action_suffix,
            "action_evidence_suffix": [
                boundaries[index].get("action_evidence") or {}
                for index in range(int(anchor["sequence"]), step - 1)
            ],
            "before": {"original": before_original_rng, "simulator": before_simulator_rng},
            "action": action,
            "action_evidence": action_evidence,
            "after": {"original": after_original_rng, "simulator": after_simulator_rng},
            "counter_delta": deltas,
        })
    elif category == "transition":
        # Comparisons describe the state at this boundary, so a recorded
        # difference was caused by the selected action on the preceding one.
        # Retain the older pre-action form for explicitly targeted matching
        # boundaries used by existing rule probes.
        difference_boundary = bool(boundary.get("differences"))
        action_step = step - 1 if difference_boundary else step
        after_step = step if difference_boundary else step + 1
        if action_step < 0 or after_step >= len(boundaries):
            raise ValueError("transition fixture requires a preceding action and result boundary")
        from sls.contracts import Action
        _, _, anchor, checkpoint, action_suffix = _simulator_at_step(
            bundle, manifest, boundaries, action_step,
        )
        action_boundary = boundaries[action_step]
        action = action_boundary.get("selected_action")
        if not action:
            raise ValueError("transition fixture requires a selected action")
        fixture["simulator_checkpoint"] = checkpoint
        fixture["action_suffix"] = action_suffix
        fixture["action_evidence_suffix"] = [
            boundaries[index].get("action_evidence") or {}
            for index in range(int(anchor["sequence"]), action_step)
        ]
        fixture["restore_anchor"] = anchor["anchor_id"]
        fixture["action"] = action
        fixture["action_evidence"] = action_boundary.get("action_evidence") or {}
        after = boundaries[after_step]
        after_decision = adapt_original(after["raw_original_payload"]).decision
        after_canonical = canonical_original(after["raw_original_payload"])
        if manifest.get("evidence_class") == "RESUMED_AUTOSAVE":
            after_canonical.get("rng", {}).pop("neow", None)
        fixture["expected"] = {
            "canonical_public_state": after_canonical,
            "canonical_decision": {
                "observation": after_decision.observation.to_dict(),
                "actions": [item.to_dict() for item in after_decision.actions],
                "terminal": after_decision.terminal,
            },
            "rng": after_canonical.get("rng", {}),
            "continuation": continuation_original(after["raw_original_payload"]),
        }
    else:
        anchors = [a for a in manifest.get("anchors", []) if int(a["sequence"]) <= step]
        anchor = max(anchors, key=lambda a: int(a["sequence"]), default=None)
        if anchor is None:
            raise ValueError("transition fixture requires an anchor")
        with gzip.open(bundle / anchor["path"] / "simulator-checkpoint.json.gz", "rt", encoding="utf-8") as stream:
            fixture["simulator_checkpoint"] = json.load(stream)
        start = int(anchor["sequence"])
        fixture["anchor"] = anchor
        fixture["action_suffix"] = [b.get("selected_action") for b in boundaries[start:step + 1]]
        fixture["action_evidence_suffix"] = [
            b.get("action_evidence") or {} for b in boundaries[start:step + 1]
        ]
        fixture["expected"] = {
            "canonical_public_state": boundary["canonical_public_state"],
            "rng": boundary.get("rng") or {},
            "continuation": boundary.get("continuation"),
        }
        if category == "continuation" and boundary["cursor"]["screen"] == "COMBAT":
            fixture["scenario_request"] = {
                "source_run_id": bundle.name, "source_step": step,
                "registered_only": True, "setup_digest": value_hash(boundary.get("continuation")),
            }
    suffix = ".instrumentation-request.json.gz" if category == "evidence_gap" else ".json.gz"
    target = output_root / f"{issue}{suffix}"
    write_json_gz(target, fixture)
    return target


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("bundle", type=Path)
    parser.add_argument("--step", type=int, required=True)
    parser.add_argument("--issue", required=True)
    parser.add_argument("--output-root", type=Path, default=ROOT / "tests" / "fixtures" / "regressions")
    parser.add_argument(
        "--target", choices=(
            "auto", "original-adapter", "simulator-adapter", "simulator-transition",
        ), default="auto",
    )
    args = parser.parse_args()
    try:
        target = extract(
            args.bundle, step=args.step, issue=args.issue, output_root=args.output_root,
            target_backend=args.target,
        )
    except (OSError, ValueError, KeyError, json.JSONDecodeError) as error:
        print(f"invalid truth bundle: {error}", file=sys.stderr)
        return 2
    print(target)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
