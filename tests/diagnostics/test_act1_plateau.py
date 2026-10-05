import hashlib
import json
import tomllib
from copy import deepcopy
from pathlib import Path

import pytest

from tools.diagnose_act1_corpus import validate_current_evaluation
from tools.prepare_act1_plateau import validate_config
from tools.submit_slurm import _parser, build_sbatch_command


def _evaluation():
    return {
        "schema": "sls-checkpoint-evaluation-v1", "checkpoint_sha256": "a" * 64,
        "profile": "IRONCLAD_A20_ACT1", "simulator": {"native_source_sha256": "b" * 64},
        "seed_range": [100, 102],
        "result": {
            "episodes": 2, "successes": 1, "success_rate": 0.5,
            "seed_results": [{"seed": 100, "success": True}, {"seed": 101, "success": False}],
            "backend_errors": 0, "backend_truncations": 0, "timeouts": 0,
            "step_limits": 0, "cycle_limits": 0,
        },
    }


def test_current_evaluation_rejects_wrong_environment_partial_and_unsafe_results():
    record = _evaluation()
    validate_current_evaluation(record, checkpoint_sha256="a" * 64, native_sha256="b" * 64)
    bad_records = []
    for key, value in (("checkpoint_sha256", "c" * 64), ("profile", "IRONCLAD_A0_ACT1")):
        bad = deepcopy(record)
        bad[key] = value
        bad_records.append(bad)
    bad = deepcopy(record)
    bad["simulator"]["native_source_sha256"] = "c" * 64
    bad_records.append(bad)
    for key, value in (("episodes", 1), ("backend_errors", 1), ("success_rate", 1.0),
                       ("success_rate", float("nan")),
                       ("seed_results", [{"seed": 100, "success": True}] * 2)):
        bad = deepcopy(record)
        bad["result"][key] = value
        bad_records.append(bad)
    for bad in bad_records:
        with pytest.raises(ValueError):
            validate_current_evaluation(bad, checkpoint_sha256="a" * 64, native_sha256="b" * 64)


def test_plateau_config_excludes_development_seeds_from_training_and_protects_source(tmp_path):
    path = Path("configs/diagnostics/ironclad_a20_act1_plateau.toml")
    config = tomllib.loads(path.read_text(encoding="utf-8"))
    validate_config(config, root=tmp_path)
    for changed in (
        {"diagnostic_seed_start": config["development_seed_start"]},
        {"output": config["checkpoint"].rsplit("/", 1)[0] + "/diagnosis"},
        {"output": "../outside"},
    ):
        with pytest.raises(ValueError):
            validate_config({**config, **changed}, root=tmp_path)


def test_plateau_slurm_dispatches_preparation_in_a_compute_job():
    config = Path("configs/diagnostics/ironclad_a20_act1_plateau.toml")
    args = _parser().parse_args(["plateau", "--config", str(config), "--constraint", "xgpg"])
    command = build_sbatch_command(args)
    assert "--partition=gpu" in command
    assert "--constraint=xgpg" in command
    assert "prepare_act1_plateau.py" in command[-1]
    assert "train_full_run.py" not in command[-1]


def test_reward_screening_configs_only_change_reward_and_output_identity():
    from sls.rl.ppo import PPOConfig
    from sls.rl.reward import WIN_REWARD_SCHEMA

    configs = [tomllib.loads(Path(f"configs/train/ironclad_a20_act1_plateau_{name}_2m.toml").read_text())
               for name in ("progress", "win")]
    control, win = configs
    for config in configs:
        PPOConfig(**config["ppo"])
    assert win["ppo"]["reward_schema"] == WIN_REWARD_SCHEMA
    assert win["ppo"]["failure_progress_scale"] == 0.0
    assert control["warm_start"] == win["warm_start"]
    assert control["stages"] == win["stages"]
    assert control["model"] == win["model"]
    assert {k: v for k, v in control["run"].items() if k not in {"output", "benchmark"}} == {
        k: v for k, v in win["run"].items() if k not in {"output", "benchmark"}}
    assert {k: v for k, v in control["ppo"].items() if k not in {"reward_schema", "failure_progress_scale"}} == {
        k: v for k, v in win["ppo"].items() if k not in {"reward_schema", "failure_progress_scale"}}


