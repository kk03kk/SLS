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


def test_wheel_card_removal_with_no_purgeable_cards_returns_to_map():
    from sls.backends.simulator.native import LightspeedRunState

    baseline = LightspeedRunState()
    baseline.reset(0, 20)
    rng = baseline.snapshot()['rng']

    ordinary = LightspeedRunState()
    ordinary.reset_event_probe(0, 'WHEEL_OF_CHANGE', rng, ascension=20)
    ordinary_action = ordinary.snapshot()['legal_actions'][0]
    ordinary_result = ordinary.step(ordinary_action['bits'])
    assert ordinary_result['public_run']['screen_state'] == 4  # card select

    empty = LightspeedRunState()
    empty.reset_event_probe(0, 'WHEEL_OF_CHANGE', rng, ascension=20, empty_deck=True)
    empty_action = empty.snapshot()['legal_actions'][0]
    empty_result = empty.step(empty_action['bits'])
    assert empty_result['public_run']['screen_state'] == 5  # map
    assert empty_result['legal_actions']
