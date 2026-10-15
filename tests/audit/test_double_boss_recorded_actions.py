"""Recorded actions must preserve target slots and reject unsupported forms."""

import pytest

from tools.reproduce_double_boss_entry import recorded_action_bits, select_scene_runs


def test_recorded_actions_preserve_indices():
    assert recorded_action_bits({'kind': 'end_turn'}) == 2147483648
    assert recorded_action_bits({'kind': 'play', 'card_index': 1, 'target_index': 0}) == 0
    assert recorded_action_bits({'kind': 'play', 'card_index': 3, 'target_index': 2}) == 131074


@pytest.mark.parametrize('action', [
    {'kind': 'proceed_to_second_boss'}, {'kind': 'potion'},
    {'kind': 'play', 'card_index': 0, 'target_index': 0},
    {'kind': 'play', 'card_index': True, 'target_index': 0},
    {'kind': 'play', 'card_index': 1, 'target_index': -1},
    {'kind': 'play', 'card_index': 1},
])
def test_recorded_actions_reject_ambiguous_inputs(action):
    with pytest.raises(ValueError):
        recorded_action_bits(action)


def test_selection_retains_order_and_original_records():
    rows = [{'scene': {'id': name}, 'seed': seed}
            for seed, name in enumerate(('bad-initial', 'valid', 'valid'))]
    assert select_scene_runs(rows, None) is rows
    selected = select_scene_runs(rows, ['valid'])
    assert [row['seed'] for row in selected] == [1, 2]
    assert selected[0] is rows[1]
    assert len(rows) == 3


@pytest.mark.parametrize('selected', [[], ['missing'], ['valid', 'valid']])
def test_selection_rejects_unaccounted_records(selected):
    with pytest.raises(ValueError, match='scene selection'):
        select_scene_runs([{'scene': {'id': 'valid'}}], selected)
