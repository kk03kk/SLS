"""Fail-closed milestone seal with exact-next-update reproducibility evidence."""

from __future__ import annotations

import argparse
import json
import os
import random
import shutil
import sys
import tempfile
import time
import tomllib
from dataclasses import asdict
from pathlib import Path
from typing import Any, Mapping

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))
sys.path.insert(0, str(ROOT / "src"))
os.environ.setdefault("CUBLAS_WORKSPACE_CONFIG", ":4096:8")

import torch

from sls.curriculum import CURRICULUM_PROFILES_BY_ID
from sls.model import ModelConfig, Policy
from sls.rl import (
    PPOConfig,
    PPOTrainer,
    ShardedWorkerPool,
    load_checkpoint,
    save_checkpoint,
)
from sls.rl.evaluate import evaluate
from sls.rl.training_contract import (
    TRAINING_CHECKPOINT_SCHEMA,
    git_state,
    native_artifact,
    native_source_digest,
    sha256_file,
)
from tools.train_full_run import _load_benchmark, _training_identity

SEAL_SCHEMA = "sls-training-milestone-seal-v1"


def _atomic_json(path: Path, value: object) -> None:
    temporary = path.with_suffix(path.suffix + ".tmp")
    temporary.write_text(
        json.dumps(value, ensure_ascii=False, indent=2, sort_keys=True) + "\n",
        encoding="utf-8",
    )
    os.replace(temporary, path)


def _profile_id(contract: Mapping[str, Any]) -> str | None:
    return getattr(contract.get("profile"), "profile_id", None)


