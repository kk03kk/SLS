from __future__ import annotations

from sls.content.energy import canonical_max_energy


def test_permanent_energy_relic_is_visible_at_every_boundary() -> None:
    assert canonical_max_energy(["RUNIC_DOME"]) == 4
    assert canonical_max_energy(["RUNIC_DOME"], in_combat=True) == 4


def test_explicit_combat_energy_is_authoritative() -> None:
    assert canonical_max_energy(
        ["RUNIC_DOME"], combat_value=5, in_combat=True,
    ) == 5


def test_slavers_collar_is_only_added_in_eligible_combat() -> None:
    assert canonical_max_energy(["SLAVERS_COLLAR"]) == 3
    assert canonical_max_energy(
        ["SLAVERS_COLLAR"], room_type="MonsterRoomElite", in_combat=True,
    ) == 4
