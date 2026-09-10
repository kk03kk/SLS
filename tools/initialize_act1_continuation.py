"""Fork a verified best checkpoint into a new Act1 budget, without resetting learning.

Run budget/evaluation bookkeeping and an explicit half-LR experiment may change.
Model, other PPO/reward settings, environment and worker layout remain strict.
The source run is read-only; child creation is atomic.
"""

from __future__ import annotations

import argparse
import json
import os
import shutil
import sys
import tempfile
import time
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
# Direct script execution puts tools/, not the repository, on sys.path.
sys.path.insert(0, str(ROOT))
sys.path.insert(0, str(ROOT / "src"))


def initialize(config_path: Path, *, root: Path = ROOT):
    import torch

    from sls.curriculum import IRONCLAD_A0_ACT1
    from sls.model import ModelConfig
    from sls.rl.ppo import PPOConfig
    from sls.rl.preparation import read_config, training_seed_limit
    from sls.rl.training_contract import native_source_digest, sha256_file
    from tools.train_full_run import MANIFEST_SCHEMA, _training_identity

    config = read_config(config_path)
    run = config["run"]
    source = (root / run["continuation_from"]).resolve()
    target = (root / run["output"]).resolve()
    if (
        source == target
        or target.is_relative_to(source)
        or source.is_relative_to(target)
    ):
        raise ValueError("continuation must use a separate sibling run directory")
    path = source / "stages/train/selection/best_progress.pt"
    digest = sha256_file(path)
    if digest != run["continuation_checkpoint_sha256"]:
        raise ValueError("parent best checkpoint does not match the pinned SHA256")
    original = read_config(source / "training-config.toml")
    final = json.loads((source / "final-evaluation.json").read_text(encoding="utf-8"))
    if final["checkpoint_sha256"] != digest:
        raise ValueError("parent best has no matching final evaluation")
    allowed_run = {
        "output",
        "benchmark",
        "continuation_from",
        "continuation_checkpoint_sha256",
        "final_evaluation_seed_start",
        "final_evaluation_seed_count",
        "training_seed_limit",
    }
    for key in set(run) | set(original["run"]):
        if key not in allowed_run and run.get(key) != original["run"].get(key):
            raise ValueError("unapproved continuation run setting: " + key)
    allowed_stage = {
        "target_environment_steps",
        "evaluate_every_steps",
        "checkpoint_every_steps",
        "minimum_final_evaluation_episodes",
    }
    if set(config["stages"]) != {"train"} or set(original["stages"]) != {"train"}:
        raise ValueError("continuation requires single-stage Act1")
    for key in set(config["stages"]["train"]) | set(original["stages"]["train"]):
        if key not in allowed_stage and config["stages"]["train"].get(key) != original[
            "stages"
        ]["train"].get(key):
            raise ValueError("unapproved continuation stage setting: " + key)
    old_lr = float(original["ppo"]["learning_rate"])
    new_lr = float(config["ppo"]["learning_rate"])
    if (
        config["model"] != original["model"]
        or {k: v for k, v in config["ppo"].items() if k != "learning_rate"}
        != {k: v for k, v in original["ppo"].items() if k != "learning_rate"}
        or new_lr not in (old_lr, old_lr / 2)
    ):
        raise ValueError(
            "unapproved model/PPO/reward change; only the explicit half-LR branch is supported"
        )
    payload = torch.load(path, map_location="cpu", weights_only=False)
    contract = payload["contract"]
    workers, shards = contract["workers"], contract["worker_shards"]
    if (
        contract["native_source_sha256"] != native_source_digest()
        or contract["profile"] != IRONCLAD_A0_ACT1
        or contract["model"] != ModelConfig(**config["model"]).to_dict()
        or contract["ppo"] != PPOConfig(**original["ppo"]).to_dict()
        or contract["training_config_sha256"]
        != _training_identity(original, workers=workers, shards=shards)
    ):
        raise ValueError("parent environment/model/training contract mismatch")
    layout = json.loads((root / run["benchmark"]).read_text(encoding="utf-8"))
    if [layout["selected_workers"], layout["selected_shards"]] != [workers, shards]:
        raise ValueError("continuation must retain the original worker layout")
    steps = int(payload["trainer"]["environment_steps"])
    best_record = json.loads(
        (source / "stages/train/selection/best_progress.json").read_text(
            encoding="utf-8"
        )
    )
    if (
        best_record["environment_steps"] != steps
        or best_record["update"] != payload["trainer"]["update"]
    ):
        raise ValueError("parent best selection metadata disagrees with checkpoint")
    if not steps < int(config["stages"]["train"]["target_environment_steps"]):
        raise ValueError(
            "target is cumulative steps and must exceed the parent step count"
        )
    identity = _training_identity(config, workers=workers, shards=shards)
    config_sha = sha256_file(config_path)
    provenance = {
        "schema": "sls-act1-state-preserving-continuation-v1",
        "parent_checkpoint_sha256": digest,
        "parent_environment_steps": steps,
        "parent_update": payload["trainer"]["update"],
        "parent_path": str(path),
        "config_sha256": config_sha,
        "old_training_identity": contract["training_config_sha256"],
        "new_training_identity": identity,
        "old_training_seed_limit": contract["training_seed_limit"],
        "preserved": "model, Adam moments/step counters, recurrent memory, episode limits, workers and RNG; no step/seed reset",
        "old_learning_rate": old_lr,
        "new_learning_rate": new_lr,
        "exact_resume_of_parent_experiment": False,
    }
    if target.exists():
        marker = json.loads((target / "continuation.json").read_text(encoding="utf-8"))
        if (
            marker["parent_checkpoint_sha256"] != digest
            or marker["config_sha256"] != config_sha
        ):
            raise ValueError(
                "existing continuation belongs to another parent/configuration"
            )
        if not (target / "latest.pt").is_file():
            raise ValueError("existing continuation has no latest checkpoint")
        return marker
    new_limit = training_seed_limit(run)
    if new_limit > contract["training_seed_limit"]:
        raise ValueError(
            "continuation may not expose historical held-out seeds to training"
        )
    if int(payload["trainer"]["next_seed"]) >= new_limit:
        raise ValueError("continuation training seeds overlap evaluation")
    contract["training_config_sha256"] = identity
    contract["training_seed_limit"] = new_limit
    contract["ppo"] = PPOConfig(**config["ppo"]).to_dict()
    for group in payload["optimizer"]["param_groups"]:
        if group["lr"] != old_lr:
            raise ValueError("parent optimizer LR disagrees with its contract")
        group["lr"] = new_lr
    provenance["new_training_seed_limit"] = new_limit
    target.parent.mkdir(parents=True, exist_ok=True)
    with tempfile.TemporaryDirectory(
        prefix="act1-continuation-", dir=target.parent
    ) as temporary:
        staging = Path(temporary) / "run"
        staging.mkdir()
        torch.save(payload, staging / "latest.pt")
        shutil.copy2(config_path, staging / "training-config.toml")
        (staging / "continuation.json").write_text(
            json.dumps(provenance, indent=2) + "\n", encoding="utf-8"
        )
        selection = staging / "stages/train/selection"
        selection.mkdir(parents=True)
        shutil.copy2(staging / "latest.pt", selection / "best_progress.pt")
        shutil.copy2(
            source / "stages/train/selection/best_progress.json",
            selection / "best_progress.json",
        )
        manifest = {
            "schema": MANIFEST_SCHEMA,
            "created_unix": time.time(),
            "status": "CONTINUATION_READY",
            "stages": {},
            "simulator_only": True,
            "profile": run["profile"],
            "workflow": run["workflow"],
            "curriculum": {"train": run["profile"]},
            "checkpoint_schema": payload["schema"],
            "encoding_schema": contract["encoding_schema"],
            "vocabulary_sha256": contract["vocabulary_sha256"],
            "content_scope_id": contract["content_scope_id"],
            "content_scope_sha256": contract["content_scope_sha256"],
            "periodic_evaluation_seeds": [int(run["periodic_evaluation_seed_start"]),
                int(run["periodic_evaluation_seed_start"]) + int(run["periodic_evaluation_seed_count"])],
            "final_evaluation_seeds": [int(run["final_evaluation_seed_start"]),
                int(run["final_evaluation_seed_start"]) + int(run["final_evaluation_seed_count"])],
            "training_identity_sha256": identity,
            "config_sha256": config_sha,
            "native_source_sha256": contract["native_source_sha256"],
            "model": contract["model"],
            "ppo": contract["ppo"],
            "workers": workers,
            "shards": shards,
            "continuation": provenance,
        }
        (staging / "run-manifest.json").write_text(
            json.dumps(manifest, indent=2) + "\n", encoding="utf-8"
        )
        os.rename(staging, target)
    assert sha256_file(path) == digest
    return provenance


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--config", type=Path, required=True)
    args = parser.parse_args()
    print(json.dumps(initialize(args.config), indent=2))


if __name__ == "__main__":
    main()
