"""Bind an Act1-2 pilot to a completed, reviewed 90M parent; never submit a job."""

from __future__ import annotations

import argparse
import copy
import json
import sys
import tomllib
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "src"))
sys.path.insert(0, str(ROOT))

import torch

from sls.curriculum import IRONCLAD_A20_ACT1
from sls.model import ModelConfig
from sls.rl.act1_transfer import validate_curriculum_simulator_transition
from sls.rl.checkpoint import policy_from_training_checkpoint
from sls.rl.ppo import PPOConfig
from sls.rl.training_contract import (
    native_source_digest,
    sha256_file,
    source_sha256,
    training_implementation_digest,
)
from tools.check_training_configs import validate

RECIPE = ROOT / "configs/experiments/act12-win-pilot-recipe.json"
HEALTH = ("backend_errors", "backend_truncations", "step_limits", "cycle_limits", "timeouts")


def repository_path(root: Path, value: str | Path) -> Path:
    path = (root / value).resolve()
    if not path.is_relative_to(root.resolve()):
        raise ValueError("path must remain within the repository")
    return path


def _relative(root: Path, path: Path) -> str:
    return path.resolve().relative_to(root.resolve()).as_posix()


def inspect_parent(parent: Path, role: str, *, root: Path = ROOT,
                   simulator_transition: dict | None = None) -> tuple[dict, dict, dict]:
    parent = repository_path(root, parent)
    if role not in {"endpoint", "selected"}:
        raise ValueError("parent role must be endpoint or selected")
    preliminary = json.loads((parent / "run-manifest.json").read_text(encoding="utf-8"))
    if (preliminary.get("status") != "COMPLETE"
            or preliminary.get("stages", {}).get("train", {}).get("status") != "COMPLETE"):
        raise ValueError("parent training is not complete; a prepared directory is not a 90M result")
    bundle = json.loads((parent / "training-bundle.json").read_text(encoding="utf-8"))
    files = bundle["files"]
    chosen = "final.pt" if role == "endpoint" else "stages/train/selection/best_progress.pt"
    evaluation = "endpoint-evaluation.json" if role == "endpoint" else "final-evaluation.json"
    evidence = ("run-manifest.json", "training-config.toml", chosen, evaluation)
    if role == "selected":
        evidence += ("stages/train/selection/best_progress.json",)
        if (parent / "stages/train/selection/best_progress.pending.json").exists():
            raise ValueError("parent checkpoint selection is incomplete")
    for name in evidence:
        path = repository_path(root, parent / name)
        if not files.get(name) or sha256_file(path) != files[name]:
            raise ValueError(f"parent bundle mismatch: {name}")
    manifest = json.loads((parent / "run-manifest.json").read_text(encoding="utf-8"))
    config = tomllib.loads((parent / "training-config.toml").read_text(encoding="utf-8"))
    stage = manifest.get("stages", {}).get("train", {})
    if (manifest.get("status") != "COMPLETE" or stage.get("status") != "COMPLETE"
            or stage.get("profile") != "IRONCLAD_A20_ACT1"
            or config["run"]["profile"] != "IRONCLAD_A20_ACT1"
            or int(config["stages"]["train"]["target_environment_steps"]) < 90_000_000
            or int(stage.get("completed_environment_steps", 0))
            < int(config["stages"]["train"]["target_environment_steps"])):
        raise ValueError("a completed A20 Act1 run targeting at least 90M is required")
    native = manifest["native_source_sha256"]
    target_native = native_source_digest()
    validate_curriculum_simulator_transition(
        {"transfer_kind": "curriculum-stage", "simulator_transition": simulator_transition},
        native, target_native, root=root,
    )
    result = json.loads((parent / evaluation).read_text(encoding="utf-8"))
    if (result.get("checkpoint_sha256") != files[chosen]
            or result.get("evaluation_role") != "development-confirmation"
            or result.get("simulator", {}).get("native_source_sha256") != native
            or any(result["result"].get(key, -1) != 0 for key in HEALTH)):
        raise ValueError("parent requires registered healthy development confirmation")
    payload = torch.load(parent / chosen, map_location="cpu", weights_only=False)
    policy = policy_from_training_checkpoint(payload)
    if (payload["contract"]["profile"] != IRONCLAD_A20_ACT1
            or payload["contract"]["native_source_sha256"] != native
            or policy.config != ModelConfig(**config["model"])
            or payload["contract"]["ppo"] != PPOConfig(**config["ppo"]).to_dict()
            or payload["contract"]["training_config_sha256"] != manifest["training_identity_sha256"]
            or int(payload["trainer"]["environment_steps"])
            != int(result["checkpoint_environment_steps"])):
        raise ValueError("parent model/profile/step identity mismatch")
    steps = int(payload["trainer"]["environment_steps"])
    if role == "endpoint" and steps != int(stage["completed_environment_steps"]):
        raise ValueError("endpoint does not match the completed parent budget")
    if role == "selected":
        best = json.loads((parent / "stages/train/selection/best_progress.json").read_text(encoding="utf-8"))
        if (best.get("selection_objective") != "ACT1_CLEAR_COUNT"
                or best.get("checkpoint_sha256") != files[chosen]
                or best.get("environment_steps") != steps
                or not 0 < steps <= int(stage["completed_environment_steps"])):
            raise ValueError("selected parent does not match its registered selection")
    record = {
        "run": _relative(root, parent), "role": role,
        "checkpoint": _relative(root, parent / chosen), "sha256": files[chosen],
        "environment_steps": int(payload["trainer"]["environment_steps"]),
        "native_source_sha256": native,
        "target_native_source_sha256": target_native,
        "training_implementation_sha256": manifest["training_implementation_sha256"],
        "evidence": {name: files[name] for name in evidence},
        **({"simulator_transition": simulator_transition} if simulator_transition else {}),
    }
    return config, record, result


