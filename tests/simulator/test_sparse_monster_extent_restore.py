import copy
import json
from pathlib import Path

import pytest

from sls.backends.simulator import native

FIXTURE=json.loads(Path('tests/fixtures/regressions/sparse-monster-extent-prefix.json').read_text())


def prefix():
    run=native.LightspeedRunState()
    run.reset(FIXTURE['seed'],20,FIXTURE['math_seed'])
    for bits in FIXTURE['actions']:
        run.step(bits)
    return run


@pytest.mark.parametrize('legacy',[False,True])
def test_private_extent_and_every_following_legal_action_are_preserved(legacy):
    state=prefix().snapshot()
    internal=state['combat_checkpoint']['game_state']['combat_state']['_internal']
    assert internal['monster_count']==4
    assert len(state['combat_checkpoint']['game_state']['combat_state']['monsters'])==3
    if legacy:
        del internal['monster_count']
    for candidate in prefix().legal_actions():
        live=prefix()
        restored=native.LightspeedRunState()
        restored.load_state(json.loads(json.dumps(state)),reconstruct_legacy=legacy)
        assert live.snapshot()==restored.snapshot()
        assert live.step(candidate['bits'])==restored.step(candidate['bits'])


def test_invalid_private_extent_is_rejected():
    state=copy.deepcopy(prefix().snapshot())
    state['combat_checkpoint']['game_state']['combat_state']['_internal']['monster_count']=99
    with pytest.raises(ValueError,match='array extent'):
        native.LightspeedRunState().load_state(state)
