"""Real 70M-to-Act2 local transfer/resume probe; NOT a 90M training result."""

# ruff: noqa: E402
import json
import math
import os
import sys
from pathlib import Path

os.environ.setdefault("CUBLAS_WORKSPACE_CONFIG", ":4096:8")
ROOT = Path(__file__).resolve().parents[3]
sys.path.insert(0, str(ROOT / "src"))

import torch

from sls.curriculum import IRONCLAD_A20_ACT2
from sls.rl import (
    PPOConfig,
    PPOTrainer,
    VectorWorkerPool,
    load_checkpoint,
    save_checkpoint,
)
from sls.rl.act1_transfer import initialize_act1_weights
from sls.rl.checkpoint import policy_from_training_checkpoint
from sls.rl.training_contract import (
    evaluation_identity,
    native_source_digest,
    sha256_file,
)

torch.set_num_threads(2)
torch.use_deterministic_algorithms(True)
torch.backends.cudnn.benchmark = False
torch.set_float32_matmul_precision("high")
source = ROOT / "local/runs/ironclad-a20-act1-win-70m-continuation/final.pt"
digest = "cb53fee1ae3f47cc906665c903068dabaf38a328be1e07697e6d17c5b21d7d5c"
assert sha256_file(source) == digest
payload = torch.load(source, map_location="cpu", weights_only=False)
model = policy_from_training_checkpoint(payload)
steps = payload["trainer"]["environment_steps"]
config = {"run": {"output": "local/reports/act12-transfer-micro-20261004"},
          "stages": {"train": {"target_environment_steps": steps + 64}},
          "warm_start": {"checkpoint": source.relative_to(ROOT).as_posix(),
                         "checkpoint_sha256": digest, "parent_environment_steps": steps,
                         "transfer_kind": "curriculum-stage"}}
ppo = PPOConfig(**{**payload["contract"]["ppo"], "rollout_steps": 16,
                   "recurrent_sequence_length": 8, "minibatch_sequences": 1})
output = ROOT / config["run"]["output"]
output.mkdir(parents=True, exist_ok=True)
with VectorWorkerPool(IRONCLAD_A20_ACT2, 1) as workers:
    trainer = PPOTrainer(model, workers, ppo, device="cuda", seed=130_000_000,
                         native_contract_digest=native_source_digest(),
                         training_seed_limit=2_000_000_000_000)
    before_environment = workers.checkpoints()
    before_rng = torch.get_rng_state().clone()
    transfer = initialize_act1_weights(trainer, config, root=ROOT)
    assert torch.equal(before_rng, torch.get_rng_state())
    assert workers.checkpoints() == before_environment
    assert not trainer.optimizer.state and trainer.update == 0
    assert all(torch.equal(value.detach().cpu(), payload["model"][name])
               for name, value in trainer.model.state_dict().items())
    first = trainer.train_update()
    checkpoint = save_checkpoint(output / "micro.pt", trainer)
    expected = trainer.train_update()
    load_checkpoint(checkpoint, trainer)
    actual = trainer.train_update()
    assert expected == actual, "same-runtime exact resume changed update metrics"
    assert all(math.isfinite(value) for value in first.values())
    assert all(math.isfinite(value) for value in actual.values())
    record = {"schema": "sls-act12-local-transfer-micro-v1", "date": "2026-10-04",
              "role": "local-implementation-validation-not-server-training",
              "parent_checkpoint_sha256": digest, "parent_steps": steps,
              "source_profile": "IRONCLAD_A20_ACT1", "target_profile": "IRONCLAD_A20_ACT2",
              "workers": 1, "rollout_steps": 16, "recurrent_sequence_length": 8,
              "new_decisions_after_replay": trainer.environment_steps - steps,
              "weight_and_state_transfer_verified": True, "exact_resume_metrics_equal": True,
              "finite_metrics": True, "source_unchanged": sha256_file(source) == digest,
              "transfer": transfer, "first_update": first, "second_update": actual,
              **evaluation_identity(device="cuda", environment_shards=1, ascension=20)}
    (output / "result.json").write_text(json.dumps(record, indent=2), encoding="utf-8")
    print(json.dumps({key: record[key] for key in (
        "role", "new_decisions_after_replay", "exact_resume_metrics_equal", "finite_metrics", "source_unchanged",
    )}))
