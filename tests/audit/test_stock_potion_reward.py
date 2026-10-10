"""Small pure-Python checks; cached original bytecode never uploaded."""

from pathlib import Path

import pytest

from sls.audit.stock_potion_reward import PotionRewardContext, stock_potion_chance

CACHE = Path('local/audits/act2-qualification-20261006/bytecode-r29/rooms.AbstractRoom.txt')


@pytest.mark.skipif(not CACHE.is_file(), reason='legal local stock bytecode is not distributed')
@pytest.mark.parametrize('room,escaped,modifier,statue,count,expected', [
    ('MONSTER', True, 0, False, 0, 0),
    ('MONSTER', True, 50, False, 0, 0),
    ('MONSTER', True, -20, False, 0, 0),
    ('MONSTER', True, 50, True, 0, 100),
    ('MONSTER', True, 50, True, 4, 0),
    ('MONSTER', False, 10, False, 1, 50),
    ('MONSTER', False, -50, False, 1, -10),
    ('MONSTER', False, 0, False, 4, 0),
    ('ELITE', True, 10, False, 1, 50),
    ('BOSS', False, 10, False, 1, 50),
    ('EVENT', True, 10, False, 1, 50),
    ('OTHER', False, 50, False, 0, 0),
    ('OTHER', False, 50, True, 0, 100),
])
def test_original_bytecode_chance_branches(room, escaped, modifier, statue, count, expected):
    context = PotionRewardContext(room, escaped, modifier, statue, count)
    assert stock_potion_chance(CACHE.read_text(), context) == expected


def test_missing_and_unsupported_bytecode_fail_closed():
    context = PotionRewardContext('MONSTER', True, 0, False, 0)
    with pytest.raises(ValueError, match='missing'):
        stock_potion_chance('', context)
    with pytest.raises(ValueError, match='unsupported'):
        stock_potion_chance('public void addPotionToRewards();\n 0: nop\n 93: return', context)


def test_bad_context_is_rejected():
    with pytest.raises(ValueError, match='context'):
        stock_potion_chance('', PotionRewardContext('TYPO', True, 0, False, 0))
