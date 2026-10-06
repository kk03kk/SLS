from sls.backends.simulator import native


def test_opening_automaton_checkpoint_preserves_both_orb_slots():
    uninterrupted = native.LightspeedBattle()
    uninterrupted.reset(131100057, "AUTOMATON", 20)
    snapshot = uninterrupted.snapshot()
    restored = native.LightspeedBattle()
    restored.load_checkpoint({"game_state": snapshot["game_state"], "rng": snapshot["_rng"]})
    uninterrupted.step("end_turn")
    restored.step("end_turn")
    assert restored.snapshot() == uninterrupted.snapshot()
    assert len(restored.snapshot()["game_state"]["combat_state"]["monsters"]) == 3
