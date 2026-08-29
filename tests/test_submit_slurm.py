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

    argv = [task, "--python", str(entry), "--dry-run"]
    args = _parser().parse_args(argv)
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

    args = _parser().parse_args([
        "preflight",
        "--python", "~/venvs/sls/bin/python",
    ])
    wrapped = _wrapped(build_sbatch_command(args, root=tmp_path / "repo"))

    assert wrapped[0] == os.path.abspath(str(entry))
    assert wrapped[0] != os.path.abspath(str(target))


def test_linux_home_example_is_preserved_exactly(monkeypatch: pytest.MonkeyPatch) -> None:
    expected = "/home/h/hengzhi/venvs/sls/bin/python"
    monkeypatch.setattr(os.path, "expanduser", lambda _value: expected)
    monkeypatch.setattr(os.path, "abspath", lambda value: value)
    args = _parser().parse_args([
        "preflight",
        "--python", "~/venvs/sls/bin/python",
    ])

    wrapped = _wrapped(build_sbatch_command(args, root=Path("/home/h/hengzhi/SLS")))

    assert wrapped[0] == expected
    assert "/usr/bin/python3.12" not in wrapped


def test_training_flags_config_and_slurm_defaults_are_unchanged(tmp_path: Path) -> None:
    python = tmp_path / "venv" / "bin" / "python"
    config = tmp_path / "custom.toml"
    config.write_text('[run]\nprofile = "IRONCLAD_A0_ACT1"\n', encoding="utf-8")
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
    assert wrapped[3] == str((tmp_path / "repo" / "configs" / "train" / "act1_train.toml").resolve())


def test_benchmark_submission_uses_the_guarded_benchmark_entrypoint(tmp_path: Path) -> None:
    python = tmp_path / "venv" / "bin" / "python"
    args = _parser().parse_args([
        "benchmark", "--python", str(python),
    ])
    wrapped = _wrapped(build_sbatch_command(args, root=tmp_path / "repo"))

    assert wrapped == [
        os.path.abspath(str(python)),
        str(tmp_path / "repo" / "tools" / "benchmark_workers.py"),
    ]


@pytest.mark.parametrize(
    "argv,script,config,extra,partition,walltime",
    (
        (("preflight",), "preflight_training.py", None, ("--jobs", "16"), "gpu", "03:00:00"),
        (("benchmark",), "benchmark_workers.py", None, (), "gpu", "03:00:00"),
        (("smoke", "--workers", "32"), "train_full_run.py", "act1_smoke.toml", ("--workers", "32"), "gpu", "03:00:00"),
        (("pilot", "--workers", "32"), "train_full_run.py", "act1_pilot.toml", ("--workers", "32"), "gpu", "03:00:00"),
        (("train", "--workers", "32"), "train_full_run.py", "act1_train.toml", ("--workers", "32"), "gpu-long", "3-00:00:00"),
        (("pilot", "--workers", "32", "--resume"), "train_full_run.py", "act1_pilot.toml", ("--workers", "32", "--resume", "auto"), "gpu", "03:00:00"),
        (("train", "--workers", "32", "--resume"), "train_full_run.py", "act1_train.toml", ("--workers", "32", "--resume", "auto"), "gpu-long", "3-00:00:00"),
    ),
)
def test_nus_production_command_matrix(
    tmp_path: Path, argv: tuple[str, ...], script: str, config: str | None,
    extra: tuple[str, ...], partition: str, walltime: str,
) -> None:
    root = tmp_path / "SLS"
    python = Path("/home/h/hengzhi/venvs/sls/bin/python")
    args = _parser().parse_args([*argv, "--python", str(python)])
    command = build_sbatch_command(args, root=root)
    wrapped = _wrapped(command)

    expected = [os.path.abspath(str(python)), str(root / "tools" / script)]
    if config is not None:
        expected += ["--config", str((root / "configs" / "train" / config).resolve())]
    expected += list(extra)
    assert wrapped == expected
    assert f"--partition={partition}" in command
    assert f"--time={walltime}" in command
    assert "--signal=B:TERM@300" in command


@pytest.mark.parametrize("task,option", (
    ("preflight", ("--workers", "32")),
    ("benchmark", ("--config", "custom.toml")),
))
def test_nontraining_jobs_reject_silently_ignored_training_options(
    task: str, option: tuple[str, str],
) -> None:
    args = _parser().parse_args([task, *option])
    with pytest.raises(ValueError, match="does not accept"):
        build_sbatch_command(args)
