"""Train the canonical FullRun policy with native simulator workers."""

from __future__ import annotations

import argparse
from dataclasses import asdict
import hashlib
import json
import math
from pathlib import Path
import platform
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
from sls.validation.readiness import readiness_report


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


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--config", type=Path, default=ROOT / "configs" / "train" / "full_run.toml")
    parser.add_argument("--resume", type=Path)
    args = parser.parse_args()
    config_bytes = args.config.read_bytes()
    payload = tomllib.loads(config_bytes.decode("utf-8"))
    run = payload["run"]
    profile = PROFILES[str(run["profile"])]
    if bool(run.get("require_readiness", False)):
        readiness_config = ROOT / str(
            run.get("readiness_config", "configs/validation/act1_training.toml")
        )
        with readiness_config.open("rb") as stream:
            requirements = tomllib.load(stream)["requirements"]
        evidence_root = ROOT / str(
            run.get("readiness_root", "validation-results/truth")
        )
        readiness = readiness_report(evidence_root, requirements)
        if not readiness["ready"]:
            print(json.dumps({
                "error": "ACT1_PARITY_READINESS_FAILED",
                "failures": readiness["failures"],
            }, sort_keys=True), file=sys.stderr)
            return 2
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
    started = time.time()
    run_manifest: dict[str, object] = {
        "schema": "sls-ppo-run-manifest-v1",
        "status": "RUNNING",
        "profile": profile.profile_id,
        "curriculum_version": profile.version,
        "seed": seed,
        "workers": int(run["workers"]),
        "updates": int(run["updates"]),
        "device": str(device),
        "python": sys.version,
        "platform": platform.platform(),
        "torch": torch.__version__,
        "cuda_available": torch.cuda.is_available(),
        "cuda_version": torch.version.cuda,
        "gpu": torch.cuda.get_device_name(device) if str(device).startswith("cuda") else None,
        "config_sha256": hashlib.sha256(config_bytes).hexdigest(),
        "encoding_schema": ENCODING_SCHEMA,
        "vocabulary_sha256": vocabulary_hash(),
        "model": model.config.to_dict(),
        "ppo": ppo.to_dict(),
        "evaluation_seeds": list(_evaluation_seeds(run)),
        "evaluation_max_steps": int(run.get("evaluation_max_steps", 512)),
        "started_unix": started,
    }
    _atomic_json(output / "run-manifest.json", run_manifest)
    if str(device).startswith("cuda"):
        torch.cuda.reset_peak_memory_stats(device)
    with WorkerPool(profile, int(run["workers"])) as workers:
        trainer = PPOTrainer(model, workers, ppo, device=device, seed=seed)
        if args.resume is not None:
            load_checkpoint(args.resume, trainer)
        updates = int(run["updates"])
        save_interval = int(run.get("save_interval", 10))
        evaluate_interval = int(run.get("evaluate_interval", save_interval))
        evaluation_seeds = _evaluation_seeds(run)
        log_path = output / "metrics.jsonl"
        while trainer.update < updates:
            update_started = time.perf_counter()
            metrics = trainer.train_update()
            non_finite = {
                key: value for key, value in metrics.items()
                if not math.isfinite(value)
            }
            if non_finite:
                raise FloatingPointError(f"non-finite PPO metrics: {non_finite}")
            update_seconds = time.perf_counter() - update_started
            record: dict[str, object] = {
                "update": trainer.update,
                "episodes": trainer.episodes,
                "update_seconds": update_seconds,
                "decisions_per_second": (
                    int(run["workers"]) * ppo.rollout_steps / update_seconds
                ),
                **metrics,
            }
            if trainer.update % evaluate_interval == 0:
                result = evaluate(
                    trainer.model, profile, evaluation_seeds, device=device,
                    max_steps=int(run.get("evaluation_max_steps", 512)),
                )
                record["evaluation"] = asdict(result)
            with log_path.open("a", encoding="utf-8") as stream:
                stream.write(json.dumps(record, sort_keys=True) + "\n")
            print(json.dumps(record, sort_keys=True), flush=True)
            if trainer.update % save_interval == 0:
                save_checkpoint(output / f"checkpoint-{trainer.update:08d}.pt", trainer)
                save_checkpoint(output / "latest.pt", trainer)
        save_checkpoint(output / "latest.pt", trainer)
    run_manifest.update({
        "status": "COMPLETE",
        "finished_unix": time.time(),
        "elapsed_seconds": time.time() - started,
        "completed_updates": trainer.update,
        "episodes": trainer.episodes,
        "cuda_peak_memory_bytes": (
            torch.cuda.max_memory_allocated(device)
            if str(device).startswith("cuda") else 0
        ),
    })
    _atomic_json(output / "run-manifest.json", run_manifest)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
