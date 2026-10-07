"""Write compact, provenance-bound progress without upgrading scope to full parity."""

from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path

from sls.rl.training_contract import native_source_digest


def summarize(production: list[Path], controlled: Path | None = None) -> dict:
    source = native_source_digest()
    result = {"schema": "sls-fullrun-parity-progress-v1", "native_source_sha256": source,
              "qualification": False, "final_holdout_used": False,
              "production_behavior_changed": False, "production": [], "controlled": [],
              "remaining": ["AUTOMATON_FIRST_DIVERGENCE_ROOT_CAUSE",
                            "ACT3_ACT4_REAL_DUNGEON_CONTEXT", "INDEPENDENT_CARD_FIELDS",
                            "ACT3_ALL_ENCOUNTERS", "DOUBLE_BOSS_AND_KEYS",
                            "SHIELD_SPEAR_HEART_CONTROLLED", "CONTENT_BRANCHES",
                            "LATE_ACT_NORMAL_START_TRAJECTORIES"]}
    for path in production:
        data = json.loads(path.read_text(encoding="utf-8"))
        if (data["native_source_sha256"] != source or not data.get("batch_sha256")
                or not data.get("launch_evidence_sha256")):
            raise ValueError("stale or unbound production evidence")
        divergence = data.get("first_divergence")
        result["production"].append({
            "seed": data["seed"], "model_sha256": data["policy"]["model_sha256"],
            "evaluation_environment": data["evaluation_environment"],
            "report_sha256": hashlib.sha256(path.read_bytes()).hexdigest(),
            "stock_capture_sha256": data["trajectory_sha256"],
            "batch_sha256": data["batch_sha256"],
            "launch_sha256": data["launch_evidence_sha256"],
            "status": data["status"], "boundaries_checked": data["boundaries_checked"],
            "first_divergence": ({key: divergence[key] for key in
                                  ("boundary", "screen", "act", "floor", "differences")
                                  if key in divergence} if divergence else None),
            "scope": "ONE_NORMAL_START_PUBLIC_TRAJECTORY_NOT_ENCOUNTER_CERTIFICATE",
        })
    if controlled:
        data = json.loads(controlled.read_text(encoding="utf-8"))
        if data["native_source_sha256"] != source:
            raise ValueError("stale controlled evidence")
        result["controlled_report_sha256"] = hashlib.sha256(controlled.read_bytes()).hexdigest()
        result["controlled"] = [{key: row[key] for key in
                                 ("scene_id", "seed", "status", "first_divergence")}
                                for row in data["runs"]]
    result["training_review_required"] = any(row["first_divergence"] for row in result["production"]) or any(
        row["first_divergence"] for row in result["controlled"])
    return result


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--production", type=Path, nargs="+", required=True)
    parser.add_argument("--controlled", type=Path)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    data = summarize(args.production, args.controlled)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    with args.output.open("x", encoding="utf-8") as stream:
        json.dump(data, stream, indent=2)
        stream.write("\n")
    print("Full-run certification: false; review required:", data["training_review_required"])
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
