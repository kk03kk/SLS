"""Local collection-only probe. No weight update; NOT a server training result."""

# ruff: noqa: E402
import os

os.environ["CUBLAS_WORKSPACE_CONFIG"] = ":4096:8"
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
sys.path[:0] = [str(ROOT / "src"), str(ROOT)]
import hashlib
import json
import platform

import torch

import sls.rl.ppo as ppo
from sls.curriculum import IRONCLAD_A20_ACT2
from sls.model import ModelConfig, Policy
from sls.rl.rollout import generalized_advantage_estimate
from sls.rl.training_contract import (
    native_source_digest,
    training_implementation_digest,
)
from sls.rl.workers import ShardedWorkerPool


def main():
    torch.set_num_threads(4)
    torch.use_deterministic_algorithms(True)
    results = {}
    for name, path in {
        "parent": ROOT / "local/runs/ironclad-a20-act1-win-90m-continuation/final.pt",
        "endpoint": ROOT / "local/runs/ironclad-a20-act12-win-pilot-r1/final.pt",
    }.items():
        torch.manual_seed(1729)
        payload = torch.load(path, map_location="cpu", weights_only=False)
        model = Policy(ModelConfig.from_dict(payload["contract"]["model"]))
        model.load_state_dict(payload["model"])
        before = {k: v.clone() for k, v in model.state_dict().items()}
        captured = {}

        def capture(rewards, values, terminated, bootstrap, gamma, lam):
            captured.update(
                rewards=rewards,
                values=values,
                terminated=terminated,
                bootstrap=bootstrap,
            )
            return generalized_advantage_estimate(
                rewards, values, terminated, bootstrap, gamma, lam
            )

        original = ppo.generalized_advantage_estimate
        ppo.generalized_advantage_estimate = capture
        workers = ShardedWorkerPool(IRONCLAD_A20_ACT2, 16, 4)
        try:
            config = ppo.PPOConfig(
                **{**payload["contract"]["ppo"], "rollout_steps": 256}
            )
            trainer = ppo.PPOTrainer(
                model, workers, config, device="cuda", seed=131000000
            )
            rollout = trainer.collect()
            a98, r98 = generalized_advantage_estimate(
                **{
                    "rewards": captured["rewards"],
                    "values": captured["values"],
                    "terminated": captured["terminated"],
                    "bootstrap_values": captured["bootstrap"],
                    "gamma": 1.0,
                    "gae_lambda": 0.98,
                }
            )
            a1, r1 = generalized_advantage_estimate(
                captured["rewards"],
                captured["values"],
                captured["terminated"],
                captured["bootstrap"],
                1.0,
                1.0,
            )
            rows = []
            for env in range(16):
                for start in (
                    rollout.episode_starts[:, env].nonzero().flatten().tolist()
                ):
                    terminals = captured["terminated"][start:, env].nonzero().flatten()
                    if not terminals.numel():
                        continue
                    end = start + int(terminals[0])
                    direct = float(captured["rewards"][start : end + 1, env].sum())
                    rows.append(
                        {
                            "env": env,
                            "start": start,
                            "end": end,
                            "length": end - start + 1,
                            "value": float(captured["values"][start, env]),
                            "mc_shaped_return": direct,
                            "gae98_return": float(r98[start, env]),
                            "gae1_return": float(r1[start, env]),
                            "advantage98": float(a98[start, env]),
                            "advantage1": float(a1[start, env]),
                        }
                    )
            assert all(
                torch.equal(before[k], v.cpu()) for k, v in model.state_dict().items()
            )
            assert not trainer.optimizer.state
            assert (
                max(abs(r["gae1_return"] - r["mc_shaped_return"]) for r in rows) < 1e-5
            )
            results[name] = {
                "checkpoint_sha256": hashlib.file_digest(
                    path.open("rb"), "sha256"
                ).hexdigest(),
                "completed_normal_starts": rows,
                "terminations": trainer.last_collect_terminations,
                "weights_unchanged": True,
                "optimizer_empty": True,
            }
        finally:
            ppo.generalized_advantage_estimate = original
            workers.close()
    out = {
        "role": "local collection-only credit diagnostic; not efficacy evidence",
        "platform": platform.platform(),
        "torch": torch.__version__,
        "cuda": torch.version.cuda,
        "gpu": torch.cuda.get_device_name(),
        "native_source_sha256": native_source_digest(),
        "training_implementation_sha256": training_implementation_digest(),
        "training_seed_start": 131000000,
        "sampling_rng_seed": 1729,
        "workers": 16,
        "shards": 4,
        "rollout_steps": 256,
        "results": results,
    }
    with (ROOT / "local/reports/act12-pilot-credit-recomputed.json").open("x") as f:
        json.dump(out, f, indent=2)
    print(json.dumps(out))


if __name__ == "__main__":
    main()
