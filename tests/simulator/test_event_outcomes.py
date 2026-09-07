"""Event effects checked against stock Designer/DeadAdventurer bytecode."""

from sls.backends.simulator import SimulatorBackend
from sls.contracts import ScreenType


def event_probe(seed, event):
    backend = SimulatorBackend()
    backend.reset(seed)
    backend._native.reset_event_probe(seed, event, backend.raw_state['rng'])
    decision = backend._adapt(backend._native.snapshot())
    return backend, decision


def test_designer_random_upgrade_costs_gold_like_selected_upgrade():
    backend, decision = event_probe(1, 'DESIGNER_IN_SPIRE')
    action = next(a for a in decision.actions if a.option_id == 'event-option:1')
    before = decision.observation
    after = backend.step(action).decision.observation
    assert after.run.gold == before.run.gold - 40
    assert sum(c.upgrades for c in after.deck) - sum(c.upgrades for c in before.deck) == 2
    assert after.screen is ScreenType.MAP


def test_dead_adventurer_third_safe_search_finishes_without_extra_fight():
    backend, decision = event_probe(8, 'DEAD_ADVENTURER')
    for attempt in range(3):
        assert decision.observation.screen is ScreenType.EVENT
        action = next(a for a in decision.actions if a.option_id == 'event-option:0')
        decision = backend.step(action).decision
        if attempt < 2:
            assert decision.observation.screen is ScreenType.EVENT
    assert decision.observation.screen is ScreenType.MAP
    assert all(a.option_id != 'event-option:0' for a in decision.actions)
