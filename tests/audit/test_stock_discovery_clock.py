import pytest

from tools.replay_act2_clock_conditioned_trajectory import stock_clock_rows


def test_clock_witness_keeps_observed_context_and_selects_exact_seed():
    payload = ('SLS_DISCOVERY_CLOCK_V1 seed=10 floor=16 serial=1 updates=15\n'
               'SLS_DISCOVERY_CLOCK_V1 seed=20 floor=23 serial=2 updates=14\n')
    assert stock_clock_rows(payload, 10) == [
        {'seed': 10, 'floor': 16, 'serial': 1, 'updates': 15}
    ]
    assert stock_clock_rows(payload, 30) == []


@pytest.mark.parametrize('updates', [0, 121, 180, 181])
def test_clock_witness_rejects_invalid_observed_count(updates):
    with pytest.raises(ValueError, match='clock count'):
        stock_clock_rows(f'SLS_DISCOVERY_CLOCK_V1 seed=10 floor=16 serial=1 updates={updates}', 10)


def test_clock_witness_rejects_duplicate_completion():
    with pytest.raises(ValueError, match='duplicate'):
        stock_clock_rows('SLS_DISCOVERY_CLOCK_V1 seed=10 floor=16 serial=1 updates=15\n' * 2, 10)
