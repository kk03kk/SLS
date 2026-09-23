"""Evaluate two checkpoints on one identical unseen seed set."""

from __future__ import annotations

import argparse
import json
import math
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
os.environ.setdefault("CUBLAS_WORKSPACE_CONFIG", ":4096:8")


def _exact_mcnemar(left_only: int, right_only: int) -> float:
    discordant = left_only + right_only
    if discordant == 0:
        return 1.0
    tail = sum(
        math.comb(discordant, value)
        for value in range(min(left_only, right_only) + 1)
    ) / 2**discordant
    return min(1.0, 2.0 * tail)


def compare_seed_results(left: list[dict], right: list[dict]) -> dict:
    left_by_seed = {int(row["seed"]): row for row in left}
    right_by_seed = {int(row["seed"]): row for row in right}
    if len(left_by_seed) != len(left) or len(right_by_seed) != len(right):
        raise ValueError("paired evaluation contains duplicate seeds")
    if set(left_by_seed) != set(right_by_seed):
        raise ValueError("paired evaluations use different seed sets")

    groups: dict[str, dict[str, object]] = {}
    for seed in sorted(left_by_seed):
        first, second = left_by_seed[seed], right_by_seed[seed]
        left_boss = first.get("bosses", {}).get("1")
        right_boss = second.get("bosses", {}).get("1")
        if left_boss is not None and right_boss is not None and left_boss != right_boss:
            raise ValueError(f"scheduled boss differs for seed {seed}")
        # A policy can die before the Act 1 map exposes its scheduled boss. The
        # other policy's observation is still valid for grouping the shared
        # seed; if neither saw it, retain the pair in an explicit unknown group.
        boss = str(left_boss or right_boss or "UNOBSERVED")
        for group in ("all", boss):
            values = groups.setdefault(group, {
                "both_win": [], "left_only": [], "right_only": [], "both_loss": [],
            })
            left_won, right_won = bool(first["success"]), bool(second["success"])
            key = (
                "both_win" if left_won and right_won
                else "left_only" if left_won
                else "right_only" if right_won
                else "both_loss"
            )
            values[key].append(seed)  # type: ignore[union-attr]

    report = {}
    for group, values in sorted(groups.items()):
        counts = {key: len(seeds) for key, seeds in values.items()}
        total = sum(counts.values())
        left_wins = counts["both_win"] + counts["left_only"]
        right_wins = counts["both_win"] + counts["right_only"]
        report[group] = {
            "episodes": total,
            **counts,
            "left_wins": left_wins,
            "right_wins": right_wins,
            "paired_success_rate_delta": (right_wins - left_wins) / total,
            "mcnemar_exact_two_sided_p": _exact_mcnemar(
                counts["left_only"], counts["right_only"],
            ),
            "transition_seeds": values,
        }
    return report


def _parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser()
    parser.add_argument("left", type=Path)
    parser.add_argument("right", type=Path)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--profile", default="IRONCLAD_A20_ACT1")
    parser.add_argument("--seed-start", type=int, default=4_000_000_000_000)
    parser.add_argument("--episodes", type=int, default=2_048)
    parser.add_argument("--device", default="cuda")
    parser.add_argument("--environment-shards", type=int, default=16)
    parser.add_argument("--max-steps", type=int, default=4_096)
    return parser


def _atomic_json(path: Path, payload: object) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    temporary = path.with_suffix(path.suffix + ".tmp")
    temporary.write_text(
        json.dumps(payload, indent=2, sort_keys=True) + "\n", encoding="utf-8",
    )
    temporary.replace(path)


def _progress_reporter(label: str):
    started = time.monotonic()
    last_report = started - 30.0

    def report(completed: int, total: int, decisions: int) -> None:
        nonlocal last_report
        now = time.monotonic()
        if completed != total and now - last_report < 30.0:
            return
        print(json.dumps({
            "paired_phase": label,
            "completed_episodes": completed,
            "total_episodes": total,
            "decisions": decisions,
            "elapsed_seconds": round(now - started, 3),
        }, sort_keys=True), flush=True)
        last_report = now

    return report


