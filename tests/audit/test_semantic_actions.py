import pytest

from sls.audit.semantic_actions import resolve_target


def test_live_target_occurrence_skips_corpses_and_never_changes_the_script():
    action = {'kind': 'play', 'card_index': 1,
              'target': {'monster_id': 'DAGGER', 'alive_ordinal': 1}}
    monsters = [{'id': 'Dagger', 'current_hp': hp, 'is_gone': hp == 0}
                for hp in (0, 20, 0, 21)]
    assert resolve_target(action, monsters)['target_index'] == 3
    assert 'target_index' not in action
    assert resolve_target({'kind': 'end_turn'}, monsters) == {'kind': 'end_turn'}


@pytest.mark.parametrize('selector', [
    {'monster_id': 'DAGGER', 'alive_ordinal': -1},
    {'monster_id': 'DAGGER', 'alive_ordinal': True},
    {'monster_id': 'DAGGER', 'alive_ordinal': 0},
    {'monster_id': 'DAGGER'},
])
def test_missing_or_invalid_target_refuses_action(selector):
    with pytest.raises(ValueError):
        resolve_target({'kind': 'play', 'target': selector}, [])


def test_conflicting_index_and_half_dead_target_are_rejected():
    selector = {'monster_id': 'DARKLING', 'alive_ordinal': 0}
    with pytest.raises(ValueError):
        resolve_target({'target': selector, 'target_index': 0}, [])
    with pytest.raises(ValueError):
        resolve_target({'target': selector}, [{'id': 'Darkling', 'current_hp': 1, 'half_dead': True}])
