from tools.audit_repeated_boundaries import grid_cycle


def test_legal_grid_toggle_is_visible_and_guard_detects_recurrence():
    result = grid_cycle()
    assert all(r['rng_unchanged'] and r['deck_unchanged'] for r in result['clicks'])
    assert result['clicks'][-1]['reason'] == 'cycle_limit'
    assert all(r['reason'] is None for r in result['clicks'][:-1])
