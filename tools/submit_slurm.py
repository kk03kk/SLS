"""Submit one-GPU SLS preflight, benchmark, smoke, pilot, or training jobs."""

from __future__ import annotations

import argparse
from pathlib import Path
import shlex
import shutil
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[1]


TASK_CONFIGS = {
    "smoke": "configs/train/act1_smoke.toml",
    "pilot": "configs/train/act1_pilot.toml",
    "train": "configs/train/full_run.toml",
}


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("task", choices=("preflight", "benchmark", "smoke", "pilot", "train"))
    parser.add_argument("--python", type=Path, default=Path(sys.executable))
    parser.add_argument("--account", default="allusers")
    parser.add_argument("--qos", default="normal")
    parser.add_argument("--partition")
    parser.add_argument("--gpu", default="a100-40")
    parser.add_argument("--cpus", type=int, default=16)
    parser.add_argument("--memory", default="64G")
    parser.add_argument("--time")
    parser.add_argument("--config", type=Path)
    parser.add_argument("--resume", action="store_true")
    parser.add_argument("--dry-run", action="store_true")
    args = parser.parse_args()
    if args.cpus <= 0:
        parser.error("--cpus must be positive")
    if args.task == "train":
        partition, walltime = "gpu-long", "3-00:00:00"
    else:
        partition, walltime = "gpu", "03:00:00"
    partition = args.partition or partition
    walltime = args.time or walltime
    if args.task == "preflight":
        command = [str(args.python.resolve()), str(ROOT / "tools" / "preflight_training.py"), "--jobs", str(args.cpus)]
    elif args.task == "benchmark":
        command = [str(args.python.resolve()), str(ROOT / "tools" / "benchmark_workers.py")]
    else:
        config = (args.config or ROOT / TASK_CONFIGS[args.task]).resolve()
        command = [str(args.python.resolve()), str(ROOT / "tools" / "train_full_run.py"), "--config", str(config)]
        if args.resume:
            command += ["--resume", "auto"]
    logs = ROOT / "runs" / "slurm-logs"
    logs.mkdir(parents=True, exist_ok=True)
    sbatch = [
        "sbatch", "--parsable", f"--account={args.account}", f"--qos={args.qos}",
        f"--partition={partition}", f"--gres=gpu:{args.gpu}:1", f"--cpus-per-task={args.cpus}",
        f"--mem={args.memory}", f"--time={walltime}", "--signal=B:TERM@300",
        f"--job-name=sls-{args.task}", f"--chdir={ROOT}",
        f"--output={logs / '%x-%j.out'}", f"--error={logs / '%x-%j.err'}",
        "--wrap", shlex.join(command),
    ]
    print(shlex.join(sbatch))
    if args.dry_run:
        return 0
    if shutil.which("sbatch") is None:
        raise SystemExit("sbatch is unavailable; run this command on an NUS login node")
    result = subprocess.run(sbatch, cwd=ROOT, check=True, text=True, capture_output=True)
    print(result.stdout.strip())
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
