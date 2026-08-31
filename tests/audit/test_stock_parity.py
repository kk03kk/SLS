from __future__ import annotations

import json
import zipfile
from pathlib import Path

from sls.audit.stock_parity import (
    ORIGINAL_CAPTURED,
    UNREVIEWED,
    build_stock_parity_manifest,
    captured_original_scenarios,
)


def test_original_capture_parser_does_not_confuse_capture_with_parity(tmp_path: Path) -> None:
    payload = {
        "game_state": {},
        "_parity_scenario": {"scenario_id": "card_probe:ANGER:0"},
    }
    log = tmp_path / "original.log"
    log.write_text(
        "INFO Sending message: " + json.dumps(payload) + "\n",
        encoding="utf-8",
    )
    assert captured_original_scenarios([log]) == {
        "cards": {"ANGER": {"card_probe:ANGER:0"}},
    }


def test_manifest_inventory_and_evidence_statuses(tmp_path: Path) -> None:
    stock = tmp_path / "desktop-1.0.jar"
    stock.write_bytes(b"stock")
    oracle = tmp_path / "oracle.jar"
    members = {
        "card": "ANGER\tAnger\n",
        "potion": "ANCIENT_POTION\tAncient Potion\n",
        "relic": "AKABEKO\tAkabeko\n",
        "encounter": "CULTIST\tCultist\n",
        "event": "BIG_FISH\tBig Fish\n",
    }
    with zipfile.ZipFile(oracle, "w") as archive:
        for category, text in members.items():
            archive.writestr(
                f"spirecomm/parity/scenario-{category}-allowlist.tsv", text,
            )
    payload = {
        "_parity_scenario": {"scenario_id": "card_probe:ANGER:0"},
    }
    log = tmp_path / "original.log"
    log.write_text(
        "Sending message: " + json.dumps(payload) + "\n", encoding="utf-8",
    )

    manifest = build_stock_parity_manifest(
        stock_jar=stock, oracle_jar=oracle, original_logs=[log],
    )
    assert manifest["summary"]["cards"]["registry"] == 370
    anger = next(
        row for row in manifest["categories"]["cards"]
        if row["content_id"] == "ANGER"
    )
    assert anger["original_evidence"] == ORIGINAL_CAPTURED
    assert anger["simulator_parity"] == UNREVIEWED
    assert anger["ironclad_a0_scope"] is True
