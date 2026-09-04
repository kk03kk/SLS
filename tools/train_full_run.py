"""Train one exact-resume Ironclad A0 FullRun recurrent PPO run."""

from __future__ import annotations

import argparse
import hashlib
import json
import math
import os
import platform
import random
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
os.environ.setdefault("CUBLAS_WORKSPACE_CONFIG", ":4096:8")

import torch

from sls.content.scope import IRONCLAD_A0_SCOPE_ID, ironclad_a0_scope_hash
from sls.curriculum import CURRICULUM_PROFILES_BY_ID
from sls.model import ENCODING_SCHEMA, ModelConfig, Policy, vocabulary_hash
from sls.rl import (
    CheckpointContractMismatch,
    PPOConfig,
    PPOTrainer,
    ShardedWorkerPool,
    evaluate,
    load_checkpoint,
    load_checkpoint_environment_migration,
    load_checkpoint_runtime_rebind,
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
MANIFEST_SCHEMA = "sls-recurrent-ppo-run-v2"
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


def _can_resume_finalization(
    *,
    stage_name: str,
    environment_steps: int,
    target_steps: int,
    manifest_status: object,
) -> bool:
    """Allow a train job to finish evaluation/export after reaching its step target."""

    return (
        stage_name == "train"
        and environment_steps >= target_steps
        and manifest_status != "COMPLETE"
    )


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
    native_digest: str,
    native_binary_sha256: str,
) -> tuple[int, int]:
    payload = json.loads(path.read_text(encoding="utf-8"))
    if payload.get("schema") != BENCHMARK_SCHEMA:
        raise ValueError("worker benchmark schema is incompatible")
    if payload.get("native_source_sha256") != native_digest:
        raise ValueError("worker benchmark belongs to different simulator sources")
    benchmark_artifact = payload.get("native_artifact") or {}
    if benchmark_artifact.get("sha256") != native_binary_sha256:
        raise ValueError("worker benchmark used a different native simulator binary")
    return int(payload["selected_workers"]), int(payload["selected_shards"])


def _training_identity(
    payload: dict[str, object], *, workers: int, shards: int,
) -> str:
    run = dict(payload["run"])
    diagnostic = {
        key: int(run[key])
        for key in (
            "diagnostic_evaluation_seed_start",
            "diagnostic_evaluation_seed_count",
            "diagnostic_rotation_stride",
        )
        if key in run
    }
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
        "diagnostic_evaluation": diagnostic,
        "model": payload["model"],
        "ppo": payload["ppo"],
        "stages": payload["stages"],
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


def _promotion_passes(evaluation: dict[str, object], stage: dict[str, object]) -> bool:
    episodes = int(evaluation["episodes"])
    failures = (
        int(evaluation.get("backend_errors", 0))
        + int(evaluation.get("backend_truncations", 0))
        + int(evaluation.get("step_limits", 0))
        + int(evaluation.get("cycle_limits", 0))
        + int(evaluation.get("self_loops", 0))
        + int(evaluation.get("timeouts", 0))
    )
    boss_rates = dict(evaluation.get("boss_success_rate") or {})
    minimum_boss_success = min(
        (float(value) for value in boss_rates.values()), default=0.0,
    )
    act1_bosses = {
        "ACT_1:HEXAGHOST", "ACT_1:SLIME_BOSS", "ACT_1:THE_GUARDIAN",
    }
    minimum_act1_boss_success = min(
        (float(boss_rates.get(name, 0.0)) for name in act1_bosses),
        default=0.0,
    )
    return (
        failures == 0
        and float(evaluation["success_rate"]) >= float(stage["minimum_success_rate"])
        and float(evaluation["reached_act2_rate"])
        >= float(stage.get("minimum_reached_act2_rate", 0.0))
        and float(evaluation["reached_act3_rate"])
        >= float(stage.get("minimum_reached_act3_rate", 0.0))
        and episodes >= int(stage.get("minimum_evaluation_episodes", 1))
        and minimum_boss_success
        >= float(stage.get("minimum_boss_success_rate", 0.0))
        and minimum_act1_boss_success
        >= float(stage.get("minimum_act1_boss_success_rate", 0.0))
    )


