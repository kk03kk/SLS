"""Fail-fast Linux/GPU training preflight with an exact-resume micro-test."""

from __future__ import annotations

import argparse
import hashlib
import json
import os
from pathlib import Path
import platform
import shutil
import subprocess
import sys
import tempfile

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "src"))

TRANSFER_GATE = ROOT / "runs" / "policy_transfer_v1.json"
SEED_8335_DUMP = ROOT / "tests/fixtures/regressions/nus-worker-23-seed-8335-invalid-decision.json"
SEED_8335_SHA256 = "bbd6fa5644223ebee07681849d5e2654466cc21e27affbd69cf688a0404eb4a7"


def _parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--mode", required=True, choices=("experimental", "production"),
    )
    parser.add_argument("--allow-cpu", action="store_true")
    parser.add_argument("--skip-build", action="store_true")
    parser.add_argument("--allow-dirty", action="store_true", help="development/test only")
    parser.add_argument("--jobs", type=int, default=min(os.cpu_count() or 4, 16))
    parser.add_argument("--transfer-gate", type=Path, default=TRANSFER_GATE)
    return parser


def verify_transfer_for_mode(mode: object, gate: Path, profile_id: str):  # type: ignore[no-untyped-def]
    """Experimental preflight never opens a production gate artifact."""

    from sls.rl.training_mode import TrainingMode, parse_training_mode
    if parse_training_mode(mode) is TrainingMode.EXPERIMENTAL:
        return None
    from sls.validation.transfer_gate import verify_policy_transfer_gate
    return verify_policy_transfer_gate(
        gate, profile_id=profile_id, require_canary=True,
    )


def main() -> int:
    args = _parser().parse_args()
    checks: dict[str, object] = {}
    try:
        if platform.system() != "Linux":
            raise RuntimeError("server preflight requires Linux")
        if platform.machine().lower() not in {"x86_64", "amd64"}:
            raise RuntimeError(f"unsupported Linux architecture: {platform.machine()}")
        if sys.version_info < (3, 12):
            raise RuntimeError("Python 3.12 or newer is required")
        if not (os.environ.get("CXX") or shutil.which("c++") or shutil.which("g++")):
            raise RuntimeError("C++ compiler not found; load a compiler module first")
        import sls
        if ROOT not in Path(sls.__file__).resolve().parents:
            raise RuntimeError("sls is not imported from this checkout; install with pip -e")
        if not args.skip_build:
            subprocess.run([sys.executable, str(ROOT / "tools" / "build_native.py"), "--jobs", str(args.jobs)], cwd=ROOT, check=True)
        import torch
        from sls.curriculum import IRONCLAD_A0_ACT1
        from sls.model import ENCODING_SCHEMA, ModelConfig, Policy, PolicyBatch
        from sls.rl import PPOConfig, PPOTrainer, VectorWorkerPool, load_checkpoint, save_checkpoint
        from sls.rl.training_mode import TrainingMode, parse_training_mode
        from sls.backends.simulator import SimulatorBackend
        from sls.content.scope import IRONCLAD_A0_SCOPE_ID, ironclad_a0_scope_hash
        from sls.rl.training_contract import git_state, native_artifact, native_source_digest

        from replay_failed_state import replay_dump

        mode = parse_training_mode(args.mode)
        repository = git_state()
        if bool(repository["dirty"]) and not args.allow_dirty:
            raise RuntimeError("preflight requires a clean Git worktree")
        if ENCODING_SCHEMA != "sls-policy-input-v3":
            raise RuntimeError("preflight requires the policy v3 encoding contract")
        decision = SimulatorBackend(IRONCLAD_A0_ACT1).reset(0)
        if decision.terminal or not decision.actions:
            raise RuntimeError("simulator smoke produced an invalid Decision")
        if hashlib.sha256(SEED_8335_DUMP.read_bytes()).hexdigest() != SEED_8335_SHA256:
            raise RuntimeError("seed 8335 regression fixture provenance is stale")
        replayed = replay_dump(SEED_8335_DUMP)
        if replayed["terminal"] or replayed["screen"] != "COMBAT_REWARD" or not replayed["actions"]:
            raise RuntimeError("seed 8335 regression no longer reaches a reward Decision")

        transfer_gate = None
        transfer_digest = "EXPERIMENTAL_UNVERIFIED"
        if mode is TrainingMode.PRODUCTION:
            transfer_gate = verify_transfer_for_mode(
                mode, args.transfer_gate, IRONCLAD_A0_ACT1.profile_id,
            )
            transfer_digest = hashlib.sha256(args.transfer_gate.read_bytes()).hexdigest()
        if not args.allow_cpu and not torch.cuda.is_available():
            raise RuntimeError("CUDA GPU is not visible to PyTorch")
        device = "cuda" if torch.cuda.is_available() else "cpu"
        model = Policy(ModelConfig(embedding_dim=32, transformer_layers=1, attention_heads=4, feedforward_dim=64)).to(device)
        with VectorWorkerPool(IRONCLAD_A0_ACT1, 1) as workers:
            trainer = PPOTrainer(
                model, workers, PPOConfig(rollout_steps=1, epochs=1, minibatch_size=1),
                device=device, seed=918273,
                readiness_lock_digest=transfer_digest,
                native_contract_digest=native_source_digest(),
                training_mode=mode,
                policy_transfer_verified=mode is TrainingMode.PRODUCTION,
                git_commit=str(repository["commit"]),
                training_config_digest="PREFLIGHT_MICRO_RESUME",
            )
            decision = trainer.decisions[0]
            batch = PolicyBatch.from_decisions((decision,), model.config).to(device)
            loss = model(*batch.model_inputs()).logits.sum() + model(*batch.model_inputs()).value.sum()
            loss.backward()
            with tempfile.TemporaryDirectory(prefix="sls-preflight-") as directory:
                checkpoint = save_checkpoint(Path(directory) / "micro.pt", trainer)
                expected = trainer.train_update()
                load_checkpoint(checkpoint, trainer)
                actual = trainer.train_update()
                if actual != expected:
                    raise RuntimeError("checkpoint exact-resume micro-test failed")
        checks = {
            "schema": "sls-linux-training-preflight-v1", "ok": True,
            "training_mode": mode.value,
            "policy_transfer_verified": mode is TrainingMode.PRODUCTION,
            "python": sys.version, "executable": sys.executable,
            "platform": platform.platform(), "git": git_state(),
            "transfer_gate_sha256": transfer_digest,
            "transfer_gate_schema": (
                transfer_gate["schema"] if transfer_gate is not None else None
            ),
            "seed_8335_regression": "PASS",
            "decision_invariant": "PASS",
            "content_scope_id": IRONCLAD_A0_SCOPE_ID,
            "content_scope_sha256": ironclad_a0_scope_hash(),
            "native_source_sha256": native_source_digest(), "native_artifact": native_artifact(),
            "torch": torch.__version__, "cuda": torch.version.cuda,
            "gpu": torch.cuda.get_device_name(0) if torch.cuda.is_available() else None,
            "device": device,
        }
    except Exception as error:
        checks = {"schema": "sls-linux-training-preflight-v1", "ok": False, "error": str(error), "error_type": type(error).__name__}
    print(json.dumps(checks, indent=2, sort_keys=True))
    return 0 if checks["ok"] else 1


if __name__ == "__main__":
    raise SystemExit(main())
