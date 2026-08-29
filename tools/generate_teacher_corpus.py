"""Generate policy-visible labels and checkpoint starts from native teachers."""

from __future__ import annotations

import argparse
import gzip
import json
import multiprocessing as mp
import os
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "src"))

from sls.backends.simulator import IRONCLAD_A0_ACT1, SimulatorBackend  # noqa: E402
from sls.model.encoding import vocabulary_hash  # noqa: E402
from sls.rl.demonstrations import TEACHER_CORPUS_SCHEMA  # noqa: E402
from sls.rl.training_contract import (  # noqa: E402
    canonical_digest, git_state, native_source_digest,
)
from sls.rl.training_mode import TrainingMode  # noqa: E402


def _generate_seed(task: tuple[int, int]):
    seed, stride = task
    examples = []
    rejections = []
    scripted = SimulatorBackend(IRONCLAD_A0_ACT1)
    scripted.reset(seed)
    scripted_result = scripted._native.scripted_playout_act1()
    scripted_actions = [
        int(value) & 0xFFFFFFFF
        for value in scripted_result.get("replay_actions", ())
    ]
    scripted_success = bool(scripted_result.get("act_one_success"))
    backend = SimulatorBackend(IRONCLAD_A0_ACT1)
    decision = backend.reset(seed)
    action_cursor = 0
    for step in range(512):
        bits = (
            scripted_actions[action_cursor]
            if action_cursor < len(scripted_actions) else None
        )
        matches = [] if bits is None else [
            action for action in decision.actions
            if int(backend._candidate_bits[action.candidate_id]) == bits
        ]
        raw_matches = [] if bits is None else [
            native for native in backend.raw_state.get("legal_actions", ())
            if int(native.get("bits", -1)) == bits
        ]
        if not matches and len(raw_matches) == 1:
            native = raw_matches[0]
            if (
                decision.observation.screen.value == "COMBAT_REWARD"
                and int(native.get("reward_type", -1)) == 0
                and int(native.get("idx2", -1)) == 6
            ):
                # Stock closes only the child CardRewardScreen here and returns
                # to the identical flattened public boundary. Consume this
                # validation-only no-op without inventing a policy label.
                decision = backend._adapt(backend._native.step(bits))
                action_cursor = len(backend.checkpoint().get("replay_actions", ()))
                continue
        if len(matches) != 1:
            rejections.append({
                "seed": seed, "step": step, "bits": bits,
                "match_count": len(matches),
                "screen": decision.observation.screen.value,
            })
            break
        action = matches[0]
        if step % stride == 0:
            checkpoint = backend.checkpoint()
            examples.append({
                "seed": seed, "step": step,
                "checkpoint": checkpoint,
                "checkpoint_sha256": canonical_digest(checkpoint),
                "candidate_id": action.candidate_id,
                "candidate_match_count": len(matches),
                "teacher_success": scripted_success,
            })
        transition = backend.step(action)
        action_cursor = len(backend.checkpoint().get("replay_actions", ()))
        decision = transition.decision
        if transition.terminated or transition.truncated:
            break
    return examples, int(scripted_success), rejections


def generate(
    seed_start: int, seed_count: int, stride: int, *, workers: int = 1,
) -> dict[str, object]:
    tasks = [(seed, stride) for seed in range(seed_start, seed_start + seed_count)]
    if workers == 1:
        rows = map(_generate_seed, tasks)
    else:
        context = mp.get_context("spawn")
        with context.Pool(min(workers, seed_count)) as pool:
            rows = pool.map(_generate_seed, tasks, chunksize=max(1, seed_count // (workers * 8)))
    examples = []
    successes = 0
    rejections = []
    for run_examples, scripted_success, rejected in rows:
        examples.extend(run_examples)
        successes += scripted_success
        rejections.extend(rejected)
    payload = {
        "schema": TEACHER_CORPUS_SCHEMA, "profile": IRONCLAD_A0_ACT1.profile_id,
        "seed_start": seed_start, "seed_count": seed_count, "stride": stride,
        "teacher_successes": successes, "rejected_labels": len(rejections),
        "rejections": rejections,
        "native_source_sha256": native_source_digest(),
        "vocabulary_sha256": vocabulary_hash(), "examples": examples,
        "training_mode": TrainingMode.EXPERIMENTAL.value,
        "policy_transfer_verified": False,
        "git_commit": str(git_state()["commit"]),
        "generation_config": {
            "seed_start": seed_start, "seed_count": seed_count,
            "stride": stride, "workers": workers,
        },
    }
    payload["generation_config_sha256"] = canonical_digest(payload["generation_config"])
    payload["corpus_sha256"] = canonical_digest(payload)
    return payload


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--seed-start", type=int, default=0)
    parser.add_argument("--seed-count", type=int, default=1_000)
    parser.add_argument("--stride", type=int, default=4)
    parser.add_argument("--workers", type=int, default=min(os.cpu_count() or 4, 16))
    parser.add_argument("--output", type=Path, default=ROOT / "runs" / "teacher-act1.json.gz")
    args = parser.parse_args()
    if args.seed_count <= 0 or args.stride <= 0 or args.workers <= 0:
        parser.error("seed-count, stride, and workers must be positive")
    payload = generate(
        args.seed_start, args.seed_count, args.stride, workers=args.workers,
    )
    args.output.parent.mkdir(parents=True, exist_ok=True)
    with gzip.open(args.output, "wt", encoding="utf-8") as stream:
        json.dump(payload, stream, sort_keys=True)
    print(json.dumps({
        "examples": len(payload["examples"]),
        "teacher_successes": payload["teacher_successes"],
        "rejected_labels": payload["rejected_labels"],
        "corpus_sha256": payload["corpus_sha256"],
    }, sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
