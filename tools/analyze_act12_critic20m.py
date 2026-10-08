"""Recompute the registered endpoint comparison without loading a model."""
from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))
sys.path.insert(0, str(ROOT / "src"))

from sls.rl.execution_health import execution_healthy, finite_payload
from sls.rl.training_contract import sha256_file, source_sha256
from tools.act12_critic20m_contract import CONFIRMATION, PERIODIC, safe_path, validate
from tools.analyze_act12_pilot import paired_act12
from tools.analyze_reward_screen import outcomes, read


def verify_bundle(run):
    files = read(run / "training-bundle.json")["files"]
    required = {"training-config.toml", "run-manifest.json", "final.pt", "latest.pt",
                "reference-evaluation.json", "endpoint-evaluation.json", "final-evaluation.json",
                "stages/train/metrics.jsonl", "stages/train/selection/best_progress.json",
                "stages/train/selection/best_progress.pt"}
    if not required <= files.keys():
        raise ValueError("training bundle lacks required evidence")
    missing_exports = {}
    for name, expected in files.items():
        path = safe_path(run, name)
        if name == run.name + ".pt" and not path.exists():
            missing_exports[name] = expected
            continue
        if sha256_file(path) != expected:
            raise ValueError("training bundle hash mismatch: " + name)
    return files, missing_exports


def decision(pair):
    return ("CONFIRMED_JOINT_GAIN" if pair["net"] / pair["paired_seeds"] >= .01
            and pair["exact_mcnemar_p"] <= .05 else "NO_CONFIRMED_GAIN")


def analyze(plan_path):
    plan, config_path, config = validate(plan_path)
    run = safe_path(ROOT, config["run"]["output"])
    files, missing = verify_bundle(run)
    manifest = read(run / "run-manifest.json")
    target = config["stages"]["train"]["target_environment_steps"]
    if (manifest.get("status") != "COMPLETE" or manifest["stages"]["train"].get("status") != "COMPLETE"
            or not target <= manifest["environment_steps"] < target + 64 * 256
            or source_sha256(run / "training-config.toml") != source_sha256(config_path)
            or manifest["config_sha256"] != plan["config_sha256"]
            or manifest["native_source_sha256"] != plan["native_source_sha256"]
            or manifest["training_implementation_sha256"] != plan["training_implementation_sha256"]):
        raise ValueError("incomplete budget or mismatched training identity")
    warmup = manifest["critic_warmup_state"]
    if warmup["phase"] != "PPO" or warmup["completed_updates"] != 32:
        raise ValueError("critic warmup not completed exactly once")
    reference, endpoint = read(run / "reference-evaluation.json"), read(run / "endpoint-evaluation.json")
    if (tuple(reference["seeds"]) != CONFIRMATION
            or reference["checkpoint_sha256"] != plan["parent"]["sha256"]
            or reference.get("source_profile") != "IRONCLAD_A20_ACT1"
            or reference.get("evaluation_profile") != "IRONCLAD_A20_ACT2"
            or endpoint["checkpoint_sha256"] != files["final.pt"]
            or endpoint["checkpoint_environment_steps"] != manifest["environment_steps"]
            or endpoint["simulator"]["native_source_sha256"] != plan["native_source_sha256"]):
        raise ValueError("fixed endpoint or frozen parent identity mismatch")
    metrics = [json.loads(line) for line in (run / "stages/train/metrics.jsonl").read_text().splitlines()]
    updates = [r for r in metrics if "update_seconds" in r]
    if (len(updates) != manifest["updates"]
            or [r["update"] for r in updates] != list(range(1, len(updates) + 1))
            or any(r["environment_steps"] != plan["parent"]["environment_steps"] + r["update"] * 16384
                   for r in updates)
            or [r.get("critic_warmup_active", 0) for r in updates] != [1.] * 32 + [0.] * (len(updates) - 32)):
        raise ValueError("missing/duplicate updates, wrong budget or repeated warmup")
    curve = []
    for row in metrics:
        if not finite_payload(row) or row.get("terminations_backend_truncated", 0):
            raise ValueError("training execution health failed")
        if "evaluation" in row:
            if not execution_healthy(row["evaluation"]):
                raise ValueError("periodic evaluation execution health failed")
            curve.append({"steps": row["environment_steps"],
                          **outcomes(row["evaluation"], PERIODIC, horizon=2)})
    selected = read(run / "stages/train/selection/best_progress.json")
    final = read(run / "final-evaluation.json")
    if (selected["checkpoint_sha256"] != files["stages/train/selection/best_progress.pt"]
            or final["checkpoint_sha256"] != selected["checkpoint_sha256"]
            or final["checkpoint_environment_steps"] != selected["environment_steps"]
            or tuple(final["seeds"]) != CONFIRMATION
            or final["simulator"] != endpoint["simulator"] or final["runtime"] != endpoint["runtime"]
            or not execution_healthy(final["result"])):
        raise ValueError("selected checkpoint identity or execution health failed")
    selected_confirmation = outcomes(final["result"], CONFIRMATION, horizon=2)
    peak = max((r["wins"] for r in curve), default=None)
    earliest = next((r["steps"] for r in curve if r["wins"] == peak), None)
    if selected["successes"] != peak or selected["environment_steps"] != earliest:
        raise ValueError("selected checkpoint does not match earliest maximum joint count")
    if not execution_healthy(reference["result"]) or not execution_healthy(endpoint["result"]):
        raise ValueError("confirmation execution health failed")
    pair = paired_act12(reference, endpoint, labels=("frozen90", "critic20m-fixed-endpoint"))
    return {"schema": "sls-critic20m-analysis-v1", "decision": decision(pair),
            "paired_fixed_endpoint": pair, "parent": outcomes(reference["result"], CONFIRMATION, horizon=2),
            "endpoint": outcomes(endpoint["result"], CONFIRMATION, horizon=2),
            "fixed_endpoint_sha256": files["final.pt"], "selected_checkpoint": selected,
            "selected_development_confirmation": selected_confirmation,
            "periodic_peak_count": peak,
            "periodic_curve": curve, "warmup": warmup, "verified_files": files,
            "deployment_export_present": (run / (run.name + ".pt")).exists(), "missing_exports": missing,
            "limitations": ["One training seed; no budget-matched control; no warmup causal superiority claim.",
                            "Development confirmation only; final holdout remains sealed.",
                            "Incomplete warmup episodes at the finite window were discarded as targets only."]}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--plan", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    result = analyze(args.plan)
    with args.output.open("x", encoding="utf-8") as stream:
        json.dump(result, stream, indent=2)
        stream.write("\n")
    print(json.dumps({"decision": result["decision"], "final_holdout": "SEALED"}))


if __name__ == "__main__":
    main()