@pytest.mark.parametrize("unsafe", [False, True])
def test_compute_orchestration_saves_evidence_and_stops_before_corpus_on_bad_baseline(
    tmp_path, monkeypatch, unsafe,
):
    import torch

    from sls.rl import training_contract
    from tools import prepare_act1_plateau as pipeline

    monkeypatch.setattr(pipeline, "ROOT", tmp_path)
    monkeypatch.setattr(pipeline.sys, "platform", "linux")
    monkeypatch.setenv("SLURM_JOB_ID", "test")
    monkeypatch.setattr(torch.cuda, "is_available", lambda: True)
    monkeypatch.setattr(torch.cuda, "get_device_name", lambda _: "NVIDIA A100")
    monkeypatch.setattr(training_contract, "native_source_digest", lambda: "b" * 64)
    monkeypatch.setattr(training_contract, "native_artifact", lambda: {"source_sha256": "b" * 64})
    monkeypatch.setattr(training_contract, "git_state", lambda: {"commit": "test"})
    monkeypatch.setattr(training_contract, "training_implementation_digest", lambda: "c" * 64)
    source = tmp_path / "local/runs/parent/best.pt"
    source.parent.mkdir(parents=True)
    source.write_bytes(b"checkpoint fixture")
    config = tomllib.loads(Path("configs/diagnostics/ironclad_a20_act1_plateau.toml").read_text())
    config.update(checkpoint=str(source), checkpoint_sha256=hashlib.sha256(source.read_bytes()).hexdigest(),
                  native_source_sha256="b" * 64, development_episodes=2, diagnostic_episodes=2,
                  output="local/runs/plateau")
    config_path = tmp_path / "config.toml"
    config_path.write_text("\n".join(f"{k} = {json.dumps(v)}" for k, v in config.items()))
    (tmp_path / "tools").mkdir()
    for name in ("prepare_act1_plateau.py", "diagnose_act1_corpus.py", "analyze_act1_corpus.py"):
        (tmp_path / "tools" / name).write_text("# test fixture")
    executed = []

    class Child:
        def __init__(self, command, **kwargs):
            script = Path(command[1]).name
            executed.append(script)
            if "--output" not in command:
                return
            path = Path(command[command.index("--output") + 1])
            if script == "preflight_training.py":
                path.write_text(json.dumps({"ok": True, "native_source_sha256": "b" * 64}))
            elif script == "evaluate_checkpoint.py":
                start = int(command[command.index("--seed-start") + 1])
                record = _evaluation()
                record.update(checkpoint_sha256=config["checkpoint_sha256"], seed_range=[start, start + 2])
                result = record["result"]
                result["seed_results"] = [
                    {"seed": start, "success": True, "floor": 16},
                    {"seed": start + 1, "success": False, "floor": 16},
                ]
                result.update(success_rate_ci95=[0.1, 0.9], boss_entry_success_rate={})
                if unsafe:
                    result["backend_errors"] = 1
                path.write_text(json.dumps(record))
            elif script == "diagnose_act1_corpus.py":
                path.mkdir()
                (path / "analysis.json").write_text(json.dumps({"boss_entries": {}, "counters": {}}))
                (path / "selection.json").write_text("{}")
                (path / "result.json").write_text(json.dumps({"result": _evaluation()["result"], "server_differences": []}))

        def wait(self, **kwargs):
            return 0

        def poll(self):
            return 0

    monkeypatch.setattr(pipeline.subprocess, "Popen", Child)
    if unsafe:
        with pytest.raises(ValueError, match="backend_errors"):
            pipeline.main(["--config", str(config_path)])
    else:
        assert pipeline.main(["--config", str(config_path)]) == 0
    output = tmp_path / config["output"]
    status = json.loads((output / "status.json").read_text())
    assert status["status"] == ("FAILED" if unsafe else "COMPLETE")
    assert ("diagnose_act1_corpus.py" in executed) is (not unsafe)
    assert (output / "report.json").exists() is (not unsafe)
    with pytest.raises(FileExistsError):
        pipeline.main(["--config", str(config_path)])
