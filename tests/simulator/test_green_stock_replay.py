"""Frozen original-game targets for conditioned real green-key trajectories."""
import hashlib
import json
from dataclasses import asdict
from pathlib import Path

import pytest

from sls.backends.simulator import SimulatorBackend, native
from sls.curriculum import IRONCLAD_A20_HEART
from tools.replay_green_key_archive import active_resources, stock_bottom_order

FIXTURE = json.loads((Path(__file__).parents[1] / 'fixtures/regressions/green-key-stock-r2.json').read_text())
EXPANDED = json.loads((Path(__file__).parents[1] / 'fixtures/regressions/green-key-stock-r3.json').read_text())


def digest(value):
    return hashlib.sha256(json.dumps(value, sort_keys=True, separators=(',', ':')).encode()).hexdigest()


@pytest.mark.parametrize('case', FIXTURE['cases'] + EXPANDED['cases'], ids=lambda c: str(c['seed']))
def test_real_stock_green_trace_public_state_rng_and_full_suffix_restore(case):
    assert FIXTURE['training_eligible'] is False
    run = native.LightspeedRunState()
    run.load_state(case['initial'])
    backend = SimulatorBackend(profile=IRONCLAD_A20_HEART)
    states = []
    for index, expected in enumerate(case['expected']):
        state = run.snapshot()
        states.append(state)
        decision = backend._adapt(state)
        assert digest(asdict(decision.observation)) == expected['observation_sha256'], index
        assert digest([asdict(a) for a in decision.actions]) == expected['actions_sha256'], index
        assert active_resources(state) == expected['resources'], index
        assert {**state['rng'], **state.get('combat_checkpoint', {}).get('rng', {})} == expected['rng'], index
        assert decision.terminal == expected['terminal'], index
        if index < len(case['bits']):
            assert case['bits'][index] in {a['bits'] for a in run.legal_actions()}, index
            run.step(case['bits'][index])
    for start, state in enumerate(states):
        restored = native.LightspeedRunState()
        restored.load_state(state)
        assert restored.snapshot() == state
        for bits in case['bits'][start:]:
            restored.step(bits)
        assert restored.snapshot() == states[-1], start


def test_stock_bottom_insertion_preserves_duplicates_and_does_not_mutate_resource():
    original = ['A', 'B', 'B', 'C']
    assert stock_bottom_order(original) == ['C', 'B', 'B', 'A']
    assert original == ['A', 'B', 'B', 'C']
