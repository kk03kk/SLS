"""Train one exact-resume Ironclad A0 FullRun recurrent PPO run."""

from __future__ import annotations

import argparse
import hashlib
import json
import math
import os
import platform
import shutil
import signal
import socket
import sys
import time
import tomllib
from dataclasses import asdict
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "src"))

import torch

from sls.content.scope import IRONCLAD_A0_SCOPE_ID, ironclad_a0_scope_hash
from sls.curriculum import CURRICULUM_PROFILES_BY_ID
from sls.model import ENCODING_SCHEMA, ModelConfig, Policy, vocabulary_hash
from sls.rl import (
    PPOConfig,
    PPOTrainer,
    ShardedWorkerPool,
    evaluate,
    load_checkpoint,
    load_checkpoint_environment_migration,
    save_checkpoint,
)
from sls.rl.best_checkpoint import best_checkpoint_record, update_best_checkpoint
from sls.rl.training_contract import (
    TRAINING_CHECKPOINT_SCHEMA,
    canonical_digest,
    git_state,
    native_artifact,
    native_source_digest,
    sha256_file,
)
from sls.runtime.artifact import export_policy_artifact

STAGES = ("smoke", "pilot", "train")
MANIFEST_SCHEMA = "sls-recurrent-ppo-run-v1"
BENCHMARK_SCHEMA = "sls-worker-benchmark-v2"


def _atomic_json(path: Path, value: object) -> None:
    temporary = path.with_suffix(path.suffix + ".tmp")
    temporary.write_text(
        json.dumps(value, ensure_ascii=False, indent=2, sort_keys=True) + "\n",
        encoding="utf-8",
    )
    temporary.replace(path)


def _positive_int(mapping: dict[str, object], key: str) -> int:
    value = int(mapping[key])
    if value <= 0:
        raise ValueError(f"{key} must be positive")
    return value


def _seed_range(start: int, count: int) -> range:
    if start < 0 or count <= 0:
        raise ValueError("seed ranges require a non-negative start and positive count")
    return range(start, start + count)


def _validate_seed_namespaces(run: dict[str, object]) -> tuple[range, range]:
    periodic = _seed_range(
        int(run["periodic_evaluation_seed_start"]),
        int(run["periodic_evaluation_seed_count"]),
    )
    final = _seed_range(
        int(run["final_evaluation_seed_start"]),
        int(run["final_evaluation_seed_count"]),
    )
    if periodic.stop > final.start and final.stop > periodic.start:
        raise ValueError("periodic and final evaluation seed ranges overlap")
    if periodic.start == 0 or final.start == 0:
        raise ValueError("held-out evaluation seeds overlap the training namespace")
    return periodic, final


def _load_benchmark(
    path: Path,
    *,
    repository: dict[str, object],
    native_digest: str,
) -> tuple[int, int]:
    payload = json.loads(path.read_text(encoding="utf-8"))
    if payload.get("schema") != BENCHMARK_SCHEMA:
        raise ValueError("worker benchmark schema is incompatible")
    if (payload.get("git") or {}).get("commit") != repository["commit"]:
        raise ValueError("worker benchmark belongs to a different Git commit")
    if payload.get("native_source_sha256") != native_digest:
        raise ValueError("worker benchmark belongs to different simulator sources")
    return int(payload["selected_workers"]), int(payload["selected_shards"])


def _training_identity(
    payload: dict[str, object], *, workers: int, shards: int,
) -> str:
    run = dict(payload["run"])
    return canonical_digest({
        "profile": run["profile"],
        "seed": int(run["seed"]),
        "worker_backend": run["worker_backend"],
        "workers": workers,
        "shards": shards,
        "periodic_evaluation_seed_start": int(run["periodic_evaluation_seed_start"]),
        "periodic_evaluation_seed_count": int(run["periodic_evaluation_seed_count"]),
        "final_evaluation_seed_start": int(run["final_evaluation_seed_start"]),
        "final_evaluation_seed_count": int(run["final_evaluation_seed_count"]),
        "model": payload["model"],
        "ppo": payload["ppo"],
    })


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


def _slurm_environment() -> dict[str, str]:
    return {
        key: os.environ[key]
        for key in (
            "SLURM_JOB_ID", "SLURM_JOB_NAME", "SLURM_JOB_PARTITION",
            "SLURM_CPUS_ON_NODE", "SLURM_JOB_GPUS", "CUDA_VISIBLE_DEVICES",
        )
        if key in os.environ
    }


