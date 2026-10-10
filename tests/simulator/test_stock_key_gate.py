"""Eight real stock key-gate outcomes after explicitly controlled boss starts."""
import json
from dataclasses import asdict
from pathlib import Path

import pytest

from sls.backends.simulator import SimulatorBackend, native
from sls.curriculum import IRONCLAD_A20_HEART
from tools.replay_rest_key_archive import native_resources
from tools.reproduce_double_boss_entry import recorded_action_bits
from tools.verify_boss_flow_suffix import replay_suffixes

FIXTURE = json.loads(Path('tests/fixtures/regressions/key-gate-stock-r1.json').read_text())


@pytest.mark.parametrize('case', FIXTURE['cases'], ids=lambda c:str(c['seed']))
def test_stock_gate_resources_rng_horizon_and_all_suffix_checkpoints(case):
    assert FIXTURE['training_eligible'] is False
    run = native.LightspeedRunState()
    run.load_state(case['initial'])
    states = [run.snapshot()]
    for action in case['actions']:
        if action['kind'] != 'proceed_to_second_boss':
            bits = recorded_action_bits(action)
            assert bits in {a['bits'] for a in run.legal_actions()}
            run.step(bits)
        states.append(run.snapshot())
    final = states[-1]
    assert final['run_state']['act'] == case['expected_act']
    assert final['run_state']['floor'] == case['expected_floor']
    assert final['rng'] == case['expected_rng']
    resources = native_resources(final, canonical_counters=False)
    assert {k:v for k,v in resources.items() if k not in {'deck', 'relics'}} == case['expected_resources']
    backend = SimulatorBackend(profile=IRONCLAD_A20_HEART)
    observation = backend._adapt(final).observation
    assert json.loads(json.dumps([asdict(c) for c in observation.deck])) == case['expected_public_deck']
    assert json.loads(json.dumps([asdict(r) for r in observation.relics])) == case['expected_public_relics']
    transition = backend._transition_from_raw(backend._adapt(states[-2]).observation, final)
    assert transition.info['success'] is False
    assert transition.reward == 0
    assert transition.truncated is False
    if len(case['keys']) == 3:
        assert case['stock_gate_status'] == 'ACT4_MAP_ENTRY'
        assert not transition.terminated and transition.info['reason'] is None
    else:
        assert case['stock_gate_status'] == 'ACT3_STOCK_ENDING'
        assert transition.terminated and transition.info['reason'] == 'HEART_NOT_REACHED'
    assert all(r['checkpoint_equal'] and r['full_remaining_trajectory_equal'] and r['final_equal']
               for r in replay_suffixes(native, case['initial'], case['actions'], states))
