"""Prepare a single-stage Act1 run on a Slurm compute node, then exec training."""

from __future__ import annotations

import argparse
import importlib.util
import json
import os
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "src"))
os.environ.setdefault("CUBLAS_WORKSPACE_CONFIG", ":4096:8")

from sls.rl.preparation import read_config, workload_contract


def run_tool(name: str, *arguments: object) -> None:
    command = [sys.executable, str(ROOT / "tools" / name), *map(str, arguments)]
    print(json.dumps({"preparation_command": command}), flush=True)
    subprocess.run(command, cwd=ROOT, check=True)


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--config", type=Path, required=True)
    args = parser.parse_args()
    if not os.environ.get("SLURM_JOB_ID"):
        raise RuntimeError("submit train --prepare to a Slurm GPU compute node")
    config = read_config(args.config)
    if config["run"].get("workflow") != "single-stage":
        raise ValueError("--prepare requires the single-stage Act1 workflow")
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
        reusable = (layout.get("workload_contract") == workload_contract(config)
                    and state_preserving_source_transition(layout.get("native_source_sha256"), native_source_digest()))
        if not reusable:
            if latest.exists():
                raise ValueError("existing training layout cannot be replaced; recover its benchmark record")
            run_tool("benchmark_workers.py", "--config", args.config, "--layouts",
                     "32:4", "64:8", "128:8", "--output", benchmark)
            layout = json.loads(benchmark.read_text())
        run_tool("preflight_training.py", "--skip-build", "--config", args.config,
                 "--benchmark", benchmark, "--output", benchmark.with_name("worker-resume.json"))
        report = {"ok": True, "contract": preparation_contract(config, torch),
                  "layout": [layout["selected_workers"], layout["selected_shards"]],
                  "gpu": torch.cuda.get_device_name(0)}
        target = benchmark.with_name("ready.json")
        temporary = target.with_suffix(".tmp")
        temporary.write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
        temporary.replace(target)
    require_preparation(config, torch)
    command = [sys.executable, str(ROOT / "tools/train_full_run.py"),
               "--stage", "train", "--config", str(args.config.resolve())]
    print(json.dumps({"preparation": "PASS", "training_command": command}), flush=True)
    os.execv(sys.executable, command)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
