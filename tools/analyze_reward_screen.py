"""Verify downloaded reward-screen evidence and recompute all paired outcomes.

No simulator execution or training: original files are read-only. Checkpoint
loading is restricted to the user's local trusted training archive.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import statistics
import sys
import tomllib
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "src"))

from compare_run_arms import _from_seed_results, report, wilson


def digest(path: Path) -> str:
    with path.open("rb") as stream:
        return hashlib.file_digest(stream, "sha256").hexdigest()


def read(path: Path) -> dict:
    return json.loads(path.read_text(encoding="utf-8"))


def outcomes(result: dict, interval: tuple[int, int]) -> dict:
    rows = result["seed_results"]
    indexed = {int(row["seed"]): row for row in rows}
    if len(indexed) != len(rows) or set(indexed) != set(range(*interval)):
        raise ValueError("duplicate, missing or unexpected evaluation seed")
    if any(type(row["success"]) is not bool for row in rows):
        raise ValueError("non-boolean outcome")
    wins = sum(row["success"] for row in rows)
    if wins != result["successes"] or len(rows) != result["episodes"]:
        raise ValueError("aggregate evaluation counts disagree with raw outcomes")
    if abs(wins / len(rows) - result["success_rate"]) > 1e-12:
        raise ValueError("aggregate win rate disagrees with raw outcomes")
    bosses = {}
    for boss in sorted({row["bosses"]["1"] for row in rows}):
        subset = [row for row in rows if row["bosses"]["1"] == boss]
        entered = [row for row in subset if f"ACT_1:{boss}" in row["entered_bosses"]]
        k = sum(row["success"] for row in subset)
        if (k != result["boss_successes"][f"ACT_1:{boss}"]
                or len(subset) != result["boss_attempts"][f"ACT_1:{boss}"]):
            raise ValueError("boss counts disagree with raw outcomes")
        bosses[boss] = {"wins": k, "seeds": len(subset), "entries": len(entered),
                        "clear_rate": k / len(subset), "entry_win_rate": k / len(entered)}
    failures = [row["floor"] for row in rows if not row["success"]]
    return {"wins": wins, "episodes": len(rows), "rate": wins / len(rows),
            "ci95": wilson(wins, len(rows)), "bosses": bosses,
            "failure_floor_mean": statistics.mean(failures) if failures else None,
            "failure_floor_median": statistics.median(failures) if failures else None,
            "mean_steps": statistics.mean(row["steps"] for row in rows),
            "health": {key: result[key] for key in (
                "backend_errors", "backend_truncations", "step_limits", "cycle_limits", "timeouts")}}


def analyze_run(run: Path) -> tuple[dict, list[dict], dict]:
    import torch

    bundle = read(run / "training-bundle.json")
    verified = {}
    for name, expected in bundle["files"].items():
        path = run / name
        if not path.resolve().is_relative_to(run.resolve()):
            raise ValueError("bundle path escapes run")
        actual = digest(path)
        if actual != expected:
            raise ValueError(f"bundle hash mismatch: {path}")
        verified[name] = actual
    config = tomllib.loads((run / "training-config.toml").read_text(encoding="utf-8"))
    manifest = read(run / "run-manifest.json")
    if manifest["status"] != "COMPLETE" or manifest["stages"]["train"]["status"] != "COMPLETE":
        raise ValueError("run incomplete")
    if digest(run / "training-config.toml") != manifest["config_sha256"]:
        raise ValueError("config digest mismatch")
    periodic = tuple(manifest["periodic_evaluation_seeds"])
    final_range = tuple(manifest["final_evaluation_seeds"])
    if not (config["run"]["training_seed_limit"] <= min(periodic[0], final_range[0])
            and (periodic[1] <= final_range[0] or final_range[1] <= periodic[0])):
        raise ValueError("train/development seed namespaces overlap")
    metrics = [json.loads(line) for line in (run / "stages/train/metrics.jsonl").read_text().splitlines()]
    evaluated = [row for row in metrics if "evaluation" in row]
    curve = [{"steps": row["environment_steps"], **outcomes(row["evaluation"], periodic)}
             for row in evaluated]
    final = read(run / "final-evaluation.json")
    best = read(run / "stages/train/selection/best_progress.json")
    if (final["checkpoint_sha256"] != best["checkpoint_sha256"]
            or final["checkpoint_sha256"] != verified["stages/train/selection/best_progress.pt"]
            or final["checkpoint_environment_steps"] != best["environment_steps"]):
        raise ValueError("selected/final identity mismatch")
    checkpoints = {}
    selected_payload = None
    paths = sorted(run.glob("checkpoint-steps-*.pt")) + [
        run / "final.pt", run / "latest.pt", run / "stages/train/selection/best_progress.pt"]
    for path in paths:
        payload = torch.load(path, map_location="cpu", weights_only=False)
        contract = payload["contract"]
        for key in ("native_source_sha256", "encoding_schema", "vocabulary_sha256"):
            if contract[key] != manifest[key]:
                raise ValueError(f"checkpoint {path.name} contract mismatch: {key}")
        if (contract["ppo"] != manifest["ppo"] or contract["model"] != manifest["model"]
                or contract["training_config_sha256"] != manifest["training_identity_sha256"]
                or contract["git_commit"] != manifest["git"]["commit"]
                or contract["profile"].profile_id != manifest["profile"]
                or contract["workers"] != manifest["workers"]
                or contract["worker_shards"] != manifest["shards"]):
            raise ValueError(f"checkpoint {path.name} training identity mismatch")
        step = payload["trainer"]["environment_steps"]
        if path.name.startswith("checkpoint-steps-") and step != int(path.stem.split("-")[-1]):
            raise ValueError("checkpoint filename step mismatch")
        if not all(torch.isfinite(value).all() for value in payload["model"].values()):
            raise ValueError("nonfinite model weights")
        checkpoints[path.relative_to(run).as_posix()] = {
            "sha256": digest(path), "steps": step, "update": payload["trainer"]["update"],
            "has_resume_state": all(key in payload for key in (
                "optimizer", "python_rng", "torch_rng", "cuda_rng", "environments"))}
        if path == run / "stages/train/selection/best_progress.pt":
            selected_payload = payload
    exported = torch.load(run / f"{run.name}.pt", map_location="cpu", weights_only=False)
    if selected_payload is None or set(exported["model"]) != set(selected_payload["model"]):
        raise ValueError("exported policy model keys differ from selected checkpoint")
    if any(not torch.equal(value.cpu(), selected_payload["model"][key].cpu())
           for key, value in exported["model"].items()):
        raise ValueError("exported policy weights differ from selected checkpoint")
    if checkpoints["latest.pt"]["steps"] != manifest["environment_steps"]:
        raise ValueError("latest checkpoint incomplete")
    updates = [row for row in metrics if "update_seconds" in row]
    totals = {key: sum(row[key] for row in updates) for key in (
        "update_seconds", "collect_seconds", "optimize_seconds", "collect_encode_seconds",
        "collect_transition_seconds", "collect_policy_seconds", "collect_worker_step_seconds")}
    health = {key: {"mean": statistics.mean(row[key] for row in updates),
                    "first_10": statistics.mean(row[key] for row in updates[:10]),
                    "last_10": statistics.mean(row[key] for row in updates[-10:])}
              for key in ("approx_kl_final", "clip_fraction", "gradient_norm",
                          "gradient_clip_fraction", "value_explained_variance", "value", "entropy")}
    neow = {f"option_{i}": sum(row[f"neow_option_{i}_count"] for row in updates) for i in range(4)}
    result = {
        "run": run.name, "git": manifest["git"], "native_source_sha256": manifest["native_source_sha256"],
        "training_implementation_sha256": manifest["training_implementation_sha256"],
        "training_identity_sha256": manifest["training_identity_sha256"],
        "config_sha256": manifest["config_sha256"], "config": config,
        "verified_bundle_files": verified, "checkpoints": checkpoints,
        "periodic_curve": curve, "selected_steps": best["environment_steps"],
        "selected_sha256": best["checkpoint_sha256"],
        "development_confirmation": outcomes(final["result"], final_range),
        "confirmation_seed_range": final_range, "periodic_seed_range": periodic,
        "final_evaluation_has_explicit_runtime": "runtime" in final,
        "exported_weights_match_selected": True,
        "new_decisions": manifest["environment_steps"] - manifest["initialization"]["parent_environment_steps"],
        "updates": len(updates), "wall_hours": (manifest["stages"]["train"]["finished_unix"]
                                                 - manifest["created_unix"]) / 3600,
        "timing_seconds": totals, "health": health, "neow_sample_counts": neow,
        "kl_early_stops": sum(row["kl_early_stop"] for row in updates),
    }
    return result, evaluated, final


def paired(left: dict, right: dict, *, source: str) -> dict:
    result = report(_from_seed_results("progress", source, left["seed_results"]),
                    _from_seed_results("win", source, right["seed_results"]), 512)
    # Normal approximation to the mean paired Bernoulli difference. Exact
    # McNemar p is reported separately; the interval is descriptive.
    n = result["paired_seeds"]
    d = result["net"] / n
    q = sum(result["discordant"].values()) / n
    se = ((q - d * d) / n) ** 0.5
    result["paired_difference_ci95_normal"] = [d - 1.96 * se, d + 1.96 * se]
    result["boss_pairs"] = {}
    for boss in sorted({row["bosses"]["1"] for row in left["seed_results"]}):
        lrows = [row for row in left["seed_results"] if row["bosses"]["1"] == boss]
        rrows = [row for row in right["seed_results"] if row["bosses"]["1"] == boss]
        result["boss_pairs"][boss] = report(_from_seed_results("progress", source, lrows),
                                                   _from_seed_results("win", source, rrows), 512)
    return result


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--progress", type=Path, required=True)
    parser.add_argument("--win", type=Path, required=True)
    parser.add_argument("--archive", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    left, levals, lfinal = analyze_run(args.progress)
    right, revals, rfinal = analyze_run(args.win)
    for key in ("native_source_sha256", "training_implementation_sha256", "git"):
        if left[key] != right[key]:
            raise ValueError(f"arms have incompatible identities: {key}")
    lp, rp = left["config"]["ppo"], right["config"]["ppo"]
    changed = {key for key in lp if lp[key] != rp[key]}
    if changed != {"failure_progress_scale", "reward_schema"}:
        raise ValueError(f"unexpected PPO differences: {changed}")
    lc, rc = left["config"], right["config"]
    for key in ("model", "stages", "warm_start"):
        if lc[key] != rc[key]:
            raise ValueError(f"non-reward experimental difference: {key}")
    ignored = {"benchmark", "output"}
    if {k: v for k, v in lc["run"].items() if k not in ignored} != {
            k: v for k, v in rc["run"].items() if k not in ignored}:
        raise ValueError("non-reward run difference")
    if [row["environment_steps"] for row in levals] != [row["environment_steps"] for row in revals]:
        raise ValueError("unmatched periodic evaluation budgets")
    baseline_left = levals[0]["evaluation"]["seed_results"]
    baseline_right = revals[0]["evaluation"]["seed_results"]
    if baseline_left != baseline_right:
        raise ValueError("baseline seed results differ")
    comparison = {
        "schema": "sls-reward-screen-analysis-v1", "archive_sha256": digest(args.archive),
        "arms": {"progress": left, "win": right},
        "periodic_pairs": [{"steps": lrow["environment_steps"],
                            **paired(lrow["evaluation"], rrow["evaluation"], source="periodic")}
                           for lrow, rrow in zip(levals, revals)],
        "selected_confirmation_pair": paired(lfinal["result"], rfinal["result"], source="confirmation"),
        "inference_limit": "one training seed, development-only sets, repeated selection; no untouched final test",
    }
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(comparison, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
