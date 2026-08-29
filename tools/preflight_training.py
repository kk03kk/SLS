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
STOCHASTIC_EVIDENCE = (
    ROOT / "configs" / "validation" / "policy_transfer_stochastic_samples.json"
)


def _parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser()
    parser.add_argument("--allow-cpu", action="store_true")
    parser.add_argument("--skip-build", action="store_true")
    parser.add_argument("--allow-dirty", action="store_true", help="development/test only")
    parser.add_argument("--jobs", type=int, default=min(os.cpu_count() or 4, 16))
    parser.add_argument("--transfer-gate", type=Path, default=TRANSFER_GATE)
    return parser


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
        from sls.model import ModelConfig, Policy, PolicyBatch
        from sls.rl import PPOConfig, PPOTrainer, VectorWorkerPool, load_checkpoint, save_checkpoint
        from sls.content.scope import IRONCLAD_A0_SCOPE_ID, ironclad_a0_scope_hash
        from sls.rl.training_contract import git_state, native_artifact, native_source_digest
        from sls.validation.transfer_gate import verify_policy_transfer_gate

        # A fresh server clone intentionally has no ignored runs/ artifacts.
        # Rebuild the commit-bound training gate from committed Original/native
        # evidence after the native module is available.
        if not args.transfer_gate.is_file():
            subprocess.run([
                sys.executable,
                str(ROOT / "tools" / "build_policy_transfer_evidence.py"),
                "--stochastic-report", str(STOCHASTIC_EVIDENCE),
            ], cwd=ROOT, check=True)
        transfer_gate = verify_policy_transfer_gate(
            args.transfer_gate, profile_id=IRONCLAD_A0_ACT1.profile_id,
            require_canary=False,
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
            "python": sys.version, "executable": sys.executable,
            "platform": platform.platform(), "git": git_state(),
            "transfer_gate_sha256": transfer_digest,
            "transfer_gate_schema": transfer_gate["schema"],
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