def build_configuration(original: dict, parent: dict, recipe: dict, *, run_name: str) -> dict:
    if not run_name or any(c not in "abcdefghijklmnopqrstuvwxyz0123456789-" for c in run_name):
        raise ValueError("run name must contain lowercase letters, digits or hyphens")
    config = {"run": {}, "stages": {"train": {}}, "model": copy.deepcopy(original["model"]),
              "ppo": copy.deepcopy(original["ppo"])}
    if config["ppo"].get("reward_schema") != "sls-curriculum-win-v1" or (
        config["ppo"].get("failure_progress_scale") != 0 or config["ppo"].get("gamma") != 1
    ):
        raise ValueError("Act1-2 parent must use the registered Win objective")
    if recipe["source_profile"] != "IRONCLAD_A20_ACT1" or recipe["target_profile"] != "IRONCLAD_A20_ACT2":
        raise ValueError("recipe must describe adjacent A20 Act1 to Act2 transfer")
    budget = int(recipe["additional_decisions"])
    if budget <= 0:
        raise ValueError("additional decisions must be positive")
    config["run"] = {
        "workflow": "single-stage", "profile": "IRONCLAD_A20_ACT2",
        "seed": recipe["training_seed"], "training_seed_limit": recipe["training_seed_limit"],
        "device": "cuda", "deterministic": True, "worker_backend": "sharded-vector",
        "worker_layout": recipe["worker_layout"],
        "benchmark": f"local/runs/preparation/{run_name}/benchmark.json",
        "output": f"local/runs/{run_name}", "selection_progress_guard": True,
        "final_evaluation_role": "development-confirmation", "evaluate_fixed_endpoint": True,
        "development_reference_checkpoint": parent["checkpoint"],
        "development_reference_sha256": parent["sha256"],
        "development_reference_profile": "IRONCLAD_A20_ACT1",
        "periodic_evaluation_seed_start": recipe["periodic_seed_start"],
        "periodic_evaluation_seed_count": recipe["periodic_seed_count"],
        "final_evaluation_seed_start": recipe["confirmation_seed_start"],
        "final_evaluation_seed_count": recipe["confirmation_seed_count"],
        "evaluation_max_steps": original["run"]["evaluation_max_steps"],
        "preparation_wall_hours": recipe["wall_limit_hours"],
        "preparation_safety_factor": recipe.get("throughput_safety_factor", 1.5),
        "preparation_evaluation_reserve_hours": recipe.get("evaluation_reserve_hours", 4),
    }
    config["stages"]["train"] = {
        "profile": "IRONCLAD_A20_ACT2",
        "target_environment_steps": parent["environment_steps"] + budget,
        "evaluate_every_steps": recipe["evaluate_every_steps"],
        "checkpoint_every_steps": recipe["checkpoint_every_steps"],
        "minimum_success_rate": 0.0, "minimum_reached_act2_rate": 0.0,
        "minimum_reached_act3_rate": 0.0,
        "minimum_evaluation_episodes": recipe["periodic_seed_count"],
        "minimum_final_evaluation_episodes": recipe["confirmation_seed_count"],
    }
    config["warm_start"] = {"transfer_kind": "curriculum-stage", "checkpoint": parent["checkpoint"],
                            "checkpoint_sha256": parent["sha256"],
                            "parent_environment_steps": parent["environment_steps"],
                            **({"simulator_transition": parent["simulator_transition"]}
                               if parent.get("simulator_transition") else {})}
    errors = validate(Path(run_name), config)
    reserved_start, reserved_stop = recipe["reserved_final_seeds"]
    exposed = [(8_000_000_000_000, 8_000_000_000_512),
               (8_000_002_000_000, 8_000_002_002_048),
               (8_000_003_000_000, 8_000_003_002_048),
               (8_000_004_000_000, 8_000_004_000_512),
               (8_000_005_000_000, 8_000_005_000_032),
               (8_000_008_000_000, 8_000_008_000_032)]
    for key in ("periodic", "final"):
        start = original["run"][f"{key}_evaluation_seed_start"]
        exposed.append((start, start + original["run"][f"{key}_evaluation_seed_count"]))
    for start, count in ((recipe["periodic_seed_start"], recipe["periodic_seed_count"]),
                         (recipe["confirmation_seed_start"], recipe["confirmation_seed_count"])):
        stop = start + count
        if start < reserved_stop and reserved_start < stop:
            errors.append("development seeds overlap reserved final seeds")
        if any(start < old_stop and old_start < stop for old_start, old_stop in exposed):
            errors.append("development seeds overlap exposed parent/probe seeds")
    if errors:
        raise ValueError("invalid pilot configuration: " + "; ".join(errors))
    return config


