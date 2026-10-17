import json
from pathlib import Path

import pytest

from sls.backends.simulator import native

FIXTURE = json.loads(Path('tests/fixtures/regressions/dormant-selector-prefix.json').read_text())


def prefix():
    run = native.LightspeedRunState()
    run.reset(FIXTURE['seed'], 20, FIXTURE['math_seed'])
    for bits in FIXTURE['actions']:
        run.step(bits)
    return run


@pytest.mark.parametrize('legacy', [False, True])
def test_dormant_selector_and_every_legal_continuation_are_preserved(legacy):
    state = prefix().snapshot()
    internal = state['combat_checkpoint']['game_state']['combat_state']['_internal']
    assert internal['selector_memory']['pick_count'] == 1
    assert 'choice' not in internal
    if legacy:
        del internal['selector_memory']
    for candidate in prefix().legal_actions():
        live = prefix()
        restored = native.LightspeedRunState()
        restored.load_state(json.loads(json.dumps(state)), reconstruct_legacy=legacy)
        assert live.snapshot() == restored.snapshot()
        assert live.step(candidate['bits']) == restored.step(candidate['bits'])
