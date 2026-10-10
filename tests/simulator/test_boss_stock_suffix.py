"""Independent stock targets for six boss orders and four Act4 RNG boundaries.

Controlled initial combat, not natural builds. Raw corpse power differences and
victory UI alignment are outside this explicitly enumerated comparison.
"""
import json
from pathlib import Path

import pytest

from sls.audit.act2_differential import (
    comparison_projection,
    production_combat_projection,
)
from sls.backends.simulator import SimulatorBackend, native
from sls.curriculum import IRONCLAD_A20_HEART
from tools.reproduce_double_boss_entry import recorded_action_bits
from tools.verify_boss_flow_suffix import replay_suffixes

FIXTURE = json.loads(Path('tests/fixtures/regressions/boss-stock-suffix-r1.json').read_text())


@pytest.mark.parametrize('case', FIXTURE['cases'], ids=lambda c:str(c['seed']))
def test_stock_boss_orders_and_act4_entry_restore_entire_remaining_trajectory(case):
    assert FIXTURE['training_eligible'] is False
    run = native.LightspeedRunState()
    run.load_state(case['initial'])
    states = [run.snapshot()]
    expected = {r['boundary']:r['stock'] for r in case['expected_aligned_combat']}
    for index, action in enumerate(case['actions'], 1):
        if action['kind'] != 'proceed_to_second_boss':
            bits = recorded_action_bits(action)
            assert bits in {a['bits'] for a in run.legal_actions()}
            run.step(bits)
        state = run.snapshot()
        states.append(state)
        if index in expected:
            battle = native.LightspeedBattle()
            battle.load_checkpoint(state['combat_checkpoint'])
            current = comparison_projection(production_combat_projection(battle),
                                            stock=False, extended_direct=True)
            for field in ('adapted', 'actual_actions', 'rng'):
                assert current[field] == expected[index][field], (index, field)
    suffixes = replay_suffixes(native, case['initial'], case['actions'], states)
    assert all(row['checkpoint_equal'] and row['full_remaining_trajectory_equal'] and row['final_equal']
               for row in suffixes)
    backend = SimulatorBackend(profile=IRONCLAD_A20_HEART)
    transition = backend._transition_from_raw(backend._adapt(states[-2]).observation, states[-1])
    assert transition.terminated is case['expected_heart_terminated']
    assert transition.info['reason'] == case['expected_heart_reason']
    assert transition.info['success'] is False
    assert transition.truncated is False
    if case['stock_act4_entry']:
        assert states[-1]['run_state']['act'] == 4


def test_suffix_persistence_checks_every_field_after_json_array_decode():
    case = FIXTURE['cases'][0]
    run = native.LightspeedRunState()
    run.load_state(case['initial'])
    states = [run.snapshot()]
    for action in case['actions']:
        if action['kind'] != 'proceed_to_second_boss':
            run.step(recorded_action_bits(action))
        states.append(run.snapshot())
    decoded = json.loads(json.dumps(states))
    assert all(r['full_remaining_trajectory_equal'] for r in replay_suffixes(
        native, case['initial'], case['actions'], decoded))
    decoded[1]['player_state']['gold'] += 1
    with pytest.raises(ValueError, match='fresh trajectory differs'):
        replay_suffixes(native, case['initial'], case['actions'], decoded)
