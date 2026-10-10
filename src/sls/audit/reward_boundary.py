"""Bounded collection of independent stock post-combat evidence."""

from __future__ import annotations

import time
from typing import Callable


def reward_boundary_ready(payload: dict) -> bool:
    evidence = payload.get('_stock_reward_state')
    if payload.get('_oracle_mode') != 'validation' or not isinstance(evidence, dict):
        raise ValueError('requires validation stock reward evidence')
    if evidence.get('schema') != 'sls-stock-reward-state-v1':
        raise ValueError('unsupported stock reward evidence')
    game = payload.get('game_state') or {}
    return (evidence.get('room_phase') == 'COMPLETE'
            and game.get('screen_type') == 'COMBAT_REWARD'
            and isinstance(evidence.get('room_rewards'), list)
            and isinstance(evidence.get('screen_rewards'), list))


def collect_reward_boundary(get_state: Callable[[], dict], *, timeout: float = 10,
                            clock: Callable[[], float] = time.monotonic,
                            sleep: Callable[[float], None] = time.sleep) -> dict:
    if not 0 < timeout <= 30:
        raise ValueError('reward boundary timeout must be in (0,30]')
    deadline = clock() + timeout
    for _ in range(300):
        if clock() >= deadline:
            break
        payload = get_state()
        if reward_boundary_ready(payload):
            return payload
        sleep(0.05)
    raise TimeoutError('stock reward boundary did not stabilize; execution failure')
