"""Train the canonical FullRun policy with native simulator workers."""

from __future__ import annotations

import argparse
from dataclasses import asdict
import hashlib
import json
import math
import os
from pathlib import Path
import platform
import signal
import socket
import sys
import time
import tomllib


ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "src"))

import torch

from sls.content.scope import IRONCLAD_A0_SCOPE_ID, ironclad_a0_scope_hash

from sls.curriculum import CURRICULUM_PROFILES_BY_ID
from sls.model import ModelConfig, Policy
from sls.model.encoding import ENCODING_SCHEMA, vocabulary_hash
from sls.rl import (
    PPOConfig, PPOTrainer, ShardedWorkerPool, VectorWorkerPool, WorkerPool,
    evaluate, load_checkpoint,
    load_model_weights, save_checkpoint,
)
from sls.rl.best_checkpoint import best_checkpoint_record, update_best_checkpoint
from sls.rl.training_contract import (
    TRAINING_CHECKPOINT_SCHEMA, canonical_digest, git_state, native_artifact,
    native_source_digest, readiness_settings,
)
from sls.rl.demonstrations import load_teacher_corpus
from sls.rl.training_mode import TrainingMode, parse_training_mode
from sls.validation.readiness_lock import verify_readiness_lock
from sls.validation.transfer_gate import verify_policy_transfer_gate


PROFILES = CURRICULUM_PROFILES_BY_ID


def _atomic_json(path: Path, value: object) -> None:
    temporary = path.with_suffix(path.suffix + ".tmp")
    temporary.write_text(
        json.dumps(value, ensure_ascii=False, indent=2, sort_keys=True) + "\n",
        encoding="utf-8",
    )
    temporary.replace(path)


def _evaluation_seeds(run: dict[str, object]) -> tuple[int, ...]:
    if "evaluation_seeds" in run:
        return tuple(int(value) for value in run["evaluation_seeds"])
    start = int(run.get("evaluation_seed_start", 10_000))
    count = int(run.get("evaluation_seed_count", 100))
    if count <= 0:
        raise ValueError("evaluation_seed_count must be positive")
    return tuple(range(start, start + count))


def _positive_int(run: dict[str, object], key: str, *, default: int | None = None) -> int:
    if key in run:
        value = int(run[key])
    elif default is not None:
        value = default
    else:
        raise ValueError(f"missing required run field: {key}")
    if value <= 0:
        raise ValueError(f"{key} must be positive")
    return value


def _require_readiness_profile(readiness: dict[str, object], profile_id: str) -> None:
    locked_profile = str(readiness.get("profile") or "")
    if locked_profile != profile_id:
        raise ValueError(
            f"readiness lock is for {locked_profile or 'an unspecified profile'}, "
            f"not {profile_id}"
        )


class StopController:
    def __init__(self) -> None:
        self.requested = False
        self.signal_name: str | None = None

    def handler(self, number: int, _frame: object) -> None:
        self.requested = True
        try:
            self.signal_name = signal.Signals(number).name
        except ValueError:
            self.signal_name = str(number)

    def install(self) -> None:
        signal.signal(signal.SIGTERM, self.handler)
        signal.signal(signal.SIGINT, self.handler)


def _resolve_resume(value: str | None, output: Path) -> Path | None:
    if value is None:
        return None
    path = output / "latest.pt" if value == "auto" else Path(value)
    if not path.exists():
        raise FileNotFoundError(f"resume checkpoint does not exist: {path}")
    return path


def _trim_metrics(path: Path, completed_update: int) -> int:
    if not path.exists():
        return 0
    kept: list[str] = []
    removed = 0
    for line in path.read_text(encoding="utf-8").splitlines():
        if not line.strip():
            continue
        record = json.loads(line)
        if int(record["update"]) <= completed_update:
            kept.append(json.dumps(record, sort_keys=True))
        else:
            removed += 1
    temporary = path.with_suffix(path.suffix + ".tmp")
    temporary.write_text("".join(item + "\n" for item in kept), encoding="utf-8")
    temporary.replace(path)
    return removed


