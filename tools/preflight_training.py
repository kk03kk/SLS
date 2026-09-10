"""Fail-fast Linux/GPU training preflight with an exact-resume micro-test."""

from __future__ import annotations

import argparse
import hashlib
import json
import os
import platform
import shutil
import subprocess
import sys
import tempfile
from dataclasses import replace
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "src"))
os.environ.setdefault("CUBLAS_WORKSPACE_CONFIG", ":4096:8")

SEED_8335_DUMP = ROOT / "tests/fixtures/regressions/nus-worker-23-seed-8335-invalid-decision.json"
SEED_8335_SHA256 = "bbd6fa5644223ebee07681849d5e2654466cc21e27affbd69cf688a0404eb4a7"


def _parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser()
    parser.add_argument("--config", type=Path)
    parser.add_argument("--benchmark", type=Path)
    parser.add_argument("--checkpoint", type=Path,
                        help="Verify an actual resume checkpoint with the full configured worker layout")
    parser.add_argument("--allow-cpu", action="store_true")
    parser.add_argument("--skip-build", action="store_true")
    parser.add_argument("--output", type=Path, default=ROOT / "local/runs/preflight.json")
    parser.add_argument(
        "--allow-dirty", action="store_true",
        help="deprecated; local source digests are authoritative",
    )
    parser.add_argument("--jobs", type=int, default=min(os.cpu_count() or 4, 16))
    return parser


