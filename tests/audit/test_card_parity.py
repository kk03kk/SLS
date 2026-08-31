from __future__ import annotations

from sls.audit.card_parity import structured_differences


def test_structured_differences_retains_exact_paths_and_values() -> None:
    assert structured_differences(
        {"hand": [{"card_id": "ANGER"}], "energy": 3},
        {"hand": [{"card_id": "BASH"}], "energy": 3},
    ) == [{
        "path": "$.hand[0].card_id",
        "original": "ANGER",
        "simulator": "BASH",
    }]
