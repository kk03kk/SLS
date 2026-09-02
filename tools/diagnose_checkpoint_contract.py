"""Print a read-only checkpoint/current-trainer contract diff as JSON."""

from __future__ import annotations

import argparse
import hashlib
import json
import os
import sys
import tomllib
from pathlib import Path
from types import SimpleNamespace
from typing import Any, Mapping

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "src"))
os.environ.setdefault("CUBLAS_WORKSPACE_CONFIG", ":4096:8")

import torch

from sls.curriculum import CURRICULUM_PROFILES_BY_ID
from sls.model import ModelConfig
from sls.rl import PPOConfig, checkpoint_contract, checkpoint_contract_diff
from sls.rl.training_contract import (
    TRAINING_CHECKPOINT_SCHEMA,
    git_state,
    native_artifact,
    native_source_digest,
)
from tools.train_full_run import _load_benchmark, _training_identity


def _sha256(path: Path) -> str:
    with path.open("rb") as stream:
        return hashlib.file_digest(stream, "sha256").hexdigest()


def _profile_id(contract: Mapping[str, Any]) -> str | None:
    return getattr(contract.get("profile"), "profile_id", None)


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("checkpoint", type=Path)
    parser.add_argument("--stage", choices=("smoke", "pilot", "train"), required=True)
    parser.add_argument(
        "--config", type=Path,
        default=ROOT / "configs" / "train" / "ironclad_a0_fullrun.toml",
    )
    args = parser.parse_args()

    checkpoint_path = args.checkpoint.resolve()
    payload = torch.load(checkpoint_path, map_location="cpu", weights_only=False)
    if payload.get("schema") != TRAINING_CHECKPOINT_SCHEMA:
        raise ValueError("unsupported training checkpoint")
    actual = payload.get("contract")
    state = payload.get("trainer")
    if not isinstance(actual, Mapping) or not isinstance(state, Mapping):
        raise ValueError("training checkpoint metadata is missing")

    config_path = args.config.resolve()
    config = tomllib.loads(config_path.read_text(encoding="utf-8"))
    run = dict(config["run"])
    stage = dict(config["stages"][args.stage])
    source_digest = native_source_digest()
    artifact = native_artifact()
    if artifact is None:
        raise RuntimeError("compiled native simulator is missing")
    workers, shards = _load_benchmark(
        ROOT / str(run["benchmark"]),
        native_digest=source_digest,
        native_binary_sha256=str(artifact["sha256"]),
    )
    profile = CURRICULUM_PROFILES_BY_ID[str(stage["profile"])]
    identity = _training_identity(config, workers=workers, shards=shards)
    repository = git_state()
    contract_source = SimpleNamespace(
        model=SimpleNamespace(config=ModelConfig(**config["model"])),
        config=PPOConfig(**config["ppo"]),
        workers=SimpleNamespace(profile=profile, size=workers, shard_count=shards),
        native_contract_digest=source_digest,
        git_commit=str(repository["commit"]),
        training_config_digest=identity,
        training_seed_limit=int(run["periodic_evaluation_seed_start"]),
    )
    current = checkpoint_contract(contract_source)
    differences = checkpoint_contract_diff(
        actual,
        current,
        allowed_runtime_rebind_fields=frozenset({
            "git_commit", "native_source_sha256",
        }),
    )
    report = {
        "schema": "sls-checkpoint-contract-diagnostic-v1",
        "checkpoint": str(checkpoint_path),
        "checkpoint_sha256": _sha256(checkpoint_path),
        "environment_steps": int(state["environment_steps"]),
        "update": int(state["update"]),
        "checkpoint_profile": _profile_id(actual),
        "current_profile": profile.profile_id,
        "workers": workers,
        "worker_shards": shards,
        "differences": differences,
        "runtime_rebind_safe": all(
            bool(item["runtime_rebind_allowed"]) for item in differences
        ),
    }
    print(json.dumps(report, indent=2, sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
