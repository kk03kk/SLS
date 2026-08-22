"""Record fixed-seed Act 1 baselines after the parity readiness gate passes."""

from __future__ import annotations

import argparse
from collections import defaultdict
import json
from pathlib import Path
import random
import statistics
import sys
import tomllib

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "src"))

import torch

from sls.backends.simulator import SimulatorBackend
from sls.curriculum import IRONCLAD_A0_ACT1
from sls.model import ModelConfig, Policy, PolicyBatch
from sls.validation.policies import PRIORITY
from sls.validation.readiness import readiness_report


def _summarize(rows: list[dict[str, object]]) -> dict[str, object]:
    by_boss: dict[str, list[bool]] = defaultdict(list)
    for row in rows:
        by_boss[str(row["boss"])].append(bool(row["success"]))
    failed_floors = [int(row["floor"]) for row in rows if not bool(row["success"])]
    return {
        "episodes": len(rows),
        "success_rate": sum(bool(row["success"]) for row in rows) / len(rows),
        "mean_terminal_floor": sum(int(row["floor"]) for row in rows) / len(rows),
        "mean_episode_length": sum(int(row["steps"]) for row in rows) / len(rows),
        "mean_failure_floor": (
            sum(failed_floors) / len(failed_floors) if failed_floors else None
        ),
        "median_failure_floor": (
            statistics.median(failed_floors) if failed_floors else None
        ),
        "self_loops": sum(row.get("termination_reason") == "SELF_LOOP" for row in rows),
        "timeouts": sum(row.get("termination_reason") == "TIME_LIMIT" for row in rows),
        "boss_success_rate": {
            boss: sum(values) / len(values) for boss, values in sorted(by_boss.items())
        },
    }


@torch.no_grad()
def _python_policy(
    name: str, seeds: range, *, device: str, model: Policy | None = None,
    max_steps: int = 512,
) -> list[dict[str, object]]:
    rows = []
    generator = random.Random(0)
    for seed in seeds:
        backend = SimulatorBackend(IRONCLAD_A0_ACT1)
        decision = backend.reset(seed)
        boss = decision.observation.run.visible_boss_id
        for steps in range(1, max_steps + 1):
            if name == "random":
                action = generator.choice(decision.actions)
            elif name == "deterministic":
                action = min(
                    decision.actions,
                    key=lambda item: (PRIORITY.get(item.kind, 1000), item.candidate_id),
                )
            elif name == "untrained":
                assert model is not None
                batch = PolicyBatch.from_decisions((decision,), model.config).to(device)
                output = model(*batch.model_inputs())
                action = decision.actions[int(output.logits[0].argmax())]
            else:
                raise ValueError(name)
            checkpoint = backend.checkpoint() if name != "random" else None
            transition = backend.step(action)
            decision = transition.decision
            if transition.terminated or transition.truncated:
                rows.append({
                    "seed": seed, "boss": boss,
                    "success": bool(transition.info.get("success")),
                    "floor": decision.observation.run.floor, "steps": steps,
                })
                break
            if checkpoint is not None and backend.checkpoint() == checkpoint:
                rows.append({
                    "seed": seed, "boss": boss, "success": False,
                    "floor": decision.observation.run.floor, "steps": steps,
                    "termination_reason": "SELF_LOOP",
                })
                break
        else:
            rows.append({
                "seed": seed, "boss": boss, "success": False,
                "floor": decision.observation.run.floor, "steps": max_steps,
                "termination_reason": "TIME_LIMIT",
            })
    return rows


@torch.no_grad()
def _untrained_policy_batched(
    seeds: range, *, device: str, model: Policy, max_steps: int,
) -> list[dict[str, object]]:
    """Evaluate all held-out seeds together so CUDA never sees batch size one."""

    seed_values = list(seeds)
    backends = [SimulatorBackend(IRONCLAD_A0_ACT1) for _ in seed_values]
    decisions = [backend.reset(seed) for backend, seed in zip(backends, seed_values)]
    bosses = [decision.observation.run.visible_boss_id for decision in decisions]
    active = list(range(len(seed_values)))
    rows: list[dict[str, object] | None] = [None] * len(seed_values)
    episode_steps = [0] * len(seed_values)
    for _ in range(max_steps):
        if not active:
            break
        batch = PolicyBatch.from_decisions(
            (decisions[index] for index in active), model.config,
        ).to(device)
        output = model(*batch.model_inputs())
        action_indices = output.logits.argmax(dim=1).cpu().tolist()
        still_active = []
        for batch_index, index in enumerate(active):
            action = decisions[index].actions[int(action_indices[batch_index])]
            transition = backends[index].step(action)
            decisions[index] = transition.decision
            episode_steps[index] += 1
            if transition.terminated or transition.truncated:
                rows[index] = {
                    "seed": seed_values[index], "boss": bosses[index],
                    "success": bool(transition.info.get("success")),
                    "floor": transition.decision.observation.run.floor,
                    "steps": episode_steps[index],
                }
            else:
                still_active.append(index)
        active = still_active
    for index in active:
        rows[index] = {
            "seed": seed_values[index], "boss": bosses[index], "success": False,
            "floor": decisions[index].observation.run.floor,
            "steps": episode_steps[index], "termination_reason": "TIME_LIMIT",
        }
    return [row for row in rows if row is not None]


def _native_simple(seeds: range) -> list[dict[str, object]]:
    rows = []
    for seed in seeds:
        backend = SimulatorBackend(IRONCLAD_A0_ACT1)
        backend.reset(seed)
        result = dict(backend._native.scripted_playout_act1())
        run = dict(result["public_run"])
        rows.append({
            "seed": seed, "boss": str(result["act_one_boss"]),
            "success": bool(result["act_one_success"]),
            "floor": int(run["floor"]), "steps": int(result["scripted_action_count"]),
        })
    return rows


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--seed-start", type=int, default=10_000)
    parser.add_argument("--seed-count", type=int, default=100)
    parser.add_argument("--device", default="auto")
    parser.add_argument("--output", type=Path, default=ROOT / "runs" / "act1-baselines.json")
    parser.add_argument("--max-steps", type=int, default=512)
    parser.add_argument("--allow-unready", action="store_true")
    args = parser.parse_args()
    with (ROOT / "configs" / "validation" / "act1_training.toml").open("rb") as stream:
        requirements = tomllib.load(stream)["requirements"]
    readiness = readiness_report(ROOT / "validation-results" / "truth", requirements)
    if not readiness["ready"] and not args.allow_unready:
        print(json.dumps({"error": "ACT1_NOT_READY", "failures": readiness["failures"]}), file=sys.stderr)
        return 2
    device = "cuda" if args.device == "auto" and torch.cuda.is_available() else (
        "cpu" if args.device == "auto" else args.device
    )
    seeds = range(args.seed_start, args.seed_start + args.seed_count)
    torch.manual_seed(0)
    model = Policy(ModelConfig()).eval().to(device)
    results = {
        "random": _python_policy("random", seeds, device=device),
        "deterministic": _python_policy("deterministic", seeds, device=device),
        "native_simple": _native_simple(seeds),
        "untrained": _untrained_policy_batched(
            seeds, device=device, model=model, max_steps=args.max_steps,
        ),
    }
    payload = {
        "schema": "sls-act1-baselines-v1",
        "seed_start": args.seed_start, "seed_count": args.seed_count,
        "device": device,
        "max_steps": args.max_steps,
        "summary": {name: _summarize(rows) for name, rows in results.items()},
        "episodes": results,
    }
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(payload, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    print(json.dumps(payload["summary"], indent=2, sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
