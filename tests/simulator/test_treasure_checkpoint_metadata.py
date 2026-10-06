import json
from pathlib import Path

from sls.backends.simulator import SimulatorBackend
from sls.contracts import Action
from sls.curriculum import IRONCLAD_A20_ACT2


def test_old_looter_gold_does_not_change_treasure_reward_checkpoint():
    fixture = json.loads(Path('tests/fixtures/regressions/treasure-stolen-gold-131100067.json').read_text())
    uninterrupted = SimulatorBackend(IRONCLAD_A20_ACT2)
    uninterrupted.reset(fixture['before']['run_state']['seed'])
    for bits in fixture['before']['replay_actions']:
        uninterrupted._native.step(bits)
    after = uninterrupted._native.step(0)  # OPEN_CHEST in the witnessed native mask
    restored = SimulatorBackend(IRONCLAD_A20_ACT2)
    restored.load_checkpoint(fixture['before'])
    restored.step(Action.from_dict(fixture['action']))
    assert after['screen_info']['stolen_gold'] == fixture['expected_stolen_gold']
    assert after['screen_info'] == restored.checkpoint()['screen_info']
