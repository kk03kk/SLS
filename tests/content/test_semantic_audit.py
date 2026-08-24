from __future__ import annotations

from pathlib import Path
import subprocess
import sys

import pytest

from sls.content.semantic_audit import (
    load_card_semantic_audit, load_potion_semantic_audit,
    load_relic_semantic_audit, load_semantic_audit, verify_semantic_audit,
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
    assert len(payload["entries"]) == 12
    assert all(entry["covered_callbacks"] for entry in payload["entries"])


def test_committed_semantic_audit_is_current_and_has_no_known_difference() -> None:
    subprocess.run(
        (sys.executable, str(ROOT / "tools" / "audit_content_semantics.py"), "--check"),
        cwd=ROOT, check=True,
    )
    payload = load_semantic_audit()
    assert payload["summary"]["status_counts"]["DIFFERENCE"] == 0
    assert payload["summary"]["status_counts"]["BLOCKED"] > 0
    assert all(entry["status"] == "VERIFIED" for entry in payload["entries"]["cards"])
    assert all(entry["status"] == "VERIFIED" for entry in payload["entries"]["potions"])
    assert payload["summary"]["act1_pilot_ready"] is False


def test_incomplete_semantic_audit_blocks_pilot_but_not_engineering_checks() -> None:
    engineering = verify_semantic_audit(require_pilot_ready=False)
    assert engineering["valid"] is True
    assert engineering["act1_pilot_ready"] is False
    with pytest.raises(ValueError, match="Act 1 pilot remains blocked"):
        verify_semantic_audit(require_pilot_ready=True)
