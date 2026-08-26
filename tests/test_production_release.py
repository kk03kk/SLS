from __future__ import annotations

from pathlib import Path
import tomllib

from sls.rl.episode_limit import EPISODE_LIMIT_SCHEMA
from sls.rl.reward import REWARD_SCHEMA
from sls.rl.training_contract import (
    ACT1_PRODUCTION_READINESS_LEVEL, ACT1_PRODUCTION_READINESS_LOCK,
    TRAINING_CHECKPOINT_SCHEMA, ROOT, source_sha256,
)
from sls.validation.readiness_lock import DEFAULT_LOCK
from tools import benchmark_workers, preflight_training
from tools.submit_slurm import TASK_CONFIGS


def test_all_nus_training_stages_share_one_strict_production_contract() -> None:
    expected_updates = {"smoke": 20, "pilot": 200, "train": 1000}
    outputs = set()
    relative_lock = ACT1_PRODUCTION_READINESS_LOCK.relative_to(ROOT).as_posix()
    assert ACT1_PRODUCTION_READINESS_LOCK.is_file()

    for task, relative in TASK_CONFIGS.items():
        path = ROOT / relative
        assert path.is_file(), (task, path)
        with path.open("rb") as stream:
            payload = tomllib.load(stream)
        run, ppo = payload["run"], payload["ppo"]
        assert run["profile"] == "IRONCLAD_A0_ACT1"
        assert run["require_readiness"] is True
        assert run["readiness_lock"] == relative_lock
        assert run["readiness_level"] == ACT1_PRODUCTION_READINESS_LEVEL
        assert run["device"] == "cuda"
        assert "allow_dirty" not in run
        assert int(run["updates"]) == expected_updates[task]
        assert int(run["workers"]) > 0
        assert int(run["save_interval"]) > 0
        assert int(run["evaluate_interval"]) > 0
        assert int(run["evaluation_seed_count"]) == 100
        assert int(run["evaluation_max_steps"]) == int(ppo["max_episode_steps"])
        assert ppo["reward_schema"] == REWARD_SCHEMA
        assert ppo["episode_limit_schema"] == EPISODE_LIMIT_SCHEMA
        output = str(run["output"])
        assert output not in outputs
        outputs.add(output)

    assert TRAINING_CHECKPOINT_SCHEMA == "sls-full-run-ppo-v3"


def test_preflight_and_benchmark_share_the_production_readiness_defaults() -> None:
    assert DEFAULT_LOCK == ACT1_PRODUCTION_READINESS_LOCK
    for args in (
        preflight_training._parser().parse_args([]),
        benchmark_workers._parser().parse_args([]),
    ):
        assert args.readiness_lock == ACT1_PRODUCTION_READINESS_LOCK
        assert args.readiness_level == ACT1_PRODUCTION_READINESS_LEVEL
        assert not args.allow_dirty


def test_source_evidence_hash_is_checkout_line_ending_independent(tmp_path: Path) -> None:
    lf = tmp_path / "lf.txt"
    crlf = tmp_path / "crlf.txt"
    lf.write_bytes(b"alpha\nbeta\n")
    crlf.write_bytes(b"alpha\r\nbeta\r\n")
    assert source_sha256(lf) == source_sha256(crlf)


def test_nus_documentation_uses_the_fixed_venv_and_real_production_modes() -> None:
    text = (ROOT / "docs" / "nus-training-zh.md").read_text(encoding="utf-8")
    assert "/home/h/hengzhi/venvs/sls/bin/python" in text
    assert "conda " not in text.lower()
    for task in ("preflight", "benchmark", "smoke", "pilot", "train"):
        assert f"tools/submit_slurm.py {task}" in text
    assert "--workers N --resume" in text