def configuration_toml(config: dict) -> str:
    """Serialize the scalar/list sections used by our training configs; no dependency."""
    sections = [("run", config["run"]), ("stages.train", config["stages"]["train"]),
                ("model", config["model"]), ("ppo", config["ppo"]),
                ("warm_start", {k: v for k, v in config["warm_start"].items() if not isinstance(v, dict)})]
    if config["warm_start"].get("simulator_transition"):
        sections.append(("warm_start.simulator_transition", config["warm_start"]["simulator_transition"]))
    return "\n\n".join("[" + name + "]\n" + "\n".join(
        f"{key} = {json.dumps(value, ensure_ascii=False, allow_nan=False)}" for key, value in values.items()
    ) for name, values in sections) + "\n"


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--parent-run", type=Path, required=True)
    parser.add_argument("--parent-role", choices=("endpoint", "selected"), required=True)
    parser.add_argument("--decision-note", required=True, help="Evidence-based rationale after 90M analysis")
    parser.add_argument("--recipe", type=Path, default=RECIPE)
    parser.add_argument("--run-name", default="ironclad-a20-act12-win-pilot-r1")
    parser.add_argument("--config", type=Path, default=ROOT / "configs/train/ironclad_a20_act12_win_pilot.toml")
    parser.add_argument("--plan", type=Path, default=ROOT / "configs/experiments/act12-win-pilot.json")
    args = parser.parse_args()
    if len(args.decision_note.strip()) < 20:
        raise ValueError("record a substantive parent-selection rationale")
    recipe = json.loads(args.recipe.read_text(encoding="utf-8"))
    transition = None
    if recipe.get("simulator_transition_evidence"):
        path = repository_path(ROOT, recipe["simulator_transition_evidence"])
        transition = {"evidence": _relative(ROOT, path), "evidence_sha256": sha256_file(path)}
    original, parent, _ = inspect_parent(args.parent_run, args.parent_role, simulator_transition=transition)
    config = build_configuration(original, parent, recipe, run_name=args.run_name)
    config_path, plan_path = repository_path(ROOT, args.config), repository_path(ROOT, args.plan)
    if config_path == plan_path or config_path.exists() or plan_path.exists():
        raise FileExistsError("configuration/plan already exists; preserve prior experiment evidence")
    if repository_path(ROOT, config["run"]["output"]).exists():
        raise FileExistsError("target run already exists")
    config_path.parent.mkdir(parents=True, exist_ok=True)
    plan_path.parent.mkdir(parents=True, exist_ok=True)
    with config_path.open("x", encoding="utf-8", newline="\n") as stream:
        stream.write(configuration_toml(config))
    plan = {"schema": "sls-act12-bound-plan-v1", "status": "READY_FOR_LOCAL_VALIDATION",
            "config": _relative(ROOT, config_path), "config_sha256": source_sha256(config_path),
            "target_training_implementation_sha256": training_implementation_digest(),
            "parent": parent, "recipe": recipe, "recipe_sha256": source_sha256(args.recipe),
            "recipe_path": _relative(ROOT, args.recipe),
            "decision_note": args.decision_note, "wall_limit_hours": recipe["wall_limit_hours"]}
    plan["operator_sources"] = {name: source_sha256(ROOT / name) for name in (
        "tools/prepare_act12_pilot.py", "tools/submit_act12_pilot.py",
        "tools/prepare_and_train.py", "tools/submit_slurm.py",
        "tools/import_act12_parent.py", "tools/analyze_act12_pilot.py",
    )}
    with plan_path.open("x", encoding="utf-8", newline="\n") as stream:
        stream.write(json.dumps(plan, indent=2, ensure_ascii=False) + "\n")
    print(json.dumps({"config": str(config_path), "plan": str(plan_path), "submitted": False}))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
