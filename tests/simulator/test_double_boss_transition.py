"""Stock-derived Time Eater lethal must produce a playable, restorable second fight."""

import json
from pathlib import Path

import pytest

from sls.backends.simulator import native

CASES = json.loads((Path(__file__).parents[1] /
                   'fixtures/regressions/double-boss-transition-before.json').read_text())['cases']


@pytest.mark.parametrize('case', CASES, ids=lambda c: str(c['seed']))
def test_first_boss_lethal_initializes_second_combat(case):
    run = native.LightspeedRunState()
    run.load_state(case['initial'])
    run.step(0)
    state = run.snapshot()
    assert state['run_state']['floor'] == 51
    assert 'combat_checkpoint' in state
    assert run.legal_actions()
    restored = native.LightspeedRunState()
    restored.load_state(state)
    assert restored.snapshot() == state
    assert restored.legal_actions() == run.legal_actions()
    action = run.legal_actions()[0]['bits']
    run.step(action)
    restored.step(action)
    assert restored.snapshot() == run.snapshot()
