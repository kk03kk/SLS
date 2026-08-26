from __future__ import annotations

import json
from pathlib import Path
import signal

import pytest

from sls.rl.training_contract import readiness_settings
from tools.train_full_run import (
    StopController, _positive_int, _resolve_resume, _trim_metrics,
)


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


def test_readiness_required_config_has_no_implicit_legacy_fallback() -> None:
    with pytest.raises(ValueError, match="missing explicit field"):
        readiness_settings({"readiness_lock": "strict.json"})
    with pytest.raises(ValueError, match="missing explicit field"):
        readiness_settings({"readiness_level": "TRAINING_READY"})


def test_positive_training_intervals_fail_before_the_training_loop() -> None:
    assert _positive_int({"updates": 20}, "updates") == 20
    with pytest.raises(ValueError, match="must be positive"):
        _positive_int({"updates": 0}, "updates")