def _require_predecessor_promotion(
    manifest: dict[str, object], stage_name: str,
) -> None:
    predecessor = {"pilot": "smoke", "train": "pilot"}.get(stage_name)
    if predecessor is None:
        return
    initialization = manifest.get("initialization")
    if (
        stage_name == "train"
        and isinstance(initialization, dict)
        and initialization.get("schema") in {
            "sls-audited-training-migration-v1",
            "sls-training-migration-v2",
        }
        and initialization.get("target_stage") == "train"
        and initialization.get("validation_passed") is True
    ):
        return
    stages = manifest.get("stages")
    previous = stages.get(predecessor) if isinstance(stages, dict) else None
    if not isinstance(previous, dict) or previous.get("status") != "COMPLETE":
        raise ValueError(f"{stage_name} requires completed {predecessor} stage")
    if previous.get("promotion_passed") is not True:
        raise ValueError(f"{stage_name} requires {predecessor} promotion gate to pass")


def _require_interrupted_smoke_resume(
    manifest: dict[str, object], training_identity: str,
) -> None:
    """Allow only an exact continuation of an interrupted smoke stage."""

    stages = manifest.get("stages")
    smoke = stages.get("smoke") if isinstance(stages, dict) else None
    if (
        manifest.get("schema") != MANIFEST_SCHEMA
        or manifest.get("training_identity_sha256") != training_identity
        or manifest.get("status") != "INTERRUPTED"
        or not isinstance(smoke, dict)
        or smoke.get("status") != "INTERRUPTED"
        or smoke.get("profile") != "IRONCLAD_A0_ACT1"
    ):
        raise FileExistsError(
            "smoke refuses to overwrite a non-interrupted or incompatible training chain"
        )


def _validate_existing_manifest(
    manifest: dict[str, object], *, identity: str, resume: str,
) -> None:
    if manifest.get("schema") != MANIFEST_SCHEMA:
        raise ValueError("existing run manifest belongs to another training chain")
    if (
        manifest.get("training_identity_sha256") != identity
        and resume != "environment-migration"
    ):
        raise ValueError(
            "training schedule changed; use explicit environment-migration"
        )


def _append_record(path: Path, record: dict[str, object]) -> None:
    with path.open("a", encoding="utf-8") as stream:
        stream.write(json.dumps(record, sort_keys=True) + "\n")
        stream.flush()
        os.fsync(stream.fileno())
    print(json.dumps(record, sort_keys=True), flush=True)


def _profile_id(contract: object) -> str | None:
    if not isinstance(contract, dict):
        return None
    profile = contract.get("profile")
    return getattr(profile, "profile_id", None)


