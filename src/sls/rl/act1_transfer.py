"""Explicit Act1 weight transfer; never restore old optimizer/environment state."""

from pathlib import Path

import torch

from sls.curriculum import IRONCLAD_A0_ACT1, IRONCLAD_A20_ACT1
from sls.rl.checkpoint import policy_from_training_checkpoint
from sls.rl.training_contract import sha256_file


def initialize_act1_weights(trainer, config: dict, *, root: Path) -> dict:
    spec = config["warm_start"]
    source = (root / spec["checkpoint"]).resolve()
    output = (root / config["run"]["output"]).resolve()
    if source.is_relative_to(output) or output.is_relative_to(source.parent):
        raise ValueError("A20 transfer must use a separate output directory")
    if trainer.environment_steps or trainer.update or trainer.optimizer.state:
        raise ValueError("weight transfer requires a fresh trainer")
    if trainer.workers.profile != IRONCLAD_A20_ACT1:
        raise ValueError("weight transfer requires IRONCLAD_A20_ACT1")
    digest = sha256_file(source)
    if digest != spec["checkpoint_sha256"]:
        raise ValueError("warm-start checkpoint SHA256 mismatch")
    payload = torch.load(source, map_location="cpu", weights_only=False)
    source_profile = payload["contract"]["profile"]
    if source_profile not in {IRONCLAD_A0_ACT1, IRONCLAD_A20_ACT1}:
        raise ValueError("warm-start parent must be an Act1 profile")
    same_profile = source_profile == IRONCLAD_A20_ACT1
    if same_profile and spec.get("transfer_kind") != "optimization-experiment":
        raise ValueError(
            "same-profile warm start requires transfer_kind = optimization-experiment"
        )
    steps = int(payload["trainer"]["environment_steps"])
    if steps != int(spec["parent_environment_steps"]):
        raise ValueError("warm-start parent step count mismatch")
    if int(config["stages"]["train"]["target_environment_steps"]) <= steps:
        raise ValueError("cumulative target must exceed parent steps")
    # Policy construction initializes parameters. Do not perturb the new run's
    # RNG just to validate/load the parent's weights.
    with torch.random.fork_rng(devices=[]):
        policy = policy_from_training_checkpoint(payload)
    if policy.config != trainer.model.config:
        raise ValueError("warm-start model configuration mismatch")
    trainer.model.load_state_dict(policy.state_dict(), strict=True)
    trainer.environment_steps = steps
    return {
        "schema": "sls-act1-weight-transfer-v2",
        "parent_checkpoint_sha256": digest,
        "parent_environment_steps": steps,
        "parent_checkpoint": str(source),
        "source_profile": source_profile.profile_id,
        "target_profile": IRONCLAD_A20_ACT1.profile_id,
        "preserved": "all model weights, including value head",
        "reset": "Adam, RNG, workers, recurrent state, update count and best selection",
        "step_accounting": "parent steps + new A20 environment steps",
        "entropy_clock": "cumulative environment steps",
        "exact_resume_of_parent_experiment": False,
    }


def initialize_a20_weights(trainer, config: dict, *, root: Path) -> dict:
    """Compatibility wrapper for callers using the original function name."""

    return initialize_act1_weights(trainer, config, root=root)
