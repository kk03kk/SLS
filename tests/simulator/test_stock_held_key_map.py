"""Original public map targets after explicit conditioned native transitions.

Stock captured controlled constructors; keys/Act/seed/RNG are synthetic native
inputs. Reachability differs between a stock rest boundary and a new-act map.
"""
import json
from dataclasses import asdict
from pathlib import Path

import pytest

from sls.backends.simulator import SimulatorBackend, native
from sls.curriculum import IRONCLAD_A20_HEART

FIXTURE = json.loads(Path('tests/fixtures/regressions/held-key-stock-map-r1.json').read_text())


@pytest.mark.parametrize('case', FIXTURE['cases'], ids=lambda case:case['id'])
def test_next_act_map_matches_actual_stock_target_and_restores(case):
    assert FIXTURE['training_eligible'] is False
    run = native.LightspeedRunState()
    run.load_state(case['initial'])
    run.step(0)
    state = run.snapshot()
    observation = SimulatorBackend(profile=IRONCLAD_A20_HEART)._adapt(state).observation
    keys = ['node_id', 'x', 'y', 'visible_room_type', 'outgoing_node_ids']
    actual = sorted([{key:asdict(node)[key] for key in keys} for node in observation.map_nodes],
                    key=lambda row:(row['y'], row['x']))
    assert json.dumps(actual, sort_keys=True) == json.dumps(case['expected_public_map'], sort_keys=True)
    restored = native.LightspeedRunState()
    restored.load_state(state)
    assert restored.snapshot() == state
    for _ in range(3):
        assert restored.legal_actions() == run.legal_actions()
        actions = run.legal_actions()
        if not actions:
            break
        bits = actions[0]['bits']
        run.step(bits)
        restored.step(bits)
        assert restored.snapshot() == run.snapshot()
