"""Bounded CPU acceptance of corrected shared rules; no model or torch."""
from __future__ import annotations

import copy
import json
from pathlib import Path

from sls.rl.training_contract import native_source_digest

COUNTERS = ((0, 0), (1, 250), (249, 250), (250, 250), (251, 500),
            (499, 500), (500, 500), (501, 750), (749, 750), (750, 750), (751, 751))


def verify(root: Path) -> dict:
    from sls.backends.simulator import native
    if native.NATIVE_SOURCE_SHA256 != native_source_digest():
        raise RuntimeError('shared-rule acceptance requires current native source')
    fixture = root / 'tests/fixtures/regressions'
    initial = json.loads((fixture / 'act2-calling-bell-boss-131100064.json')
                         .read_text(encoding='utf-8'))['before']
    transitions = 0
    for act in (1, 2):
        for counter, expected in COUNTERS:
            state = copy.deepcopy(initial)
            state['run_state'].update(act=act, floor=16 if act == 1 else 33)
            state['derived_rng']['map'].update(act=act,
                derived_seed=state['run_state']['seed'] + (1 if act == 1 else 200))
            state['rng']['card']['counter'] = counter
            run = native.LightspeedRunState()
            run.load_state(state)
            before = run.snapshot()
            restored = native.LightspeedRunState()
            restored.load_state(before)
            if not any(a['bits'] == 0 for a in run.legal_actions()):
                raise RuntimeError('boss reward action missing')
            run.step(0)
            restored.step(0)
            after = run.snapshot()
            if (after != restored.snapshot() or after['run_state']['act'] != act + 1
                    or after['rng']['card']['counter'] != expected
                    or (counter == expected and after['rng']['card'] != before['rng']['card'])):
                raise RuntimeError('stock strict transition interval or restore mismatch')
            end = native.LightspeedRunState()
            end.load_state(after)
            if end.snapshot() != after or end.legal_actions() != run.legal_actions():
                raise RuntimeError('post-transition restore mismatch')
            transitions += 1
    rows = json.loads((fixture / 'act12-thief-city-rewards-131200180.json')
                      .read_text(encoding='utf-8'))['runs']
    if len(rows) != 15 or [r['seed'] for r in rows] != list(range(131200180, 131200195)):
        raise RuntimeError('missing stock thief reward cases')
    for row in rows:
        run = native.LightspeedRunState()
        run.load_state(row['initial'])
        for bits in row['action_bits']:
            state = run.snapshot()
            actions = run.legal_actions()
            restored = native.LightspeedRunState()
            restored.load_state(state)
            if (restored.snapshot() != state or restored.legal_actions() != actions
                    or not any(a['bits'] == bits for a in actions)):
                raise RuntimeError('thief boundary restore/action mismatch')
            run.step(bits)
            restored.step(bits)
            if run.snapshot() != restored.snapshot():
                raise RuntimeError('thief next action replay mismatch')
        state = run.snapshot()
        screen = state['public_screen']
        measured = {
            'potion_modifier': state['progress_state']['potion_chance'],
            'potion_rng': state['rng']['potion'], 'potions': screen['potions'],
            'all_rng': state['rng'], 'player_hp': state['player_state']['current_hp'],
            'player_gold': state['player_state']['gold'], 'reward_gold': screen['gold'],
            'reward_relics': screen['relics'],
            'reward_cards': [[{'id': c['id'], 'upgrades': c['upgrades'], 'misc': c['special_data']}
                              for c in cards] for cards in screen['card_rewards']],
        }
        if measured != row['expected']:
            raise RuntimeError('stock thief reward or RNG mismatch')
    return {'status': 'PASS', 'synthetic_transition_cases': transitions,
            'stock_thief_cases': len(rows), 'native_source_sha256': native.NATIVE_SOURCE_SHA256}
