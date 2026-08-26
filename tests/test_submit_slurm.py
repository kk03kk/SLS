from __future__ import annotations

import os
from pathlib import Path
import shlex

import pytest

from tools.submit_slurm import _parser, build_sbatch_command


def _wrapped(command: list[str]) -> list[str]:
    return shlex.split(command[command.index("--wrap") + 1])


def _simulate_symlink_resolution(
    monkeypatch: pytest.MonkeyPatch, entry: Path, target: Path,
) -> None:
    original = Path.resolve

    def resolve(path: Path, *args: object, **kwargs: object) -> Path:
        if path == entry:
            return Path(os.path.abspath(str(target)))
        return original(path, *args, **kwargs)

    monkeypatch.setattr(Path, "resolve", resolve)


@pytest.mark.parametrize("task", ("preflight", "benchmark", "smoke", "pilot", "train"))
def test_all_tasks_preserve_virtualenv_python_symlink(
    tmp_path: Path, monkeypatch: pytest.MonkeyPatch, task: str,
) -> None:
    target = tmp_path / "system-python"
    target.write_text("", encoding="utf-8")
    environment = tmp_path / "venv" / "bin"
    environment.mkdir(parents=True)
    entry = environment / "python"
    entry.write_text("", encoding="utf-8")
    _simulate_symlink_resolution(monkeypatch, entry, target)
    assert entry.resolve() == Path(os.path.abspath(str(target)))

    args = _parser().parse_args([task, "--python", str(entry), "--dry-run"])
    wrapped = _wrapped(build_sbatch_command(args, root=tmp_path / "repo"))

    assert wrapped[0] == os.path.abspath(str(entry))
    assert wrapped[0] != os.path.abspath(str(target))


def test_python_path_expands_home_without_resolving_symlink(
    tmp_path: Path, monkeypatch: pytest.MonkeyPatch,
) -> None:
    home = tmp_path / "home"
    target = tmp_path / "system-python"
    target.write_text("", encoding="utf-8")
    entry = home / "venvs" / "sls" / "bin" / "python"
    entry.parent.mkdir(parents=True)
    entry.write_text("", encoding="utf-8")
    _simulate_symlink_resolution(monkeypatch, entry, target)
    monkeypatch.setenv("HOME", str(home))
    monkeypatch.setenv("USERPROFILE", str(home))

    args = _parser().parse_args(["preflight", "--python", "~/venvs/sls/bin/python"])
    wrapped = _wrapped(build_sbatch_command(args, root=tmp_path / "repo"))

    assert wrapped[0] == os.path.abspath(str(entry))
    assert wrapped[0] != os.path.abspath(str(target))


def test_linux_home_example_is_preserved_exactly(monkeypatch: pytest.MonkeyPatch) -> None:
    expected = "/home/h/hengzhi/venvs/sls/bin/python"
    monkeypatch.setattr(os.path, "expanduser", lambda _value: expected)
    monkeypatch.setattr(os.path, "abspath", lambda value: value)
    args = _parser().parse_args(["preflight", "--python", "~/venvs/sls/bin/python"])

    wrapped = _wrapped(build_sbatch_command(args, root=Path("/home/h/hengzhi/SLS")))

    assert wrapped[0] == expected
    assert "/usr/bin/python3.12" not in wrapped


def test_training_flags_config_and_slurm_defaults_are_unchanged(tmp_path: Path) -> None:
    python = tmp_path / "venv" / "bin" / "python"
    config = tmp_path / "custom.toml"
    args = _parser().parse_args([
        "pilot", "--python", str(python), "--workers", "24", "--resume",
        "--config", str(config),
    ])
    command = build_sbatch_command(args, root=tmp_path / "repo")
    wrapped = _wrapped(command)

    assert wrapped == [
        os.path.abspath(str(python)), str(tmp_path / "repo" / "tools" / "train_full_run.py"),
        "--config", str(config.resolve()), "--workers", "24", "--resume", "auto",
    ]
    assert "--account=allusers" in command
    assert "--qos=normal" in command
    assert "--partition=gpu" in command
    assert "--gres=gpu:a100-40:1" in command
    assert "--cpus-per-task=16" in command
    assert "--mem=64G" in command
    assert "--time=03:00:00" in command
    assert "--signal=B:TERM@300" in command


def test_train_keeps_long_partition_and_default_config(tmp_path: Path) -> None:
    args = _parser().parse_args(["train", "--python", str(tmp_path / "python")])
    command = build_sbatch_command(args, root=tmp_path / "repo")
    wrapped = _wrapped(command)
    assert "--partition=gpu-long" in command
    assert "--time=3-00:00:00" in command
    assert wrapped[3] == str((tmp_path / "repo" / "configs" / "train" / "full_run.toml").resolve())


def test_benchmark_submission_uses_the_guarded_benchmark_entrypoint(tmp_path: Path) -> None:
    python = tmp_path / "venv" / "bin" / "python"
    args = _parser().parse_args(["benchmark", "--python", str(python)])
    wrapped = _wrapped(build_sbatch_command(args, root=tmp_path / "repo"))

    assert wrapped == [
        os.path.abspath(str(python)),
        str(tmp_path / "repo" / "tools" / "benchmark_workers.py"),
    ]
