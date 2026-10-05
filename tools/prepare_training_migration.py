"""Create a new FullRun chain from an exact curriculum checkpoint."""

from __future__ import annotations

import argparse
import hashlib
import json
import os
import random
import shutil
import sys
import tempfile
import time
import tomllib
from pathlib import Path
from typing import Any, Mapping

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))
sys.path.insert(0, str(ROOT / "src"))
os.environ.setdefault("CUBLAS_WORKSPACE_CONFIG", ":4096:8")

import torch

from sls.content.scope import (
    IRONCLAD_A0_SCOPE_ID,
    ironclad_a0_scope_hash,
    validate_scope_source_hashes,
)
from sls.curriculum import CURRICULUM_PROFILES_BY_ID
from sls.model import ENCODING_SCHEMA, ModelConfig, Policy, vocabulary_hash
from sls.rl import (
    PPOConfig,
    PPOTrainer,
    ShardedWorkerPool,
    load_checkpoint_environment_migration,
    save_checkpoint,
)
from sls.rl.training_contract import (
    TRAINING_CHECKPOINT_SCHEMA,
    git_state,
    native_artifact,
    native_source_digest,
    sha256_file,
)
from tools.train_full_run import (
    MANIFEST_SCHEMA,
    _load_benchmark,
    _training_identity,
)

MIGRATION_SCHEMA = "sls-training-migration-v2"


def _profile_id(contract: Mapping[str, Any]) -> str | None:
    profile = contract.get("profile")
    return getattr(profile, "profile_id", None)


