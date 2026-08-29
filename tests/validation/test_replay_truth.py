from tools.replay_truth import _has_seed_local_origin


def test_action_history_fallback_requires_the_real_seed_origin() -> None:
    assert _has_seed_local_origin([
        {"sequence": 0, "cursor": {"floor": 0, "screen": "NEOW"}},
    ])
    assert not _has_seed_local_origin([
        {"sequence": 0, "cursor": {"floor": 6, "screen": "COMBAT"}},
    ])
    assert not _has_seed_local_origin([
        {"sequence": 4, "cursor": {"floor": 0, "screen": "NEOW"}},
    ])
