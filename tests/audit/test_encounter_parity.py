from __future__ import annotations

import json
from pathlib import Path

from sls.audit.encounter_parity import encounter_scenario_runs


def test_encounter_parser_retains_pre_constructor_rng_boundary(
    tmp_path: Path,
) -> None:
    before = {"_rng": {"ai": {"counter": 7}}}
    first = {
        "_parity_scenario": {
            "scenario_id": "encounter_probe:CULTIST",
            "setup_digest": "digest",
        },
        "game_state": {"action_phase": "WAITING_ON_USER"},
    }
    repeated = {**first, "ready_for_command": True}
    log = tmp_path / "encounter.log"
    log.write_text(
        "".join(
            "Sending message: " + json.dumps(payload) + "\n"
            for payload in (before, first, repeated)
        ),
        encoding="utf-8",
    )

    runs = encounter_scenario_runs([log])
    assert len(runs) == 1
    assert runs[0]["before"] == before
    assert runs[0]["payloads"] == [first, repeated]
