"""Evidence-aware final acceptance over immutable LIVE_FULLRUN bundles."""

from __future__ import annotations

import argparse
import json
from pathlib import Path
import sys
import tomllib


ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "src"))

from sls.validation.truth import load_bundle


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("root", type=Path, nargs="?", default=ROOT / "validation-results" / "truth")
    parser.add_argument("--config", type=Path, default=ROOT / "configs" / "validation" / "full_run.toml")
    parser.add_argument("--output", type=Path)
    args = parser.parse_args()
    with args.config.open("rb") as stream:
        config = tomllib.load(stream)
    acceptance = config.get("acceptance") or {}
    required_screens = set(map(str, acceptance.get("require_screens", ())))
    required_candidates = set(map(str, acceptance.get("require_candidate_actions", ())))
    required_selected = set(map(str, acceptance.get("require_selected_actions", required_candidates)))
    eligible = []
    rejected = []
    for path in sorted(p.parent for p in args.root.rglob("manifest.json")):
        try:
            manifest, boundaries = load_bundle(path)
        except Exception as error:
            rejected.append({"bundle": path.name, "reason": f"INVALID: {error}"})
            continue
        reasons = []
        if manifest.get("evidence_class") != "LIVE_FULLRUN": reasons.append("NOT_LIVE_FULLRUN")
        if manifest.get("capture_mode", "PAIRED") != "PAIRED": reasons.append("NOT_PAIRED")
        if not manifest.get("acceptance_eligible", False): reasons.append("NOT_ELIGIBLE")
        if (manifest.get("git") or {}).get("dirty", True): reasons.append("DIRTY_GIT")
        if not manifest.get("complete"): reasons.append("INCOMPLETE")
        if set((manifest.get("jars") or {})) != {"game", "ModTheSpire", "BaseMod", "CommunicationMod", "Oracle"}:
            reasons.append("INCOMPLETE_JAR_HASHES")
        if not (manifest.get("python") or {}).get("executable"): reasons.append("MISSING_PYTHON_IDENTITY")
        if not (manifest.get("native_build") or {}).get("sha256"): reasons.append("MISSING_NATIVE_BUILD_HASH")
        if not (manifest.get("policy") or {}).get("sha256"): reasons.append("MISSING_POLICY_HASH")
        if set((manifest.get("code") or {})) != {"adapter", "canonicalizer"}:
            reasons.append("MISSING_CODE_HASHES")
        if (manifest.get("instrumentation") or {}).get("observed_schemas") != ["spirecomm-parity-v5"]:
            reasons.append("INSTRUMENTATION_SCHEMA_MISMATCH")
        statuses = [
            (boundary.get("comparison") or {}).get(
                "status", "DIFFERENCE" if boundary.get("differences") else "MATCH"
            ) for boundary in boundaries
        ]
        if any(status != "MATCH" for status in statuses): reasons.append("NON_MATCH_BOUNDARY")
        if reasons:
            rejected.append({"bundle": path.name, "reason": reasons})
        else:
            eligible.append((manifest, boundaries, path))
    screens, candidates, selected = set(), set(), set()
    heart_victory = False
    seeds = set()
    for manifest, boundaries, _ in eligible:
        seeds.add(int(manifest["seed"]))
        for boundary in boundaries:
            screens.add(str(boundary["cursor"]["screen"]))
            candidates.update(str(item["kind"]) for item in boundary.get("candidates") or ())
            action = boundary.get("selected_action")
            if action: selected.add(str(action["kind"]))
            run = (boundary.get("canonical_public_state") or {}).get("run") or {}
            keys = run.get("keys") or (False, False, False)
            if int(run.get("act", 0)) == 4 and all(keys) and boundary.get("terminal_kind") == "VICTORY":
                heart_victory = True
    required_runs = int(acceptance.get("require_complete_runs", len(config.get("seeds", ()))))
    failures = []
    if len(seeds) < required_runs: failures.append("COMPLETE_RUN_COUNT")
    if int(acceptance.get("require_max_act", 1)) >= 4 and not heart_victory: failures.append("NO_THREE_KEY_ACT4_VICTORY")
    if not required_screens.issubset(screens): failures.append("MISSING_SCREENS")
    if not required_candidates.issubset(candidates): failures.append("MISSING_CANDIDATE_ACTIONS")
    if not required_selected.issubset(selected): failures.append("MISSING_SELECTED_ACTIONS")
    report = {
        "schema": "sls-live-fullrun-acceptance-v1", "accepted": not failures,
        "eligible_runs": len(eligible), "seeds": sorted(seeds), "heart_victory": heart_victory,
        "coverage": {"screens": sorted(screens), "candidate_actions": sorted(candidates),
                     "selected_actions": sorted(selected)},
        "failures": failures, "rejected": rejected,
    }
    text = json.dumps(report, ensure_ascii=False, indent=2, sort_keys=True) + "\n"
    if args.output:
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(text, encoding="utf-8")
    else:
        print(text, end="")
    return 0 if report["accepted"] else 1


if __name__ == "__main__":
    raise SystemExit(main())
