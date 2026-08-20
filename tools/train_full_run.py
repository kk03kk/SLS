"""Train the canonical FullRun policy with native simulator workers."""

from __future__ import annotations

import argparse
from dataclasses import asdict
import json
from pathlib import Path
import sys
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
from sls.rl import PPOConfig, PPOTrainer, WorkerPool, evaluate, load_checkpoint, save_checkpoint


PROFILES = {
    item.profile_id: item
    for item in (IRONCLAD_A0_ACT1, IRONCLAD_A0_ACT2, IRONCLAD_A0_ACT3, IRONCLAD_A0_HEART)
}


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--config", type=Path, default=ROOT / "configs" / "train" / "full_run.toml")
    parser.add_argument("--resume", type=Path)
    args = parser.parse_args()
    payload = tomllib.loads(args.config.read_text(encoding="utf-8"))
    run = payload["run"]
    profile = PROFILES[str(run["profile"])]
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
    with WorkerPool(profile, int(run["workers"])) as workers:
        trainer = PPOTrainer(model, workers, ppo, device=device, seed=seed)
        if args.resume is not None:
            load_checkpoint(args.resume, trainer)
        updates = int(run["updates"])
        save_interval = int(run.get("save_interval", 10))
        evaluate_interval = int(run.get("evaluate_interval", save_interval))
        evaluation_seeds = tuple(int(value) for value in run.get("evaluation_seeds", [0, 1, 2, 3]))
        log_path = output / "metrics.jsonl"
        while trainer.update < updates:
            metrics = trainer.train_update()
            record: dict[str, object] = {
                "update": trainer.update,
                "episodes": trainer.episodes,
                **metrics,
            }
            if trainer.update % evaluate_interval == 0:
                result = evaluate(trainer.model, profile, evaluation_seeds, device=device)
                record["evaluation"] = asdict(result)
            with log_path.open("a", encoding="utf-8") as stream:
                stream.write(json.dumps(record, sort_keys=True) + "\n")
            print(json.dumps(record, sort_keys=True), flush=True)
            if trainer.update % save_interval == 0:
                save_checkpoint(output / f"checkpoint-{trainer.update:08d}.pt", trainer)
                save_checkpoint(output / "latest.pt", trainer)
        save_checkpoint(output / "latest.pt", trainer)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
