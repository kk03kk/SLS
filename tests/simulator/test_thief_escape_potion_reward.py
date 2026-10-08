"""Original-derived probability/RNG regression, bounded CPU-only native steps."""

import json
from pathlib import Path

import pytest

from sls.backends.simulator import native

FIXTURE = Path('tests/fixtures/regressions/act12-all-thieves-escaped-131200180.json')


@pytest.mark.parametrize('index', [0, 1, 2])
def test_all_thieves_escape_no_potion_even_with_positive_pity(index):
    row = json.loads(FIXTURE.read_text())['runs'][index]
    run = native.LightspeedRunState()
    run.load_state(row['initial'])
    for _ in range(8):
        state = run.snapshot()
        actions = run.legal_actions()
        restored = native.LightspeedRunState()
        restored.load_state(state)
        assert restored.snapshot() == state
        assert restored.legal_actions() == actions
        ends = [a for a in actions if a['bits'] == 2147483648]
        if not ends:
            break
        run.step(ends[0]['bits'])
        restored.step(ends[0]['bits'])
        assert run.snapshot() == restored.snapshot()
    else:
        pytest.fail('thief AI did not terminate within8 decisions')
    final = run.snapshot()
    assert final['progress_state']['potion_chance'] == row['expected_potion_modifier']
    assert final['rng']['potion'] == row['expected_potion_rng']
    assert final['public_screen']['potions'] == row['expected_potions']
    assert final['public_screen']['gold'] == []
    assert len(final['public_screen']['card_rewards']) == 1
