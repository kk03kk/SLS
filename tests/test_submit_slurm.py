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


@pytest.mark.parametrize(
    "task", ("preflight", "benchmark", "evaluate", "compare", "corpus", "smoke", "pilot", "train"),
)
def test_all_tasks_preserve_virtualenv_python_path(tmp_path: Path, task: str) -> None:
    python = tmp_path / "venv" / "bin" / "python"
    arguments = [task, "--python", str(python)]
    if task == "compare":
        arguments.extend((
            "--checkpoint", str(tmp_path / "left.pt"),
            "--comparison-checkpoint", str(tmp_path / "right.pt"),
        ))
    elif task == "corpus":
        arguments.extend((
            "--diagnostic-run", str(tmp_path / "run"),
            "--diagnostic-output", str(tmp_path / "corpus"),
        ))
    args = _parser().parse_args(arguments)
    wrapped = _wrapped(build_sbatch_command(args, root=tmp_path / "SLS"))
    assert wrapped[0] == os.path.abspath(str(python))


@pytest.mark.parametrize(
    "task,script,stage,partition,walltime",
    (
        ("preflight", "preflight_training.py", None, "gpu", "03:00:00"),
        ("benchmark", "benchmark_workers.py", None, "gpu", "03:00:00"),
        ("evaluate", "evaluate_checkpoint.py", None, "gpu", "03:00:00"),
        ("compare", "compare_checkpoints.py", None, "gpu", "03:00:00"),
        ("corpus", "diagnose_act1_corpus.py", None, "gpu", "03:00:00"),
        ("smoke", "train_full_run.py", "smoke", "gpu-long", "1-00:00:00"),
        ("pilot", "train_full_run.py", "pilot", "gpu-long", "12:00:00"),
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
    arguments = [task, "--python", str(python)]
    if task == "compare":
        arguments.extend((
            "--checkpoint", str(root / "left.pt"),
            "--comparison-checkpoint", str(root / "right.pt"),
        ))
    elif task == "corpus":
        arguments.extend((
            "--diagnostic-run", str(root / "run"),
            "--diagnostic-output", str(root / "corpus"),
        ))
    args = _parser().parse_args(arguments)
    command = build_sbatch_command(args, root=root)
    wrapped = _wrapped(command)
    expected = [os.path.abspath(str(python)), str(root / "tools" / script)]
    if task == "preflight":
        expected += ["--jobs", "16"]
    elif task == "evaluate":
        expected += [
            str((root / "local" / "runs" / "ironclad-a0-fullrun-v2" / "latest.pt").resolve()),
            "--output",
            str((root / "local" / "runs" / "evaluations" / "latest-act1-current-sim-1000.json").resolve()),
            "--profile", "IRONCLAD_A0_ACT1", "--episodes", "1000",
            "--seed-start", "3000000000000", "--device", "cuda",
            "--environment-shards", "16",
        ]
    elif task == "compare":
        expected = [
            os.path.abspath(str(python)), str(root / "tools" / script),
            str((root / "left.pt").resolve()), str((root / "right.pt").resolve()),
            "--output", str((root / "local/runs/evaluations/paired-checkpoints.json").resolve()),
            "--profile", "IRONCLAD_A0_ACT1", "--episodes", "1000",
            "--seed-start", "3000000000000", "--device", "cuda",
            "--environment-shards", "16",
        ]
    elif task == "corpus":
        expected = [
            os.path.abspath(str(python)), str(root / "tools" / script),
            "--run", str((root / "run").resolve()),
            "--output", str((root / "corpus").resolve()),
            "--device", "cuda", "--analyze",
        ]
    elif stage is not None:
        expected += [
            "--stage", stage,
            "--config",
            str((root / "configs" / "train" / "ironclad_a0_fullrun.toml").resolve()),
            "--resume", "auto",
        ]
    assert wrapped == expected
    assert f"--partition={partition}" in command
    assert f"--time={walltime}" in command
    assert "--account=allusers" in command
    assert "--qos=normal" in command
    assert "--export=ALL,CUBLAS_WORKSPACE_CONFIG=:4096:8" in command
    assert "--gres=gpu:a100-40:1" in command
    assert "--cpus-per-task=16" in command
    assert "--mem=64G" in command
    assert "--signal=B:TERM@300" in command


def test_training_custom_config_is_forwarded(tmp_path: Path) -> None:
    config = tmp_path / "custom.toml"
    config.write_text("[run]\n", encoding="utf-8")
    args = _parser().parse_args(["pilot", "--config", str(config)])
    wrapped = _wrapped(build_sbatch_command(args, root=tmp_path / "SLS"))
    assert wrapped[wrapped.index("--config") + 1] == str(config.resolve())
    assert wrapped[-2:] == ["--resume", "auto"]
    assert wrapped[wrapped.index("--stage") + 1] == "pilot"


def test_node_constraint_is_forwarded_to_slurm(tmp_path: Path) -> None:
    args = _parser().parse_args(["train", "--constraint", "xgpg"])
    command = build_sbatch_command(args, root=tmp_path / "SLS")
    assert "--constraint=xgpg" in command


def test_nontraining_jobs_reject_training_config() -> None:
    args = _parser().parse_args(["evaluate", "--config", "custom.toml"])
    with pytest.raises(ValueError, match="does not accept"):
        build_sbatch_command(args)


def test_benchmark_layouts_are_forwarded(tmp_path: Path) -> None:
    args = _parser().parse_args([
        "benchmark", "--benchmark-layouts", "48:16",
    ])
    wrapped = _wrapped(build_sbatch_command(args, root=tmp_path / "SLS"))
    assert wrapped[-2:] == ["--layouts", "48:16"]


def test_training_rejects_benchmark_layouts() -> None:
    args = _parser().parse_args([
        "pilot", "--benchmark-layouts", "48:16",
    ])
    with pytest.raises(ValueError, match="only valid for benchmark"):
        build_sbatch_command(args)


def test_pilot_forwards_explicit_environment_migration(tmp_path: Path) -> None:
    args = _parser().parse_args(["pilot", "--resume", "environment-migration"])
    wrapped = _wrapped(build_sbatch_command(args, root=tmp_path / "SLS"))
    assert wrapped[-2:] == ["--resume", "environment-migration"]


def test_model_warm_start_submission_requires_source_and_config(tmp_path: Path) -> None:
    args = _parser().parse_args(["warm-start"])
    with pytest.raises(ValueError, match="requires"):
        build_sbatch_command(args)
    args = _parser().parse_args(["warm-start", "--checkpoint", "ten.pt", "--config", "fifteen.toml"])
    wrapped = _wrapped(build_sbatch_command(args, root=tmp_path))
    assert "--source" in wrapped and "--config" in wrapped
    assert wrapped[1].endswith("prepare_model_warm_start.py")


def test_evaluation_can_explicitly_select_fullrun(tmp_path: Path) -> None:
    args = _parser().parse_args(["evaluate", "--evaluation-profile", "IRONCLAD_A0_FULLRUN"])
    wrapped = _wrapped(build_sbatch_command(args, root=tmp_path))
    assert wrapped[wrapped.index("--profile") + 1] == "IRONCLAD_A0_FULLRUN"


def test_compare_requires_two_checkpoints(tmp_path: Path) -> None:
    args = _parser().parse_args(["compare", "--checkpoint", "left.pt"])
    with pytest.raises(ValueError, match="requires"):
        build_sbatch_command(args, root=tmp_path)


def test_corpus_requires_run_and_output(tmp_path: Path) -> None:
    args = _parser().parse_args(["corpus", "--diagnostic-run", "run"])
    with pytest.raises(ValueError, match="requires"):
        build_sbatch_command(args, root=tmp_path)
