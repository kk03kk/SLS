"""Slay the Spire user-facing seed conversions."""

from __future__ import annotations

STS_SEED_ALPHABET = "0123456789ABCDEFGHIJKLMNPQRSTUVWXYZ"


def seed_string_to_long(seed: str | int) -> int:
    """Mirror ``SeedHelper.getLong`` and return the unsigned 64-bit bits."""

    total = 0
    normalized = str(seed).strip().upper().replace("O", "0")
    if not normalized:
        raise ValueError("A deterministic STS seed cannot be empty")
    for character in normalized:
        try:
            remainder = STS_SEED_ALPHABET.index(character)
        except ValueError as exc:
            raise ValueError(f"Invalid Slay the Spire seed character: {character}") from exc
        total = (total * len(STS_SEED_ALPHABET) + remainder) % (2**64)
    return total


def long_to_seed_string(seed: int) -> str:
    """Mirror ``SeedHelper.getString`` for unsigned 64-bit seed bits."""

    value = int(seed) % (2**64)
    if value == 0:
        # Java returns an empty string for zero, which means "random seed" at
        # the command boundary. An explicit zero digit parses to the same bits.
        return "0"
    characters: list[str] = []
    base = len(STS_SEED_ALPHABET)
    while value:
        value, remainder = divmod(value, base)
        characters.append(STS_SEED_ALPHABET[remainder])
    return "".join(reversed(characters))


__all__ = ["STS_SEED_ALPHABET", "long_to_seed_string", "seed_string_to_long"]
