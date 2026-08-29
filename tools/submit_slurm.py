"""Submit one-GPU SLS preflight, benchmark, smoke, pilot, or training jobs."""

from __future__ import annotations

import argparse
import os
import shlex
import shutil
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


TRAIN_CONFIG = "configs/train/ironclad_a0_fullrun.toml"


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
    parser.add_argument("--initialize-from", type=Path)
    parser.add_argument(
        "--resume", choices=("auto", "environment-migration"), default="auto",
    )
    parser.add_argument("--dry-run", action="store_true")
    return parser


def build_sbatch_command(args: argparse.Namespace, *, root: Path = ROOT) -> list[str]:
    """Build a Slurm command without executing it or touching the filesystem."""

    if args.cpus <= 0:
        raise ValueError("--cpus must be positive")
    if args.task in {"preflight", "benchmark"}:
        unsupported = []
        if args.config is not None:
            unsupported.append("--config")
        if args.initialize_from is not None:
            unsupported.append("--initialize-from")
        if args.resume != "auto":
            unsupported.append("--resume")
        if unsupported:
            raise ValueError(
                f"{args.task} does not accept training option(s): "
                + ", ".join(unsupported)
            )
    python = _absolute_without_symlink_resolution(args.python)
    if args.task in {"pilot", "train"}:
        partition, walltime = "gpu-long", "3-00:00:00"
    else:
        partition, walltime = "gpu", "03:00:00"
    if args.task == "pilot":
        walltime = "12:00:00"
    partition = args.partition or partition
    walltime = args.time or walltime
    if args.task == "preflight":
        command = [
            str(python), str(root / "tools" / "preflight_training.py"),
            "--jobs", str(args.cpus),
        ]
    elif args.task == "benchmark":
        command = [
            str(python), str(root / "tools" / "benchmark_workers.py"),
        ]
    else:
        config = (args.config or root / TRAIN_CONFIG).resolve()
        if not config.is_file() and args.config is not None:
            raise ValueError(f"training config does not exist: {config}")
        command = [
            str(python), str(root / "tools" / "train_full_run.py"),
            "--stage", args.task, "--config", str(config), "--resume", args.resume,
        ]
        if args.initialize_from is not None:
            command += [
                "--initialize-from",
                _absolute_without_symlink_resolution(args.initialize_from),
            ]
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
