"""Stock static key rule and exact continuation at conditioned Act boundaries.

These are synthetic key/Act interventions, not captured stock trajectories.
Stock setEmeraldElite returns when Settings.hasEmeraldKey is true.
"""
from sls.backends.simulator import SimulatorBackend, native
from sls.curriculum import IRONCLAD_A20_HEART
from tools.probe_held_green_transition import run_cases


def test_held_key_suppresses_next_act_burning_and_checkpoint_continues():
    rows = run_cases(native)
    assert len(rows) == 4
    backend = SimulatorBackend(profile=IRONCLAD_A20_HEART)
    for row in rows:
        state = row['after']
        assert state['run_state']['act'] == row['act_before'] + 1
        assert state['player_state']['green_key'] is row['green_key']
        coordinates = [state['run_state']['burning_elite_' + key] for key in ('x', 'y', 'buff')]
        if row['green_key']:
            assert coordinates == [-1, -1, -1]
        else:
            assert 0 <= coordinates[0] < 7
            assert 0 <= coordinates[1] < 15
            assert 0 <= coordinates[2] < 4
        visible = backend._adapt(state).observation.map_nodes
        burning = [node for node in visible if node.visible_room_type == 'BURNING_ELITE']
        assert len(burning) == (0 if row['green_key'] else 1)
        assert row['checkpoint_equal']
        assert row['next_action_restored_equal']
