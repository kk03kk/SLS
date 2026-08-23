from __future__ import annotations

import json
from pathlib import Path
import tomllib

import pytest

from sls.rl.training_contract import canonical_digest
from sls.validation.readiness import BundleRecord
from sls.validation.readiness_lock import (
    ENGINEERING_READY, READINESS_LOCK_SCHEMA, TRAINING_READY, _contract,
    _select_routes, _validate_expansion, verify_readiness_lock,
)
from sls.validation.truth import value_hash


def _route(seed: int, boss: str, leaf: str, actions: tuple[str, ...]):
    return {
        "seed": seed, "leaf": leaf, "chain": [leaf], "used_boundaries": 1,
        "coverage": {"bosses": [boss], "screens": ["MAP"], "selected_actions": list(actions), "rooms": ["MonsterRoomBoss"]},
    }


def test_route_selection_uses_unique_seed_and_maximizes_coverage() -> None:
    requirements = {"bosses": ["A", "B", "C"], "screens": ["MAP"], "selected_actions": ["X", "Y"]}
    report = {"valid_routes": [
        _route(0, "A", "a-poor", ("X",)), _route(0, "A", "a-rich", ("X", "Y")),
        _route(1, "B", "b", ("X",)), _route(2, "C", "c", ("X",)),
    ]}
    selected = _select_routes(report, requirements)
    assert {item["leaf"] for item in selected} == {"a-rich", "b", "c"}


def test_lock_verifier_rejects_tampering(tmp_path: Path) -> None:
    requirements = {"bosses": ["A"], "routes": 1}
    lock = {
        "schema": READINESS_LOCK_SCHEMA, "profile": "P",
        "requirements": requirements, "requirements_sha256": canonical_digest(requirements),
        "contract": _contract(), "routes": [{"boss": "A", "seed": 0}],
        "bundles": [], "coverage": {},
    }
    lock["lock_sha256"] = canonical_digest(lock)
    path = tmp_path / "lock.json"
    path.write_text(json.dumps(lock), encoding="utf-8")
    assert verify_readiness_lock(path, require_clean=False)["valid"]
    assert verify_readiness_lock(path, require_clean=False)["level"] == ENGINEERING_READY
    with pytest.raises(ValueError, match="level mismatch"):
        verify_readiness_lock(
            path, require_clean=False, expected_level=TRAINING_READY,
        )
    lock["profile"] = "TAMPERED"
    path.write_text(json.dumps(lock), encoding="utf-8")
    with pytest.raises(ValueError, match="digest mismatch"):
        verify_readiness_lock(path, require_clean=False)


def test_training_level_cannot_masquerade_without_expansion(tmp_path: Path) -> None:
    requirements = {"bosses": ["A"], "routes": 1, "expansion": {
        "rounds": 2, "seeds_per_round": 4,
    }}
    lock = {
        "schema": READINESS_LOCK_SCHEMA, "level": TRAINING_READY, "profile": "P",
        "requirements": requirements, "requirements_sha256": canonical_digest(requirements),
        "contract": _contract(), "routes": [{"boss": "A", "seed": 0}],
        "bundles": [], "coverage": {},
    }
    lock["lock_sha256"] = canonical_digest(lock)
    path = tmp_path / "lock.json"
    path.write_text(json.dumps(lock), encoding="utf-8")
    with pytest.raises(ValueError, match="incomplete expansion"):
        verify_readiness_lock(path, require_clean=False, expected_level=TRAINING_READY)


def test_training_configs_use_the_required_readiness_level() -> None:
    root = Path(__file__).resolve().parents[2]
    expected = {
        "act1_smoke.toml": (ENGINEERING_READY, "act1_readiness.lock.json"),
        "act1_pilot.toml": (TRAINING_READY, "act1_training_readiness.lock.json"),
        "full_run.toml": (TRAINING_READY, "act1_training_readiness.lock.json"),
    }
    for name, (level, lock_name) in expected.items():
        with (root / "configs" / "train" / name).open("rb") as stream:
            run = tomllib.load(stream)["run"]
        assert run["readiness_level"] == level
        assert Path(run["readiness_lock"]).name == lock_name


def _expansion_records(*, short_seed: int | None = None, bad_seed: int | None = None):
    records = {}
    for seed in range(10, 18):
        variant = seed % 4
        count = 49 if seed == short_seed else 50
        boundaries = [
            {
                "sequence": index,
                "cursor": {"act": 1, "floor": 8, "screen": "COMBAT", "room": "MonsterRoom"},
                "canonical_public_state": {"run": {"boss": "SLIME_BOSS"}},
                "selected_action": {"kind": "END_TURN"},
                "comparison": {"status": "DIFFERENCE" if seed == bad_seed and index == count - 1 else "MATCH"},
                "terminal_kind": "DEATH" if index == count - 1 else None,
            }
            for index in range(count)
        ]
        records[f"leaf-{seed}"] = BundleRecord(
            Path(f"leaf-{seed}"),
            {
                "seed": seed, "profile_id": "IRONCLAD_A0_ACT1",
                "evidence_class": "LIVE_FULLRUN", "capture_mode": "PAIRED",
                "policy_id": f"deterministic-action-v1:variant-{variant}",
                "instrumentation": {"schema": "spirecomm-parity-v10"},
            },
            boundaries,
        )
    return records


def _expansion_report():
    rounds = []
    for number, seeds in enumerate((range(10, 14), range(14, 18)), 1):
        selections = [{"seed": seed, "variant": seed % 4} for seed in seeds]
        selection = {"schema": "sls-act1-validation-selection-v1", "selections": selections}
        selection["selection_sha256"] = value_hash(selection)
        rounds.append({
            "round": number, "selection": selection,
            "evidence": [
                {"seed": seed, "variant": seed % 4, "leaf": f"leaf-{seed}"}
                for seed in seeds
            ],
        })
    return {"schema": "sls-act1-validation-expansion-v1", "rounds": rounds}


def test_training_expansion_requires_two_clean_unique_rounds() -> None:
    requirements = {"expansion": {
        "rounds": 2, "seeds_per_round": 4, "min_floor": 8,
        "min_boundaries": 50, "oracle_schema": "spirecomm-parity-v10",
    }}
    base = tuple({"seed": seed} for seed in (0, 3, 4))
    result = _validate_expansion(
        _expansion_records(), base, requirements, _expansion_report(),
        replay_validator=lambda *_: None,
    )
    assert result["lock"]["unique_seed_count"] == 11
    with pytest.raises(ValueError, match="too short"):
        _validate_expansion(
            _expansion_records(short_seed=10), base, requirements, _expansion_report(),
            replay_validator=lambda *_: None,
        )
    with pytest.raises(ValueError, match="invalid validation expansion evidence"):
        _validate_expansion(
            _expansion_records(bad_seed=10), base, requirements, _expansion_report(),
            replay_validator=lambda *_: None,
        )
