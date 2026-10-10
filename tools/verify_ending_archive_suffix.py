"""Sealed stock Act4 combat probes and exact isolated-battle suffix replay."""
from __future__ import annotations

import argparse
import hashlib
import json
import os
import zipfile
from pathlib import Path

os.environ['CUDA_VISIBLE_DEVICES'] = '-1'

from sls.audit.act2_differential import direct_projection, production_combat_projection
from sls.audit.card_parity import structured_differences
from sls.audit.stock_clock import verify_sealed_oracle
from sls.audit.terminal_resources import (
    native_terminal_resources,
    stock_terminal_resources,
)


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def prepare(case):
    from sls.backends.simulator import native
    battle = native.LightspeedBattle()
    battle.reset_encounter_probe(case['seed'], case['encounter'], case['initial_rng'],
                                 20, 4, case['floor'], case['scene_id'])
    initial = case['initial']
    battle.set_player_health(initial['hp'], initial['max_hp'])
    payload = battle.snapshot()
    combat = payload['game_state']['combat_state']
    combat['player']['energy'], combat['player']['block'] = initial['energy'], initial['block']
    for monster in combat['monsters']:
        if monster['monster_id'] in initial.get('monster_hp', {}):
            monster['current_hp'] = initial['monster_hp'][monster['monster_id']]
    battle.load_checkpoint({'game_state': payload['game_state'], 'rng': payload['_rng']})
    battle.set_card_piles(initial['hand'], initial['draw'], [], [])
    battle.set_potions([initial['potion']] if 'potion' in initial else [])
    payload = battle.snapshot()
    payload['game_state']['combat_state']['_internal']['potion_capacity'] = 2
    battle.load_checkpoint({'game_state': payload['game_state'], 'rng': payload['_rng']})
    return battle


def step(battle, action):
    command = dict(action)
    battle.step(command.pop('kind'), **command)


def replay(case):
    battle = prepare(case)
    states = [battle.snapshot()]
    projected = [production_combat_projection(battle)]
    for action in case['actions']:
        step(battle, action)
        states.append(battle.snapshot())
        # A victory may remove combat state; preserve its terminal snapshot.
        projected.append(production_combat_projection(battle) if 'combat_state' in states[-1]['game_state']
                         else states[-1])
    suffixes = []
    for index, state in enumerate(states):
        if 'combat_state' not in state['game_state']:
            suffixes.append(dict(boundary=index, supported=False, checkpoint_equal=None,
                                 full_suffix_equal=None, reason='ISOLATED_LOADER_HAS_NO_COMBAT_STATE'))
            continue
        restored = prepare(case)
        restored.load_checkpoint({'game_state': state['game_state'], 'rng': state['_rng']})
        equal = restored.snapshot() == state
        first_difference = None
        for offset, action in enumerate(case['actions'][index:], index + 1):
            step(restored, action)
            if restored.snapshot() != states[offset]:
                first_difference = offset
                break
        suffixes.append(dict(boundary=index, supported=True, checkpoint_equal=equal,
                             full_suffix_equal=equal and first_difference is None and restored.snapshot() == states[-1],
                             first_difference=first_difference, remaining_actions=len(case['actions'])-index))
    return projected, suffixes


