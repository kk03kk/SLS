from __future__ import annotations

from pathlib import Path
import subprocess
import sys

import pytest

from sls.content.semantic_audit import (
    load_card_semantic_audit, load_potion_semantic_audit,
    load_relic_semantic_audit, load_mechanism_semantic_audit,
    load_encounter_semantic_audit,
    load_event_semantic_audit,
    load_semantic_audit, verify_semantic_audit,
)


ROOT = Path(__file__).resolve().parents[2]


def test_committed_card_semantic_audit_covers_every_scoped_variant() -> None:
    payload = load_card_semantic_audit()
    assert len(payload["entries"]) == 130
    assert all(
        [variant["upgrades"] for variant in entry["variants"]] == [0, 1]
        for entry in payload["entries"]
    )


def test_committed_potion_semantic_audit_covers_every_scoped_potion() -> None:
    payload = load_potion_semantic_audit()
    assert len(payload["entries"]) == 33
    assert all(entry["variants"] for entry in payload["entries"])


def test_committed_relic_semantic_audit_covers_first_turn_batch() -> None:
    payload = load_relic_semantic_audit()
    assert len(payload["entries"]) == 151
    assert all(
        sorted(entry["covered_callbacks"] + entry["remaining_callbacks"])
        for entry in payload["entries"]
        if entry["covered_callbacks"] or entry["remaining_callbacks"]
    )


def test_committed_event_semantic_audit_covers_every_non_neow_event() -> None:
    assert len(load_event_semantic_audit()["entries"]) == 51


def test_committed_mechanism_audit_has_two_boundary_original_native_traces() -> None:
    payload = load_mechanism_semantic_audit()
    assert {entry["id"] for entry in payload["entries"]} == {
        "damage_buffer_intangible", "duration_weak", "retain_ethereal",
        "engine_orb", "engine_stance", "noncombat_potion_actions",
        "run_and_checkpoint",
    }
    assert all(entry["boundary_hashes"] for entry in payload["entries"])


def test_committed_encounter_audit_covers_exact_act1_encounters_and_monsters() -> None:
    payload = load_encounter_semantic_audit()
    assert len(payload["entries"]) == 20
    assert len({
        monster_id
        for entry in payload["entries"]
        for monster_id in entry["monster_ids"]
    }) == 25
    assert all(len(entry["boundary_hashes"]) == 2 for entry in payload["entries"])


def test_committed_semantic_audit_is_current_and_has_no_known_difference() -> None:
    subprocess.run(
        (sys.executable, str(ROOT / "tools" / "audit_content_semantics.py"), "--check"),
        cwd=ROOT, check=True,
    )
    payload = load_semantic_audit()
    assert payload["summary"]["status_counts"]["DIFFERENCE"] == 0
    assert payload["summary"]["status_counts"]["BLOCKED"] == 0
    assert payload["summary"]["status_counts"]["VERIFIED"] == 418
    assert all(entry["status"] == "VERIFIED" for entry in payload["entries"]["cards"])
    assert all(entry["status"] == "VERIFIED" for entry in payload["entries"]["potions"])
    assert payload["summary"]["act1_pilot_ready"] is True


def test_complete_semantic_audit_allows_pilot_and_engineering_checks() -> None:
    engineering = verify_semantic_audit(require_pilot_ready=False)
    assert engineering["valid"] is True
    assert engineering["act1_pilot_ready"] is True
    pilot = verify_semantic_audit(require_pilot_ready=True)
    assert pilot["valid"] is True
    assert pilot["act1_pilot_ready"] is True
