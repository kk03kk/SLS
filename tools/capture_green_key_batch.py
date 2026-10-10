"""Play actual stock elite combat; never fabricate outcomes or rewards."""
from __future__ import annotations

import argparse
import hashlib
import json
import os
from dataclasses import asdict
from pathlib import Path

from sls.backends.original.adapter import adapt_original
from sls.backends.original.environment import OriginalBackend
from sls.backends.original.session import OriginalSession
from sls.contracts import ActionKind
from sls.curriculum import IRONCLAD_A20_HEART
from tools.capture_key_room_batch import collect
from tools.capture_original_card_batch import _write_completion
from tools.run_green_key_batch import validate
from tools.run_key_room_batch import root_path


def choose(decision, strategy):
    if strategy == 'PLAY_ATTACKS_THEN_END':
        attacks = [action for action in decision.actions if action.kind is ActionKind.PLAY_CARD]
        if attacks:
            return attacks[0]
    ends = [action for action in decision.actions if action.kind is ActionKind.END_TURN]
    if len(ends) != 1:
        raise ValueError('expected one end-turn action in frozen combat')
    return ends[0]


def initial_ready(payload):
    direct = payload.get('_stock_direct', {})
    return (direct.get('act') == 2 and direct.get('current_node_has_emerald_key') is True
            and payload.get('_parity_scenario', {}).get('corpus') == 'fullrun-green-key-r2'
            and direct.get('turn') == 1
            and direct.get('monsters') and all(m['next_move'] >= 0 for m in direct['monsters'])
            and (payload.get('game_state', {}).get('combat_state', {}).get('hand'))
            and adapt_original(payload).decision.observation.screen == 'COMBAT'
            and any(action.kind is ActionKind.END_TURN for action in adapt_original(payload).decision.actions))


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--manifest', type=Path, required=True)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    os.environ['CUDA_VISIBLE_DEVICES'] = '-1'
    manifest = json.loads(args.manifest.read_text())
    validate(manifest)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    with args.output.open('x') as stream:
        stream.write('{}\n')
    report = dict(schema='sls-green-key-capture-v1', manifest_sha256=hashlib.sha256(args.manifest.read_bytes()).hexdigest(),
                  purpose='CONTROLLED_NOT_NATURAL_NOT_WIN_RATE', execution_complete=False, runs=[])

    def flush():
        args.output.write_text(json.dumps(report, indent=2) + '\n', encoding='utf-8')

    try:
        session = OriginalSession()
        backend = OriginalBackend(session=session, profile=IRONCLAD_A20_HEART)
        for scene in manifest['scenes']:
            for seed in scene['seeds']:
                decision = backend.reset(seed)
                prefix = []
                for _ in range(40):
                    if decision.observation.screen == 'COMBAT':
                        break
                    if decision.terminal or not decision.actions:
                        raise ValueError('normal prefix cannot reach initial combat')
                    action = next((a for a in decision.actions if a.kind in {
                        ActionKind.CHOOSE_NEOW_OPTION, ActionKind.CHOOSE_MAP_NODE, ActionKind.SELECT_CARD,
                        ActionKind.CONFIRM}), decision.actions[0])
                    prefix.append(dict(raw=backend.raw_payload, action=asdict(action)))
                    decision = backend.step(action).decision
                else:
                    raise ValueError('normal prefix budget exhausted')
                row = dict(scene=scene, seed=seed, normal_prefix=prefix, before=session.payload,
                           setup_command=f"parity_key_room {scene['id']} {args.manifest.stem}",
                           boundaries=[], actions=[], status='IN_PROGRESS')
                report['runs'].append(row)
                flush()
                if 'parity_key_room' not in session.payload.get('available_commands', []):
                    raise ValueError('validation setup unavailable')
                row['setup_response'] = session.execute(row['setup_command'])
                initial = collect(session, initial_ready, 'actual burning elite combat')
                row['initial_settle_commands'] = []
                initial = backend._settle_command_boundary(initial, row['initial_settle_commands'])
                if not initial_ready(initial):
                    raise ValueError('elite initial boundary changed during settle')
                row['initial_root_path'] = root_path(initial)
                backend._adapted = backend._adapt(initial)
                decision = backend._adapted.decision
                row['boundaries'].append(initial)
                flush()
                for _ in range(scene['max_decisions']):
                    if decision.terminal:
                        row['status'] = 'GAME_TERMINAL'
                        break
                    if backend.raw_payload['_parity_run']['emerald_key']:
                        row['status'] = 'GREEN_KEY_ACQUIRED'
                        break
                    if decision.observation.screen == 'COMBAT':
                        action = choose(decision, scene['strategy'])
                    else:
                        matches = [a for a in decision.actions if a.reward_id == 'reward-key:emerald']
                        if len(matches) != 1:
                            raise ValueError('combat ended without unique actual green-key reward')
                        action = matches[0]
                    transition = backend.step(action)
                    row['actions'].append(dict(actual=asdict(action), commands=list(backend.last_executed_commands),
                                               info=dict(transition.info)))
                    decision = transition.decision
                    row['boundaries'].append(backend.raw_payload)
                    flush()
                else:
                    row['status'] = ('GAME_TERMINAL' if decision.terminal else
                                     'GREEN_KEY_ACQUIRED' if backend.raw_payload['_parity_run']['emerald_key']
                                     else 'DIAGNOSTIC_LIMIT_UNFINISHED')
                flush()
        backend.return_to_menu()
        report['execution_complete'] = True
        flush()
        _write_completion(0)
        return 0
    except BaseException as error:
        report['execution_error'] = f'{type(error).__name__}: {error}'
        flush()
        _write_completion(2, report['execution_error'])
        return 2


if __name__ == '__main__':
    raise SystemExit(main())
