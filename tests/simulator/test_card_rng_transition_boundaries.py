"""Shared transition guard regression; synthetic boundary initial states, not natural runs.

Expected intervals come from stock AbstractDungeon.dungeonTransitionSetup bytecode.
The underlying boss reward checkpoint is stock-derived, but counter overrides are
explicit native probes, not claimed as independently captured original trajectories.
"""
import copy
import json
from pathlib import Path

import pytest

from sls.backends.simulator import native


@pytest.mark.parametrize('act', [1, 2])
@pytest.mark.parametrize('counter,expected', [(0, 0), (1, 250), (249, 250),
                                             (250, 250), (251, 500), (499, 500),
                                             (500, 500), (501, 750), (749, 750),
                                             (750, 750), (751, 751)])
def test_shared_transition_stock_strict_counter_intervals(act, counter, expected):
    initial = json.loads(Path('tests/fixtures/regressions/act2-calling-bell-boss-131100064.json')
                        .read_text(encoding='utf-8'))['before']
    initial['run_state']['act'] = act
    initial['derived_rng']['map']['act'] = act
    initial['derived_rng']['map']['derived_seed'] = initial['run_state']['seed'] + (1 if act == 1 else 200)
    initial['run_state']['floor'] = 16 if act == 1 else 33
    initial['rng']['card']['counter'] = counter
    run = native.LightspeedRunState()
    run.load_state(initial)
    before = run.snapshot()
    restored = native.LightspeedRunState()
    restored.load_state(copy.deepcopy(before))
    # Existing first reward is Black Star; no random card/reward effects.
    assert any(a['bits'] == 0 for a in run.legal_actions())
    run.step(0)
    restored.step(0)
    after = run.snapshot()
    assert after == restored.snapshot()
    assert after['run_state']['act'] == act + 1
    assert after['rng']['card']['counter'] == expected
    if counter == expected:
        assert after['rng']['card'] == before['rng']['card']
    terminal = native.LightspeedRunState()
    terminal.load_state(after)
    assert terminal.snapshot() == after
    assert terminal.legal_actions() == run.legal_actions()
