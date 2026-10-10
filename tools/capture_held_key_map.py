"""Capture stock maps after controlled initial flags; no fabricated map nodes."""
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
from tools.run_key_room_batch import root_path


def validate(manifest):
    expected = {(act, green, final) for act in (2, 3)
                for green, final in ((False, True), (True, True), (False, False))}
    if (manifest.get('schema') != 'sls-held-key-map-scenes-v1'
            or manifest.get('natural_trajectory') is not False
            or manifest.get('training_eligible') is not False
            or manifest.get('repeated_seed_policy') != 'EXPLICIT_SAME_SEED_PAIRED_FLAGS_AND_ACTS'
            or manifest.get('seed_namespace') != [131200410, 131200411]):
        raise ValueError('undeclared controlled map corpus')
    rows = manifest['scenes']
    if len(rows) != 6 or len({r['id'] for r in rows}) != 6:
        raise ValueError('missing or repeated scene')
    if any(set(r) != {'id', 'seed', 'act', 'ascension', 'emerald_key', 'final_act_available'}
           or type(r['seed']) is not int or r['seed'] != 131200410
           or type(r['act']) is not int or type(r['ascension']) is not int
           or r['ascension'] != 20 or type(r['emerald_key']) is not bool
           or type(r['final_act_available']) is not bool for r in rows):
        raise ValueError('invalid map input')
    if {(r['act'], r['emerald_key'], r['final_act_available']) for r in rows} != expected:
        raise ValueError('map pair coverage differs')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--manifest', required=True, type=Path)
    parser.add_argument('--output', required=True, type=Path)
    args = parser.parse_args()
    os.environ['CUDA_VISIBLE_DEVICES'] = '-1'
    manifest = json.loads(args.manifest.read_text())
    validate(manifest)
    with args.output.open('x', encoding='utf-8') as stream:
        stream.write('{}\n')
    report = dict(schema='sls-held-key-map-capture-v1', execution_complete=False,
                  manifest_sha256=hashlib.sha256(args.manifest.read_bytes()).hexdigest(),
                  scope='CONTROLLED_FLAGS_NOT_NATURAL_TRANSITION_NOT_WIN_RATE', runs=[])
    def flush():
        args.output.write_text(json.dumps(report, indent=2) + '\n', encoding='utf-8')
    try:
        session = OriginalSession()
        backend = OriginalBackend(session=session, profile=IRONCLAD_A20_HEART)
        for scene in manifest['scenes']:
            decision = backend.reset(scene['seed'])
            prefix = []
            for _ in range(40):
                if decision.observation.screen == 'COMBAT':
                    break
                if decision.terminal or not decision.actions:
                    raise ValueError('normal prefix cannot reach initial combat')
                action = next((a for a in decision.actions if a.kind in {
                    ActionKind.CHOOSE_NEOW_OPTION, ActionKind.CHOOSE_MAP_NODE,
                    ActionKind.SELECT_CARD, ActionKind.CONFIRM}), decision.actions[0])
                prefix.append(dict(raw=backend.raw_payload, action=asdict(action)))
                decision = backend.step(action).decision
            else:
                raise ValueError('prefix unfinished')
            row = dict(scene=scene, normal_prefix=prefix, before=session.payload,
                       setup_command='parity_map_room ' + scene['id'], status='IN_PROGRESS')
            report['runs'].append(row)
            flush()
            if 'parity_map_room' not in session.payload.get('available_commands', []):
                raise ValueError('validation map command unavailable')
            row['setup_response'] = session.execute(row['setup_command'])
            def ready(payload):
                return (payload.get('_parity_scenario', {}).get('scenario_id') == scene['id']
                        and payload.get('_stock_direct', {}).get('act') == scene['act']
                        and adapt_original(payload).decision.observation.screen == 'REST')
            row['boundary'] = collect(session, ready, 'stock generated map in rest room')
            row['root_path'] = root_path(row['boundary'])
            row['status'] = 'STOCK_CONSTRUCTOR_MAP_CAPTURED'
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
