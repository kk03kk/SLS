from __future__ import annotations

import importlib.util
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]


def test_static_java_cpp_audit_has_no_critical_failures() -> None:
    path = ROOT / "tools" / "audit_static.py"
    spec = importlib.util.spec_from_file_location("sls_static_audit", path)
    assert spec is not None and spec.loader is not None
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)

    report = module.audit()
    assert report["status"] == "pass", report["failures"]
    assert report["evidence"]["reference_java_files"] >= 4870
    assert report["evidence"]["ironclad_playable_cards"] == 75
    assert report["evidence"]["colorless_reward_cards"] == 35
    assert report["evidence"]["ironclad_relic_pool"] == 130
    assert report["evidence"]["ironclad_potion_pool"] == 33
    assert report["evidence"]["ironclad_potions_missing_use_paths"] == []
    assert report["evidence"]["pooled_events"] == 51
    assert report["evidence"]["pooled_events_missing_choice_paths"] == []
