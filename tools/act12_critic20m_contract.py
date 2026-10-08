"""Hash-bound new recipe validation, safe on a login node."""
from __future__ import annotations

import json
import tomllib
from pathlib import Path

from sls.rl.critic_warmup import CriticWarmupConfig
from sls.rl.training_contract import (
    native_source_digest,
    source_sha256,
    training_implementation_digest,
)

ROOT = Path(__file__).resolve().parents[1]
PLAN = "configs/experiments/act12-critic20m-r1.json"
PERIODIC = (8000012000000, 8000012000512)
CONFIRMATION = (8000013000000, 8000013004096)
OPERATOR_PATHS = (
    "tools/act12_critic20m_contract.py", "tools/submit_act12_critic20m.py",
    "tools/run_act12_critic20m.py", "tools/verify_act12_critic_warmup.py",
    "tools/analyze_act12_critic20m.py", "tools/import_act12_parent.py",
    "tools/operator_paths.py", "tools/preflight_training.py", "tools/benchmark_workers.py",
    "tools/submit_slurm.py", "tools/prepare_act12_long_run.py",
    "tools/analyze_act12_pilot.py", "tools/analyze_reward_screen.py", "tools/compare_run_arms.py",
    "requirements/model.lock", "pyproject.toml",
    "tools/check_training_configs.py",
    "tests/fixtures/regressions/act2-empty-cage-grid-131100069.json",
)


def safe_path(root, relative):
    path = (root / relative).resolve()
    if not path.is_relative_to(root.resolve()):
        raise ValueError("recipe path escapes repository")
    return path


def validate_compute_gate(gate, identity):
    checks = gate.get("checks", {})
    if (gate.get("schema") != "sls-critic20m-compute-gate-v1"
            or gate.get("ok") is not True
            or gate.get("training_identity_sha256") != identity
            or gate.get("production_probe_updates_discarded") is not True
            or any(checks.get(k) != "PASS" for k in
                   ("grid", "cross-rollout", "last-warmup", "actor_and_gru_frozen", "first-ppo"))
            or type(checks.get("complete_return_states_checked")) is not int
            or checks["complete_return_states_checked"] <= 0):
        raise ValueError("missing, incomplete or stale compute acceptance")


def validate_config(config, control):
    if set(config) != set(control) | {"critic_warmup"} or set(config["stages"]) != {"train"}:
        raise ValueError("unregistered recipe sections")
    if config["model"] != control["model"] or config["ppo"] != control["ppo"] or config["warm_start"] != control["warm_start"]:
        raise ValueError("recipe changes model/PPO/reward/parent beyond the warmup intervention")
    warm = CriticWarmupConfig(**config["critic_warmup"])
    if warm.to_dict() != {"rollout_updates": 32, "epochs": 2, "batch_size": 1024}:
        raise ValueError("registered warmup differs")
    run = config["run"]
    base = "ironclad-a20-act12-critic20m-r1"
    if (run["output"] != "local/runs/" + base
            or run["benchmark"] != "local/runs/preparation/" + base + "/benchmark.json"
            or run["compute_gate_report"] != "local/runs/preparation/" + base + "/compute-gate.json"
            or run["preparation_wall_hours"] != 48):
        raise ValueError("unregistered recipe output/preparation paths")
    allowed = {"output", "benchmark", "evaluation_health_policy", "periodic_evaluation_seed_start",
               "periodic_evaluation_seed_count", "final_evaluation_seed_start",
               "final_evaluation_seed_count", "preparation_wall_hours", "compute_gate_report"}
    if {k: v for k, v in run.items() if k not in allowed} != {
            k: v for k, v in control["run"].items() if k not in allowed}:
        raise ValueError("recipe changes normal-start or worker contract")
    if (run["profile"] != "IRONCLAD_A20_ACT2" or run["worker_layout"] != [64, 16]
            or run["evaluation_health_policy"] != "execution-only-v1"
            or run["seed"] != 130000000
            or (run["periodic_evaluation_seed_start"], run["periodic_evaluation_seed_count"]) != (PERIODIC[0], 512)
            or (run["final_evaluation_seed_start"], run["final_evaluation_seed_count"]) != (CONFIRMATION[0], 4096)):
        raise ValueError("registered training/evaluation layout changed")
    stage = config["stages"]["train"]
    expected = {**control["stages"]["train"], "target_environment_steps": 110013696,
                "evaluate_every_steps": 2000000, "checkpoint_every_steps": 1000000,
                "minimum_evaluation_episodes": 512, "minimum_final_evaluation_episodes": 4096}
    if stage != expected or config["ppo"]["gamma"] != 1:
        raise ValueError("registered budget or stage changed")


def validate(plan_path=None, *, root=ROOT, check_sources=True):
    path = safe_path(root, plan_path or PLAN)
    plan = json.loads(path.read_text(encoding="utf-8"))
    if (plan.get("schema") != "sls-act12-critic20m-plan-v1"
            or plan.get("status") != "READY_FOR_COMPUTE_GATE"
            or plan.get("allocations") != 3 or plan.get("allocation_hours") != 48
            or plan.get("additional_decisions") != 20000000
            or plan.get("benchmark_rate") != 114.7
            or plan["parent"]["sha256"] != "274963f4fe32b75003c5a1b4ccd394b5185304156e6ea22a76aee2764d54f1e0"):
        raise ValueError("invalid registered critic20m plan")
    config_path = safe_path(root, plan["config"])
    if source_sha256(config_path) != plan["config_sha256"]:
        raise ValueError("bound config changed")
    config = tomllib.loads(config_path.read_text(encoding="utf-8"))
    if (plan["parent"]["checkpoint"] != config["warm_start"]["checkpoint"]
            or plan["parent"]["environment_steps"] != config["warm_start"]["parent_environment_steps"]):
        raise ValueError("parent binding differs from config")
    control = tomllib.loads((root / "configs/train/ironclad_a20_act12_lambda098_r1.toml").read_text())
    validate_config(config, control)
    if set(plan["operator_sha256"]) != set(OPERATOR_PATHS):
        raise ValueError("operator source binding is incomplete")
    if (plan["native_source_sha256"] != "b100427d1e3ae6eee05818b6904047049609b1aa70807872c1f5f5e0edd6d62f"
            or plan["parent"]["target_native_source_sha256"] != plan["native_source_sha256"]):
        raise ValueError("new recipe must use the corrected lambda-study environment")
    for name, sha in plan["operator_sha256"].items():
        if source_sha256(safe_path(root, name)) != sha:
            raise ValueError("bound operator changed: " + name)
    if check_sources and (plan["native_source_sha256"] != native_source_digest()
                          or plan["training_implementation_sha256"] != training_implementation_digest()):
        raise ValueError("bound training/native source changed")
    return plan, config_path, config