def _equal(left: object, right: object) -> bool:
    if isinstance(left, torch.Tensor) and isinstance(right, torch.Tensor):
        return left.dtype == right.dtype and left.shape == right.shape and torch.equal(left, right)
    if isinstance(left, Mapping) and isinstance(right, Mapping):
        return set(left) == set(right) and all(_equal(left[key], right[key]) for key in left)
    if isinstance(left, (list, tuple)) and isinstance(right, (list, tuple)):
        return len(left) == len(right) and all(_equal(a, b) for a, b in zip(left, right))
    return bool(left == right)


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("checkpoint", type=Path)
    parser.add_argument("--stage", choices=("smoke", "pilot", "train"), required=True)
    parser.add_argument("--config", type=Path, default=ROOT / "configs/train/ironclad_a0_fullrun.toml")
    parser.add_argument("--evaluation-episodes", type=int, default=1000)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--expected-steps", type=int, default=7_004_160)
    parser.add_argument("--expected-update", type=int, default=570)
    args = parser.parse_args()
    checkpoint = args.checkpoint.resolve()
    output = args.output.resolve()
    if not checkpoint.is_file():
        raise FileNotFoundError(checkpoint)
    if output.exists():
        raise FileExistsError(f"seal output already exists: {output}")
    output.parent.mkdir(parents=True, exist_ok=True)
    if args.evaluation_episodes <= 0:
        raise ValueError("evaluation episode count must be positive")

    config_bytes = args.config.read_bytes()
    config = tomllib.loads(config_bytes.decode("utf-8"))
    run = dict(config["run"])
    stage = dict(config["stages"][args.stage])
    profile = CURRICULUM_PROFILES_BY_ID[str(stage["profile"])]
    preview = torch.load(checkpoint, map_location="cpu", weights_only=False)
    contract = preview.get("contract")
    state = preview.get("trainer")
    if preview.get("schema") != TRAINING_CHECKPOINT_SCHEMA:
        raise ValueError("checkpoint schema is incompatible")
    if not isinstance(contract, Mapping) or not isinstance(state, Mapping):
        raise ValueError("checkpoint contract/trainer state is missing")
    required = {
        "environment_steps": (int(state.get("environment_steps", -1)), args.expected_steps),
        "update": (int(state.get("update", -1)), args.expected_update),
        "profile": (_profile_id(contract), profile.profile_id),
        "workers": (int(contract.get("workers", -1)), 48),
        "worker_shards": (int(contract.get("worker_shards", -1)), 16),
    }
    mismatches = [name for name, pair in required.items() if pair[0] != pair[1]]
    if mismatches:
        raise ValueError("milestone does not match the requested seal: " + ", ".join(mismatches))

    repository = git_state()
    native_digest = native_source_digest()
    artifact = native_artifact()
    if artifact is None:
        raise RuntimeError("seal requires the compiled native simulator")
    workers_count, shards = _load_benchmark(
        ROOT / str(run["benchmark"]),
        native_digest=native_digest,
        native_binary_sha256=str(artifact["sha256"]),
    )
    identity = _training_identity(config, workers=workers_count, shards=shards)
    seed = int(run["seed"])
    device = str(run["device"])
    ppo = PPOConfig(**config["ppo"])

    def next_update(destination: Path) -> tuple[dict[str, float], dict[str, Any]]:
        random.seed(seed)
        torch.manual_seed(seed)
        torch.use_deterministic_algorithms(bool(run.get("deterministic", True)))
        torch.backends.cudnn.benchmark = False
        model = Policy(ModelConfig(**config["model"]))
        with ShardedWorkerPool(
            profile, workers_count, shard_count=shards,
            crash_dump_dir=destination.parent / "crashes",
        ) as workers:
            trainer = PPOTrainer(
                model, workers, ppo, device=device, seed=seed,
                native_contract_digest=native_digest,
                git_commit=str(repository["commit"]),
                training_config_digest=identity,
                training_seed_limit=int(run["periodic_evaluation_seed_start"]),
            )
            load_checkpoint(checkpoint, trainer)
            metrics = trainer.train_update()
            save_checkpoint(destination, trainer)
        result = torch.load(destination, map_location="cpu", weights_only=False)
        return metrics, result

    temporary = Path(tempfile.mkdtemp(prefix=output.name + ".sealing-", dir=output.parent))
    try:
        metrics_a, next_a = next_update(temporary / "next-a.pt")
        metrics_b, next_b = next_update(temporary / "next-b.pt")
        reproducible = _equal(metrics_a, metrics_b) and _equal(next_a, next_b)
        if not reproducible:
            raise RuntimeError("next-update exact reproducibility check failed")

        model = Policy(ModelConfig(**config["model"]))
        model.load_state_dict(preview["model"], strict=True)
        seed_start = int(run["periodic_evaluation_seed_start"])
        evaluation = asdict(evaluate(
            model,
            profile,
            tuple(range(seed_start, seed_start + args.evaluation_episodes)),
            device=device,
            max_steps=int(run["evaluation_max_steps"]),
            max_boundary_visits=ppo.max_boundary_visits,
            failure_progress_scale=ppo.failure_progress_scale,
        ))
        safety = sum(int(evaluation.get(key, 0)) for key in (
            "backend_errors", "backend_truncations", "timeouts",
            "step_limits", "cycle_limits", "self_loops",
        ))
        if safety:
            raise RuntimeError("milestone evaluation failed the zero-safety-error gate")

        copied = temporary / "source-checkpoint.pt"
        shutil.copy2(checkpoint, copied)
        if sha256_file(copied) != sha256_file(checkpoint):
            raise IOError("sealed checkpoint copy hash mismatch")
        evidence = {
            "schema": SEAL_SCHEMA,
            "passed": True,
            "source_checkpoint_sha256": sha256_file(copied),
            "environment_steps": args.expected_steps,
            "update": args.expected_update,
            "profile": profile.profile_id,
            "workers": workers_count,
            "shards": shards,
            "training_identity_sha256": identity,
            "git": repository,
            "native_source_sha256": native_digest,
            "native_artifact": artifact,
            "next_update_exact": True,
            "next_update_metrics": metrics_a,
            "evaluation_episodes": args.evaluation_episodes,
            "created_unix": time.time(),
        }
        _atomic_json(temporary / "seal.json", evidence)
        _atomic_json(temporary / "evaluation.json", evaluation)
        shutil.copy2(args.config, temporary / "training-config.toml")
        for name in ("next-a.pt", "next-b.pt"):
            (temporary / name).unlink()
        shutil.rmtree(temporary / "crashes", ignore_errors=True)
        os.replace(temporary, output)
    except BaseException:
        shutil.rmtree(temporary, ignore_errors=True)
        raise
    print(json.dumps(evidence, sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
