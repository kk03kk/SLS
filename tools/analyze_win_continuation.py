"""Verify a completed Win continuation and recompute paired development evidence."""

from __future__ import annotations

import argparse
import contextlib
import io
import json
import math
import statistics
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from tools.analyze_reward_screen import analyze_run, digest, outcomes, read
from tools.compare_run_arms import _from_seed_results, report


def paired_evaluations(left: dict, right: dict, *, label: str) -> dict:
    """Refuse mismatched evaluation contracts before pairing recorded outcomes."""
    for key in ("seeds", "runtime", "simulator", "environment", "reward", "decoding"):
        if key in left or key in right:
            if left.get(key) != right.get(key):
                raise ValueError(f"incompatible evaluation identity: {key}")
    if not left.get("runtime") or left.get("evaluation_role") != "development-confirmation":
        raise ValueError("missing runtime or development role")
    if right.get("evaluation_role") != "development-confirmation":
        raise ValueError("wrong candidate evaluation role")
    interval = tuple(left["seeds"])
    outcomes(left["result"], interval)
    outcomes(right["result"], interval)
    lrows, rrows = left["result"]["seed_results"], right["result"]["seed_results"]
    lindex = {row["seed"]: row for row in lrows}
    if any(row["bosses"] != lindex[row["seed"]]["bosses"] for row in rrows):
        raise ValueError("seed-to-boss assignment changed")

    def pair(lr: list[dict], rr: list[dict]) -> dict:
        with contextlib.redirect_stdout(io.StringIO()):
            result = report(_from_seed_results("reference", label, lr),
                            _from_seed_results("candidate", label, rr), 512)
        n = result["paired_seeds"]
        delta = result["net"] / n
        discordance = sum(result["discordant"].values()) / n
        se = math.sqrt((discordance - delta * delta) / n)
        result["paired_difference_ci95_normal"] = [delta - 1.96 * se, delta + 1.96 * se]
        return result

    result = pair(lrows, rrows)
    result["boss_pairs"] = {
        boss: pair([r for r in lrows if r["bosses"]["1"] == boss],
                   [r for r in rrows if r["bosses"]["1"] == boss])
        for boss in sorted({r["bosses"]["1"] for r in lrows})
    }
    return result


def analyze(run: Path, archive: Path, evidence: Path) -> dict:
    summary, _, selected = analyze_run(run)
    manifest = read(run / "run-manifest.json")
    config = summary["config"]
    if (config["ppo"]["reward_schema"] != "sls-curriculum-win-v1"
            or config["ppo"]["gamma"] != 1.0
            or config["ppo"]["failure_progress_scale"] != 0.0):
        raise ValueError("not the expected Win objective")
    identity_text = (evidence / "SERVER-IDENTITY.txt").read_text(encoding="utf-8")
    if manifest["git"]["commit"] not in identity_text or manifest["git"]["dirty"]:
        raise ValueError("archive/server source identity mismatch")
    endpoint = read(run / "endpoint-evaluation.json")
    reference = read(run / "reference-evaluation.json")
    for evaluation, name in ((endpoint, "final.pt"), (selected, "stages/train/selection/best_progress.pt")):
        if (evaluation["checkpoint_sha256"] != summary["checkpoints"][name]["sha256"]
                or evaluation["checkpoint_environment_steps"] != summary["checkpoints"][name]["steps"]):
            raise ValueError("evaluated checkpoint identity mismatch")
    if reference["checkpoint_sha256"] != config["run"]["development_reference_sha256"]:
        raise ValueError("frozen reference identity mismatch")
    confirmation_range = tuple(manifest["final_evaluation_seeds"])
    for evaluation in (reference, endpoint, selected):
        if tuple(evaluation["seeds"]) != confirmation_range:
            raise ValueError("confirmation namespace mismatch")
        if evaluation["simulator"]["native_source_sha256"] != manifest["native_source_sha256"]:
            raise ValueError("evaluation simulator source mismatch")
    if manifest["training_implementation_sha256"] != manifest["continuation"]["new_training_implementation_sha256"]:
        raise ValueError("continuation implementation mismatch")
    records = [json.loads(line) for line in (run / "stages/train/metrics.jsonl").read_text().splitlines()]
    updates = [row for row in records if "update_seconds" in row]
    batch = manifest["workers"] * manifest["ppo"]["rollout_steps"]
    start = manifest["continuation"]["parent_environment_steps"]
    if (len(updates) * batch != manifest["environment_steps"] - start
            or any(row["environment_steps"] != start + batch * (i + 1) for i, row in enumerate(updates))):
        raise ValueError("missing, reordered or inconsistent updates")
    if manifest["environment_steps"] < config["stages"]["train"]["target_environment_steps"]:
        raise ValueError("training budget incomplete")
    if manifest["environment_steps"] >= config["stages"]["train"]["target_environment_steps"] + batch:
        raise ValueError("unexpected training overshoot")
    diagnostics = {}
    for domain in ("combat", "run", "choice"):
        for field in ("advantage_mean", "advantage_std", "advantage_scale", "samples"):
            key = f"{field}_{domain}" + ("_fraction" if field == "samples" else "")
            values = sorted(float(row[key]) for row in updates)
            if not all(math.isfinite(v) for v in values):
                raise ValueError("nonfinite diagnostic")
            diagnostics[key] = {"mean": statistics.mean(values), "min": min(values),
                                "median": statistics.median(values), "max": max(values)}
    endpoint_pair = paired_evaluations(reference, endpoint, label="fixed-endpoint")
    selected_pair = paired_evaluations(reference, selected, label="selected")
    between = paired_evaluations(endpoint, selected, label="endpoint-v-selected")
    health_clean = all(not any(outcomes(e["result"], confirmation_range)["health"].values())
                       for e in (reference, endpoint, selected))
    ci = endpoint_pair["paired_difference_ci95_normal"]
    success = (endpoint_pair["net"] / endpoint_pair["paired_seeds"] >= .02
               and endpoint_pair["exact_mcnemar_p"] < .05 and ci[0] > 0 and health_clean
               and selected_pair["paired_difference_ci95_normal"][1] >= 0)
    return {"schema": "sls-win-continuation-analysis-v1", "archive_sha256": digest(archive),
            "server_identity": identity_text, "run": summary,
            "reference": outcomes(reference["result"], confirmation_range),
            "endpoint": outcomes(endpoint["result"], confirmation_range),
            "evaluation_identity": {k: endpoint.get(k) for k in (
                "runtime", "simulator", "environment", "reward", "decoding")},
            "endpoint_vs_reference": endpoint_pair, "selected_vs_reference": selected_pair,
            "selected_vs_endpoint": between, "domain_diagnostics": diagnostics,
            "preregistered_development_success": success,
            "preparation_hashes": {p.name: digest(p)
                                   for p in (run.parent / "preparation" / run.name).glob("*")
                                   if p.is_file()},
            "evidence_hashes": {p.relative_to(evidence).as_posix(): digest(p)
                                for p in evidence.rglob("*") if p.is_file()},
            "inference_limit": "single training seed, simulator and development evidence; no stock-game or reserved-final result"}


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--run", type=Path, required=True)
    parser.add_argument("--archive", type=Path, required=True)
    parser.add_argument("--evidence", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    result = analyze(args.run, args.archive, args.evidence)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(result, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    print(json.dumps({"success": result["preregistered_development_success"],
                      "selected_vs_endpoint": result["selected_vs_endpoint"]["exact_mcnemar_p"]}))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
