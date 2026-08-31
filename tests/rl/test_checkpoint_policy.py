from __future__ import annotations

from dataclasses import asdict

import pytest

from sls.content.scope import IRONCLAD_A0_SCOPE_ID, ironclad_a0_scope_hash
from sls.model import ENCODING_SCHEMA, ModelConfig, Policy, vocabulary_hash
from sls.rl.checkpoint import policy_from_training_checkpoint
from sls.rl.training_contract import TRAINING_CHECKPOINT_SCHEMA


def _checkpoint(model_config: dict[str, object]) -> dict[str, object]:
    model = Policy(ModelConfig.from_dict(model_config))
    return {
        "schema": TRAINING_CHECKPOINT_SCHEMA,
        "contract": {
            "model": model_config,
            "encoding_schema": ENCODING_SCHEMA,
            "vocabulary_sha256": vocabulary_hash(),
            "content_scope_id": IRONCLAD_A0_SCOPE_ID,
            "content_scope_sha256": ironclad_a0_scope_hash(),
            "native_source_sha256": "a" * 64,
            "simulator_only": True,
        },
        "model": model.state_dict(),
    }


def _small_config() -> ModelConfig:
    return ModelConfig(
        embedding_dim=32, transformer_layers=1, attention_heads=4,
        feedforward_dim=64, recurrent_hidden_dim=64,
    )


def test_6b2fe23_style_checkpoint_model_contract_loads_safely() -> None:
    config = _small_config()
    payload = _checkpoint(config.to_dict())

    loaded = policy_from_training_checkpoint(payload)

    assert loaded.config == config


def test_constructor_only_model_contract_also_loads_safely() -> None:
    config = _small_config()
    payload = _checkpoint(asdict(config))

    loaded = policy_from_training_checkpoint(payload)

    assert loaded.config == config


@pytest.mark.parametrize(
    "field,bad_value,match",
    (
        ("encoding_schema", "wrong-input", "encoding_schema"),
        ("vocabulary_sha256", "b" * 64, "vocabulary_sha256"),
        ("content_scope_id", "wrong-scope", "content_scope_id"),
    ),
)
def test_checkpoint_outer_policy_identity_remains_strict(
    field: str, bad_value: str, match: str,
) -> None:
    payload = _checkpoint(_small_config().to_dict())
    payload["contract"][field] = bad_value  # type: ignore[index]

    with pytest.raises(ValueError, match=match):
        policy_from_training_checkpoint(payload)


def test_legacy_embedded_model_identity_remains_strict() -> None:
    model_config = _small_config().to_dict()
    model_config["vocabulary_hash"] = "b" * 64

    with pytest.raises(ValueError, match="vocabulary"):
        ModelConfig.from_dict(model_config)


def test_model_architecture_remains_strict() -> None:
    model_config = asdict(_small_config())
    model_config["architecture"] = "unknown-policy"

    with pytest.raises(ValueError, match="unsupported policy architecture"):
        ModelConfig.from_dict(model_config)
