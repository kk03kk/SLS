import json
from pathlib import Path

from sls.backends.simulator import SimulatorBackend
from sls.contracts import Action, ActionKind
from sls.curriculum import IRONCLAD_A20_ACT2


def test_colosseum_reopen_consumes_stock_discarded_potion_and_deck_prep():
    fixture = json.loads(Path('tests/fixtures/regressions/act2-colosseum-lifecycle-131100066.json').read_text())
    backend = SimulatorBackend(IRONCLAD_A20_ACT2)
    backend.load_checkpoint(fixture['before'])
    backend.step(Action.from_dict(fixture['action']))
    streams = backend.validation_snapshot().rng_streams
    for key, expected in fixture['expected_rng'].items():
        assert streams[key] == expected


def test_colosseum_second_battle_keeps_same_room_random_streams():
    fixture = json.loads(Path('tests/fixtures/regressions/act2-colosseum-lifecycle-131100066.json').read_text())
    backend = SimulatorBackend(IRONCLAD_A20_ACT2)
    backend.load_checkpoint(fixture['before'])
    backend.step(Action.from_dict(fixture['action']))
    decision = backend.step(Action(ActionKind.CHOOSE_EVENT_OPTION, option_id='event-option:1')).decision
    actual = decision.observation.to_dict()
    for key, expected in fixture['stock_next_combat'].items():
        assert actual[key] == expected
