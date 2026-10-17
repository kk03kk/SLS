import pytest

from sls.audit.development_seeds import seed_intervals


@pytest.mark.parametrize('schema',[
    'sls-frozen-parent-cpu-probe-v1','sls-frozen-parent-complete-return-probe-v1'])
def test_exclusive_progress_counter_bounds_a_completed_probe_without_guessing(schema):
    ranges=list(seed_intervals({'schema':schema,
        'seed_start':8000102001000,'next_seed':8000102001002}))
    assert ranges==[(8000102001000,8000102001002)]
    assert not any(a<=8000103000000<b for a,b in ranges)


def test_declared_reservation_is_not_shrunk_to_completed_progress():
    ranges=list(seed_intervals({'schema':'sls-frozen-parent-cpu-probe-v1',
        'seed_start':100,'seed_count':1000,'next_seed':102}))
    assert (100,1100) in ranges


def test_unknown_progress_still_reserves_a_million():
    assert (100,1000100) in list(seed_intervals({'seed_start':100}))
    assert (100,1000100) in list(seed_intervals({'seed_start':100,'next_seed':99}))


def test_unrelated_training_progress_cannot_shrink_an_evaluation_reservation():
    assert (100,1000100) in list(seed_intervals({'evaluation_seed_start':100,'next_seed':102}))


def test_unfinished_or_unknown_run_progress_does_not_free_future_seeds():
    assert (100,1000100) in list(seed_intervals({'seed_start':100,'next_seed':102}))
