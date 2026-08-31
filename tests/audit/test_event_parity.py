from __future__ import annotations

import json
from pathlib import Path

from sls.audit.event_parity import event_scenario_runs


def test_event_parser_retains_constructor_rng_boundary(tmp_path: Path) -> None:
    before = {"_rng": {"event": {"counter": 3}}}
    event = {
        "_parity_scenario": {"scenario_id": "event_probe:BIG_FISH"},
        "game_state": {"screen_type": "EVENT"},
    }
    log = tmp_path / "event.log"
    log.write_text(
        "".join(
            "Sending message: " + json.dumps(payload) + "\n"
            for payload in (before, event, event)
        ),
        encoding="utf-8",
    )

    runs = event_scenario_runs([log])
    assert len(runs) == 1
    assert runs[0]["before"] == before
    assert runs[0]["payloads"] == [event, event]
