from __future__ import annotations

from pathlib import Path
import tomllib

import pytest
import torch

from sls.model import ModelConfig, Policy
from sls.rl.training_mode import TrainingMode, parse_training_mode
from sls.runtime.artifact import export_policy_artifact
from tools import preflight_training, train_full_run


def test_mode_is_required_and_illegal_values_fail_fast() -> None:
    with pytest.raises(ValueError, match="missing required training_mode"):
        parse_training_mode(None)
    with pytest.raises(ValueError, match="invalid training_mode"):
        parse_training_mode("debug")


def test_fresh_clone_experimental_does_not_open_missing_gate(tmp_path: Path) -> None:
    gate = tmp_path / "runs" / "policy_transfer_v1.json"
    assert not gate.exists()
    safety = train_full_run.resolve_training_safety({
        "training_mode": "EXPERIMENTAL", "require_transfer_gate": False,
    }, "IRONCLAD_A0_ACT1", root=tmp_path)
    assert safety["training_mode"] is TrainingMode.EXPERIMENTAL
    assert safety["policy_transfer_verified"] is False
    assert preflight_training.verify_transfer_for_mode(
        "EXPERIMENTAL", gate, "IRONCLAD_A0_ACT1",
    ) is None


def test_fresh_clone_production_missing_gate_fails_fast(tmp_path: Path) -> None:
    with pytest.raises(FileNotFoundError):
        train_full_run.resolve_training_safety({
            "training_mode": "PRODUCTION", "require_transfer_gate": True,
            "transfer_gate": "runs/policy_transfer_v1.json",
        }, "IRONCLAD_A0_ACT1", root=tmp_path)


def test_complete_production_evidence_enables_production_mode(
    tmp_path: Path, monkeypatch: pytest.MonkeyPatch,
) -> None:
    gate = tmp_path / "gate.json"
    gate.write_text("{}", encoding="utf-8")
    calls = []

    def verify(path: Path, *, profile_id: str, require_canary: bool):
        calls.append((path, profile_id, require_canary))
        return {"schema": "sls-policy-transfer-v1"}

    monkeypatch.setattr(train_full_run, "verify_policy_transfer_gate", verify)
    safety = train_full_run.resolve_training_safety({
        "training_mode": "PRODUCTION", "require_transfer_gate": True,
        "transfer_gate": str(gate),
    }, "IRONCLAD_A0_ACT1", root=tmp_path)
    assert safety["policy_transfer_verified"] is True
    assert calls == [(gate, "IRONCLAD_A0_ACT1", True)]


def test_experimental_checkpoint_cannot_be_renamed_into_production(
    tmp_path: Path,
) -> None:
    model = Policy(ModelConfig(
        embedding_dim=32, transformer_layers=1,
        attention_heads=4, feedforward_dim=64,
    ))
    checkpoint = tmp_path / "experimental.pt"
    torch.save({
        "contract": {
            "model": model.config.to_dict(),
            "training_mode": "EXPERIMENTAL",
            "policy_transfer_verified": False,
        },
        "model": model.state_dict(),
    }, checkpoint)
    renamed = tmp_path / "production-final.pt"
    checkpoint.rename(renamed)
    with pytest.raises(ValueError, match="cannot be used for production"):
        export_policy_artifact(
            renamed, tmp_path / "live.pt",
            ascension_min=0, ascension_max=0, goal="ACT1",
        )


def test_committed_stage_configs_have_non_implicit_modes() -> None:
    expected = {
        "act1_smoke.toml": "EXPERIMENTAL",
        "act1_pilot.toml": "EXPERIMENTAL",
        "act1_train.toml": "PRODUCTION",
    }
    root = Path(__file__).resolve().parents[1]
    for name, mode in expected.items():
        with (root / "configs" / "train" / name).open("rb") as stream:
            assert tomllib.load(stream)["run"]["training_mode"] == mode
