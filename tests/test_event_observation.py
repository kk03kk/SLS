"""Displayed choices must reach the policy and still execute the right effect."""

from copy import deepcopy
from dataclasses import replace

import pytest
import torch

from sls.backends.original.adapter import adapt_original
from sls.backends.simulator import SimulatorBackend
from sls.curriculum import IRONCLAD_A0_ACT1
from sls.model import PolicyBatch
from tests.original.test_adapter import base_game
from tests.simulator.test_event_outcomes import event_probe


@pytest.mark.parametrize('event', ['FALLING', 'WE_MEET_AGAIN', 'NLOTH',
    'WORLD_OF_GOOP', 'DEAD_ADVENTURER', 'SCRAP_OOZE', 'KNOWING_SKULL', 'NOTE_FOR_YOURSELF'])
def test_event_details_are_encodable_and_only_displayed_fields(event):
    backend, decision = event_probe(8, event)
    details = backend.raw_state['public_screen']['event_option_details']
    assert details
    assert all(set(row) <= {'subject_id', 'card', 'properties'} for row in details.values())
    PolicyBatch.from_decisions((decision,))


def test_falling_action_identifies_exact_removed_card():
    backend, decision = event_probe(8, 'FALLING')
    action = next(a for a in decision.actions if a.option_id == 'event-option:2')
    before = list(decision.observation.deck)
    index = int(action.subject_id.split(':')[1])
    assert before[index].card_id in {'STRIKE_RED', 'BASH'}
    after = backend.step(action).decision.observation.deck
    expected = before[:index] + before[index + 1:]
    assert [(c.card_id, c.upgrades) for c in after] == [(c.card_id, c.upgrades) for c in expected]


def test_target_changes_action_reference_even_for_identical_copies():
    backend, decision = event_probe(8, 'FALLING')
    raw = deepcopy(backend.raw_state)
    indices = [i for i, c in enumerate(decision.observation.deck) if c.card_id == 'STRIKE_RED']
    raw['public_screen']['event_option_details']['2']['subject_id'] = f'DECK:{indices[0]}'
    first = backend._adapt(raw)
    raw['public_screen']['event_option_details']['2']['subject_id'] = f'DECK:{indices[1]}'
    second = backend._adapt(raw)
    x, y = PolicyBatch.from_decisions((first,)), PolicyBatch.from_decisions((second,))
    assert not torch.equal(x.action_references, y.action_references)


def test_public_goop_loss_matches_effect_and_changes_tensor():
    backend, decision = event_probe(8, 'WORLD_OF_GOOP')
    amount = dict(decision.observation.event_options[1].properties)['gold_loss']
    raw = deepcopy(backend.raw_state)
    raw['public_screen']['event_option_details']['1']['properties']['gold_loss'] = amount + 1
    changed = backend._adapt(raw)
    assert not torch.equal(PolicyBatch.from_decisions((decision,)).entity_numeric,
                           PolicyBatch.from_decisions((changed,)).entity_numeric)
    action = next(a for a in decision.actions if a.option_id == 'event-option:1')
    assert backend.step(action).decision.observation.run.gold == decision.observation.run.gold - amount


def test_skull_repeated_gold_cost_increases_and_original_wire_order_is_correct():
    backend, decision = event_probe(8, 'KNOWING_SKULL')
    action = next(a for a in decision.actions if a.option_id == 'event-option:0')
    after = backend.step(action).decision
    assert after.observation.player.current_hp == decision.observation.player.current_hp - 6
    assert after.observation.run.gold == decision.observation.run.gold + 90
    assert dict(after.observation.event_options[0].properties)['hp_loss'] == 7
    payload = {'in_game': True, 'ready_for_command': True, 'available_commands': ['choose'],
        'game_state': base_game(screen_type='EVENT', choice_list=['potion', 'gold', 'card', 'leave'],
            screen_state={'event_id': 'Knowing Skull',
                          'event_option_details': backend.raw_state['public_screen']['event_option_details']})}
    original = adapt_original(payload)
    gold = next(a for a in original.decision.actions if a.option_id == 'event-option:0')
    assert original.commands[gold.candidate_id] == ('choose 1',)
    assert original.decision.observation.event_options[1] == after.observation.event_options[0]