def _atomic_json(path: Path, value: object) -> None:
    temporary = path.with_suffix(path.suffix + ".tmp")
    temporary.write_text(
        json.dumps(value, ensure_ascii=False, indent=2, sort_keys=True) + "\n",
        encoding="utf-8",
    )
    os.replace(temporary, path)


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--source", type=Path, required=True)
    parser.add_argument("--config", type=Path, required=True)
    parser.add_argument("--stage", choices=("train",), default="train")
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--expected-steps", type=int, default=7_004_160)
    parser.add_argument("--expected-update", type=int, default=570)
    parser.add_argument("--expected-workers", type=int, default=48)
    parser.add_argument("--expected-shards", type=int, default=16)
    args = parser.parse_args()

    source = args.source.resolve()
    config = args.config.resolve()
    output = args.output.resolve()
    if not source.is_file():
        raise FileNotFoundError(source)
    if output.exists():
        raise FileExistsError(f"migration output already exists: {output}")
    validate_scope_source_hashes(ROOT)

    config_bytes = config.read_bytes()
    payload = tomllib.loads(config_bytes.decode("utf-8"))
    run = dict(payload["run"])
    stage = dict(payload["stages"][args.stage])
    if str(run["output"]).replace("\\", "/").rstrip("/") != str(
        output.relative_to(ROOT)
    ).replace("\\", "/"):
        raise ValueError("config run.output does not match --output")
    profile = CURRICULUM_PROFILES_BY_ID[str(stage["profile"])]
    if profile.profile_id != "IRONCLAD_A0_FULLRUN":
        raise ValueError("migration target must be IRONCLAD_A0_FULLRUN")

    preview = torch.load(source, map_location="cpu", weights_only=False)
    if preview.get("schema") != TRAINING_CHECKPOINT_SCHEMA:
        raise ValueError("source checkpoint schema is incompatible")
    contract = preview.get("contract")
    trainer_state = preview.get("trainer")
    if not isinstance(contract, Mapping) or not isinstance(trainer_state, Mapping):
        raise ValueError("source checkpoint contract/trainer state is missing")
    exact = {
        "environment_steps": (int(trainer_state.get("environment_steps", -1)), args.expected_steps),
        "update": (int(trainer_state.get("update", -1)), args.expected_update),
        "workers": (int(contract.get("workers", -1)), args.expected_workers),
        "worker_shards": (int(contract.get("worker_shards", -1)), args.expected_shards),
        "profile": (_profile_id(contract), "IRONCLAD_A0_ACT2"),
        "encoding_schema": (contract.get("encoding_schema"), ENCODING_SCHEMA),
        "vocabulary_sha256": (contract.get("vocabulary_sha256"), vocabulary_hash()),
    }
    mismatches = [name for name, values in exact.items() if values[0] != values[1]]
    if mismatches:
        raise ValueError("source milestone mismatch: " + ", ".join(mismatches))

    repository = git_state()
    native_digest = native_source_digest()
    artifact = native_artifact()
    if artifact is None:
        raise RuntimeError("migration requires a compiled native simulator")
    workers_count, shard_count = _load_benchmark(
        ROOT / str(run["benchmark"]),
        native_digest=native_digest,
        native_binary_sha256=str(artifact["sha256"]),
    )
    if (workers_count, shard_count) != (args.expected_workers, args.expected_shards):
        raise ValueError("benchmark is not the required 48:16 layout")
    identity = _training_identity(payload, workers=workers_count, shards=shard_count)
    model_config = ModelConfig(**payload["model"])
    ppo_config = PPOConfig(**payload["ppo"])
    if contract.get("model") != model_config.to_dict():
        raise ValueError("migration would change the model/Policy ABI")
    if contract.get("ppo") != ppo_config.to_dict():
        raise ValueError("migration would change the PPO contract")

    output.parent.mkdir(parents=True, exist_ok=True)
    temporary = Path(tempfile.mkdtemp(prefix=output.name + ".migrating-", dir=output.parent))
    try:
        copied_source = temporary / "source-checkpoint.pt"
        shutil.copy2(source, copied_source)
        source_sha = sha256_file(copied_source)
        if source_sha != sha256_file(source):
            raise IOError("source checkpoint copy hash mismatch")
        shutil.copy2(config, temporary / "training-config.toml")

        seed = int(run["seed"])
        random.seed(seed)
        torch.manual_seed(seed)
        torch.use_deterministic_algorithms(bool(run.get("deterministic", True)))
        torch.backends.cudnn.benchmark = False
        device = str(run["device"])
        model = Policy(model_config)
        with ShardedWorkerPool(
            profile, workers_count, shard_count=shard_count,
            crash_dump_dir=temporary / "crashes",
        ) as workers:
            trainer = PPOTrainer(
                model, workers, ppo_config, device=device, seed=seed,
                native_contract_digest=native_digest,
                git_commit=str(repository["commit"]),
                training_config_digest=identity,
                training_seed_limit=int(run["periodic_evaluation_seed_start"]),
            )
            load_checkpoint_environment_migration(copied_source, trainer)
            if (trainer.environment_steps, trainer.update) != (
                args.expected_steps, args.expected_update,
            ):
                raise RuntimeError("migration changed cumulative learning progress")
            save_checkpoint(temporary / "latest.pt", trainer)

        initialization = {
            "schema": MIGRATION_SCHEMA,
            "validation_passed": True,
            "target_stage": args.stage,
            "source_checkpoint": "source-checkpoint.pt",
            "source_checkpoint_sha256": source_sha,
            "source_profile": _profile_id(contract),
            "target_profile": profile.profile_id,
            "environment_steps": args.expected_steps,
            "update": args.expected_update,
            "old_training_identity_sha256": contract.get("training_config_sha256"),
            "new_training_identity_sha256": identity,
            "old_content_scope_id": contract.get("content_scope_id"),
            "new_content_scope_id": IRONCLAD_A0_SCOPE_ID,
            "old_content_scope_sha256": contract.get("content_scope_sha256"),
            "new_content_scope_sha256": ironclad_a0_scope_hash(),
            "old_native_source_sha256": contract.get("native_source_sha256"),
            "new_native_source_sha256": native_digest,
            "reset_fields": [
                "environments", "episode_limits", "recurrent_memory",
                "episode_starts", "previous_action_types", "previous_rewards",
            ],
            "preserved_fields": [
                "model", "optimizer", "update", "environment_steps", "episodes",
                "next_seed", "trainer_random", "python_rng", "torch_rng", "cuda_rng",
            ],
            "created_unix": time.time(),
            "source_acceptance": "operator-approved-current-environment",
        }
        manifest = {
            "schema": MANIFEST_SCHEMA,
            "status": "MIGRATION_READY",
            "active_stage": None,
            "simulator_only": True,
            "profile": str(run["profile"]),
            "training_identity_sha256": identity,
            "config_sha256": hashlib.sha256(config_bytes).hexdigest(),
            "git": repository,
            "native_source_sha256": native_digest,
            "native_artifact": artifact,
            "encoding_schema": ENCODING_SCHEMA,
            "vocabulary_sha256": vocabulary_hash(),
            "content_scope_id": IRONCLAD_A0_SCOPE_ID,
            "content_scope_sha256": ironclad_a0_scope_hash(),
            "checkpoint_schema": TRAINING_CHECKPOINT_SCHEMA,
            "model": model_config.to_dict(),
            "ppo": ppo_config.to_dict(),
            "workers": workers_count,
            "shards": shard_count,
            "environment_steps": args.expected_steps,
            "updates": args.expected_update,
            "initialization": initialization,
            "stages": {},
        }
        _atomic_json(temporary / "migration.json", initialization)
        _atomic_json(temporary / "run-manifest.json", manifest)
        os.replace(temporary, output)
    except BaseException:
        shutil.rmtree(temporary, ignore_errors=True)
        raise

    print(json.dumps({
        "schema": MIGRATION_SCHEMA,
        "output": str(output),
        "source_checkpoint_sha256": source_sha,
        "environment_steps": args.expected_steps,
        "update": args.expected_update,
        "training_identity_sha256": identity,
    }, sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
