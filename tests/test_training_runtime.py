from __future__ import annotations

import json
from pathlib import Path
import signal

from tools.train_full_run import StopController, _resolve_resume, _trim_metrics


def test_stop_controller_defers_signal_to_safe_boundary() -> None:
    controller = StopController()
    controller.handler(signal.SIGTERM, None)
    assert controller.requested is True
    assert controller.signal_name == "SIGTERM"


def test_resume_auto_and_metric_reconciliation(tmp_path: Path) -> None:
    latest = tmp_path / "latest.pt"
    latest.write_bytes(b"checkpoint")
    assert _resolve_resume("auto", tmp_path) == latest
    metrics = tmp_path / "metrics.jsonl"
    metrics.write_text(
        "".join(json.dumps({"update": value}) + "\n" for value in (1, 2, 3)),
        encoding="utf-8",
    )
    assert _trim_metrics(metrics, 2) == 1
    assert [json.loads(line)["update"] for line in metrics.read_text().splitlines()] == [1, 2]
