"""Prepare a single-stage curriculum run on a Slurm compute node, then exec training."""

from __future__ import annotations

import argparse
import importlib.util
import json
import os
import subprocess
import sys
import time
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "src"))
os.environ.setdefault("CUBLAS_WORKSPACE_CONFIG", ":4096:8")

from sls.rl.preparation import (
    benchmark_matches_workload,
    fixed_worker_layout,
    read_config,
)

PRODUCTION_LAYOUTS = ("32:4", "64:8", "64:16", "128:8", "128:16")


def budget_estimate(config: dict, benchmark: dict, *, elapsed: float,
                    completed_steps: int | None = None) -> dict | None:
    """Conservative preparation gate, not a guarantee about later Act2 throughput."""
    run = config["run"]
    if "preparation_wall_hours" not in run:
        return None
    layout = fixed_worker_layout(config)
    rows = [row for row in benchmark["results"]
            if (row["workers"], row["shards"]) == layout]
    if len(rows) != 1 or float(rows[0]["decisions_per_second"]) <= 0:
        raise ValueError("budget gate requires the measured pinned worker layout")
    start = (config["warm_start"]["parent_environment_steps"]
             if completed_steps is None else completed_steps)
    remaining = max(0, config["stages"]["train"]["target_environment_steps"] - start)
    rate = float(rows[0]["decisions_per_second"])
    estimate = (elapsed + remaining / rate * float(run["preparation_safety_factor"])
                + float(run["preparation_evaluation_reserve_hours"]) * 3600)
    available = float(run["preparation_wall_hours"]) * 3600
    return {"schema": "sls-training-wall-estimate-v1", "remaining_decisions": remaining,
            "benchmark_decisions_per_second": rate, "estimated_seconds": estimate,
            "available_seconds": available, "fits": estimate <= available,
            "limitation": "Short measured workload; later state distribution and evaluation costs may differ."}


def run_tool(name: str, *arguments: object) -> None:
    command = [sys.executable, str(ROOT / "tools" / name), *map(str, arguments)]
    print(json.dumps({"preparation_command": command}), flush=True)
    subprocess.run(command, cwd=ROOT, check=True)


def main() -> int:
    preparation_started = time.monotonic()
    parser = argparse.ArgumentParser()
    parser.add_argument("--config", type=Path, required=True)
    args = parser.parse_args()
    if not os.environ.get("SLURM_JOB_ID"):
        raise RuntimeError("submit train --prepare to a Slurm GPU compute node")
    config = read_config(args.config)
    if config["run"].get("workflow") != "single-stage":
        raise ValueError("--prepare requires the single-stage curriculum workflow")
    benchmark = ROOT / config["run"]["benchmark"]
    benchmark.parent.mkdir(parents=True, exist_ok=True)
    # The lock lives outside the fresh training directory. Keep it across exec.
    import fcntl
    lock = open(benchmark.with_name("run.lock"), "a")
    fcntl.flock(lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
    os.set_inheritable(lock.fileno(), True)
    if importlib.util.find_spec("torch") is None:
        subprocess.run([sys.executable, "-m", "pip", "install", "-r",
                        str(ROOT / "requirements/model.lock")], check=True, cwd=ROOT)
    probe = subprocess.run([
        sys.executable, "-c",
        "import sys; sys.path.insert(0, 'src'); "
        "from sls.rl.training_contract import native_artifact; assert native_artifact()",
    ], cwd=ROOT)
    if probe.returncode:
        run_tool("build_native.py", "--jobs", os.environ.get("SLURM_CPUS_PER_TASK", "16"))
    import torch

    from sls.rl.preparation import preparation_contract, require_preparation
    from sls.rl.training_contract import (
        native_source_digest,
        state_preserving_source_transition,
    )
    torch.use_deterministic_algorithms(bool(config["run"].get("deterministic", True)))
    torch.backends.cudnn.benchmark = False
    torch.set_float32_matmul_precision("high")
    if not torch.cuda.is_available():
        raise RuntimeError("CUDA is unavailable on allocated compute node")
    if config["run"].get("continuation_from"):
        run_tool("initialize_act1_continuation.py", "--config", args.config)
    latest = ROOT / config["run"]["output"] / "latest.pt"
    if latest.exists():
        saved = torch.load(latest, map_location="cpu", weights_only=False)
        if not state_preserving_source_transition(saved["contract"]["native_source_sha256"], native_source_digest()):
            raise ValueError("existing run uses different environment semantics; no automatic migration")
        del saved
    try:
        report = require_preparation(config, torch)
        print(json.dumps({"preparation": "REUSED", "gpu": torch.cuda.get_device_name(0),
                          "validated_gpu": report.get("gpu")}), flush=True)
    except (OSError, ValueError, KeyError):
        run_tool("preflight_training.py", "--skip-build", "--config", args.config,
                 "--output", benchmark.with_name("preflight.json"))
        layout = json.loads(benchmark.read_text()) if benchmark.exists() else {}
        pinned = fixed_worker_layout(config)
        reusable = (benchmark_matches_workload(config, layout)
                    and state_preserving_source_transition(
                        layout.get("native_source_sha256"), native_source_digest(),
                    )
                    and (
                        pinned is None
                        or (
                            layout.get("selected_workers"), layout.get("selected_shards")
                        ) == pinned
                    ))
        if not reusable:
            if latest.exists():
                raise ValueError("existing training layout cannot be replaced; recover its benchmark record")
            layouts = (f"{pinned[0]}:{pinned[1]}",) if pinned else PRODUCTION_LAYOUTS
            run_tool("benchmark_workers.py", "--config", args.config, "--layouts",
                     *layouts, "--output", benchmark)
            layout = json.loads(benchmark.read_text())
        checkpoint_arguments = ("--checkpoint", latest) if latest.exists() else ()
        run_tool("preflight_training.py", "--skip-build", "--config", args.config,
                 "--benchmark", benchmark, *checkpoint_arguments,
                 "--output", benchmark.with_name("worker-resume.json"))
        report = {"ok": True, "contract": preparation_contract(config, torch),
                  "layout": [layout["selected_workers"], layout["selected_shards"]],
                  "gpu": torch.cuda.get_device_name(0)}
        target = benchmark.with_name("ready.json")
        temporary = target.with_suffix(".tmp")
        temporary.write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
        temporary.replace(target)
    require_preparation(config, torch)
    estimate = budget_estimate(
        config, json.loads(benchmark.read_text()),
        elapsed=time.monotonic() - preparation_started,
        completed_steps=(int(torch.load(latest, map_location="cpu", weights_only=False)
                             ["trainer"]["environment_steps"]) if latest.exists() else None),
    )
    if estimate is not None:
        benchmark.with_name("budget-estimate.json").write_text(
            json.dumps(estimate, indent=2) + "\n", encoding="utf-8",
        )
        print(json.dumps({"budget_estimate": estimate}), flush=True)
        if not estimate["fits"]:
            raise ValueError("measured budget does not fit allocated pilot wall time; inspect, do not silently change recipe")
    command = [sys.executable, str(ROOT / "tools/train_full_run.py"),
               "--stage", "train", "--config", str(args.config.resolve())]
    print(json.dumps({"preparation": "PASS", "training_command": command}), flush=True)
    os.execv(sys.executable, command)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
