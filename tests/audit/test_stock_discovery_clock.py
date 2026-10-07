import pytest

from tools.replay_act2_clock_conditioned_trajectory import stock_clock_rows


def test_historical_clock_build_is_sealed_and_detects_tampering(tmp_path):
    import hashlib
    import zipfile

    from tools.replay_act2_clock_conditioned_trajectory import verify_sealed_oracle

    oracle = tmp_path / 'old.jar'
    with zipfile.ZipFile(oracle, 'w') as archive:
        archive.writestr('Old.class', b'old-source-build')
    build = {'schema': 'sls-oracle-build-v1', 'used_existing_oracle': False,
             'sources': {'src/Old.java': 'a' * 64},
             'output_sha256': hashlib.sha256(oracle.read_bytes()).hexdigest(),
             'members': {'Old.class': hashlib.sha256(b'old-source-build').hexdigest()}}
    verify_sealed_oracle(oracle, build)
    build['members']['Old.class'] = 'b' * 64
    with pytest.raises(ValueError, match='member identity'):
        verify_sealed_oracle(oracle, build)
    oracle.write_bytes(b'tampered')
    with pytest.raises(ValueError, match='build identity'):
        verify_sealed_oracle(oracle, build)


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
