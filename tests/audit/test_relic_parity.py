from __future__ import annotations

from sls.audit.relic_parity import (
    _is_known_equip_boundary,
    _specialized_projection,
)


def test_trigger_projection_excludes_oracle_batch_state_leakage() -> None:
    result = {
        "weak": 1,
        "vulnerable": 0,
        "hp_after": 45,
        "hp_delta": 0,
    }
    assert _specialized_projection(
        "relic_trigger_probe:CHAMPION_BELT", result,
    ) == {"weak": 1, "vulnerable": 0}


def test_only_documented_equip_timing_fields_are_boundaries() -> None:
    assert _is_known_equip_boundary(
        "relic_equip_probe:ASTROLABE",
        [{"path": "$.upgraded_delta", "original": 0, "simulator": 3}],
    )
    assert not _is_known_equip_boundary(
        "relic_equip_probe:ASTROLABE",
        [{"path": "$.affected", "original": 3, "simulator": 0}],
    )
