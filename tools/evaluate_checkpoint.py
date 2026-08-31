"""Evaluate one training checkpoint against the current native simulator."""

from __future__ import annotations

import argparse
import json
import os
import platform
import signal
import socket
import sys
import time
from dataclasses import asdict
from pathlib import Path
from typing import Mapping

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "src"))

import torch

from sls.content.scope import ironclad_a0_scope_hash
from sls.curriculum import CURRICULUM_PROFILES_BY_ID
from sls.rl import evaluate, policy_from_training_checkpoint
from sls.rl.training_contract import (
    TRAINING_CHECKPOINT_SCHEMA,
    native_artifact,
    native_source_digest,
    sha256_file,
)
from sls.runtime.artifact import model_state_sha256


class StopController:
    def __init__(self) -> None:
        self.requested = False

    def handler(self, _number: int, _frame: object) -> None:
        self.requested = True


def _atomic_json(path: Path, payload: object) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    temporary = path.with_suffix(path.suffix + ".tmp")
    temporary.write_text(
        json.dumps(payload, ensure_ascii=False, indent=2, sort_keys=True) + "\n",
        encoding="utf-8",
    )
    os.replace(temporary, path)


def _parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser()
    parser.add_argument("checkpoint", type=Path)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument(
        "--profile", choices=tuple(sorted(CURRICULUM_PROFILES_BY_ID)),
        default="IRONCLAD_A0_ACT1",
    )
    parser.add_argument("--seed-start", type=int, default=3_000_000_000_000)
    parser.add_argument("--episodes", type=int, default=1000)
    parser.add_argument("--device", default="cuda")
    parser.add_argument("--max-steps", type=int, default=4096)
    parser.add_argument("--max-boundary-visits", type=int, default=4)
    return parser


def main(argv: list[str] | None = None) -> int:
    args = _parser().parse_args(argv)
    if args.episodes <= 0 or args.seed_start < 0:
        raise ValueError("evaluation seed range is invalid")
    if args.max_steps <= 0 or args.max_boundary_visits <= 0:
        raise ValueError("evaluation limits must be positive")
    if args.device.startswith("cuda") and not torch.cuda.is_available():
        raise RuntimeError("CUDA evaluation requested but CUDA is unavailable")

    checkpoint = args.checkpoint.resolve()
    payload = torch.load(checkpoint, map_location="cpu", weights_only=False)
    if payload.get("schema") != TRAINING_CHECKPOINT_SCHEMA:
        raise ValueError("unsupported training checkpoint")
    contract = payload.get("contract")
    model_state = payload.get("model")
    if not isinstance(contract, Mapping) or not isinstance(model_state, Mapping):
        raise ValueError("checkpoint model contract is missing")
    profile = CURRICULUM_PROFILES_BY_ID[args.profile]
    model = policy_from_training_checkpoint(payload)
    torch.use_deterministic_algorithms(True)
    torch.backends.cudnn.benchmark = False

    controller = StopController()
    signal.signal(signal.SIGTERM, controller.handler)
    signal.signal(signal.SIGINT, controller.handler)
    seeds = tuple(range(args.seed_start, args.seed_start + args.episodes))
    started = time.time()
    print(
        f"evaluating {checkpoint} profile={profile.profile_id} "
        f"episodes={args.episodes} seeds=[{seeds[0]}, {seeds[-1] + 1})",
        flush=True,
    )
    result = asdict(evaluate(
        model, profile, seeds, device=args.device,
        max_steps=args.max_steps,
        max_boundary_visits=args.max_boundary_visits,
        failure_progress_scale=float(
            dict(contract.get("ppo") or {}).get("failure_progress_scale", 0.8)
        ),
        stop_requested=lambda: controller.requested,
    ))
    artifact = native_artifact()
    record = {
        "schema": "sls-checkpoint-evaluation-v1",
        "checkpoint": str(checkpoint),
        "checkpoint_sha256": sha256_file(checkpoint),
        "model_sha256": model_state_sha256(model_state),
        "profile": profile.profile_id,
        "seed_range": [seeds[0], seeds[-1] + 1],
        "simulator": {
            "native_source_sha256": native_source_digest(),
            "native_artifact": artifact,
            "content_scope_sha256": ironclad_a0_scope_hash(),
        },
        "checkpoint_environment": {
            "native_source_sha256": contract.get("native_source_sha256"),
            "content_scope_sha256": contract.get("content_scope_sha256"),
        },
        "runtime": {
            "python": sys.version,
            "platform": platform.platform(),
            "hostname": socket.gethostname(),
            "torch": torch.__version__,
            "cuda": torch.version.cuda,
            "gpu": (
                torch.cuda.get_device_name(args.device)
                if args.device.startswith("cuda") else None
            ),
        },
        "elapsed_seconds": time.time() - started,
        "result": result,
    }
    _atomic_json(args.output.resolve(), record)
    print(json.dumps(record, ensure_ascii=False, sort_keys=True), flush=True)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
