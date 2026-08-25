from __future__ import annotations

import importlib.util
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]


def _module():
    spec = importlib.util.spec_from_file_location(
        "audit_mechanism_semantics",
        ROOT / "tools" / "audit_mechanism_semantics.py",
    )
    assert spec is not None and spec.loader is not None
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def test_mechanism_scenarios_are_explicit_and_action_bounded() -> None:
    module = _module()
    assert module.SCENARIOS == {
        "damage_buffer_intangible": ("DAMAGE_PIPELINE", ("END_TURN",)),
        "duration_weak": ("POWER_ORDER", ("END_TURN",)),
        "retain_ethereal": ("POWER_ORDER", ("END_TURN",)),
    }