def _validate_completed_migration_lineage(
    latest_payload: dict[str, object],
    backup: Path,
    manifest: dict[str, object],
    *,
    target_profile: str,
) -> dict[str, object]:
    """Prove that a retained backup is the source of the active curriculum chain."""

    if not backup.is_file():
        raise FileNotFoundError(f"completed environment migration has no backup: {backup}")
    source_sha256 = sha256_file(backup)
    migrations = manifest.get("learning_migrations")
    records = migrations if isinstance(migrations, list) else []
    matching = [
        value for value in records
        if isinstance(value, dict)
        and value.get("schema") == "sls-learning-environment-migration-v2"
        and value.get("source_checkpoint_sha256") == source_sha256
        and value.get("new_profile") == target_profile
    ]
    if len(matching) != 1:
        raise FileExistsError(
            "environment-migration backup has no unique recorded source identity"
        )
    migration = matching[0]
    # Verify the digest before deserializing the retained source checkpoint.
    source = torch.load(backup, map_location="cpu", weights_only=False)
    if source.get("schema") != TRAINING_CHECKPOINT_SCHEMA:
        raise ValueError("environment-migration backup schema is incompatible")
    source_contract = source.get("contract")
    latest_contract = latest_payload.get("contract")
    if not isinstance(source_contract, dict) or not isinstance(latest_contract, dict):
        raise ValueError("environment-migration checkpoint contract is missing")
    if _profile_id(latest_contract) != target_profile:
        raise ValueError("latest checkpoint is not the recorded migration target")
    allowed_migration_changes = {
        "git_commit", "native_source_sha256", "content_scope_id",
        "content_scope_sha256",
        "profile", "curriculum_version", "training_config_sha256",
        "training_seed_limit",
    }
    incompatible_source_fields = {
        key for key in set(source_contract) | set(latest_contract)
        if key not in allowed_migration_changes
        and source_contract.get(key) != latest_contract.get(key)
    }
    if incompatible_source_fields:
        raise ValueError(
            "environment-migration source contract is incompatible: "
            + ", ".join(sorted(incompatible_source_fields))
        )
    checks = {
        "old_profile": _profile_id(source_contract),
        "old_git_commit": source_contract.get("git_commit"),
        "old_native_source_sha256": source_contract.get("native_source_sha256"),
        "old_content_scope_sha256": source_contract.get("content_scope_sha256"),
        "old_training_identity_sha256": source_contract.get("training_config_sha256"),
        "new_content_scope_sha256": latest_contract.get("content_scope_sha256"),
        "new_training_identity_sha256": latest_contract.get("training_config_sha256"),
    }
    mismatched = [
        key for key, expected in checks.items() if migration.get(key) != expected
    ]
    source_state = source.get("trainer")
    latest_state = latest_payload.get("trainer")
    if not isinstance(source_state, dict) or not isinstance(latest_state, dict):
        raise ValueError("environment-migration trainer state is missing")
    if (
        int(migration.get("environment_steps", -1))
        != int(source_state.get("environment_steps", -2))
        or int(migration.get("update", -1)) != int(source_state.get("update", -2))
    ):
        mismatched.append("source_progress")
    if (
        int(latest_state.get("environment_steps", -1))
        < int(migration.get("environment_steps", 0))
        or int(latest_state.get("update", -1)) < int(migration.get("update", 0))
    ):
        mismatched.append("latest_progress")
    if mismatched:
        raise ValueError(
            "environment-migration lineage is incompatible: "
            + ", ".join(sorted(set(mismatched)))
        )
    return migration