def verify(capture_path, build_path, fixture_path):
    from sls.backends.simulator import native
    from sls.rl.training_contract import native_source_digest
    source, build, fixture, launch = [json.loads(p.read_text()) for p in
        (capture_path, build_path, fixture_path, capture_path.with_suffix('.launch.json'))]
    jar = build_path.with_name(build_path.name.removesuffix('.build.json') + '.jar')
    verify_sealed_oracle(jar, build)
    manifest_hash = fixture.get('manifest_sha256', fixture.get('scene_manifest_sha256'))
    capture_hash = fixture.get('capture_sha256', fixture.get('stock_capture_sha256'))
    members = [k for k,v in build['members'].items() if k.startswith('spirecomm/parity/fullrun-')
               and k.endswith('.json') and v == manifest_hash]
    if (len(members) != 1 or sha(capture_path) != capture_hash
            or source.get('scene_manifest_sha256') != manifest_hash
            or not source.get('execution_complete') or source.get('execution_error')
            or launch.get('mode') != 'validation' or launch.get('recovery_status') != 'RECOVERED'
            or launch.get('completion', {}).get('exit_code') != 0 or launch.get('execution_error')
            or fixture['oracle_sha256'] != build['output_sha256']
            or launch['oracle_sha256'] != build['output_sha256']
            or not (source['stock_jar_sha256'] == build['dependencies']['game'] == fixture['stock_jar_sha256'])
            or native.NATIVE_SOURCE_SHA256 != native_source_digest()):
        raise ValueError('archive, fixture, source or recovery identity differs')
    with zipfile.ZipFile(jar) as archive:
        manifest = json.loads(archive.read(members[0]))
    if manifest['stock_jar_sha256'] != source['stock_jar_sha256']:
        raise ValueError('manifest stock identity differs')
    rows = {r['seed']:r for r in source['runs']}
    if len(rows) != len(source['runs']) or set(rows) != {c['seed'] for c in fixture['cases']}:
        raise ValueError('duplicate or missing source cases')
    results = []
    for case in fixture['cases']:
        row = rows[case['seed']]
        if (row['scene'] not in manifest['scenes'] or case['scene_id'] != row['scene']['id']
                or case['seed'] not in row['scene']['seeds'] or case['initial'] != row['scene']['initial']
                or row['ascension'] != 20 or row['act'] != 4 or row['floor'] != case['floor']
                or row['encounter'] != case['encounter'] or case['initial_rng'] != row['before']['_rng']
                or not (case['actions'] == row['actions'] == row['scene']['actions'])
                or row.get('resolved_actions') != row['actions']
                or len(row['boundaries']) != len(row['actions']) + 1):
            raise ValueError('case declaration, RNG or resolved action differs')
        first = row['boundaries'][0]
        direct = first['_stock_direct']
        if (first.get('_oracle_mode') != 'validation' or direct.get('ascension') != 20
                or direct.get('act') != 4 or direct.get('floor') != case['floor']
                or direct.get('dungeon_id') != 'TheEnding'
                or direct.get('dungeon_class') != 'com.megacrit.cardcrawl.dungeons.TheEnding'
                or direct.get('room_class') != ('com.megacrit.cardcrawl.rooms.MonsterRoomBoss'
                    if case['encounter'] == 'THE_HEART' else 'com.megacrit.cardcrawl.rooms.MonsterRoomElite')):
            raise ValueError('actual stock Act4 combat context is not witnessed')
        current, suffixes = replay(case)
        boundaries = []
        terminal = 'expected_terminal_resources' in case
        for index, (stock, simulated) in enumerate(zip(row['boundaries'], current, strict=True)):
            if terminal and index == len(current)-1:
                expected = stock_terminal_resources(stock)
                if expected != case['expected_terminal_resources']:
                    raise ValueError('fixture terminal expectation differs from actual stock')
                differences = structured_differences(expected, native_terminal_resources(simulated))
                scope = 'TERMINAL_HP_OUTCOME_POTIONS_RNG_ONLY'
            else:
                expected = dict(direct=direct_projection(stock, stock=True), rng=stock['_rng'])
                if not terminal:
                    bound = case['expected'][index]
                    if expected['direct'] != bound['direct'] or expected['rng'] != bound['rng']:
                        raise ValueError('fixture expectation differs from actual stock boundary')
                observed = dict(direct=direct_projection(simulated, stock=False, extended=True), rng=simulated['_rng'])
                differences = structured_differences(expected, observed)
                scope = 'RAW_DIRECT_COMBAT_AND_RNG'
            boundaries.append(dict(boundary=index, scope=scope, raw_equal=not differences, differences=differences))
        results.append(dict(seed=case['seed'], scene_id=case['scene_id'], encounter=case['encounter'],
                            boundaries=boundaries, suffixes=suffixes))
    return dict(schema='sls-sealed-ending-battle-suffix-v1', capture_sha256=sha(capture_path),
                fixture_sha256=sha(fixture_path), oracle_sha256=sha(jar), manifest_sha256=manifest_hash,
                native_source_sha256=native.NATIVE_SOURCE_SHA256, native_binary_sha256=sha(Path(native.__file__)),
                source_sha256=sha(Path(__file__)), cuda_visible_devices=os.environ['CUDA_VISIBLE_DEVICES'],
                scope='CONTROLLED_ISOLATED_ACT4_BATTLES_NOT_CONTINUOUS_ENDING_RUN', runs=results,
                training_gate='NOT_QUALIFIED', training_eligible=False, natural_trajectory=False)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    for name in ('capture', 'oracle-build', 'fixture', 'output'):
        parser.add_argument('--'+name, type=Path, required=True)
    args = parser.parse_args()
    if args.output.exists():
        raise FileExistsError('refuse to overwrite archive replay evidence')
    report = verify(args.capture, args.oracle_build, args.fixture)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    with args.output.open('x', encoding='utf-8') as stream:
        json.dump(report, stream, indent=2)
    print(json.dumps(dict(cases=len(report['runs']), raw_different_boundaries=sum(
        not b['raw_equal'] for r in report['runs'] for b in r['boundaries']), suffix_failed=sum(
        s['supported'] and not s['full_suffix_equal'] for r in report['runs'] for s in r['suffixes']),
        suffix_unsupported=sum(not s['supported'] for r in report['runs'] for s in r['suffixes']))))
    return 2 if any(s['supported'] and not s['full_suffix_equal'] for r in report['runs'] for s in r['suffixes']) else 0


if __name__ == '__main__':
    raise SystemExit(main())
