"""Conditioned native Act transitions; not an original-game runtime capture."""
from __future__ import annotations

import argparse
import copy
import hashlib
import json
import os
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
FIXTURE = ROOT / 'tests/fixtures/regressions/act2-calling-bell-boss-131100064.json'


def initial_state(act: int, green_key: bool):
    initial = json.loads(FIXTURE.read_text(encoding='utf-8'))['before']
    initial['run_state'].update(act=act, floor=16 if act == 1 else 33)
    initial['derived_rng']['map'].update(act=act, derived_seed=131100064 + (1 if act == 1 else 200))
    # Explicit synthetic intervention at a conditioned boss-reward boundary.
    # Do not remove a burning node from the map in which the key was acquired.
    initial['player_state']['green_key'] = green_key
    return initial


def run_cases(native):
    rows = []
    for act in (1, 2):
        for green in (False, True):
            run = native.LightspeedRunState()
            run.load_state(initial_state(act, green))
            before = run.snapshot()
            run.step(0)  # Existing Black Star reward, then real native transition.
            after = run.snapshot()
            restored = native.LightspeedRunState()
            restored.load_state(copy.deepcopy(after))
            exact = restored.snapshot() == after and restored.legal_actions() == run.legal_actions()
            actions = run.legal_actions()
            if not actions:
                raise ValueError('unexpected empty next-act actions')
            bits = actions[0]['bits']
            run.step(bits)
            restored.step(bits)
            rows.append(dict(act_before=act, green_key=green, before=before, after=after,
                             checkpoint_equal=exact, next_action_bits=bits,
                             next_action_restored_equal=restored.snapshot() == run.snapshot()))
    return rows


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--map-executable', required=True, type=Path)
    parser.add_argument('--stock-sources', required=True, type=Path)
    parser.add_argument('--output', required=True, type=Path)
    args = parser.parse_args()
    if args.output.exists():
        raise FileExistsError('refuse to replace evidence')
    os.environ['CUDA_VISIBLE_DEVICES'] = '-1'
    from sls.backends.simulator import native
    from sls.rl.training_contract import native_source_digest
    if native.NATIVE_SOURCE_SHA256 != native_source_digest():
        raise ValueError('stale native binary')
    sources = json.loads((args.stock_sources / 'manifest.json').read_text())
    stock = args.stock_sources / 'dungeons.AbstractDungeon.txt'
    record = next(c for c in sources['classes'] if c['name'].endswith('.AbstractDungeon'))
    if hashlib.sha256(stock.read_bytes()).hexdigest() != record['disassembly_sha256']:
        raise ValueError('changed stock disassembly')
    bytecode = stock.read_text(encoding='utf-8')
    method = bytecode.split('protected static void setEmeraldElite();', 1)[1].split('\n  protected', 1)[0]
    required = ['Settings.isFinalActAvailable:Z', 'Settings.hasEmeraldKey:Z',
                'Random.random:(II)I', 'MapRoomNode.hasEmeraldKey:Z']
    if not all(token in method for token in required):
        raise ValueError('stock method evidence is incomplete')
    maps = json.loads(subprocess.check_output([str(args.map_executable.resolve())], timeout=30))
    rows = run_cases(native)
    for row in rows:
        after = row['after']['run_state']
        expected = next(m for m in maps if m['act'] == row['act_before'] + 1
                        and m['assign_burning'] == (not row['green_key']))
        row['map_constructor_equal'] = all(after['burning_elite_' + key] == expected[key]
                                           for key in ('x', 'y', 'buff')) and after['map'] == bytes.fromhex(expected['map_hex']).decode()
        row['stock_static_key_condition_equal'] = (after['burning_elite_x'] == -1) == row['green_key']
        if not all(row[k] for k in ('map_constructor_equal', 'stock_static_key_condition_equal',
                                   'checkpoint_equal', 'next_action_restored_equal')):
            raise ValueError('held-key transition or restore mismatch')
    inputs = ['tools/probe_key_map.cpp', 'tools/probe_held_green_transition.py',
              'native/simulator/src/game/Map.cpp', 'native/simulator/src/game/GameContext.cpp',
              'native/simulator/python/module.cpp', 'native/simulator/include/game/Map.h',
              'native/simulator/include/game/Random.h', 'native/simulator/include/sts_common.h',
              'native/simulator/include/constants/Rooms.h']
    report = dict(schema='sls-held-green-transition-native-probe-v1',
                  scope='STOCK_STATIC_RULE_AND_CONDITIONED_NATIVE_TRANSITION_NOT_STOCK_RUNTIME',
                  training_eligible=False, natural_trajectory=False,
                  stock_sources=sources, native_source_sha256=native.NATIVE_SOURCE_SHA256,
                  input_sha256={p:hashlib.sha256((ROOT / p).read_bytes()).hexdigest() for p in inputs},
                  map_executable_sha256=hashlib.sha256(args.map_executable.read_bytes()).hexdigest(),
                  fixture_sha256=hashlib.sha256(FIXTURE.read_bytes()).hexdigest(),
                  independently_constructed_maps=maps, cases=rows,
                  runtime_gaps=['actual stock next-act map capture', 'stock map RNG comparison',
                                'final-act-unlocked flag absent from native contract'],
                  training_gate='NOT_QUALIFIED')
    with args.output.open('x', encoding='utf-8') as stream:
        json.dump(report, stream, indent=2)
        stream.write('\n')
    print(json.dumps([dict(act=r['act_before'] + 1, green_key=r['green_key'],
                           map_constructor_equal=r['map_constructor_equal'],
                           checkpoint_equal=r['checkpoint_equal'],
                           next_action_restored_equal=r['next_action_restored_equal']) for r in rows]))


if __name__ == '__main__':
    main()
