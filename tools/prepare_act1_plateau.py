"""Build a current-environment baseline and failure corpus in one Slurm GPU job."""

from __future__ import annotations

import argparse
import json
import os
import signal
import subprocess
import sys
import tomllib
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))
sys.path.insert(0, str(ROOT / "src"))
os.environ.setdefault("CUBLAS_WORKSPACE_CONFIG", ":4096:8")


def validate_config(config: dict, *, root: Path = ROOT) -> None:
    if config["profile"] != "IRONCLAD_A20_ACT1":
        raise ValueError("plateau diagnosis requires Ironclad A20 Act1")
    for key in ("development_episodes", "diagnostic_episodes", "environment_shards"):
        if not isinstance(config[key], int) or config[key] <= 0:
            raise ValueError(f"{key} must be positive")
    diagnostic_start = config["diagnostic_seed_start"]
    diagnostic_end = diagnostic_start + config["diagnostic_episodes"]
    development_start = config["development_seed_start"]
    if not 0 <= diagnostic_start < diagnostic_end <= config["training_seed_limit"] <= development_start:
        raise ValueError("diagnostic training seeds and held-out development seeds overlap")
    for key in ("checkpoint_sha256", "native_source_sha256"):
        value = config[key]
        if not isinstance(value, str) or len(value) != 64 or any(c not in "0123456789abcdef" for c in value):
            raise ValueError(f"invalid {key}")
    output = (root / config["output"]).resolve()
    if not output.is_relative_to((root / "local/runs").resolve()):
        raise ValueError("plateau output must be under local/runs")
    source = (root / config["checkpoint"]).resolve()
    if output.is_relative_to(source.parent) or source.is_relative_to(output):
        raise ValueError("plateau output must be separate from its source checkpoint")


