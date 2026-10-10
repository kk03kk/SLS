import pytest

from sls.audit.boss_flow import collect_key_gate_outcome


def state(keys, *, act=3, screen='EVENT'):
    return dict(_oracle_mode='validation',
                _stock_direct=dict(ascension=20, act=act, dungeon_id='TheEnding' if act == 4 else 'TheBeyond'),
                _parity_run=dict(ruby_key='RUBY' in keys, emerald_key='EMERALD' in keys, sapphire_key='SAPPHIRE' in keys),
                game_state=dict(screen_type=screen), available_commands=['choose'] if screen == 'EVENT' else [])


@pytest.mark.parametrize('mask', range(8))
def test_actual_dialogue_collector_preserves_all_key_subsets_and_commands(mask):
    keys = [key for bit,key in enumerate(['RUBY', 'EMERALD', 'SAPPHIRE']) if mask & (1 << bit)]
    values = iter([state(keys), state(keys, act=4, screen='MAP') if mask == 7 else state(keys, screen='GAME_OVER')])
    commands = []
    result = collect_key_gate_outcome(lambda:next(values), commands.append, keys)
    assert result['status'] == ('ACT4_MAP_ENTRY' if mask == 7 else 'ACT3_STOCK_ENDING')
    assert commands == result['commands'] == ['choose 0']
    assert len(result['history']) == 2


def test_collector_rejects_act4_without_three_key_witness():
    with pytest.raises(ValueError, match='without declared three-key'):
        collect_key_gate_outcome(lambda:state([], act=4, screen='MAP'), lambda _:None, [])


def test_collector_does_not_classify_a_timeout_as_game_failure():
    values = iter([0, 31])
    with pytest.raises(TimeoutError, match='unfinished'):
        collect_key_gate_outcome(lambda:state([]), lambda _:None, [], clock=lambda:next(values))


def test_collector_rejects_key_flag_mutation():
    with pytest.raises(ValueError, match='context changed'):
        collect_key_gate_outcome(lambda:state(['RUBY']), lambda _:None, [])
