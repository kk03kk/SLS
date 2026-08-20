from __future__ import annotations

import random

import pytest

from sls.content.seed import long_to_seed_string, seed_string_to_long


def test_seed_conversion_round_trips_unsigned_long_bits() -> None:
    values = [0, 1, 34, 35, 2**63 - 1, 2**63, 2**64 - 1]
    generator = random.Random(0)
    values.extend(generator.getrandbits(64) for _ in range(1_000))
    for value in values:
        assert seed_string_to_long(long_to_seed_string(value)) == value


def test_seed_input_matches_original_ui_normalization() -> None:
    assert seed_string_to_long(" o ") == 0
    assert seed_string_to_long("10") == 35
    with pytest.raises(ValueError, match="seed character"):
        seed_string_to_long("@")
    with pytest.raises(ValueError, match="empty"):
        seed_string_to_long("  ")