def _last_evaluation_step(path: Path) -> int:
    result = -1
    if path.exists():
        for line in path.read_text(encoding="utf-8").splitlines():
            record = json.loads(line)
            if "evaluation" in record:
                result = max(result, int(record["environment_steps"]))
    return result


def _baseline_evaluation(path: Path) -> dict[str, object] | None:
    if not path.exists():
        return None
    for line in path.read_text(encoding="utf-8").splitlines():
        record = json.loads(line)
        if record.get("baseline") is True and isinstance(record.get("evaluation"), dict):
            return dict(record["evaluation"])
    return None


def _progress_from_baseline(
    baseline: dict[str, object], evaluation: dict[str, object],
) -> dict[str, float | None]:
    def delta(key: str) -> float | None:
        before = baseline.get(key)
        after = evaluation.get(key)
        if before is None or after is None:
            return None
        return float(after) - float(before)

    return {
        "reached_act2_rate_delta": delta("reached_act2_rate"),
        "reached_act3_rate_delta": delta("reached_act3_rate"),
        "median_failure_floor_delta": delta("median_failure_floor"),
    }


def _append_record(path: Path, record: dict[str, object]) -> None:
    with path.open("a", encoding="utf-8") as stream:
        stream.write(json.dumps(record, sort_keys=True) + "\n")
        stream.flush()
        os.fsync(stream.fileno())
    print(json.dumps(record, sort_keys=True), flush=True)


