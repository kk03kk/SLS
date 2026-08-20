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
from sls.validation.compare import canonical_original
from sls.validation.evidence import original_evidence_gaps
from sls.validation.truth import load_bundle, value_hash, write_json_gz


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
    folded_ui_boundary = (
        int(game.get("floor", 0) or 0) == 0
        and len(game.get("choice_list") or ()) == 1
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
        anchors = [a for a in manifest.get("anchors", []) if int(a["sequence"]) <= step]
        anchor = max(anchors, key=lambda a: int(a["sequence"]), default=None)
        if anchor is None:
            raise ValueError("simulator adapter fixture requires a native anchor")
        from sls.backends.simulator import (
            IRONCLAD_A0_ACT1, IRONCLAD_A0_ACT2, IRONCLAD_A0_ACT3,
            IRONCLAD_A0_HEART, SimulatorBackend,
        )
        from sls.contracts import Action
        profiles = {p.profile_id: p for p in (
            IRONCLAD_A0_ACT1, IRONCLAD_A0_ACT2, IRONCLAD_A0_ACT3, IRONCLAD_A0_HEART,
        )}
        with gzip.open(bundle / anchor["path"] / "simulator-checkpoint.json.gz", "rt", encoding="utf-8") as stream:
            checkpoint = json.load(stream)
        simulator = SimulatorBackend(profiles[manifest["profile_id"]])
        simulator.load_checkpoint(checkpoint)
        for sequence in range(int(anchor["sequence"]), step):
            previous_action = boundaries[sequence].get("selected_action")
            if not previous_action:
                raise ValueError(f"missing action at step {sequence}")
            simulator.step(Action.from_dict(previous_action))
        fixture["simulator_checkpoint"] = simulator.checkpoint()
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
            anchors = [a for a in manifest.get("anchors", []) if int(a["sequence"]) <= step]
            anchor = max(anchors, key=lambda a: int(a["sequence"]), default=None)
            if anchor is None:
                raise ValueError("adapter fixture requires a native anchor")
            from sls.backends.simulator import (
                IRONCLAD_A0_ACT1, IRONCLAD_A0_ACT2, IRONCLAD_A0_ACT3,
                IRONCLAD_A0_HEART, SimulatorBackend,
            )
            from sls.contracts import Action
            profiles = {p.profile_id: p for p in (
                IRONCLAD_A0_ACT1, IRONCLAD_A0_ACT2, IRONCLAD_A0_ACT3, IRONCLAD_A0_HEART,
            )}
            with gzip.open(bundle / anchor["path"] / "simulator-checkpoint.json.gz", "rt", encoding="utf-8") as stream:
                checkpoint = json.load(stream)
            simulator = SimulatorBackend(profiles[manifest["profile_id"]])
            decision = simulator.load_checkpoint(checkpoint)
            for sequence in range(int(anchor["sequence"]), step):
                action = boundaries[sequence].get("selected_action")
                if not action:
                    raise ValueError(f"missing action at step {sequence}")
                decision = simulator.step(Action.from_dict(action)).decision
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
        fixture["before"] = boundary.get("rng") or {}
        fixture["action"] = boundary.get("selected_action")
        fixture["after"] = boundaries[step + 1].get("rng") if step + 1 < len(boundaries) else None
    elif category == "transition":
        anchors = [a for a in manifest.get("anchors", []) if int(a["sequence"]) <= step]
        anchor = max(anchors, key=lambda a: int(a["sequence"]), default=None)
        if anchor is None or step + 1 >= len(boundaries):
            raise ValueError("transition fixture requires an anchor and next boundary")
        from sls.backends.simulator import (
            IRONCLAD_A0_ACT1, IRONCLAD_A0_ACT2, IRONCLAD_A0_ACT3,
            IRONCLAD_A0_HEART, SimulatorBackend,
        )
        from sls.contracts import Action
        profiles = {p.profile_id: p for p in (
            IRONCLAD_A0_ACT1, IRONCLAD_A0_ACT2, IRONCLAD_A0_ACT3, IRONCLAD_A0_HEART,
        )}
        with gzip.open(bundle / anchor["path"] / "simulator-checkpoint.json.gz", "rt", encoding="utf-8") as stream:
            checkpoint = json.load(stream)
        simulator = SimulatorBackend(profiles[manifest["profile_id"]])
        simulator.load_checkpoint(checkpoint)
        for sequence in range(int(anchor["sequence"]), step):
            previous_action = boundaries[sequence].get("selected_action")
            if not previous_action:
                raise ValueError(f"missing action at step {sequence}")
            simulator.step(Action.from_dict(previous_action))
        action = boundary.get("selected_action")
        if not action:
            raise ValueError("transition fixture requires a selected action")
        fixture["simulator_checkpoint"] = simulator.checkpoint()
        fixture["action"] = action
        after = boundaries[step + 1]
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
        "--target", choices=("auto", "original-adapter", "simulator-adapter"), default="auto",
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
