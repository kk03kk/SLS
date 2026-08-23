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

from sls.curriculum import (
    IRONCLAD_A0_ACT1,
    IRONCLAD_A0_ACT2,
    IRONCLAD_A0_ACT3,
    IRONCLAD_A0_HEART,
)
from sls.model import ModelConfig, Policy
from sls.model.encoding import ENCODING_SCHEMA, vocabulary_hash
from sls.rl import PPOConfig, PPOTrainer, WorkerPool, evaluate, load_checkpoint, save_checkpoint
from sls.rl.training_contract import (
    canonical_digest, git_state, native_artifact, native_source_digest,
)
from sls.validation.readiness_lock import DEFAULT_LOCK, verify_readiness_lock


PROFILES = {
    item.profile_id: item
    for item in (IRONCLAD_A0_ACT1, IRONCLAD_A0_ACT2, IRONCLAD_A0_ACT3, IRONCLAD_A0_HEART)
}


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


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--config", type=Path, default=ROOT / "configs" / "train" / "full_run.toml")
    parser.add_argument("--resume", help="checkpoint path or 'auto' for OUTPUT/latest.pt")
    args = parser.parse_args()
    config_bytes = args.config.read_bytes()
    payload = tomllib.loads(config_bytes.decode("utf-8"))
    run = payload["run"]
    profile = PROFILES[str(run["profile"])]
    readiness_digest = "UNVERIFIED"
    if bool(run.get("require_readiness", False)):
        readiness_path = ROOT / str(run.get("readiness_lock", DEFAULT_LOCK.relative_to(ROOT)))
        try:
            readiness = verify_readiness_lock(
                readiness_path, require_clean=not bool(run.get("allow_dirty", False)),
            )
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
    model = Policy(ModelConfig(**payload.get("model", {})))
    ppo = PPOConfig(**payload.get("ppo", {}))
    output = ROOT / str(run.get("output", "runs/full-run"))
    output.mkdir(parents=True, exist_ok=True)
    resume_path = _resolve_resume(args.resume, output)
    started = time.time()
    source_digest = native_source_digest()
    repository = git_state()
    run_manifest: dict[str, object] = {
        "schema": "sls-ppo-run-manifest-v2",
        "status": "RUNNING",
        "profile": profile.profile_id,
        "curriculum_version": profile.version,
        "seed": seed,
        "workers": int(run["workers"]),
        "updates": int(run["updates"]),
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
        "resolved_config_sha256": canonical_digest(payload),
        "encoding_schema": ENCODING_SCHEMA,
        "vocabulary_sha256": vocabulary_hash(),
        "readiness_lock_sha256": readiness_digest,
        "native_source_sha256": source_digest,
        "native_artifact": native_artifact(),
        "model": model.config.to_dict(),
        "ppo": ppo.to_dict(),
        "evaluation_seeds": list(_evaluation_seeds(run)),
        "evaluation_max_steps": int(run.get("evaluation_max_steps", 512)),
        "started_unix": started,
        "resume": str(resume_path.resolve()) if resume_path else None,
    }
    _atomic_json(output / "run-manifest.json", run_manifest)
    if str(device).startswith("cuda"):
        torch.cuda.reset_peak_memory_stats(device)
    controller = StopController()
    controller.install()
    trainer: PPOTrainer | None = None
    try:
        with WorkerPool(profile, int(run["workers"])) as workers:
            trainer = PPOTrainer(
                model, workers, ppo, device=device, seed=seed,
                readiness_lock_digest=readiness_digest,
                native_contract_digest=source_digest,
            )
            if resume_path is not None:
                load_checkpoint(resume_path, trainer)
            updates = int(run["updates"])
            save_interval = int(run.get("save_interval", 10))
            evaluate_interval = int(run.get("evaluate_interval", save_interval))
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
                    "decisions_per_second": int(run["workers"]) * ppo.rollout_steps / update_seconds,
                    **metrics,
                }
                if not controller.requested and trainer.update % evaluate_interval == 0:
                    result = evaluate(
                        trainer.model, profile, evaluation_seeds, device=device,
                        max_steps=ppo.max_episode_steps,
                        max_boundary_visits=ppo.max_boundary_visits,
                    )
                    record["evaluation"] = asdict(result)
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