def _slurm_environment() -> dict[str, str]:
    return {
        key: os.environ[key]
        for key in (
            "SLURM_JOB_ID", "SLURM_JOB_NAME", "SLURM_JOB_PARTITION",
            "SLURM_CPUS_ON_NODE", "SLURM_JOB_GPUS", "CUDA_VISIBLE_DEVICES",
        )
        if key in os.environ
    }


def resolve_training_safety(
    run: dict[str, object], profile_id: str, *, root: Path = ROOT,
) -> dict[str, object]:
    """Resolve the explicit mode without an experimental-to-production fallback."""

    mode = parse_training_mode(run.get("training_mode"))
    requires_transfer = bool(run.get("require_transfer_gate", False))
    if mode is TrainingMode.EXPERIMENTAL:
        if requires_transfer:
            raise ValueError(
                "experimental config must not claim the production transfer gate"
            )
        return {
            "training_mode": mode,
            "policy_transfer_verified": False,
            "readiness_digest": "EXPERIMENTAL_UNVERIFIED",
            "readiness_level": "EXPERIMENTAL",
        }
    if not requires_transfer:
        raise ValueError("production config must require the policy-transfer gate")
    configured = run.get("transfer_gate")
    if not configured:
        raise ValueError("production config requires an explicit transfer_gate")
    gate_path = Path(str(configured))
    if not gate_path.is_absolute():
        gate_path = root / gate_path
    gate = verify_policy_transfer_gate(
        gate_path, profile_id=profile_id, require_canary=True,
    )
    return {
        "training_mode": mode,
        "policy_transfer_verified": True,
        "readiness_digest": hashlib.sha256(gate_path.read_bytes()).hexdigest(),
        "readiness_level": str(gate["schema"]),
    }


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--config", type=Path, default=ROOT / "configs" / "train" / "act1_train.toml")
    parser.add_argument("--resume", help="checkpoint path or 'auto' for OUTPUT/latest.pt")
    parser.add_argument("--warm-start", type=Path, help="compatible checkpoint weights only")
    parser.add_argument("--workers", type=int, help="override configured worker count")
    parser.add_argument(
        "--state-corpus", type=Path,
        help="teacher corpus whose checkpoints provide half of mid-run resets",
    )
    args = parser.parse_args()
    if torch.cuda.is_available():
        torch.set_float32_matmul_precision("high")
    if args.resume and args.warm_start:
        parser.error("--resume and --warm-start are mutually exclusive")
    config_bytes = args.config.read_bytes()
    payload = tomllib.loads(config_bytes.decode("utf-8"))
    run = payload["run"]
    if args.workers is not None:
        if args.workers <= 0:
            parser.error("--workers must be positive")
        run = {**run, "workers": args.workers}
        payload = {**payload, "run": run}
    profile = PROFILES[str(run["profile"])]
    safety = resolve_training_safety(run, profile.profile_id)
    training_mode = safety["training_mode"]
    assert isinstance(training_mode, TrainingMode)
    policy_transfer_verified = bool(safety["policy_transfer_verified"])
    readiness_digest = str(safety["readiness_digest"])
    readiness_level = str(safety["readiness_level"])
    if bool(run.get("require_readiness", False)):
        try:
            readiness_path, readiness_level = readiness_settings(run)
        except Exception as error:
            print(json.dumps({"error": "ACT1_PARITY_READINESS_FAILED", "reason": str(error)}, sort_keys=True), file=sys.stderr)
            return 2
        try:
            readiness = verify_readiness_lock(
                readiness_path, require_clean=not bool(run.get("allow_dirty", False)),
                expected_level=readiness_level,
            )
            _require_readiness_profile(dict(readiness), profile.profile_id)
        except Exception as error:
            print(json.dumps({"error": "ACT1_PARITY_READINESS_FAILED", "reason": str(error)}, sort_keys=True), file=sys.stderr)
            return 2
        readiness_digest = str(readiness["lock_sha256"])
    seed = int(run["seed"])
    torch.manual_seed(seed)
    device_name = str(run.get("device", "auto"))
    device = "cuda" if device_name == "auto" and torch.cuda.is_available() else (
        "cpu" if device_name == "auto" else device_name
    )
    if str(device).startswith("cuda") and not torch.cuda.is_available():
        raise RuntimeError("training config requires CUDA but CUDA is unavailable")
    model = Policy(ModelConfig(**payload.get("model", {})))
    warm_start = None
    if args.warm_start is not None:
        warm_path = args.warm_start if args.warm_start.is_absolute() else ROOT / args.warm_start
        warm_start = {
            "path": str(warm_path.resolve()),
            **load_model_weights(
                warm_path, model, target_training_mode=training_mode,
            ),
        }
    checkpoint_reservoir = ()
    state_corpus = None
    configured_corpus = args.state_corpus or (
        Path(str(run["state_corpus"])) if run.get("state_corpus") else None
    )
    if configured_corpus is not None:
        corpus_path = configured_corpus if configured_corpus.is_absolute() else ROOT / configured_corpus
        corpus = load_teacher_corpus(corpus_path)
        if training_mode is TrainingMode.PRODUCTION:
            raise ValueError(
                "experimental teacher corpus requires an explicit production promotion"
            )
        if str(corpus.get("profile")) != profile.profile_id:
            raise ValueError("state corpus profile does not match the training profile")
        checkpoint_reservoir = tuple(
            dict(example["checkpoint"]) for example in corpus["examples"]
        )
        state_corpus = {
            "path": str(corpus_path.resolve()),
            "examples": len(checkpoint_reservoir),
            "sha256": hashlib.sha256(corpus_path.read_bytes()).hexdigest(),
        }
    ppo = PPOConfig(**payload.get("ppo", {}))
    workers_count = _positive_int(run, "workers")
    updates = _positive_int(run, "updates")
    save_interval = _positive_int(run, "save_interval", default=10)
    evaluate_interval = _positive_int(run, "evaluate_interval", default=save_interval)
    evaluation_max_steps = _positive_int(run, "evaluation_max_steps", default=512)
    output = ROOT / str(run.get("output", "runs/full-run"))
    output.mkdir(parents=True, exist_ok=True)
    resume_path = _resolve_resume(args.resume, output)
    started = time.time()
    source_digest = native_source_digest()
    repository = git_state()
    if bool(repository["dirty"]):
        raise ValueError("training requires a clean Git worktree")
    resolved_config_digest = canonical_digest(payload)
    run_manifest: dict[str, object] = {
        "schema": "sls-ppo-run-manifest-v2",
        "training_mode": training_mode.value,
        "policy_transfer_verified": policy_transfer_verified,
        "status": "RUNNING",
        "profile": profile.profile_id,
        "curriculum_version": profile.version,
        "seed": seed,
        "workers": workers_count,
        "updates": updates,
        "device": str(device),
        "python": sys.version,
        "platform": platform.platform(),
        "hostname": socket.gethostname(),
        "git": repository,
        "slurm": _slurm_environment(),
        "torch": torch.__version__,
        "cuda_available": torch.cuda.is_available(),
        "cuda_version": torch.version.cuda,
        "gpu": torch.cuda.get_device_name(device) if str(device).startswith("cuda") else None,
        "config_sha256": hashlib.sha256(config_bytes).hexdigest(),
        "resolved_config_sha256": resolved_config_digest,
        "checkpoint_schema": TRAINING_CHECKPOINT_SCHEMA,
        "encoding_schema": ENCODING_SCHEMA,
        "vocabulary_sha256": vocabulary_hash(),
        "readiness_lock_sha256": readiness_digest,
        "content_scope_id": IRONCLAD_A0_SCOPE_ID,
        "content_scope_sha256": ironclad_a0_scope_hash(),
        "readiness_level": readiness_level,
        "native_source_sha256": source_digest,
        "native_artifact": native_artifact(),
        "model": model.config.to_dict(),
        "ppo": ppo.to_dict(),
        "evaluation_seeds": list(_evaluation_seeds(run)),
        "evaluation_max_steps": evaluation_max_steps,
        "started_unix": started,
        "resume": str(resume_path.resolve()) if resume_path else None,
        "warm_start": warm_start,
        "state_corpus": state_corpus,
    }
    _atomic_json(output / "run-manifest.json", run_manifest)
    if str(device).startswith("cuda"):
        torch.cuda.reset_peak_memory_stats(device)
    controller = StopController()
    controller.install()
    trainer: PPOTrainer | None = None
    try:
        worker_backend = str(run.get("worker_backend", "sharded-vector"))
        pool_types = {
            "sharded-vector": ShardedWorkerPool,
            "local-vector": VectorWorkerPool,
            "spawned": WorkerPool,
        }
        if worker_backend not in pool_types:
            raise ValueError(f"unknown worker backend: {worker_backend}")
        pool_type = pool_types[worker_backend]
        with pool_type(
            profile, workers_count, crash_dump_dir=output / "crashes",
        ) as workers:
            trainer = PPOTrainer(
                model, workers, ppo, device=device, seed=seed,
                readiness_lock_digest=readiness_digest,
                native_contract_digest=source_digest,
                checkpoint_reservoir=checkpoint_reservoir,
                checkpoint_reservoir_digest=(
                    str(state_corpus["sha256"]) if state_corpus else "NONE"
                ),
                training_mode=training_mode,
                policy_transfer_verified=policy_transfer_verified,
                git_commit=str(repository["commit"]),
                training_config_digest=resolved_config_digest,
            )
            if resume_path is not None:
                load_checkpoint(resume_path, trainer)
            evaluation_seeds = _evaluation_seeds(run)
            log_path = output / "metrics.jsonl"
            run_manifest["trimmed_metric_records"] = _trim_metrics(log_path, trainer.update) if resume_path else 0
            while trainer.update < updates:
                update_started = time.perf_counter()
                metrics = trainer.train_update()
                non_finite = {key: value for key, value in metrics.items() if not math.isfinite(value)}
                if non_finite:
                    raise FloatingPointError(f"non-finite PPO metrics: {non_finite}")
                update_seconds = time.perf_counter() - update_started
                record: dict[str, object] = {
                    "update": trainer.update, "episodes": trainer.episodes,
                    "update_seconds": update_seconds,
                    "decisions_per_second": workers_count * ppo.rollout_steps / update_seconds,
                    **metrics,
                }
                if not controller.requested and trainer.update % evaluate_interval == 0:
                    try:
                        result = evaluate(
                            trainer.model, profile, evaluation_seeds, device=device,
                            max_steps=evaluation_max_steps,
                            max_boundary_visits=ppo.max_boundary_visits,
                            stop_requested=lambda: controller.requested,
                        )
                        evaluation = asdict(result)
                        record["evaluation"] = evaluation
                        best_record = best_checkpoint_record(
                            evaluation, update=trainer.update, warm_start=warm_start,
                        )
                        record["best_checkpoint_updated"] = update_best_checkpoint(
                            output, best_record,
                            save=lambda path: save_checkpoint(path, trainer),
                        )
                    except InterruptedError:
                        record["evaluation_interrupted"] = True
                with log_path.open("a", encoding="utf-8") as stream:
                    stream.write(json.dumps(record, sort_keys=True) + "\n")
                    stream.flush()
                    os.fsync(stream.fileno())
                print(json.dumps(record, sort_keys=True), flush=True)
                if trainer.update % save_interval == 0 or controller.requested:
                    save_checkpoint(output / f"checkpoint-{trainer.update:08d}.pt", trainer)
                    save_checkpoint(output / "latest.pt", trainer)
                if controller.requested:
                    break
            save_checkpoint(output / "latest.pt", trainer)
        status = "INTERRUPTED" if controller.requested else "COMPLETE"
        run_manifest.update({
            "status": status, "stop_signal": controller.signal_name,
            "finished_unix": time.time(), "elapsed_seconds": time.time() - started,
            "completed_updates": trainer.update, "episodes": trainer.episodes,
            "termination_counts": dict(trainer.termination_counts),
            "cuda_peak_memory_bytes": torch.cuda.max_memory_allocated(device) if str(device).startswith("cuda") else 0,
        })
        _atomic_json(output / "run-manifest.json", run_manifest)
        return 3 if controller.requested else 0
    except BaseException as error:
        run_manifest.update({
            "status": "FAILED", "finished_unix": time.time(),
            "elapsed_seconds": time.time() - started,
            "completed_updates": trainer.update if trainer else 0,
            "error_type": type(error).__name__, "error": str(error),
        })
        _atomic_json(output / "run-manifest.json", run_manifest)
        raise


if __name__ == "__main__":
    raise SystemExit(main())
