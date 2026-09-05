"""Verify and initialize a new v4 training chain from the audited 10M v3 policy."""

from __future__ import annotations

import argparse
import json
import math
import os
import random
import shutil
import sys
import tempfile
import tomllib
import warnings
from dataclasses import replace
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))
sys.path.insert(0, str(ROOT / "src"))
os.environ.setdefault("CUBLAS_WORKSPACE_CONFIG", ":4096:8")

import torch

from sls.content.scope import validate_scope_source_hashes
from sls.curriculum import IRONCLAD_A0_FULLRUN
from sls.model import ModelConfig
from sls.rl import (
    PPOConfig,
    PPOTrainer,
    ShardedWorkerPool,
    load_checkpoint,
    save_checkpoint,
)
from sls.rl.episode_limit import EpisodeLimitState
from sls.rl.model_migration import migrate_v3_policy
from sls.rl.training_contract import (
    git_state,
    local_source_digest,
    native_artifact,
    native_source_digest,
    runtime_contract,
    sha256_file,
    training_validation_digest,
)
from tools.train_full_run import (
    MANIFEST_SCHEMA,
    _atomic_json,
    _load_benchmark,
    _training_identity,
)


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source", type=Path, required=True)
    parser.add_argument("--config", type=Path, required=True)
    parser.add_argument("--verify-only", action="store_true", help="CPU micro-check; never authorizes a server run")
    parser.add_argument("--report", type=Path)
    parser.add_argument("--preflight-report", type=Path, default=ROOT / "local/runs/preflight.json")
    args = parser.parse_args()
    config_bytes = args.config.read_bytes()
    config = tomllib.loads(config_bytes.decode())
    run = config["run"]
    output = ROOT / run["output"]
    if not args.verify_only and output.exists():
        raise FileExistsError(f"refusing to overwrite a training chain: {output}")
    validate_scope_source_hashes(ROOT)
    source_hash = sha256_file(args.source)
    if source_hash != run.get("warm_start_source_sha256"):
        raise ValueError("source file is not the audited 10M checkpoint named by the config")
    source = torch.load(args.source, map_location="cpu", weights_only=False)
    old_state = source["trainer"]
    if (old_state["environment_steps"], old_state["update"]) != (10_002_432, 814):
        raise ValueError("source must be the audited 10,002,432-step / update-814 milestone")
    if source["contract"]["profile"] != IRONCLAD_A0_FULLRUN:
        raise ValueError("source profile differs from audited FullRun profile")
    random.seed(int(run["seed"]))
    torch.manual_seed(int(run["seed"]))
    torch.use_deterministic_algorithms(True)
    torch.backends.cudnn.benchmark = False
    if torch.cuda.is_available():
        torch.set_float32_matmul_precision("high")
    if args.verify_only:
        torch.set_num_threads(2)
    model, report = migrate_v3_policy(source)
    if model.config != ModelConfig(**config["model"]):
        raise ValueError("target model dimensions differ from migrated policy")
    ppo = PPOConfig(**config["ppo"])
    artifact = native_artifact()
    digest = native_source_digest()
    if not artifact or artifact.get("source_sha256") != digest:
        raise RuntimeError("native binary is missing or stale; rebuild before migration")
    if args.verify_only:
        workers, shards, device = 2, 1, "cpu"
        ppo = replace(ppo, rollout_steps=64, recurrent_sequence_length=64, minibatch_sequences=2)
    else:
        if not sys.platform.startswith("linux") or not os.environ.get("SLURM_JOB_ID"):
            raise RuntimeError("production migration must run in a Linux Slurm allocation")
        if not torch.cuda.is_available():
            raise RuntimeError("production migration requires CUDA")
        workers, shards = _load_benchmark(ROOT / run["benchmark"], native_digest=digest,
                                          native_binary_sha256=artifact["sha256"])
        device = "cuda"
        preflight = json.loads(args.preflight_report.read_text())
        if (preflight.get("ok") is not True or preflight.get("device") != "cuda"
                or preflight.get("native_source_sha256") != digest
                or (preflight.get("native_artifact") or {}).get("sha256") != artifact["sha256"]):
            raise ValueError("missing/stale/failed A100 preflight")
    identity = _training_identity(config, workers=workers, shards=shards)
    limit = min(int(run[k]) for k in ("periodic_evaluation_seed_start", "final_evaluation_seed_start"))
    output.parent.mkdir(parents=True, exist_ok=True)
    # This directory is freshly created under the output parent and is the only
    # directory this tool's TemporaryDirectory context may remove.
    with tempfile.TemporaryDirectory(prefix="warm-start-check-", dir=output.parent) as temporary:
        temporary = Path(temporary)
        with ShardedWorkerPool(IRONCLAD_A0_FULLRUN, workers, shard_count=shards,
                               crash_dump_dir=output.parent / "warm-start-crashes") as pool:
            trainer = PPOTrainer(model, pool, ppo, device=device, seed=int(run["seed"]),
                                 native_contract_digest=digest, git_commit=str(git_state()["commit"]),
                                 training_config_digest=identity, training_seed_limit=limit)
            trainer.environment_steps = int(old_state["environment_steps"])
            trainer.update = int(old_state["update"])
            trainer.episodes = int(old_state["episodes"])
            first_seed = int(old_state["next_seed"])
            trainer.decisions = pool.reset(range(first_seed, first_seed + workers))
            trainer.next_seed = first_seed + workers
            trainer.episode_limits = [EpisodeLimitState.initial(d) for d in trainer.decisions]
            initial = save_checkpoint(temporary / "initial.pt", trainer)
            first = trainer.train_update()
            if any(not math.isfinite(value) for value in first.values()):
                raise RuntimeError("non-finite training metric during verification")
            if not args.verify_only and (first["approx_kl_final"] > 0.05 or first["clip_fraction"] > 0.5):
                warnings.warn("First-update KL/clip is elevated; inspect learning diagnostics. "
                              "This is not a checkpoint compatibility failure.", RuntimeWarning)
            learned = {k: v.detach().cpu().clone() for k, v in model.state_dict().items()}
            if not any(not torch.equal(learned[k], source["model"][k]) for k in learned
                       if learned[k].shape == source["model"][k].shape):
                raise RuntimeError("verification update did not change any preserved parameter")
            load_checkpoint(initial, trainer)
            second = trainer.train_update()
            if first != second or any(not torch.equal(v, model.state_dict()[k].cpu()) for k, v in learned.items()):
                raise RuntimeError("migrated checkpoint failed exact update replay in target runtime")
            if not all(torch.isfinite(v).all() for v in model.state_dict().values()):
                raise RuntimeError("non-finite model after verification update")
            # The verification updates are discarded. Production still starts at 10M.
            load_checkpoint(initial, trainer)
            report.update({
                "source_checkpoint_sha256": source_hash, "source_environment_steps": trainer.environment_steps,
                "source_native_source_sha256": source["contract"]["native_source_sha256"],
                "source_model_config": source["contract"]["model"],
                "source_ppo": source["contract"]["ppo"],
                "source_update": trainer.update, "target_stage": "train", "validation_passed": True,
                "production_ready": not args.verify_only, "device": device, "workers": workers, "shards": shards,
                "verified_update": first, "verification_updates_discarded": True,
                "native_source_sha256": digest, "native_artifact": artifact,
                "training_identity_sha256": identity,
                "source_tree_sha256": local_source_digest(("src", "tools", "configs")),
                "training_validation_sha256": training_validation_digest(),
                "runtime": runtime_contract(torch),
                "git": git_state(), "target_ppo": ppo.to_dict(),
                "initial_checkpoint_sha256": sha256_file(initial),
            })
            if not args.verify_only:
                # Exclusive mkdir means existing checkpoints are never overwritten.
                output.mkdir()
                shutil.copy2(initial, output / "latest.pt")
                shutil.copy2(initial, output / "initial-10m.pt")
                shutil.copy2(args.config, output / "training-config.toml")
                _atomic_json(output / "migration.json", report)
                _atomic_json(output / "run-manifest.json", {
                    "schema": MANIFEST_SCHEMA, "status": "MIGRATION_READY", "stages": {},
                    "training_identity_sha256": identity, "initialization": report,
                    "profile": "IRONCLAD_A0_FULLRUN", "simulator_only": True,
                })
    if sha256_file(args.source) != source_hash:
        raise RuntimeError("source checkpoint changed during migration")
    if args.report:
        args.report.parent.mkdir(parents=True, exist_ok=True)
        _atomic_json(args.report, report)
    print(json.dumps(report, sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
