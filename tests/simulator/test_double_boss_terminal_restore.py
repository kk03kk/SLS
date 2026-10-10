"""A reachable two-boss victory checkpoint must remain terminal after restore."""

import json
from pathlib import Path

import pytest

from sls.backends.simulator import native

CASES = json.loads((Path(__file__).parents[1] /
                   'fixtures/regressions/double-boss-complete-initial.json').read_text())['cases']


@pytest.mark.parametrize('case', CASES, ids=lambda c: str(c['seed']))
def test_complete_double_boss_terminal_restore(case):
    run = native.LightspeedRunState()
    run.load_state(case['initial'])
    for bits in (0, 0, 2147483648, 65536):
        assert any(a['bits'] == bits for a in run.legal_actions())
        run.step(bits)
    state = run.snapshot()
    assert state['progress_state']['outcome'] == 2
    assert not run.legal_actions()
    restored = native.LightspeedRunState()
    restored.load_state(state)
    assert restored.snapshot() == state
    assert not restored.legal_actions()