def summarize_failures(result: dict) -> dict:
    """Population counts; combat-entry preparation is studied in a separate cohort."""
    from collections import Counter

    failures = [row for row in result["seed_results"] if not row["success"]]
    early = [row for row in failures if row["floor"] < 16]
    groups = Counter(tuple(row.get("last_context", {}).get("enemy_ids", [])) for row in early)
    return {
        "episodes": result["episodes"], "successes": result["successes"],
        "success_rate": result["success_rate"], "success_rate_ci95": result["success_rate_ci95"],
        "failures": len(failures), "boss_floor_deaths": sum(row["floor"] == 16 for row in failures),
        "before_boss_deaths": len(early),
        "before_boss_last_enemies": [
            {"enemy_ids": list(ids), "deaths": count} for ids, count in groups.most_common()
        ],
        "boss_entry_success_rate": result["boss_entry_success_rate"],
        "qualification": "Last-enemy death counts are not conditional encounter failure rates.",
    }


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--config", type=Path, required=True)
    args = parser.parse_args(argv)
    config = tomllib.loads(args.config.read_text(encoding="utf-8"))
    validate_config(config)
    # Refuse heavy work on a workstation or login node.
    if not sys.platform.startswith("linux") or not os.environ.get("SLURM_JOB_ID"):
        raise RuntimeError("submit plateau through tools/submit_slurm.py on a Slurm compute node")
    import torch

    from sls.rl.training_contract import (
        git_state,
        native_artifact,
        native_source_digest,
        sha256_file,
        training_implementation_digest,
    )
    from tools.diagnose_act1_corpus import validate_current_evaluation

    checkpoint = (ROOT / config["checkpoint"]).resolve()
    if sha256_file(checkpoint) != config["checkpoint_sha256"]:
        raise ValueError("56M checkpoint SHA256 mismatch")
    if native_source_digest() != config["native_source_sha256"]:
        raise ValueError("native source differs from pinned plateau environment")
    if not torch.cuda.is_available() or "A100" not in torch.cuda.get_device_name(0):
        raise RuntimeError("plateau job requires an allocated A100")
    if "MIG" in torch.cuda.get_device_name(0).upper():
        raise RuntimeError("plateau job requires a physical A100, not a MIG slice")
    output = (ROOT / config["output"]).resolve()
    output.mkdir(parents=True, exist_ok=False)
    (output / "diagnostic-config.toml").write_bytes(args.config.read_bytes())
    status = {"schema": "sls-act1-plateau-v1", "status": "RUNNING", "completed_stages": []}

    def save_status() -> None:
        temporary = output / "status.tmp"
        temporary.write_text(json.dumps(status, indent=2) + "\n", encoding="utf-8")
        temporary.replace(output / "status.json")

    def interrupted(_number: int, _frame: object) -> None:
        raise InterruptedError("Slurm stop requested; preserve diagnostic evidence")

    signal.signal(signal.SIGTERM, interrupted)
    signal.signal(signal.SIGINT, interrupted)

    def run(stage: str, script: str, *arguments: object) -> None:
        status["active_stage"] = stage
        save_status()
        command = [sys.executable, str(ROOT / "tools" / script), *map(str, arguments)]
        print(json.dumps({"stage": stage, "command": command}), flush=True)
        process = None
        try:
            with (output / f"{stage}.stdout.log").open("wb") as stdout, (output / f"{stage}.stderr.log").open("wb") as stderr:
                process = subprocess.Popen(command, cwd=ROOT, stdout=stdout, stderr=stderr)
                code = process.wait()
                if code:
                    raise RuntimeError(f"{stage} exited {code}; inspect {output / (stage + '.stderr.log')}")
        finally:
            if process is not None and process.poll() is None:
                process.terminate()
                try:
                    process.wait(timeout=15)
                except subprocess.TimeoutExpired:
                    process.kill()
                    process.wait(timeout=15)
        status["completed_stages"].append(stage)
        save_status()

    def evaluation(name: str, start: int, count: int) -> dict:
        path = output / f"{name}.json"
        run(name, "evaluate_checkpoint.py", checkpoint, "--output", path,
            "--profile", config["profile"], "--seed-start", start, "--episodes", count,
            "--device", "cuda", "--environment-shards", config["environment_shards"],
            "--allow-environment-migration")
        record = json.loads(path.read_text(encoding="utf-8"))
        validate_current_evaluation(record, checkpoint_sha256=config["checkpoint_sha256"],
                                    native_sha256=config["native_source_sha256"])
        if record["seed_range"] != [start, start + count]:
            raise ValueError("evaluation ran different seeds than configured")
        return record

    try:
        run("build", "build_native.py", "--jobs", os.environ.get("SLURM_CPUS_PER_TASK", "16"))
        run("preflight", "preflight_training.py", "--skip-build", "--config",
            ROOT / config["preflight_config"], "--output", output / "preflight.json")
        preflight = json.loads((output / "preflight.json").read_text(encoding="utf-8"))
        if preflight.get("ok") is not True or preflight.get("native_source_sha256") != config["native_source_sha256"]:
            raise ValueError("preflight evidence failed or has different native source")
        artifact = native_artifact()
        if artifact.get("source_sha256") != config["native_source_sha256"]:
            raise ValueError("built artifact source identity mismatch")
        baseline = evaluation("baseline", config["development_seed_start"], config["development_episodes"])
        diagnostic = evaluation("diagnostic-evaluation", config["diagnostic_seed_start"], config["diagnostic_episodes"])
        run("corpus", "diagnose_act1_corpus.py", "--evaluation", output / "diagnostic-evaluation.json",
            "--output", output / "corpus", "--device", "cuda", "--allow-environment-migration", "--analyze")
        analysis = json.loads((output / "corpus/analysis.json").read_text(encoding="utf-8"))
        captured = json.loads((output / "corpus/result.json").read_text(encoding="utf-8"))
        for key in ("backend_errors", "backend_truncations", "timeouts", "step_limits", "cycle_limits"):
            if captured["result"][key] != 0:
                raise ValueError(f"diagnostic capture has {key}")
        report = {
            "schema": "sls-act1-plateau-report-v1", "checkpoint_sha256": sha256_file(checkpoint),
            "config_sha256": sha256_file(output / "diagnostic-config.toml"),
            "native_source_sha256": config["native_source_sha256"], "native_artifact": artifact,
            "git": git_state(), "training_implementation_sha256": training_implementation_digest(),
            "tool_source_sha256": {
                name: sha256_file(ROOT / "tools" / name) for name in (
                    "prepare_act1_plateau.py", "diagnose_act1_corpus.py", "analyze_act1_corpus.py",
                )
            },
            "baseline": summarize_failures(baseline["result"]),
            "diagnostic_population": summarize_failures(diagnostic["result"]),
            "boss_entries_stratified": analysis["boss_entries"],
            "diagnostic_counters": analysis["counters"],
            "capture_reference_differences": len(captured["server_differences"]),
            "evidence_sha256": {
                str(path.relative_to(output)): sha256_file(path) for path in (
                    output / "preflight.json", output / "baseline.json", output / "diagnostic-evaluation.json",
                    output / "corpus/selection.json", output / "corpus/result.json", output / "corpus/analysis.json",
                )
            },
            "qualification": "Preparation/action-order findings are diagnostic associations, not causal proof. Capture batching can change argmax trajectories; reference differences are preserved. No training or champion promotion performed.",
        }
        (output / "report.json").write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
        status["status"] = "COMPLETE"
        print(json.dumps({"report": str(output / "report.json"), "baseline": report["baseline"]}), flush=True)
    except BaseException as error:
        status["status"] = "STOPPED" if isinstance(error, (InterruptedError, KeyboardInterrupt)) else "FAILED"
        status["error"] = str(error)
        raise
    finally:
        save_status()
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
