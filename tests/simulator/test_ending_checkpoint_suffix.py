"""Replay every remaining action from each recorded controlled Act4 boundary."""
import json
from pathlib import Path

import pytest

from sls.audit.act2_differential import direct_projection
from sls.audit.card_parity import structured_differences
from sls.audit.terminal_resources import native_terminal_resources
from tools.verify_ending_archive_suffix import replay

CASES = [case for name in ('ending-interaction-stock', 'ending-death-stock', 'heart-loss-stock', 'heart-lethal-stock')
         for case in json.loads((Path('tests/fixtures') / (name + '.json')).read_text())['cases']]


@pytest.mark.parametrize('case', CASES, ids=lambda c:str(c['seed']))
def test_controlled_ending_stock_expectations_and_full_checkpoint_suffix(case):
    states, suffixes = replay(case)
    assert len(states) == len(suffixes) == len(case['actions']) + 1
    for index, (state, suffix) in enumerate(zip(states, suffixes, strict=True)):
        if suffix['supported']:
            assert suffix['checkpoint_equal'] and suffix['full_suffix_equal'], suffix
        else:
            assert 'combat_state' not in state['game_state']
            assert suffix['checkpoint_equal'] is None and suffix['full_suffix_equal'] is None
        if 'expected_terminal_resources' in case:
            if index == len(states)-1:
                assert native_terminal_resources(state) == case['expected_terminal_resources']
        else:
            expected = case['expected'][index]
            differences = structured_differences(expected['direct'], direct_projection(state, stock=False, extended=True))
            # Preserve the original raw corpse mismatch instead of deleting powers.
            assert differences == expected.get('declared_residue', [])
            assert state['_rng'] == expected['rng']
