from __future__ import annotations

import json
from pathlib import Path

import pytest

from sls.rl.training_contract import canonical_digest
from sls.validation.readiness_lock import (
    READINESS_LOCK_SCHEMA, _contract, _select_routes, verify_readiness_lock,
)


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
    lock["profile"] = "TAMPERED"
    path.write_text(json.dumps(lock), encoding="utf-8")
    with pytest.raises(ValueError, match="digest mismatch"):
        verify_readiness_lock(path, require_clean=False)
