from __future__ import annotations

import os
import shlex
from pathlib import Path

import pytest

from tools.submit_slurm import _parser, build_sbatch_command


def _wrapped(command: list[str]) -> list[str]:
    wrapped = shlex.split(command[command.index("--wrap") + 1])
    assert wrapped[0] == "exec"
    return wrapped[1:]


@pytest.mark.parametrize("task", ("preflight", "benchmark", "smoke", "pilot", "train"))
def test_all_tasks_preserve_virtualenv_python_path(tmp_path: Path, task: str) -> None:
    python = tmp_path / "venv" / "bin" / "python"
    args = _parser().parse_args([task, "--python", str(python)])
    wrapped = _wrapped(build_sbatch_command(args, root=tmp_path / "SLS"))
    assert wrapped[0] == os.path.abspath(str(python))


@pytest.mark.parametrize(
    "task,script,stage,partition,walltime",
    (
        ("preflight", "preflight_training.py", None, "gpu", "03:00:00"),
        ("benchmark", "benchmark_workers.py", None, "gpu", "03:00:00"),
        ("smoke", "train_full_run.py", "smoke", "gpu", "03:00:00"),
        ("pilot", "train_full_run.py", "pilot", "gpu", "03:00:00"),
        ("train", "train_full_run.py", "train", "gpu-long", "3-00:00:00"),
    ),
)
def test_nus_command_matrix(
    tmp_path: Path,
    task: str,
    script: str,
    stage: str | None,
    partition: str,
    walltime: str,
) -> None:
    root = tmp_path / "SLS"
    python = Path("/home/h/hengzhi/venvs/sls/bin/python")
    args = _parser().parse_args([task, "--python", str(python)])
    command = build_sbatch_command(args, root=root)
    wrapped = _wrapped(command)
    expected = [os.path.abspath(str(python)), str(root / "tools" / script)]
    if task == "preflight":
        expected += ["--jobs", "16"]
    elif stage is not None:
        expected += [
            "--stage", stage,
            "--config",
            str((root / "configs" / "train" / "ironclad_a0_fullrun.toml").resolve()),
        ]
    assert wrapped == expected
    assert f"--partition={partition}" in command
    assert f"--time={walltime}" in command
    assert "--account=allusers" in command
    assert "--qos=normal" in command
    assert "--gres=gpu:a100-40:1" in command
    assert "--cpus-per-task=16" in command
    assert "--mem=64G" in command
    assert "--signal=B:TERM@300" in command


def test_training_custom_config_is_forwarded(tmp_path: Path) -> None:
    config = tmp_path / "custom.toml"
    config.write_text("[run]\n", encoding="utf-8")
    args = _parser().parse_args(["pilot", "--config", str(config)])
    wrapped = _wrapped(build_sbatch_command(args, root=tmp_path / "SLS"))
    assert wrapped[-2:] == ["--config", str(config.resolve())]
    assert wrapped[wrapped.index("--stage") + 1] == "pilot"


def test_nontraining_jobs_reject_training_config() -> None:
    args = _parser().parse_args(["benchmark", "--config", "custom.toml"])
    with pytest.raises(ValueError, match="does not accept"):
        build_sbatch_command(args)
