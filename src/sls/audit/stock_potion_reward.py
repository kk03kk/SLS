"""Execute the reviewed javap probability prefix, without native/game imports.

Only computes chance at offset93; excludes RNG, reward generation and game flow.
Unsupported instructions fail rather than guess semantics.
"""

from __future__ import annotations

import re
from dataclasses import dataclass


@dataclass(frozen=True)
class PotionRewardContext:
    room: str
    all_monsters_escaped: bool
    potion_modifier: int
    white_beast_statue: bool
    reward_count: int


def stock_potion_chance(bytecode: str, context: PotionRewardContext) -> int:
    if context.room not in {'MONSTER', 'ELITE', 'BOSS', 'EVENT', 'OTHER'} or context.reward_count < 0:
        raise ValueError('invalid stock probability context')
    method = bytecode.split('public void addPotionToRewards();', 1)
    if len(method) != 2:
        raise ValueError('missing stock potion reward method')
    instructions = {}
    for line in method[1].splitlines():
        match = re.match(r'\s*(\d+):\s+(\w+)(.*)', line)
        if match:
            offset, operation, rest = match.groups()
            offset = int(offset)
            if offset > 93:
                break
            instructions[offset] = (operation, rest.strip())
    if 0 not in instructions or 93 not in instructions:
        raise ValueError('incomplete stock probability prefix')
    ordered = sorted(instructions)
    following = dict(zip(ordered, ordered[1:]))
    stack, local = [], {}
    pc = 0
    for _ in range(128):
        if pc == 93:
            if stack or 1 not in local:
                raise ValueError('invalid stock probability exit')
            return local[1]
        if pc not in instructions:
            raise ValueError('invalid stock branch target')
        op, rest = instructions[pc]
        next_pc = following[pc]
        if op == 'iconst_0':
            stack.append(0)
        elif op == 'iconst_4':
            stack.append(4)
        elif op == 'bipush':
            stack.append(int(rest))
        elif op == 'aload_0':
            stack.append('ROOM')
        elif op == 'istore_1':
            local[1] = stack.pop()
        elif op == 'iload_1':
            stack.append(local[1])
        elif op == 'iadd':
            right, left = stack.pop(), stack.pop()
            stack.append(left + right)
        elif op == 'goto':
            next_pc = int(rest)
        elif op in {'ifeq', 'ifne'}:
            value = stack.pop()
            if (value == 0) == (op == 'ifeq'):
                next_pc = int(rest)
        elif op == 'if_icmplt':
            right, left = stack.pop(), stack.pop()
            if left < right:
                next_pc = int(rest)
        elif op == 'instanceof':
            if stack.pop() != 'ROOM':
                raise ValueError('invalid stock room receiver')
            class_name = rest.split('// class ', 1)[-1].rsplit('/', 1)[-1]
            memberships = {'MonsterRoomElite': {'ELITE'},
                           'MonsterRoom': {'MONSTER', 'ELITE', 'BOSS'},
                           'EventRoom': {'EVENT'}}
            if class_name not in memberships:
                raise ValueError('unsupported stock room class')
            stack.append(int(context.room in memberships[class_name]))
        elif op == 'getstatic' and 'Field blizzardPotionMod:I' in rest:
            stack.append(context.potion_modifier)
        elif op == 'getstatic' and 'AbstractDungeon.player:' in rest:
            stack.append('PLAYER')
        elif op == 'invokestatic' and 'AbstractDungeon.getMonsters:' in rest:
            stack.append('MONSTERS')
        elif op == 'invokevirtual' and 'MonsterGroup.haveMonstersEscaped:()Z' in rest:
            if stack.pop() != 'MONSTERS':
                raise ValueError('invalid stock monster receiver')
            stack.append(int(context.all_monsters_escaped))
        elif op == 'ldc_w' and '// String White Beast Statue' in rest:
            stack.append('WHITE_BEAST_STATUE')
        elif op == 'invokevirtual' and 'hasRelic:(Ljava/lang/String;)Z' in rest:
            if stack.pop() != 'WHITE_BEAST_STATUE' or stack.pop() != 'PLAYER':
                raise ValueError('invalid stock relic receiver')
            stack.append(int(context.white_beast_statue))
        elif op == 'getfield' and '// Field rewards:Ljava/util/ArrayList;' in rest:
            if stack.pop() != 'ROOM':
                raise ValueError('invalid stock rewards receiver')
            stack.append('REWARDS')
        elif op == 'invokevirtual' and 'java/util/ArrayList.size:()I' in rest:
            if stack.pop() != 'REWARDS':
                raise ValueError('invalid stock rewards list')
            stack.append(context.reward_count)
        else:
            raise ValueError(f'unsupported stock instruction at {pc}: {op} {rest}')
        pc = next_pc
    raise ValueError('stock prefix instruction budget exceeded')