def _resume_or_migrate_environment(
    latest: Path,
    backup: Path,
    trainer: PPOTrainer,
    manifest: dict[str, object],
) -> tuple[dict[str, object] | None, str]:
    """Resume a completed migration exactly, or perform the first migration."""

    preview = torch.load(latest, map_location="cpu", weights_only=False)
    if preview.get("schema") != TRAINING_CHECKPOINT_SCHEMA:
        raise ValueError("unsupported training checkpoint")
    actual_profile = _profile_id(preview.get("contract"))
    target_profile = trainer.workers.profile.profile_id
    if actual_profile == target_profile:
        _validate_completed_migration_lineage(
            preview, backup, manifest, target_profile=target_profile,
        )
        try:
            load_checkpoint(latest, trainer)
            return None, "exact"
        except CheckpointContractMismatch:
            pass
        load_checkpoint_runtime_rebind(latest, trainer)
        return None, "runtime-rebind"

    if backup.exists():
        if sha256_file(backup) != sha256_file(latest):
            raise FileExistsError(
                "environment-migration source backup differs from the source checkpoint"
            )
    else:
        shutil.copy2(latest, backup)
    previous = load_checkpoint_environment_migration(latest, trainer)
    return dict(previous), "migration"


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
        "--stop-after-additional-steps", type=int,
        help=(
            "Run a bounded soak on the same exact-resume chain. The stop is "
            "rounded up to a whole PPO update and is not a training identity change."
        ),
    )
    args = parser.parse_args()
    if torch.cuda.is_available():
        torch.set_float32_matmul_precision("high")

    config_bytes = args.config.read_bytes()
    payload = tomllib.loads(config_bytes.decode("utf-8"))
    run = dict(payload["run"])
    stage = dict(payload["stages"][args.stage])
    periodic_seeds, final_seeds = _validate_seed_namespaces(run)
    diagnostic_seed_start = int(run.get("diagnostic_evaluation_seed_start", 0))
    diagnostic_seed_count = int(run.get("diagnostic_evaluation_seed_count", 0))
    diagnostic_stride = int(run.get("diagnostic_rotation_stride", diagnostic_seed_count))
    if bool(diagnostic_seed_start) != bool(diagnostic_seed_count):
        raise ValueError("diagnostic evaluation seed start/count must be configured together")
    if diagnostic_seed_count and diagnostic_stride < diagnostic_seed_count:
        raise ValueError("diagnostic seed rotation stride would reuse seeds")
    if diagnostic_seed_count:
        diagnostic_first = _seed_range(diagnostic_seed_start, diagnostic_seed_count)
        for held_out in (periodic_seeds, final_seeds):
            if diagnostic_first.stop > held_out.start and held_out.stop > diagnostic_first.start:
                raise ValueError("diagnostic and selection/final evaluation seeds overlap")
    repository = git_state()
    source_digest = native_source_digest()
    artifact = native_artifact()
    if artifact is None:
        raise RuntimeError("training requires the compiled native simulator")
    benchmark_path = ROOT / str(run["benchmark"])
    workers_count, shard_count = _load_benchmark(
        benchmark_path, native_digest=source_digest,
        native_binary_sha256=artifact["sha256"],
    )
    identity = _training_identity(payload, workers=workers_count, shards=shard_count)
    profile = CURRICULUM_PROFILES_BY_ID[str(stage["profile"])]
    expected_profiles = {
        "smoke": "IRONCLAD_A0_ACT1",
        "pilot": "IRONCLAD_A0_ACT2",
        "train": "IRONCLAD_A0_FULLRUN",
    }
    if profile.profile_id != expected_profiles[args.stage]:
        raise ValueError(
            f"{args.stage} must use curriculum profile {expected_profiles[args.stage]}"
        )
    seed = int(run["seed"])
    random.seed(seed)
    torch.manual_seed(seed)
    torch.use_deterministic_algorithms(bool(run.get("deterministic", True)))
    torch.backends.cudnn.benchmark = False
    device = str(run["device"])
    if device.startswith("cuda") and not torch.cuda.is_available():
        raise RuntimeError("training config requires CUDA but CUDA is unavailable")
    model = Policy(ModelConfig(**payload["model"]))
    ppo = PPOConfig(**payload["ppo"])
    target_steps = _positive_int(stage, "target_environment_steps")
    if args.stop_after_additional_steps is not None and args.stop_after_additional_steps <= 0:
        raise ValueError("--stop-after-additional-steps must be positive")
    evaluate_every = _positive_int(stage, "evaluate_every_steps")
    evaluation_max_steps = int(run["evaluation_max_steps"])
    output = ROOT / str(run["output"])
    latest = output / "latest.pt"
    manifest_path = output / "run-manifest.json"
    output_existed = output.exists()
    if manifest_path.exists():
        manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
        _validate_existing_manifest(
            manifest, identity=identity, resume=args.resume,
        )
    else:
        manifest = None
    if args.stage == "smoke":
        if latest.exists():
            if manifest is None:
                raise FileExistsError("smoke checkpoint has no matching run manifest")
            _require_interrupted_smoke_resume(manifest, identity)
        elif output_existed:
            raise FileExistsError(
                "smoke refuses an existing training directory without a resumable checkpoint"
            )
    if args.stage == "smoke" and args.resume != "auto":
        raise ValueError("smoke cannot use environment migration")
    if args.stage != "smoke" and not latest.exists():
        raise FileNotFoundError(f"{args.stage} requires the smoke/pilot checkpoint: {latest}")
    output.mkdir(parents=True, exist_ok=True)
    stage_output = output / "stages" / args.stage
    stage_output.mkdir(parents=True, exist_ok=True)
    metrics_path = stage_output / "metrics.jsonl"
    selection_output = stage_output / "selection"
    started = time.time()
    if manifest is None:
        manifest = {
            "schema": MANIFEST_SCHEMA,
            "simulator_only": True,
            "profile": str(run["profile"]),
            "curriculum": {
                name: str(value["profile"])
                for name, value in payload["stages"].items()
            },
            "training_identity_sha256": identity,
            "config_sha256": hashlib.sha256(config_bytes).hexdigest(),
            "git": repository,
            "native_source_sha256": source_digest,
            "native_artifact": artifact,
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
            "diagnostic_evaluation_seed_start": diagnostic_seed_start or None,
            "diagnostic_evaluation_seed_count": diagnostic_seed_count or None,
            "diagnostic_rotation_stride": diagnostic_stride or None,
            "created_unix": started,
            "stages": {},
        }
    _require_predecessor_promotion(manifest, args.stage)
    manifest.update({
        "status": "RUNNING",
        "active_stage": args.stage,
        "python": sys.version,
        "platform": platform.platform(),
        "hostname": socket.gethostname(),
        "torch": torch.__version__,
        "cuda": torch.version.cuda,
        "gpu": torch.cuda.get_device_name(device) if device.startswith("cuda") else None,
        "deterministic_algorithms": torch.are_deterministic_algorithms_enabled(),
        "slurm": _slurm_environment(),
    })
    _atomic_json(manifest_path, manifest)
    if device.startswith("cuda"):
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
                loaded_exactly = False
                previous: dict[str, object] | None = None
                checkpoint_preview = torch.load(
                    latest, map_location="cpu", weights_only=False,
                )
                previous_profile = _profile_id(checkpoint_preview.get("contract"))
                profile_changed = previous_profile != profile.profile_id
                if args.resume == "environment-migration" or profile_changed:
                    backup = output / f"latest.pre-{args.stage}-migration.pt"
                    previous, resume_mode = _resume_or_migrate_environment(
                        latest, backup, trainer, manifest,
                    )
                    loaded_exactly = previous is None
                    if resume_mode == "runtime-rebind":
                        rebind = {
                            "schema": "sls-exact-runtime-rebind-v1",
                            "environment_steps": trainer.environment_steps,
                            "update": trainer.update,
                            "source_checkpoint_sha256": sha256_file(latest),
                            "old_git_commit": checkpoint_preview["contract"]["git_commit"],
                            "new_git_commit": repository["commit"],
                            "old_native_source_sha256": checkpoint_preview["contract"][
                                "native_source_sha256"
                            ],
                            "new_native_source_sha256": source_digest,
                        }
                        print(json.dumps({
                            **rebind, "resume": "exact-runtime-rebind",
                        }, sort_keys=True), flush=True)
                        manifest["git"] = repository
                        manifest["native_source_sha256"] = source_digest
                        manifest["native_artifact"] = artifact
                        rebinds = manifest.setdefault("exact_runtime_rebinds", [])
                        if not isinstance(rebinds, list):
                            raise ValueError("runtime rebind manifest history is invalid")
                        rebinds.append(rebind)
                        _atomic_json(manifest_path, manifest)
                else:
                    load_checkpoint(latest, trainer)
                    loaded_exactly = True
                if not loaded_exactly:
                    assert previous is not None
                    save_checkpoint(latest, trainer)
                    migration = {
                        "schema": "sls-learning-environment-migration-v2",
                        "environment_steps": trainer.environment_steps,
                        "update": trainer.update,
                        "abandoned_environments": workers_count,
                        "first_fresh_seed": trainer.next_seed - workers_count,
                        "next_seed": trainer.next_seed,
                        "source_checkpoint_sha256": sha256_file(backup),
                        "old_profile": getattr(previous["contract"]["profile"], "profile_id", None),
                        "new_profile": profile.profile_id,
                        "old_git_commit": previous["contract"]["git_commit"],
                        "new_git_commit": repository["commit"],
                        "old_native_source_sha256": previous["contract"][
                            "native_source_sha256"
                        ],
                        "new_native_source_sha256": source_digest,
                        "old_content_scope_sha256": previous["contract"][
                            "content_scope_sha256"
                        ],
                        "new_content_scope_sha256": ironclad_a0_scope_hash(),
                        "old_training_identity_sha256": manifest.get(
                            "training_identity_sha256"
                        ),
                        "new_training_identity_sha256": identity,
                    }
                    _append_record(metrics_path, {
                        "environment_steps": trainer.environment_steps,
                        "update": trainer.update,
                        "environment_migration": migration,
                    })
                    manifest["git"] = repository
                    manifest["native_source_sha256"] = source_digest
                    manifest["native_artifact"] = artifact
                    manifest["content_scope_sha256"] = ironclad_a0_scope_hash()
                    manifest["training_identity_sha256"] = identity
                    manifest["config_sha256"] = hashlib.sha256(config_bytes).hexdigest()
                    manifest["periodic_evaluation_seeds"] = [
                        periodic_seeds.start, periodic_seeds.stop,
                    ]
                    manifest["final_evaluation_seeds"] = [
                        final_seeds.start, final_seeds.stop,
                    ]
                    manifest.setdefault("learning_migrations", []).append(migration)
                    _atomic_json(manifest_path, manifest)
                elif not loaded_exactly:
                    load_checkpoint(latest, trainer)
            resume_finalization = _can_resume_finalization(
                stage_name=args.stage,
                environment_steps=trainer.environment_steps,
                target_steps=target_steps,
                manifest_status=manifest.get("status"),
            )
            if trainer.environment_steps >= target_steps and not resume_finalization:
                raise ValueError(f"stage target already reached: {trainer.environment_steps}")
            run_until = target_steps
            if args.stop_after_additional_steps is not None:
                batch_steps = workers_count * ppo.rollout_steps
                updates = math.ceil(args.stop_after_additional_steps / batch_steps)
                run_until = min(
                    target_steps,
                    trainer.environment_steps + updates * batch_steps,
                )

            last_eval = _last_evaluation_step(metrics_path)
            baseline_evaluation = _baseline_evaluation(metrics_path)
            if last_eval < 0:
                baseline = asdict(evaluate(
                    trainer.model, profile, tuple(periodic_seeds), device=device,
                    max_steps=evaluation_max_steps,
                    max_boundary_visits=ppo.max_boundary_visits,
                    failure_progress_scale=ppo.failure_progress_scale,
                ))
                record = {
                    "update": trainer.update,
                    "environment_steps": trainer.environment_steps,
                    "evaluation": baseline,
                    "baseline": True,
                }
                update_best_checkpoint(
                    selection_output,
                    best_checkpoint_record(baseline, update=trainer.update),
                    save=lambda path: save_checkpoint(path, trainer),
                )
                _append_record(metrics_path, record)
                baseline_evaluation = baseline

            checkpoint_every = int(stage.get("checkpoint_every_steps", 500_000))
            if checkpoint_every <= 0:
                raise ValueError("checkpoint_every_steps must be positive")
            next_save = (
                (trainer.environment_steps // checkpoint_every) + 1
            ) * checkpoint_every
            next_eval = ((trainer.environment_steps // evaluate_every) + 1) * evaluate_every
            diagnose_every = int(stage.get("diagnose_every_steps", 0))
            next_diagnose = (
                ((trainer.environment_steps // diagnose_every) + 1) * diagnose_every
                if diagnose_every else 0
            )
            while trainer.environment_steps < run_until and not controller.requested:
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
                if diagnose_every and trainer.environment_steps >= next_diagnose:
                    rotation = next_diagnose // diagnose_every - 1
                    start = diagnostic_seed_start + rotation * diagnostic_stride
                    diagnostic_seeds = tuple(range(start, start + diagnostic_seed_count))
                    record["diagnostic_evaluation"] = asdict(evaluate(
                        trainer.model, profile, diagnostic_seeds, device=device,
                        max_steps=evaluation_max_steps,
                        max_boundary_visits=ppo.max_boundary_visits,
                        failure_progress_scale=ppo.failure_progress_scale,
                        stop_requested=lambda: controller.requested,
                    ))
                    record["diagnostic_seed_range"] = [
                        start, start + diagnostic_seed_count,
                    ]
                    while next_diagnose <= trainer.environment_steps:
                        next_diagnose += diagnose_every
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
                            selection_output, best_checkpoint_record(evaluation, update=trainer.update),
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
                        next_save += checkpoint_every

            save_checkpoint(latest, trainer)
            completed = trainer.environment_steps >= target_steps
            promoted = False
            if completed and not controller.requested:
                best = selection_output / "best_progress.pt"
                selected = best if best.exists() else latest
                best_record_path = selection_output / "best_progress.json"
                best_record = json.loads(best_record_path.read_text(encoding="utf-8"))
                promoted = _promotion_passes(best_record, stage)
                # FullRun artifacts are exported only after the never-used
                # final seed set passes below. Smoke/pilot keep their stage
                # artifacts for curriculum diagnostics.
                if promoted and args.stage != "train":
                    goal = {"smoke": "ACT1", "pilot": "ACT2", "train": "FULLRUN"}[args.stage]
                    export_policy_artifact(
                        selected, stage_output / f"{output.name}-{args.stage}.pt",
                        ascension_min=0, ascension_max=0, goal=goal,
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
                final_stage = dict(stage)
                final_stage["minimum_evaluation_episodes"] = int(
                    stage.get("minimum_final_evaluation_episodes", 1)
                )
                final_promoted = _promotion_passes(final_result, final_stage)
                _atomic_json(output / "final-evaluation.json", {
                    "schema": "sls-final-evaluation-v2",
                    "checkpoint": selected.name,
                    "seeds": [final_seeds.start, final_seeds.stop],
                    "result": final_result,
                    "promotion_passed": final_promoted,
                })
                if final_promoted:
                    export_policy_artifact(
                        selected, output / f"{output.name}.pt",
                        ascension_min=0, ascension_max=0, goal="FULLRUN",
                    )
                promoted = final_promoted

        soak_complete = (
            args.stop_after_additional_steps is not None
            and trainer.environment_steps < target_steps
            and not controller.requested
        )
        status = (
            "INTERRUPTED" if controller.requested
            else "SOAK_COMPLETE" if soak_complete
            else "COMPLETE"
        )
        manifest["stages"][args.stage] = {
            "status": status,
            "target_environment_steps": target_steps,
            "completed_environment_steps": trainer.environment_steps,
            "finished_unix": time.time(),
            "stop_signal": controller.signal_name,
            "profile": profile.profile_id,
            "promotion_passed": promoted,
        }
        manifest.update({
            "status": status,
            "active_stage": None,
            "environment_steps": trainer.environment_steps,
            "updates": trainer.update,
            "episodes": trainer.episodes,
            "termination_counts": dict(trainer.termination_counts),
            "cuda_peak_memory_bytes": (
                torch.cuda.max_memory_allocated(device) if device.startswith("cuda") else 0
            ),
        })
        _atomic_json(manifest_path, manifest)
        if args.stage == "train" and status == "COMPLETE":
            bundle_paths = [
                output / "run-manifest.json", output / "latest.pt",
                output / "final.pt", output / "final-evaluation.json",
                output / f"{output.name}.pt",
            ]
            for stage_name in STAGES:
                stage_dir = output / "stages" / stage_name
                bundle_paths.extend((
                    stage_dir / "metrics.jsonl",
                    stage_dir / "selection" / "best_progress.json",
                    stage_dir / "selection" / "best_progress.pt",
                    stage_dir / f"{output.name}-{stage_name}.pt",
                ))
            _atomic_json(output / "training-bundle.json", {
                "schema": "sls-training-bundle-v2",
                "files": {
                    path.relative_to(output).as_posix(): sha256_file(path)
                    for path in bundle_paths
                    if path.is_file()
                },
                "crash_files": sorted(
                    str(path.relative_to(output))
                    for path in (output / "crashes").glob("*.json")
                ) if (output / "crashes").is_dir() else [],
                "live_action_journals": "collected locally under local/logs/",
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