def main(argv: list[str] | None = None) -> int:
    args = _parser().parse_args(argv)
    if args.episodes <= 0 or args.seed_start < 0:
        raise ValueError("paired evaluation seed range is invalid")
    import torch

    from sls.curriculum import CURRICULUM_PROFILES_BY_ID
    from sls.rl.checkpoint import policy_from_training_checkpoint
    from sls.rl.evaluate import evaluate
    from sls.rl.training_contract import (
        TRAINING_CHECKPOINT_SCHEMA,
        native_artifact,
        native_source_digest,
        sha256_file,
    )
    from sls.runtime.artifact import model_state_sha256

    if args.device.startswith("cuda") and not torch.cuda.is_available():
        raise RuntimeError("CUDA paired evaluation requested but CUDA is unavailable")
    profile = CURRICULUM_PROFILES_BY_ID[args.profile]
    source_digest = native_source_digest()
    checkpoints = []
    precisions = set()
    for path in (args.left.resolve(), args.right.resolve()):
        payload = torch.load(path, map_location="cpu", weights_only=False)
        if payload.get("schema") != TRAINING_CHECKPOINT_SCHEMA:
            raise ValueError(f"unsupported training checkpoint: {path}")
        contract = payload.get("contract")
        if not isinstance(contract, Mapping):
            raise ValueError(f"checkpoint model contract is missing: {path}")
        if contract.get("native_source_sha256") != source_digest:
            raise ValueError(f"checkpoint simulator source differs from current source: {path}")
        saved_profile = contract.get("profile")
        if saved_profile != profile:
            raise ValueError(f"checkpoint profile differs from paired profile: {path}")
        runtime = contract.get("runtime")
        if not isinstance(runtime, Mapping):
            raise ValueError(f"checkpoint runtime contract is missing: {path}")
        precision = runtime.get("float32_matmul_precision")
        if precision not in {"highest", "high", "medium"}:
            raise ValueError(f"checkpoint matmul precision is invalid: {path}")
        precisions.add(precision)
        checkpoints.append((path, payload, policy_from_training_checkpoint(payload)))
    if len(precisions) != 1:
        raise ValueError("paired checkpoints require one identical matmul precision")

    torch.use_deterministic_algorithms(True)
    torch.backends.cudnn.benchmark = False
    torch.set_float32_matmul_precision(precisions.pop())
    seeds = tuple(range(args.seed_start, args.seed_start + args.episodes))
    stop_requested = False

    def stop(_number: int, _frame: object) -> None:
        nonlocal stop_requested
        stop_requested = True

    signal.signal(signal.SIGTERM, stop)
    signal.signal(signal.SIGINT, stop)
    evaluations = []
    started = time.time()
    output = args.output.resolve()
    partial_output = output.with_suffix(output.suffix + ".partial")
    for label, (path, payload, model) in zip(("left", "right"), checkpoints):
        phase_started = time.time()
        print(json.dumps({"paired_phase": label, "checkpoint": str(path)}), flush=True)
        result = asdict(evaluate(
            model, profile, seeds, device=args.device,
            max_steps=args.max_steps,
            environment_shards=args.environment_shards,
            stop_requested=lambda: stop_requested,
            progress_callback=_progress_reporter(label),
        ))
        if stop_requested or int(result["episodes"]) != args.episodes:
            raise RuntimeError(
                f"paired evaluation interrupted during {label}; no comparison was written"
            )
        evaluations.append({
            "label": label,
            "checkpoint": str(path),
            "checkpoint_sha256": sha256_file(path),
            "checkpoint_environment_steps": int(payload["trainer"]["environment_steps"]),
            "model_sha256": model_state_sha256(payload["model"]),
            "elapsed_seconds": time.time() - phase_started,
            "result": result,
        })
        _atomic_json(partial_output, {
            "schema": "sls-paired-checkpoint-evaluation-partial-v1",
            "profile": profile.profile_id,
            "seed_range": [seeds[0], seeds[-1] + 1],
            "evaluations": evaluations,
            "complete": False,
        })

    comparison = compare_seed_results(
        evaluations[0]["result"]["seed_results"],
        evaluations[1]["result"]["seed_results"],
    )
    record = {
        "schema": "sls-paired-checkpoint-evaluation-v1",
        "profile": profile.profile_id,
        "seed_range": [seeds[0], seeds[-1] + 1],
        "evaluations": evaluations,
        "comparison": comparison,
        "simulator": {"native_source_sha256": source_digest, "artifact": native_artifact()},
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
            "float32_matmul_precision": torch.get_float32_matmul_precision(),
            "environment_shards": args.environment_shards,
        },
        "elapsed_seconds": time.time() - started,
    }
    _atomic_json(output, record)
    partial_output.unlink(missing_ok=True)
    print(json.dumps({
        "output": str(output),
        "comparison": comparison["all"],
    }, sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
