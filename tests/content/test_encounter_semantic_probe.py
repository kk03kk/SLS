from __future__ import annotations

import importlib.util
from pathlib import Path

from sls.content.scope import load_ironclad_a0_scope


ROOT = Path(__file__).resolve().parents[2]


def _module():
    spec = importlib.util.spec_from_file_location(
        "audit_encounter_semantics",
        ROOT / "tools" / "audit_encounter_semantics.py",
    )
    assert spec is not None and spec.loader is not None
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def test_stock_monster_source_locator_covers_the_exact_act1_scope() -> None:
    module = _module()
    sources = module._monster_sources()
    expected = set(map(str, load_ironclad_a0_scope()["monsters"]["act1"]))
    assert expected <= sources.keys()
    assert all(sources[identifier].is_file() for identifier in expected)
