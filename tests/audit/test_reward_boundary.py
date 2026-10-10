import pytest

from sls.audit.reward_boundary import collect_reward_boundary, reward_boundary_ready


def state(phase='COMPLETE', screen='COMBAT_REWARD'):
    return {'_oracle_mode': 'validation', 'game_state': {'screen_type': screen},
            '_stock_reward_state': {'schema': 'sls-stock-reward-state-v1',
                                    'room_phase': phase, 'room_rewards': [], 'screen_rewards': []}}


def test_requires_actual_post_combat_boundary():
    assert reward_boundary_ready(state())
    assert not reward_boundary_ready(state('COMBAT'))
    assert not reward_boundary_ready(state(screen='NONE'))


def test_rejects_production_or_missing_evidence():
    for payload in ({}, dict(state(), _oracle_mode='production')):
        with pytest.raises(ValueError, match='validation'):
            reward_boundary_ready(payload)


def test_collection_waits_for_complete_reward_boundary():
    rows = iter([state('COMBAT'), state()])
    result = collect_reward_boundary(lambda: next(rows), clock=lambda: 0, sleep=lambda _: None)
    assert reward_boundary_ready(result)


def test_never_treats_timeout_as_pass():
    clock_rows = iter([0, 0, 2])
    with pytest.raises(TimeoutError, match='execution failure'):
        collect_reward_boundary(lambda: state('COMBAT'), timeout=1,
                                clock=lambda: next(clock_rows), sleep=lambda _: None)


def test_poll_count_is_bounded_even_with_stalled_clock():
    with pytest.raises(TimeoutError):
        collect_reward_boundary(lambda: state('COMBAT'), clock=lambda: 0, sleep=lambda _: None)
