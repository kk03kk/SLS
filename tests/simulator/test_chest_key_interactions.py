"""Stock source rules for chest links; controlled route skips combat only.

AbstractChest.open links sapphire to the last (primary) relic before
NlothsMask.onChestOpenAfter removes the first relic and its adjacent key.
These regressions are source-derived, not independent stock runtime witnesses.
"""

from pathlib import Path

import pytest

from sls.backends.simulator import native
from tools.reproduce_double_boss_entry import enum_ids

RELICS = enum_ids(Path(__file__).parents[2] /
                  "native/simulator/include/constants/Relics.h",
                  "enum class RelicId : std::uint8_t {")
ROUTE = [0, 0, 4, 805306368, 4, 0, 5, 0, 5, 0, 0,
         5, 0, 0, 0, 4, 2, 4, 805306368, 5, 805306368, 4]


def _open_chest(relics, blue_key):
    run = native.LightspeedRunState()
    run.reset(0, 20)
    run._set_skip_battles_for_testing(True)
    for bits in ROUTE:
        assert any(a['bits'] == bits for a in run.legal_actions())
        run.step(bits)
    state = run.snapshot()
    assert state['screen_info']['screen_state'] == 6  # unopened treasure
    state['player_state']['blue_key'] = blue_key
    state['player_state']['relics'].extend(
        {'id': RELICS[identifier], 'data': 1} for identifier in relics)
    run.load_state(state)
    run.step(0)
    return run


@pytest.mark.parametrize('blue_key', [False, True])
def test_hungry_face_removes_primary_relic_and_its_linked_key(blue_key):
    run = _open_chest(['NLOTHS_HUNGRY_FACE'], blue_key)
    state = run.snapshot()
    rewards = state['screen_info']['rewards']
    assert rewards['relics'] == []
    assert rewards['sapphire_key'] is False
    assert not any(a['reward_type'] == 2 for a in run.legal_actions())


@pytest.mark.parametrize('blue_key', [False, True])
def test_matryoshka_absorbs_hungry_face_without_losing_primary_or_key(blue_key):
    baseline = _open_chest(['MATRYOSHKA'], blue_key).snapshot()
    run = _open_chest(['MATRYOSHKA', 'NLOTHS_HUNGRY_FACE'], blue_key)
    state = run.snapshot()
    rewards = state['screen_info']['rewards']
    expected = baseline['screen_info']['rewards']['relics']
    assert len(expected) == 2
    assert rewards['relics'] == expected[1:]
    assert rewards['sapphire_key'] is (not blue_key)
    assert state['rng'] == baseline['rng']
    assert next(r['counter'] for r in state['public_inventory']['relics']
                if r['content_id'] == 'MATRYOSHKA') == -2
    restored = native.LightspeedRunState()
    restored.load_state(state)
    assert restored.snapshot() == state
    assert restored.legal_actions() == run.legal_actions()