def test_note_preview_and_explicit_start_context_survive_restore():
    profile = replace(IRONCLAD_A0_ACT1, note_for_yourself_card='BASH+1', secret_portal_eligible=False)
    backend = SimulatorBackend(profile)
    initial = backend.reset(42)
    checkpoint = backend.checkpoint()
    assert checkpoint['run_state']['event_start_context'] == {'note_card': 'BASH+1', 'portal_eligible': False}
    restored = SimulatorBackend(profile)
    assert restored.load_checkpoint(checkpoint) == initial
    with pytest.raises(ValueError, match='event start context'):
        SimulatorBackend().load_checkpoint(checkpoint)
    # Default independent episodes expose the stock initial stored card.
    _, note = event_probe(8, 'NOTE_FOR_YOURSELF')
    preview, = note.observation.choice_options
    assert preview.content_id == 'IRON_WAVE'
    assert dict(preview.properties)['upgrades'] == 0
    assert next(a for a in note.actions if a.option_id == 'event-option:0').subject_id == preview.instance_id
    backend._native.reset_event_probe(42, 'NOTE_FOR_YOURSELF', backend.raw_state['rng'],
                                     note_card='BASH+1', portal_eligible=False)
    custom = backend._adapt(backend._native.snapshot())
    preview, = custom.observation.choice_options
    assert preview.content_id == 'BASH'
    assert dict(preview.properties)['upgrades'] == 1
    assert backend.raw_state['progress_state']['speedrun_pace'] is True
    action = next(a for a in custom.actions if a.option_id == 'event-option:0')
    after = backend.step(action).decision
    assert any(c.card_id == 'BASH' and c.upgrades == 1 for c in after.observation.deck)


def test_stale_oracle_and_native_fail_instead_of_silently_dropping_details():
    backend, _ = event_probe(8, 'FALLING')
    raw = deepcopy(backend.raw_state)
    del raw['public_screen']['event_option_details']
    with pytest.raises(ValueError, match='public event details missing'):
        backend._adapt(raw)
    payload = {'in_game': True, 'ready_for_command': True, 'available_commands': ['choose'],
        'game_state': base_game(screen_type='EVENT', choice_list=['take', 'leave'],
                               screen_state={'event_id': 'World of Goop'})}
    with pytest.raises(ValueError, match='observation oracle'):
        adapt_original(payload)


def test_note_preview_has_same_public_field_presence_with_aligned_account_context():
    # Stock account in the live closeout stored Doubt, not the training default
    # Iron Wave. Match that context before comparing the two observations.
    card = {'content_id': 'Doubt', 'upgrades': 0, 'base_cost': -2,
            'cost_for_turn': -2, 'current_cost': -2, 'base_damage': -1,
            'free_to_play_once': False, 'retain': False, 'self_retain': False,
            'bottled_flame': False, 'bottled_lightning': False, 'bottled_tornado': False}
    payload = {'in_game': True, 'ready_for_command': True, 'available_commands': ['choose'],
        'game_state': base_game(screen_type='EVENT', choice_list=['trade', 'leave'],
            screen_state={'event_id': 'NoteForYourself',
                          'event_option_details': {'0': {'card': card}}})}
    original = adapt_original(payload).decision
    backend = SimulatorBackend()
    backend.reset(8)
    backend._native.reset_event_probe(8, 'NOTE_FOR_YOURSELF', backend.raw_state['rng'],
                                     note_card='DOUBT')
    native = backend._adapt(backend._native.snapshot())
    assert original.observation.choice_options == native.observation.choice_options
    assert original.actions == native.actions


def test_nloth_action_identifies_the_relic_actually_traded():
    backend, decision = event_probe(8, 'NLOTH')
    action = next(a for a in decision.actions if a.option_id == 'event-option:0')
    before = list(decision.observation.relics)
    target = before[int(action.subject_id.split(':')[1])].content_id
    after = backend.step(action).decision.observation.relics
    ids = [r.content_id for r in after]
    assert target not in ids
    assert 'NLOTHS_GIFT' in ids


def test_falling_no_eligible_card_uses_canonical_leave_without_remapping_intro():
    payload = {'in_game': True, 'ready_for_command': True, 'available_commands': ['choose'],
        '_continuation': {'event_id': 'Falling', 'event_phase': 'CHOICE'},
        'game_state': base_game(screen_type='EVENT', choice_list=['leave'],
            screen_state={'event_id': 'Falling', 'event_option_details': {}})}
    adapted = adapt_original(payload)
    action, = adapted.decision.actions
    assert action.option_id == 'event-option:3'
    assert adapted.commands[action.candidate_id] == ('choose 0',)
    payload['_continuation']['event_phase'] = 'INTRO'
    assert adapt_original(payload).decision.actions[0].option_id == 'event-option:0'
    # Live generic Oracle may report inherited screenNum=0 instead of CHOICE.
    payload['_continuation']['event_phase'] = '0'
    payload['game_state']['screen_state']['event_choice_phase'] = 'CHOICE'
    assert adapt_original(payload).decision.actions[0].option_id == 'event-option:3'


def test_dead_adventurer_risk_increases_after_safe_search():
    backend, decision = event_probe(8, 'DEAD_ADVENTURER')
    props = dict(decision.observation.event_options[0].properties)
    assert props['displayed_chance'] == 25
    assert sum(props[k] for k in ['hint_sentries', 'hint_nob', 'hint_lagavulin']) == 1
    action = next(a for a in decision.actions if a.option_id == 'event-option:0')
    after = backend.step(action).decision
    assert dict(after.observation.event_options[0].properties)['displayed_chance'] == 50
