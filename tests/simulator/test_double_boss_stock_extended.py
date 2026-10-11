"""Stock-derived full decision projections at two stable boss boundaries."""

import json
from pathlib import Path

import pytest

from sls.audit.act2_differential import (
    comparison_projection,
    production_combat_projection,
)
from sls.backends.simulator import native

CASES = json.loads((Path(__file__).parents[1] /
                   'fixtures/regressions/double-boss-entry-stock-extended.json').read_text())['cases']


def projection(state):
    battle = native.LightspeedBattle()
    battle.load_checkpoint(state['combat_checkpoint'])
    return comparison_projection(production_combat_projection(battle),
                                 stock=False, extended_direct=True)


@pytest.mark.parametrize('case', CASES, ids=lambda c: str(c['seed']))
def test_stock_initial_and_second_decision_projections(case):
    run = native.LightspeedRunState()
    run.load_state(case['initial'])
    assert projection(run.snapshot()) == case['expected_initial']
    run.step(0)
    state = run.snapshot()
    assert projection(state) == case['expected_second']
    restored = native.LightspeedRunState()
    restored.load_state(state)
    assert restored.snapshot() == state
    assert restored.legal_actions() == run.legal_actions()