def main() -> int:
    args = _parser().parse_args()
    checks: dict[str, object] = {}
    try:
        if args.checkpoint and not (args.config and args.benchmark):
            raise ValueError("checkpoint preflight requires --config and --benchmark")
        if platform.system() != "Linux" and not args.allow_cpu:
            raise RuntimeError("server preflight requires Linux")
        if platform.machine().lower() not in {"x86_64", "amd64"}:
            raise RuntimeError(f"unsupported Linux architecture: {platform.machine()}")
        if sys.version_info < (3, 12):
            raise RuntimeError("Python 3.12 or newer is required")
        if not args.skip_build and not (os.environ.get("CXX") or shutil.which("c++") or shutil.which("g++")):
            raise RuntimeError("C++ compiler not found; load a compiler module first")
        import sls
        if ROOT not in Path(sls.__file__).resolve().parents:
            raise RuntimeError("sls is not imported from this checkout; install with pip -e")
        if not args.skip_build:
            subprocess.run([sys.executable, str(ROOT / "tools" / "build_native.py"), "--jobs", str(args.jobs)], cwd=ROOT, check=True)
        import torch
        torch.use_deterministic_algorithms(True)
        torch.backends.cudnn.benchmark = False
        if torch.cuda.is_available():
            torch.set_float32_matmul_precision("high")
        from replay_failed_state import replay_dump

        from sls.backends.simulator import SimulatorBackend
        from sls.content.scope import IRONCLAD_A0_SCOPE_ID, ironclad_a0_scope_hash
        from sls.curriculum import CURRICULUM_PROFILES_BY_ID, IRONCLAD_A0_FULLRUN
        from sls.model import ENCODING_SCHEMA, ModelConfig, Policy, PolicyBatch
        from sls.rl import (
            PPOConfig,
            PPOTrainer,
            ShardedWorkerPool,
            load_checkpoint,
            save_checkpoint,
        )
        from sls.rl.preparation import read_config, workload_contract
        from sls.rl.training_contract import (
            git_state,
            native_artifact,
            native_source_digest,
        )

        repository = git_state()
        if ENCODING_SCHEMA != "sls-policy-input-v5":
            raise RuntimeError("preflight requires the policy v5 encoding contract")
        payload = read_config(args.config) if args.config else None
        torch.use_deterministic_algorithms(bool(payload["run"].get("deterministic", True)) if payload else True)
        profile = CURRICULUM_PROFILES_BY_ID[payload["run"]["profile"]] if payload else IRONCLAD_A0_FULLRUN
        decision = SimulatorBackend(profile).reset(0)
        if decision.terminal or not decision.actions:
            raise RuntimeError("simulator smoke produced an invalid Decision")
        if hashlib.sha256(SEED_8335_DUMP.read_bytes()).hexdigest() != SEED_8335_SHA256:
            raise RuntimeError("seed 8335 regression fixture provenance is stale")
        replayed = replay_dump(SEED_8335_DUMP)
        if replayed["terminal"] or replayed["screen"] != "COMBAT_REWARD" or not replayed["actions"]:
            raise RuntimeError("seed 8335 regression no longer reaches a reward Decision")

        if not args.allow_cpu and not torch.cuda.is_available():
            raise RuntimeError("CUDA GPU is not visible to PyTorch")
        device = "cuda" if torch.cuda.is_available() else "cpu"
        model_config = ModelConfig(**payload["model"]) if payload else ModelConfig(
            embedding_dim=32, transformer_layers=1, attention_heads=4,
            feedforward_dim=64, recurrent_hidden_dim=64,
        )
        ppo = PPOConfig(**payload["ppo"]) if payload else PPOConfig(
            rollout_steps=1, recurrent_sequence_length=1, minibatch_sequences=1, epochs=1,
        )
        layout = json.loads(args.benchmark.read_text()) if args.benchmark else {}
        if payload and not args.benchmark:
            ppo = replace(ppo, rollout_steps=ppo.recurrent_sequence_length)
        model = Policy(model_config).to(device)
        with ShardedWorkerPool(profile, int(layout.get("selected_workers", 1)),
                               shard_count=int(layout.get("selected_shards", 1))) as workers:
            trainer = PPOTrainer(
                model, workers, ppo,
                device=device, seed=918273,
                training_seed_limit=1_000_000_000_000,
                native_contract_digest=native_source_digest(),
                git_commit=str(repository["commit"]),
                training_config_digest="PREFLIGHT_MICRO_RESUME",
            )
            if args.checkpoint:
                from sls.rl.checkpoint import load_checkpoint_runtime_rebind
                from tools.train_full_run import _training_identity
                trainer.training_config_digest = _training_identity(
                    payload, workers=workers.size, shards=workers.shard_count,
                    checkpoint_reference=read_config(args.checkpoint.parent / "training-config.toml"),
                )
                from sls.rl.preparation import training_seed_limit
                trainer.training_seed_limit = training_seed_limit(payload["run"])
                load_checkpoint_runtime_rebind(args.checkpoint, trainer)
            decision = trainer.decisions[0]
            batch = PolicyBatch.from_decisions((decision,), model.config).to(device)
            loss = model(*batch.model_inputs()).logits.sum() + model(*batch.model_inputs()).value.sum()
            loss.backward()
            args.output.parent.mkdir(parents=True, exist_ok=True)
            with tempfile.TemporaryDirectory(prefix="sls-preflight-", dir=args.output.parent) as directory:
                if not args.checkpoint:
                    trainer.train_update()
                checkpoint = save_checkpoint(Path(directory) / "micro.pt", trainer)
                expected = trainer.train_update()
                expected_model = {k: v.detach().clone() for k, v in trainer.model.state_dict().items()}
                load_checkpoint(checkpoint, trainer)
                actual = trainer.train_update()
                if actual != expected or any(not torch.equal(v, expected_model[k]) for k, v in trainer.model.state_dict().items()):
                    raise RuntimeError("checkpoint exact-resume micro-test failed")
        checks = {
            "schema": "sls-linux-training-preflight-v1", "ok": True,
            "simulator_only": True,
            "workload_contract": workload_contract(payload) if payload else None,
            "exact_resume": "PASS",
            "source_checkpoint_sha256": (
                hashlib.sha256(args.checkpoint.read_bytes()).hexdigest() if args.checkpoint else None
            ),
            "python": sys.version, "executable": sys.executable,
            "platform": platform.platform(), "git": git_state(),
            "seed_8335_regression": "PASS",
            "decision_invariant": "PASS",
            "content_scope_id": IRONCLAD_A0_SCOPE_ID,
            "content_scope_sha256": ironclad_a0_scope_hash(),
            "native_source_sha256": native_source_digest(), "native_artifact": native_artifact(),
            "torch": torch.__version__, "cuda": torch.version.cuda,
            "gpu": torch.cuda.get_device_name(0) if torch.cuda.is_available() else None,
            "device": device,
            "cublas_workspace_config": os.environ.get("CUBLAS_WORKSPACE_CONFIG"),
            "deterministic_algorithms": torch.are_deterministic_algorithms_enabled(),
            "policy_architecture": model.config.architecture,
            "recurrent_memory_size": model.config.recurrent_hidden_dim,
        }
    except Exception as error:
        checks = {"schema": "sls-linux-training-preflight-v1", "ok": False, "error": str(error), "error_type": type(error).__name__}
    print(json.dumps(checks, indent=2, sort_keys=True))
    args.output.parent.mkdir(parents=True, exist_ok=True)
    temporary = args.output.with_suffix(".tmp")
    temporary.write_text(json.dumps(checks, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    temporary.replace(args.output)
    return 0 if checks["ok"] else 1


if __name__ == "__main__":
    raise SystemExit(main())
