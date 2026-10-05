from __future__ import annotations

import json
from pathlib import Path

from sls.audit.mechanism_parity import mechanism_payloads


def test_mechanism_payloads_only_collects_supported_scenarios(tmp_path: Path) -> None:
    log = tmp_path / "original.log"
    lines = []
    for scenario_id in (
        "retain_ethereal", "engine_probe:ORB", "card_probe:ANGER:0",
    ):
        payload = {"_parity_scenario": {"scenario_id": scenario_id}}
        lines.append("Sending message: " + json.dumps(payload))
    log.write_text("\n".join(lines), encoding="utf-8")
    assert set(mechanism_payloads([log])) == {
        "retain_ethereal", "engine_probe:ORB",
    }