def _archive_pre_migration_best(output: Path) -> list[str]:
    """Retain best artifacts selected with pre-migration evaluation semantics."""

    pairs = [
        (output / "best_success.pt", output / "best_success.pre-environment-migration.pt"),
        (
            output / "best_success.json",
            output / "best_success.pre-environment-migration.json",
        ),
    ]
    for source, target in pairs:
        if source.exists() and target.exists():
            raise FileExistsError(f"cannot archive both existing best artifacts: {target}")
    archived = []
    for source, target in pairs:
        if source.exists():
            source.replace(target)
            archived.append(target.name)
        elif target.exists():
            archived.append(target.name)
    return archived


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--stage", choices=STAGES, required=True)
    parser.add_argument(
        "--resume", choices=("auto", "environment-migration"), default="auto",
        help="Use environment-migration once after an approved simulator contract change.",
    )
    parser.add_argument(
        "--config", type=Path,
        default=ROOT / "configs" / "train" / "ironclad_a0_fullrun.toml",
    )
    parser.add_argument(
        "--initialize-from", type=Path,
        help="Start a fresh smoke chain from checkpoint model weights only.",
    )
    args = parser.parse_args()
    if torch.cuda.is_available():
        torch.set_float32_matmul_precision("high")

    config_bytes = args.config.read_bytes()
    payload = tomllib.loads(config_bytes.decode("utf-8"))
    run = dict(payload["run"])
    stage = dict(payload["stages"][args.stage])
    periodic_seeds, final_seeds = _validate_seed_namespaces(run)
    repository = git_state()
    if bool(repository["dirty"]):
        raise ValueError("training requires a clean Git worktree")
    source_digest = native_source_digest()
    benchmark_path = ROOT / str(run["benchmark"])
    workers_count, shard_count = _load_benchmark(
        benchmark_path, repository=repository, native_digest=source_digest,
    )
    identity = _training_identity(payload, workers=workers_count, shards=shard_count)
    profile = CURRICULUM_PROFILES_BY_ID[str(run["profile"])]
    if profile.profile_id != "IRONCLAD_A0_FULLRUN":
        raise ValueError("canonical training config must use IRONCLAD_A0_FULLRUN")
    seed = int(run["seed"])
    torch.manual_seed(seed)
    device = str(run["device"])
    if device.startswith("cuda") and not torch.cuda.is_available():
        raise RuntimeError("training config requires CUDA but CUDA is unavailable")
    model = Policy(ModelConfig(**payload["model"]))
    ppo = PPOConfig(**payload["ppo"])
    target_steps = _positive_int(stage, "target_environment_steps")
    evaluate_every = _positive_int(stage, "evaluate_every_steps")
    evaluation_max_steps = int(run["evaluation_max_steps"])
    output = ROOT / str(run["output"])
    latest = output / "latest.pt"
    if args.initialize_from is not None and args.stage != "smoke":
        raise ValueError("--initialize-from is only valid for a fresh smoke chain")
    if args.stage == "smoke" and latest.exists():
        raise FileExistsError("smoke refuses to overwrite an existing training chain")
    if args.stage == "smoke" and args.resume != "auto":
        raise ValueError("smoke cannot use environment migration")
    if args.stage != "smoke" and not latest.exists():
        raise FileNotFoundError(f"{args.stage} requires the smoke/pilot checkpoint: {latest}")
    initialization = None
    if args.initialize_from is not None:
        source = args.initialize_from.resolve()
        if not source.is_file():
            raise FileNotFoundError(f"initialization checkpoint does not exist: {source}")
        source_payload = torch.load(source, map_location="cpu", weights_only=False)
        source_contract = source_payload.get("contract")
        if (
            source_payload.get("schema") != TRAINING_CHECKPOINT_SCHEMA
            or not isinstance(source_contract, dict)
            or not isinstance(source_payload.get("model"), dict)
        ):
            raise ValueError("initialization source is not a training checkpoint")
        required_contract = {
            "model": model.config.to_dict(),
            "profile": profile,
            "encoding_schema": ENCODING_SCHEMA,
            "vocabulary_sha256": vocabulary_hash(),
            "content_scope_id": IRONCLAD_A0_SCOPE_ID,
            "content_scope_sha256": ironclad_a0_scope_hash(),
        }
        incompatible = [
            key for key, value in required_contract.items()
            if source_contract.get(key) != value
        ]
        if incompatible:
            raise ValueError(
                "initialization checkpoint is incompatible: "
                + ", ".join(sorted(incompatible))
            )
        model.load_state_dict(source_payload["model"], strict=True)
        source_trainer = dict(source_payload.get("trainer") or {})
        initialization = {
            "schema": "sls-policy-initialization-v1",
            "source": str(source),
            "source_sha256": sha256_file(source),
            "source_git_commit": source_contract.get("git_commit"),
            "source_environment_steps": source_trainer.get("environment_steps"),
            "source_update": source_trainer.get("update"),
            "optimizer": "RESET",
            "environments": "FRESH",
            "recurrent_memory": "ZERO",
        }
    output.mkdir(parents=True, exist_ok=True)
    metrics_path = output / "metrics.jsonl"
    started = time.time()
    manifest_path = output / "run-manifest.json"
    if manifest_path.exists():
        manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
        if (
            manifest.get("schema") != MANIFEST_SCHEMA
            or manifest.get("training_identity_sha256") != identity
        ):
            raise ValueError("existing run manifest belongs to another training chain")
    else:
        manifest = {
            "schema": MANIFEST_SCHEMA,
            "simulator_only": True,
            "profile": profile.profile_id,
            "training_identity_sha256": identity,
            "config_sha256": hashlib.sha256(config_bytes).hexdigest(),
            "git": repository,
            "native_source_sha256": source_digest,
            "native_artifact": native_artifact(),
            "encoding_schema": ENCODING_SCHEMA,
            "vocabulary_sha256": vocabulary_hash(),
            "content_scope_id": IRONCLAD_A0_SCOPE_ID,
            "content_scope_sha256": ironclad_a0_scope_hash(),
            "checkpoint_schema": TRAINING_CHECKPOINT_SCHEMA,
            "model": model.config.to_dict(),
            "ppo": ppo.to_dict(),
            "workers": workers_count,
            "shards": shard_count,
            "periodic_evaluation_seeds": [periodic_seeds.start, periodic_seeds.stop],
            "final_evaluation_seeds": [final_seeds.start, final_seeds.stop],
            "created_unix": started,
            "stages": {},
        }
        if initialization is not None:
            manifest["initialization"] = initialization
    manifest.update({
        "status": "RUNNING",
        "active_stage": args.stage,
        "python": sys.version,
        "platform": platform.platform(),
        "hostname": socket.gethostname(),
        "torch": torch.__version__,
        "cuda": torch.version.cuda,
        "gpu": torch.cuda.get_device_name(device),
        "slurm": _slurm_environment(),
    })
    _atomic_json(manifest_path, manifest)
    torch.cuda.reset_peak_memory_stats(device)
    controller = StopController()
    controller.install()
    trainer: PPOTrainer | None = None
    try:
        with ShardedWorkerPool(
            profile, workers_count, shard_count=shard_count,
            crash_dump_dir=output / "crashes",
        ) as workers:
            trainer = PPOTrainer(
                model, workers, ppo, device=device, seed=seed,
                native_contract_digest=source_digest,
                git_commit=str(repository["commit"]),
                training_config_digest=identity,
                training_seed_limit=periodic_seeds.start,
            )
            if latest.exists():
                if args.resume == "environment-migration":
                    backup = output / "latest.pre-environment-migration.pt"
                    if backup.exists():
                        if sha256_file(backup) != sha256_file(latest):
                            raise FileExistsError(
                                "environment-migration backup already exists with different data"
                            )
                    else:
                        shutil.copy2(latest, backup)
                    previous = load_checkpoint_environment_migration(latest, trainer)
                    archived_best = _archive_pre_migration_best(output)
                    save_checkpoint(latest, trainer)
                    migration = {
                        "schema": "sls-environment-migration-v1",
                        "environment_steps": trainer.environment_steps,
                        "update": trainer.update,
                        "abandoned_environments": workers_count,
                        "first_fresh_seed": trainer.next_seed - workers_count,
                        "next_seed": trainer.next_seed,
                        "source_checkpoint_sha256": sha256_file(backup),
                        "archived_pre_migration_best": archived_best,
                        "old_git_commit": previous["contract"]["git_commit"],
                        "new_git_commit": repository["commit"],
                        "old_native_source_sha256": previous["contract"][
                            "native_source_sha256"
                        ],
                        "new_native_source_sha256": source_digest,
                    }
                    _append_record(metrics_path, {
                        "environment_steps": trainer.environment_steps,
                        "update": trainer.update,
                        "environment_migration": migration,
                    })
                    manifest["git"] = repository
                    manifest["native_source_sha256"] = source_digest
                    manifest["native_artifact"] = native_artifact()
                    manifest.setdefault("environment_migrations", []).append(migration)
                    _atomic_json(manifest_path, manifest)
                else:
                    load_checkpoint(latest, trainer)
            if trainer.environment_steps >= target_steps:
                raise ValueError(f"stage target already reached: {trainer.environment_steps}")

            last_eval = _last_evaluation_step(metrics_path)
            baseline_evaluation = _baseline_evaluation(metrics_path)
            if trainer.environment_steps == 0 and last_eval < 0:
                baseline = asdict(evaluate(
                    trainer.model, profile, tuple(periodic_seeds), device=device,
                    max_steps=evaluation_max_steps,
                    max_boundary_visits=ppo.max_boundary_visits,
                    failure_progress_scale=ppo.failure_progress_scale,
                ))
                record = {
                    "update": 0, "environment_steps": 0, "evaluation": baseline,
                    "baseline": True,
                }
                update_best_checkpoint(
                    output, best_checkpoint_record(baseline, update=0),
                    save=lambda path: save_checkpoint(path, trainer),
                )
                _append_record(metrics_path, record)
                baseline_evaluation = baseline

            next_save = ((trainer.environment_steps // 500_000) + 1) * 500_000
            next_eval = ((trainer.environment_steps // evaluate_every) + 1) * evaluate_every
            while trainer.environment_steps < target_steps and not controller.requested:
                update_started = time.perf_counter()
                metrics = trainer.train_update()
                non_finite = {
                    key: value for key, value in metrics.items()
                    if not math.isfinite(value)
                }
                if non_finite:
                    raise FloatingPointError(f"non-finite PPO metrics: {non_finite}")
                elapsed = time.perf_counter() - update_started
                record = {
                    "update": trainer.update,
                    "environment_steps": trainer.environment_steps,
                    "episodes": trainer.episodes,
                    "update_seconds": elapsed,
                    "decisions_per_second": workers_count * ppo.rollout_steps / elapsed,
                    **metrics,
                }
                if trainer.environment_steps >= next_eval:
                    try:
                        evaluation = asdict(evaluate(
                            trainer.model, profile, tuple(periodic_seeds), device=device,
                            max_steps=evaluation_max_steps,
                            max_boundary_visits=ppo.max_boundary_visits,
                            failure_progress_scale=ppo.failure_progress_scale,
                            stop_requested=lambda: controller.requested,
                        ))
                        record["evaluation"] = evaluation
                        if baseline_evaluation is not None:
                            record["progress_from_baseline"] = _progress_from_baseline(
                                baseline_evaluation, evaluation,
                            )
                        record["best_checkpoint_updated"] = update_best_checkpoint(
                            output, best_checkpoint_record(evaluation, update=trainer.update),
                            save=lambda path: save_checkpoint(path, trainer),
                        )
                        while next_eval <= trainer.environment_steps:
                            next_eval += evaluate_every
                    except InterruptedError:
                        record["evaluation_interrupted"] = True
                _append_record(metrics_path, record)
                if trainer.environment_steps >= next_save:
                    save_checkpoint(
                        output / f"checkpoint-steps-{trainer.environment_steps:012d}.pt",
                        trainer,
                    )
                    save_checkpoint(latest, trainer)
                    while next_save <= trainer.environment_steps:
                        next_save += 500_000

            save_checkpoint(latest, trainer)
            completed = trainer.environment_steps >= target_steps
            if completed and not controller.requested:
                best = output / "best_success.pt"
                selected = best if best.exists() else latest
                export_policy_artifact(
                    selected, output / f"{output.name}-{args.stage}.pt",
                    ascension_min=0, ascension_max=0, goal="FULLRUN",
                )
            if args.stage == "train" and completed and not controller.requested:
                save_checkpoint(output / "final.pt", trainer)
                selected_payload = torch.load(
                    selected, map_location="cpu", weights_only=False,
                )
                trainer.model.load_state_dict(selected_payload["model"])
                final_result = asdict(evaluate(
                    trainer.model, profile, tuple(final_seeds), device=device,
                    max_steps=evaluation_max_steps,
                    max_boundary_visits=ppo.max_boundary_visits,
                    failure_progress_scale=ppo.failure_progress_scale,
                ))
                _atomic_json(output / "final-evaluation.json", {
                    "schema": "sls-final-evaluation-v1",
                    "checkpoint": selected.name,
                    "seeds": [final_seeds.start, final_seeds.stop],
                    "result": final_result,
                })
                export_policy_artifact(
                    selected, output / f"{output.name}.pt",
                    ascension_min=0, ascension_max=0, goal="FULLRUN",
                )

        status = "INTERRUPTED" if controller.requested else "COMPLETE"
        manifest["stages"][args.stage] = {
            "status": status,
            "target_environment_steps": target_steps,
            "completed_environment_steps": trainer.environment_steps,
            "finished_unix": time.time(),
            "stop_signal": controller.signal_name,
        }
        manifest.update({
            "status": status,
            "active_stage": None,
            "environment_steps": trainer.environment_steps,
            "updates": trainer.update,
            "episodes": trainer.episodes,
            "termination_counts": dict(trainer.termination_counts),
            "cuda_peak_memory_bytes": torch.cuda.max_memory_allocated(device),
        })
        _atomic_json(manifest_path, manifest)
        if args.stage == "train" and status == "COMPLETE":
            bundle_names = (
                "run-manifest.json", "metrics.jsonl", "best_success.pt",
                "latest.pt", "final.pt", "final-evaluation.json",
                f"{output.name}.pt",
            )
            _atomic_json(output / "training-bundle.json", {
                "schema": "sls-training-bundle-v1",
                "files": {
                    name: sha256_file(output / name)
                    for name in bundle_names
                    if (output / name).is_file()
                },
                "crash_files": sorted(
                    str(path.relative_to(output))
                    for path in (output / "crashes").glob("*.json")
                ) if (output / "crashes").is_dir() else [],
                "live_action_journals": "collected locally under logs/",
            })
        # A handled signal is a successful safe shutdown.  The manifest carries
        # INTERRUPTED state; a non-zero process exit incorrectly marks the batch
        # step FAILED even though latest.pt was saved atomically.
        return 0
    except BaseException as error:
        manifest.update({
            "status": "FAILED",
            "active_stage": args.stage,
            "error_type": type(error).__name__,
            "error": str(error),
            "failed_unix": time.time(),
        })
        _atomic_json(manifest_path, manifest)
        raise


if __name__ == "__main__":
    raise SystemExit(main())
