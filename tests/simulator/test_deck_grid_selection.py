import json
from pathlib import Path

import pytest

from sls.backends.simulator import SimulatorBackend
from sls.contracts import Action
from sls.curriculum import IRONCLAD_A20_ACT2


def test_partial_empty_cage_keeps_stock_candidates_and_selected_observation():
    fixture = json.loads(Path('tests/fixtures/regressions/act2-empty-cage-grid-131100069.json').read_text())
    backend = SimulatorBackend(IRONCLAD_A20_ACT2)
    initial = backend.load_checkpoint(fixture['before'])
    decision = backend.step(Action.from_dict(fixture['action'])).decision
    assert len(decision.observation.reward_options) == fixture['stock_candidate_count']
    assert decision.observation.to_dict()['selected_cards'] == fixture['stock_partial_selection']
    assert decision.observation.deck == initial.observation.deck
    restored = SimulatorBackend(IRONCLAD_A20_ACT2)
    restored.load_checkpoint(backend.checkpoint())
    # Clicking the selected card again deselects it, without state/RNG effects.
    undone = backend.step(Action.from_dict(fixture['action'])).decision
    restored.step(Action.from_dict(fixture['action']))
    assert undone.observation.to_dict() == initial.observation.to_dict()
    assert restored.checkpoint() == backend.checkpoint()


def test_partial_grid_without_new_contract_is_not_silently_migrated():
    fixture = json.loads(Path('tests/fixtures/regressions/act2-empty-cage-grid-131100069.json').read_text())
    backend = SimulatorBackend(IRONCLAD_A20_ACT2)
    backend.load_checkpoint(fixture['before'])
    backend.step(Action.from_dict(fixture['action']))
    legacy = backend.checkpoint()
    legacy['screen_info'].pop('grid_selection_contract')
    with pytest.raises(ValueError, match='legacy partial GRID|Deterministic replay'):
        SimulatorBackend(IRONCLAD_A20_ACT2).load_checkpoint(legacy)
