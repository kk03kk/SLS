"""Capture a stratified Act1 corpus through the canonical deterministic evaluator.

The instrumentation is local to this CLI and never changes training/inference inputs.
JSONL stores the initial observation and each following observation once; together
with every legal action set and selected action these cover both sides of every step.
Private native floor-entry snapshots are separate replay evidence, never model input.
"""

from __future__ import annotations

import argparse
import gzip
import hashlib
import importlib
import json
import os
import subprocess
import sys
from collections import defaultdict
from dataclasses import asdict
from pathlib import Path
from unittest.mock import patch

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "src"))
os.environ.setdefault("CUBLAS_WORKSPACE_CONFIG", ":4096:8")


def select_seeds(rows):
    groups = defaultdict(list)
    for row in rows:
        outcome = (
            "win"
            if row["success"]
            else "boss_death"
            if row["floor"] == 16
            else "early_death"
        )
        groups[(outcome, row["bosses"]["1"])].append(row)
    selected = []
    for outcome, quotas in [
        ("win", (14, 13, 13)),
        ("boss_death", (14, 13, 13)),
        ("early_death", (6, 7, 7)),
    ]:
        for boss, quota in zip(["THE_GUARDIAN", "HEXAGHOST", "SLIME_BOSS"], quotas):
            ordered = sorted(
                groups[outcome, boss],
                key=lambda r: hashlib.sha256(
                    f"act1-v4-diagnostic-v1:{r['seed']}".encode()
                ).hexdigest(),
            )
            if len(ordered) < quota:
                raise ValueError(f"not enough seeds for {outcome}/{boss}")
            selected.extend({**r, "stratum": outcome} for r in ordered[:quota])
    return sorted(selected, key=lambda r: r["seed"])


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--run", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--device", default="cuda")
    parser.add_argument(
        "--analyze", action="store_true",
        help="Replay and summarize the completed corpus in the same compute job.",
    )
    args = parser.parse_args()
    import torch

    from sls.backends.simulator import SimulatorBackend
    from sls.curriculum import CURRICULUM_PROFILES_BY_ID, EpisodeHorizon
    from sls.rl.checkpoint import policy_from_training_checkpoint
    from sls.rl.training_contract import (
        native_artifact,
        native_source_digest,
        runtime_contract,
        sha256_file,
    )

    module = importlib.import_module("sls.rl.evaluate")
    final = json.loads((args.run / "final-evaluation.json").read_text(encoding="utf-8"))
    chosen = select_seeds(final["result"]["seed_results"])
    checkpoint = args.run / "stages/train/selection/best_progress.pt"
    if sha256_file(checkpoint) != final["checkpoint_sha256"]:
        raise ValueError("selected checkpoint does not match final evaluation")
    payload = torch.load(checkpoint, map_location="cpu", weights_only=False)
    saved_profile = payload["contract"]["profile"]
    profile_id = (
        saved_profile.profile_id
        if hasattr(saved_profile, "profile_id")
        else saved_profile["profile_id"]
    )
    profile = CURRICULUM_PROFILES_BY_ID[profile_id]
    if profile.horizon is not EpisodeHorizon.ACT_1:
        raise ValueError("Act1 corpus capture requires an Act1 checkpoint")
    if payload["contract"]["native_source_sha256"] != native_source_digest():
        raise ValueError("checkpoint simulator source differs from current source")
    if saved_profile != profile:
        raise ValueError("checkpoint profile contract is not canonical")
    torch.set_num_threads(4)
    torch.use_deterministic_algorithms(True)
    torch.backends.cudnn.benchmark = False
    torch.set_float32_matmul_precision(
        payload["contract"]["runtime"]["float32_matmul_precision"]
    )
    model = policy_from_training_checkpoint(payload)
    args.output.mkdir(parents=True, exist_ok=False)
    (args.output / "selection.json").write_text(
        json.dumps(
            {
                "rule": "SHA256(act1-v4-diagnostic-v1:seed) ascending within outcome/boss strata; 40 wins, 40 boss deaths, 20 early deaths; not a win-rate estimator",
                "seeds": chosen,
                "profile": profile.profile_id,
                "checkpoint_sha256": sha256_file(checkpoint),
                "checkpoint_steps": payload["trainer"]["environment_steps"],
                "runtime": runtime_contract(torch),
                "native": native_artifact(),
                "inference": "canonical evaluate(), eval/no_grad/argmax, recurrent previous-action and raw transition reward unchanged",
            },
            indent=2,
        ),
        encoding="utf-8",
    )
    streams = []

    class RecordingBackend(SimulatorBackend):
        def reset(self, seed):
            self.seed = seed
            self.step_number = 0
            self.stream = gzip.open(
                args.output / f"seed-{seed}.jsonl.gz", "wt", encoding="utf-8"
            )
            streams.append(self.stream)
            self.previous = super().reset(seed)
            self.write(
                {
                    "type": "initial",
                    "seed": seed,
                    "observation": self.previous.observation.to_dict(),
                }
            )
            return self.previous

        def write(self, record):
            self.stream.write(json.dumps(record, separators=(",", ":")) + "\n")

        def step(self, action, **kwargs):
            previous = self.previous
            assert action in previous.actions
            if self.step_number == 0 or previous.observation.run.floor != getattr(
                self, "saved_floor", -1
            ):
                with gzip.open(
                    args.output / f"replay-{self.seed}-{self.step_number}.json.gz",
                    "wt",
                    encoding="utf-8",
                ) as stream:
                    json.dump(self.checkpoint(), stream, separators=(",", ":"))
                self.saved_floor = previous.observation.run.floor
            bits = self._candidate_bits[action.candidate_id]
            transition = super().step(action, **kwargs)
            following = transition.decision.observation
            assert 0 <= following.player.current_hp <= following.player.max_hp
            assert following.run.gold >= 0
            assert following.run.floor >= previous.observation.run.floor
            self.write(
                {
                    "type": "step",
                    "step": self.step_number,
                    "action": action.to_dict(),
                    "native_bits": bits,
                    "legal_actions": [a.to_dict() for a in previous.actions],
                    "observation": following.to_dict(),
                    "reward": transition.reward,
                    "terminated": transition.terminated,
                    "truncated": transition.truncated,
                    "info": dict(transition.info),
                }
            )
            self.previous = transition.decision
            self.step_number += 1
            return transition

    try:
        with patch.object(module, "SimulatorBackend", RecordingBackend):
            result = module.evaluate(
                model,
                profile,
                tuple(r["seed"] for r in chosen),
                device=args.device,
                max_steps=4096,
                max_boundary_visits=4,
                progress_callback=lambda done, total, steps: print(
                    json.dumps({"done": done, "total": total, "steps": steps}),
                    flush=True,
                )
                if steps % 1000 < total
                else None,
            )
    finally:
        for stream in streams:
            stream.close()
    actual = {row["seed"]: row for row in result.seed_results}
    comparisons = [
        {"seed": row["seed"], "server": row, "local": actual[row["seed"]]}
        for row in chosen
        if any(
            row[k] != actual[row["seed"]][k]
            for k in ("success", "floor", "steps", "route", "deck")
        )
    ]
    (args.output / "result.json").write_text(
        json.dumps(
            {"result": asdict(result), "server_differences": comparisons}, indent=2
        ),
        encoding="utf-8",
    )
    print(json.dumps({"completed": len(chosen), "differences": len(comparisons)}))
    if args.analyze:
        subprocess.run(
            [sys.executable, str(ROOT / "tools" / "analyze_act1_corpus.py"), str(args.output)],
            cwd=ROOT,
            check=True,
        )


if __name__ == "__main__":
    main()
