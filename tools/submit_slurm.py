"""Submit one-GPU SLS preflight, benchmark, smoke, pilot, or training jobs."""

from __future__ import annotations

import argparse
import os
from pathlib import Path
import shlex
import shutil
import subprocess
import sys
import tomllib

ROOT = Path(__file__).resolve().parents[1]


TASK_CONFIGS = {
    "smoke": "configs/train/act1_smoke.toml",
    "pilot": "configs/train/act1_pilot.toml",
    "train": "configs/train/act1_train.toml",
}
TASK_MODES = {"smoke": "EXPERIMENTAL", "pilot": "EXPERIMENTAL", "train": "PRODUCTION"}


def _absolute_without_symlink_resolution(path: Path) -> str:
    """Expand a user path and absolutize it while preserving env entry symlinks."""

    expanded = os.path.expanduser(os.fspath(path))
    return os.path.abspath(expanded)


def _parser() -> argparse.ArgumentParser:
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
    parser.add_argument("--warm-start", type=Path)
    parser.add_argument("--workers", type=int, help="selected result from benchmark_workers.py")
    parser.add_argument("--dry-run", action="store_true")
    parser.add_argument("--mode", choices=("experimental", "production"))
    return parser


def build_sbatch_command(args: argparse.Namespace, *, root: Path = ROOT) -> list[str]:
    """Build a Slurm command without executing it or touching the filesystem."""

    if args.cpus <= 0:
        raise ValueError("--cpus must be positive")
    if args.task in {"preflight", "benchmark"}:
        unsupported = [
            name for name, value in (
                ("--config", args.config), ("--resume", args.resume),
                ("--warm-start", args.warm_start),
                ("--workers", args.workers),
            )
            if value not in (None, False)
        ]
        if unsupported:
            raise ValueError(
                f"{args.task} does not accept training option(s): "
                + ", ".join(unsupported)
            )
        if args.mode is None:
            raise ValueError(f"{args.task} requires explicit --mode")
    elif args.resume and args.warm_start:
        raise ValueError("--resume and --warm-start are mutually exclusive")
    elif args.mode is not None:
        raise ValueError("training task mode is defined only by its TOML config")
    python = _absolute_without_symlink_resolution(args.python)
    if args.task == "train":
        partition, walltime = "gpu-long", "3-00:00:00"
    else:
        partition, walltime = "gpu", "03:00:00"
    partition = args.partition or partition
    walltime = args.time or walltime
    if args.task == "preflight":
        command = [
            str(python), str(root / "tools" / "preflight_training.py"),
            "--mode", str(args.mode), "--jobs", str(args.cpus),
        ]
    elif args.task == "benchmark":
        command = [
            str(python), str(root / "tools" / "benchmark_workers.py"),
            "--mode", str(args.mode),
        ]
    else:
        config = (args.config or root / TASK_CONFIGS[args.task]).resolve()
        if config.is_file():
            with config.open("rb") as stream:
                configured_mode = str(
                    tomllib.load(stream).get("run", {}).get("training_mode") or ""
                ).upper()
        elif args.config is not None:
            raise ValueError(f"training config does not exist: {config}")
        else:
            # Unit/dry-run callers may provide a synthetic root. The task's
            # built-in default remains statically bound to its required mode.
            configured_mode = TASK_MODES[args.task]
        if configured_mode != TASK_MODES[args.task]:
            raise ValueError(
                f"{args.task} config training_mode must be {TASK_MODES[args.task]}"
            )
        command = [str(python), str(root / "tools" / "train_full_run.py"), "--config", str(config)]
        if args.workers is not None:
            command += ["--workers", str(args.workers)]
        if args.resume:
            command += ["--resume", "auto"]
        if args.warm_start is not None:
            command += ["--warm-start", str(args.warm_start.resolve())]
    logs = root / "runs" / "slurm-logs"
    return [
        "sbatch", "--parsable", f"--account={args.account}", f"--qos={args.qos}",
        f"--partition={partition}", f"--gres=gpu:{args.gpu}:1", f"--cpus-per-task={args.cpus}",
        f"--mem={args.memory}", f"--time={walltime}", "--signal=B:TERM@300",
        f"--job-name=sls-{args.task}", f"--chdir={root}",
        f"--output={logs / '%x-%j.out'}", f"--error={logs / '%x-%j.err'}",
        # Replace the batch shell with Python so Slurm's B:TERM signal reaches
        # StopController instead of stopping at an intermediate shell process.
        "--wrap", "exec " + shlex.join(command),
    ]


def main(argv: list[str] | None = None) -> int:
    parser = _parser()
    args = parser.parse_args(argv)
    try:
        sbatch = build_sbatch_command(args)
    except ValueError as error:
        parser.error(str(error))
    (ROOT / "runs" / "slurm-logs").mkdir(parents=True, exist_ok=True)
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
