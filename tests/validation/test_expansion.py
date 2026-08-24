from __future__ import annotations

from pathlib import Path

import pytest

from sls.validation.expansion import assemble_expansion_round
from sls.validation.readiness import BundleRecord
from sls.validation.truth import value_hash


def _selection(seed: int, variant: int) -> dict:
    result = {
        "schema": "sls-act1-validation-selection-v1",
        "selections": [{"seed": seed, "variant": variant}],
    }
    result["selection_sha256"] = value_hash(result)
    return result


def _record(
    seed: int, variant: int, *, count: int = 50, status: str = "MATCH",
    leaf: str | None = None,
) -> BundleRecord:
    return BundleRecord(
        Path(leaf or f"leaf-{seed}"),
        {
            "seed": seed, "profile_id": "IRONCLAD_A0_ACT1",
            "evidence_class": "LIVE_FULLRUN", "capture_mode": "PAIRED",
            "policy_id": f"deterministic-action-v1:variant-{variant}",
            "instrumentation": {"schema": "spirecomm-parity-v10"},
        },
        [
            {
                "sequence": index,
                "cursor": {"act": 1, "floor": 8, "screen": "COMBAT"},
                "comparison": {"status": status if index == count - 1 else "MATCH"},
                "terminal_kind": "DEATH" if index == count - 1 else None,
            }
            for index in range(count)
        ],
    )


def test_expansion_round_assembles_only_clean_selected_evidence(monkeypatch) -> None:
    records = {"leaf-10": _record(10, 2)}
    monkeypatch.setattr(
        "sls.validation.expansion.load_records", lambda _root: (records, []),
    )
    result = assemble_expansion_round(Path("truth"), _selection(10, 2), round_number=1)
    assert result["rounds"][0]["evidence"] == [
        {"seed": 10, "variant": 2, "leaf": "leaf-10"},
    ]
    with pytest.raises(ValueError, match="appended consecutively"):
        assemble_expansion_round(Path("truth"), _selection(11, 0), round_number=3, previous=result)


def test_expansion_round_rejects_difference_and_tampered_selection(monkeypatch) -> None:
    records = {"leaf-10": _record(10, 2, status="DIFFERENCE")}
    monkeypatch.setattr(
        "sls.validation.expansion.load_records", lambda _root: (records, []),
    )
    with pytest.raises(ValueError, match="no clean current-schema evidence"):
        assemble_expansion_round(Path("truth"), _selection(10, 2), round_number=1)
    selection = _selection(10, 2)
    selection["selections"][0]["seed"] = 11
    with pytest.raises(ValueError, match="digest mismatch"):
        assemble_expansion_round(Path("truth"), selection, round_number=1)


def test_expansion_round_prefers_latest_equally_strong_recapture(monkeypatch) -> None:
    records = {
        leaf: _record(10, 2, leaf=leaf)
        for leaf in (
            "20260824T100000Z-seed-10",
            "20260824T110000Z-seed-10",
        )
    }
    monkeypatch.setattr(
        "sls.validation.expansion.load_records", lambda _root: (records, []),
    )

    result = assemble_expansion_round(Path("truth"), _selection(10, 2), round_number=1)

    assert result["rounds"][0]["evidence"][0]["leaf"] == "20260824T110000Z-seed-10"
