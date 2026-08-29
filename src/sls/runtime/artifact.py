"""Standalone simulator-trained policy artifact for evaluation and live play."""

from __future__ import annotations

import hashlib
from dataclasses import asdict, dataclass
from pathlib import Path
from typing import Any, Mapping

import torch

from sls.content.scope import policy_excluded_content_ids
from sls.model import ENCODING_SCHEMA, ModelConfig, Policy, vocabulary_hash

POLICY_ARTIFACT_SCHEMA = "sls-policy-artifact-v5"


def model_state_sha256(state: Mapping[str, Any]) -> str:
    """Hash a state dict without relying on pickle serialization details."""

    digest = hashlib.sha256()
    for name in sorted(state):
        tensor = state[name]
        if not isinstance(tensor, torch.Tensor):
            raise ValueError(f"model state entry is not a tensor: {name}")
        value = tensor.detach().cpu().contiguous()
        digest.update(name.encode("utf-8"))
        digest.update(str(value.dtype).encode("ascii"))
        digest.update(str(tuple(value.shape)).encode("ascii"))
        digest.update(value.view(torch.uint8).numpy().tobytes())
    return digest.hexdigest()


@dataclass(frozen=True, slots=True)
class PolicyArtifactMetadata:
    model: Mapping[str, Any]
    encoding_schema: str
    vocabulary_sha256: str
    simulator_only: bool
    source_git_commit: str
    native_source_sha256: str
    training_config_sha256: str
    model_sha256: str
    recurrent_memory_size: int
    ascension_min: int = 0
    ascension_max: int = 0
    goal: str = "ACT1"
    excluded_content_ids: tuple[str, ...] = ("PRISMATIC_SHARD",)

    def validate(self) -> None:
        if self.simulator_only is not True:
            raise ValueError("policy artifacts must declare simulator-only provenance")
        if not self.source_git_commit:
            raise ValueError("policy artifact source Git commit is missing")
        if not self.native_source_sha256:
            raise ValueError("policy artifact native source digest is missing")
        if not self.training_config_sha256:
            raise ValueError("policy artifact training config digest is missing")
        if len(self.model_sha256) != 64:
            raise ValueError("policy artifact model digest is invalid")
        if self.recurrent_memory_size <= 0:
            raise ValueError("policy artifact recurrent memory size is invalid")
        if int(self.model.get("recurrent_hidden_dim", 0)) != self.recurrent_memory_size:
            raise ValueError("policy artifact recurrent metadata is inconsistent")
        if self.encoding_schema != ENCODING_SCHEMA:
            raise ValueError("policy artifact encoding schema is incompatible")
        if self.vocabulary_sha256 != vocabulary_hash():
            raise ValueError("policy artifact vocabulary is incompatible")
        if not 0 <= self.ascension_min <= self.ascension_max <= 20:
            raise ValueError("policy artifact ascension range is invalid")
        if self.goal not in {"ACT1", "ACT2", "ACT3", "FULLRUN", "HEART"}:
            raise ValueError("policy artifact goal is invalid")
        if set(self.excluded_content_ids) != set(policy_excluded_content_ids()):
            raise ValueError("policy artifact excluded-content contract is incompatible")


@dataclass(frozen=True, slots=True)
class LoadedPolicyArtifact:
    model: Policy
    metadata: PolicyArtifactMetadata


def _metadata(
    model_config: Mapping[str, Any], *, ascension_min: int,
    ascension_max: int, goal: str, provenance: Mapping[str, object],
    model_sha256: str,
) -> PolicyArtifactMetadata:
    return PolicyArtifactMetadata(
        model=dict(model_config),
        encoding_schema=ENCODING_SCHEMA,
        vocabulary_sha256=vocabulary_hash(),
        simulator_only=True,
        source_git_commit=str(provenance.get("git_commit") or ""),
        native_source_sha256=str(provenance.get("native_source_sha256") or ""),
        training_config_sha256=str(provenance.get("training_config_sha256") or ""),
        model_sha256=model_sha256,
        recurrent_memory_size=int(model_config["recurrent_hidden_dim"]),
        ascension_min=ascension_min,
        ascension_max=ascension_max,
        goal=goal,
        excluded_content_ids=tuple(sorted(policy_excluded_content_ids())),
    )


def export_policy_artifact(
    checkpoint: str | Path, output: str | Path, *, ascension_min: int,
    ascension_max: int, goal: str,
) -> Path:
    payload = torch.load(Path(checkpoint), map_location="cpu", weights_only=False)
    contract = payload.get("contract")
    if not isinstance(contract, Mapping) or not isinstance(payload.get("model"), Mapping):
        raise ValueError("training checkpoint has no model transfer contract")
    config_payload = dict(contract.get("model") or {})
    config_payload.pop("encoding_schema", None)
    config_payload.pop("vocabulary_hash", None)
    config = ModelConfig(**config_payload)
    model = Policy(config)
    model.load_state_dict(payload["model"], strict=True)
    return save_policy_artifact(
        model, output, ascension_min=ascension_min,
        ascension_max=ascension_max, goal=goal, provenance=contract,
    )


def save_policy_artifact(
    model: Policy, output: str | Path, *, ascension_min: int,
    ascension_max: int, goal: str, provenance: Mapping[str, object],
) -> Path:
    """Write an already-loaded v3 policy as a strict production artifact."""

    metadata = _metadata(
        model.config.to_dict(), ascension_min=ascension_min,
        ascension_max=ascension_max, goal=goal, provenance=provenance,
        model_sha256=model_state_sha256(model.state_dict()),
    )
    metadata.validate()
    target = Path(output)
    target.parent.mkdir(parents=True, exist_ok=True)
    temporary = target.with_suffix(target.suffix + ".tmp")
    torch.save({
        "schema": POLICY_ARTIFACT_SCHEMA,
        "metadata": asdict(metadata),
        "model": model.state_dict(),
    }, temporary)
    temporary.replace(target)
    return target


def load_policy_artifact(
    path: str | Path,
    *,
    device: str | torch.device = "cpu",
) -> LoadedPolicyArtifact:
    payload = torch.load(Path(path), map_location="cpu", weights_only=True)
    if payload.get("schema") != POLICY_ARTIFACT_SCHEMA:
        raise ValueError("unsupported production policy artifact")
    raw = payload.get("metadata")
    if not isinstance(raw, Mapping):
        raise ValueError("policy artifact metadata is missing")
    metadata = PolicyArtifactMetadata(**dict(raw))
    metadata.validate()
    config_payload = dict(metadata.model)
    config_payload.pop("encoding_schema", None)
    config_payload.pop("vocabulary_hash", None)
    model = Policy(ModelConfig(**config_payload))
    model.load_state_dict(payload["model"], strict=True)
    if model_state_sha256(model.state_dict()) != metadata.model_sha256:
        raise ValueError("policy artifact model digest does not match its weights")
    model.eval().to(device)
    return LoadedPolicyArtifact(model, metadata)
