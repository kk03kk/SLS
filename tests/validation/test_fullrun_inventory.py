from __future__ import annotations

from tools.audit_fullrun_semantics import build_audit
from tools.generate_fullrun_inventory import build_inventory

from sls.validation.fullrun_audit import load_fullrun_audit, load_fullrun_inventory


def test_fullrun_inventory_is_complete_and_deterministic() -> None:
    inventory = load_fullrun_inventory()
    assert inventory == build_inventory()
    assert inventory["acts"] == [1, 2, 3, 4]
    assert inventory["ascension_range"] == [0, 20]
    assert len(inventory["profiles"]) == 42
    assert len(inventory["ascension_modifiers"]) == 20
    assert inventory["rooms"] == [
        "BOSS", "BOSS_TREASURE", "ELITE", "EVENT", "MONSTER", "REST", "SHOP", "TREASURE",
    ]
    assert inventory["counts"]["original_theoretical_a0_heart"] == {
        "cards": 344, "encounters": 63, "events": 52,
            "monsters": 65, "potions": 33, "relics": 151,
    }
    assert inventory["counts"]["original_theoretical_a20_heart"]["cards"] == 345
    assert inventory["counts"]["original_theoretical_a20_heart"]["events"] == 51
    assert inventory["counts"]["native_current_a0_heart"]["cards"] == 130
    assert inventory["counts"]["native_current_a0_heart"]["relics"] == 150


def test_fullrun_audit_cannot_claim_readiness_with_blocked_entries() -> None:
    audit = load_fullrun_audit()
    assert audit == build_audit()
    assert audit["summary"]["status_counts"]["VERIFIED"] == 418
    assert audit["summary"]["status_counts"]["DIFFERENCE"] == 1
    assert audit["summary"]["status_counts"]["BLOCKED"] > 0
    assert audit["summary"]["fullrun_training_ready"] is False
    assert audit["stages"]["A0_ACT1"]["status"] == "TRAINING_READY"
    assert all(
        stage["status"] == "BLOCKED"
        for name, stage in audit["stages"].items() if name != "A0_ACT1"
    )
