"""Explicit curriculum weight transfer; never restore old optimizer/environment state."""

import json
from pathlib import Path

import torch

from sls.curriculum import IRONCLAD_A0_ACT1, IRONCLAD_A20_ACT1, IRONCLAD_A20_CURRICULUM
from sls.rl.checkpoint import policy_from_training_checkpoint
from sls.rl.training_contract import native_source_digest, sha256_file


def validate_curriculum_simulator_transition(spec: dict, source: str, target: str, *, root: Path) -> dict | None:
    """Require pinned, reviewed evidence for weight transfer across changed rules.

    This does not authorize restoring old environment/optimizer state.
    """
    if source == target:
        return None
    binding = spec.get("simulator_transition")
    if spec.get("transfer_kind") != "curriculum-stage" or not isinstance(binding, dict):
        raise ValueError("changed simulator requires an explicit curriculum weights-only transition")
    path = (root / binding["evidence"]).resolve()
    if not path.is_relative_to(root.resolve()) or sha256_file(path) != binding["evidence_sha256"]:
        raise ValueError("simulator transition evidence hash/path mismatch")
    evidence = json.loads(path.read_text(encoding="utf-8"))
    if (evidence.get("schema") != "sls-curriculum-simulator-transition-v1"
            or evidence.get("status") != "VERIFIED_RULE_CORRECTION"
            or evidence.get("source_native_source_sha256") != source
            or evidence.get("target_native_source_sha256") != target
            or evidence.get("transfer_mode") != "weights-only"
            or evidence.get("exact_resume_allowed") is not False
            or not evidence.get("semantics_revision")):
        raise ValueError("simulator transition does not match the reviewed source and target")
    proofs = evidence.get("evidence")
    if not isinstance(proofs, dict) or len(proofs) < 2:
        raise ValueError("simulator transition lacks before/after evidence")
    for name, expected in proofs.items():
        proof = (path.parent / name).resolve()
        if not proof.is_relative_to(root.resolve()) or sha256_file(proof) != expected:
            raise ValueError("simulator transition supporting evidence changed")
    return evidence


def initialize_act1_weights(trainer, config: dict, *, root: Path) -> dict:
    spec = config["warm_start"]
    source = (root / spec["checkpoint"]).resolve()
    output = (root / config["run"]["output"]).resolve()
    if source.is_relative_to(output) or output.is_relative_to(source.parent):
        raise ValueError("A20 transfer must use a separate output directory")
    if trainer.environment_steps or trainer.update or trainer.optimizer.state:
        raise ValueError("weight transfer requires a fresh trainer")
    target_profile = trainer.workers.profile
    if target_profile not in IRONCLAD_A20_CURRICULUM:
        raise ValueError("weight transfer requires an A20 curriculum profile")
    digest = sha256_file(source)
    if digest != spec["checkpoint_sha256"]:
        raise ValueError("warm-start checkpoint SHA256 mismatch")
    payload = torch.load(source, map_location="cpu", weights_only=False)
    source_profile = payload["contract"]["profile"]
    target_index = IRONCLAD_A20_CURRICULUM.index(target_profile)
    if target_index == 0:
        if source_profile not in {IRONCLAD_A0_ACT1, IRONCLAD_A20_ACT1}:
            raise ValueError("warm-start parent must be an Act1 profile")
    elif (source_profile != IRONCLAD_A20_CURRICULUM[target_index - 1]
          or spec.get("transfer_kind") != "curriculum-stage"):
        raise ValueError("curriculum-stage transfer requires the immediately preceding A20 profile")
    same_profile = source_profile == target_profile
    if same_profile and spec.get("transfer_kind") != "optimization-experiment":
        raise ValueError(
            "same-profile warm start requires transfer_kind = optimization-experiment"
        )
    simulator_transition = validate_curriculum_simulator_transition(
        spec, payload["contract"]["native_source_sha256"], native_source_digest(), root=root,
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
        "schema": ("sls-curriculum-weight-transfer-v2" if simulator_transition else
                   "sls-act1-weight-transfer-v2" if target_index == 0 else "sls-curriculum-weight-transfer-v1"),
        "parent_checkpoint_sha256": digest,
        "parent_environment_steps": steps,
        "parent_checkpoint": str(source),
        "source_profile": source_profile.profile_id,
        "target_profile": target_profile.profile_id,
        "preserved": "all model weights, including value head",
        "reset": "Adam, RNG, workers, recurrent state, update count and best selection",
        "step_accounting": "parent steps + new A20 environment steps",
        "entropy_clock": "cumulative environment steps",
        "exact_resume_of_parent_experiment": False,
        **({"simulator_transition": simulator_transition} if simulator_transition else {}),
    }


def initialize_a20_weights(trainer, config: dict, *, root: Path) -> dict:
    """Compatibility wrapper for callers using the original function name."""

    return initialize_act1_weights(trainer, config, root=root)
