"""Launch sealed controlled stock elite battles with owned runtime recovery."""
from __future__ import annotations

import argparse
import hashlib
import json
import os
import sys
import zipfile
from pathlib import Path

from tools.run_original_canary import original_runtime_paths
from tools.verify_oracle import inspect, runtime_smoke


def validate(manifest):
    if (manifest.get('schema') not in {'sls-green-key-scenes-v1', 'sls-green-key-scenes-v2', 'sls-green-key-scenes-v3'}
            or manifest.get('map_node_policy') != 'ROOT_REACHABLE_STOCK_BURNING_ELITE'
            or manifest.get('room_rng_policy') != 'STOCK_SEED_PLUS_DERIVED_FLOOR_FIVE_STREAMS'
            or manifest.get('natural_trajectory') is not False
            or manifest.get('training_eligible') is not False):
        raise ValueError('undeclared controlled green corpus')
    v3 = manifest['schema'] == 'sls-green-key-scenes-v3'
    v2 = manifest['schema'] in {'sls-green-key-scenes-v2', 'sls-green-key-scenes-v3'}
    if v2 and manifest.get('entry_policy') != 'STOCK_ROOM_ON_ENTRY_AND_PRE_BATTLE_PREP':
        raise ValueError('undeclared actual stock combat entry')
    seeds, identifiers = [], []
    for scene in manifest['scenes']:
        initial = scene['initial']
        if (scene['act'] != 2 or scene['ascension'] != 20 or scene['room'] != 'ELITE'
                or scene.get('floor_policy') != 'ACT2_MAP_Y_PLUS_18'
                or 'floor' in scene or not 0 < initial['hp'] <= initial['max_hp'] <= 1000
                or initial['emerald_key'] or initial['final_act_available'] is not True
                or scene['strategy'] not in {'PLAY_ATTACKS_THEN_END', 'END_TURN_ONLY', 'PLAY_MAX_HP_TARGET_THEN_END'}
                or not 1 <= scene['max_decisions'] <= 256):
            raise ValueError('unsupported green initial state or strategy')
        seeds.extend(scene['seeds'])
        identifiers.append(scene['id'])
    if (not seeds or len(seeds) != len(set(seeds)) or len(identifiers) != len(set(identifiers))
            or any(type(seed) is not int or not (131200370 <= seed < 131200402 if v3 else
                                               131200362 <= seed < 131200364 if v2
                                               else 131200360 <= seed < 131200362) for seed in seeds)):
        raise ValueError('green seed namespace collision or invalid selection')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--oracle', type=Path, required=True)
    parser.add_argument('--manifest', type=Path, required=True)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    if args.output.exists() or args.output.with_suffix('.launch.json').exists():
        raise FileExistsError('refuse to overwrite capture')
    manifest = json.loads(args.manifest.read_text())
    validate(manifest)
    if manifest['schema'] not in {'sls-green-key-scenes-v2', 'sls-green-key-scenes-v3'}:
        raise ValueError('R1 is retained prototype evidence; new execution requires R2')
    report = inspect(args.oracle)
    member = 'spirecomm/parity/' + args.manifest.name
    if hashlib.sha256(args.manifest.read_bytes()).hexdigest() != report['members'].get(member):
        raise ValueError('green manifest differs from sealed Oracle')
    _, game = original_runtime_paths(None)
    stock = game / 'desktop-1.0.jar'
    actual = hashlib.sha256(stock.read_bytes()).hexdigest()
    if actual != manifest['stock_jar_sha256'] or actual != report['dependencies']['game']:
        raise ValueError('wrong stock JAR')
    required = {'dungeons.AbstractDungeon', 'rooms.MonsterRoom', 'rooms.MonsterRoomElite',
                'rooms.AbstractRoom', 'rewards.RewardItem', 'vfx.ObtainKeyEffect',
                'map.MapRoomNode', 'dungeons.TheCity'}
    if manifest['schema'] in {'sls-green-key-scenes-v2', 'sls-green-key-scenes-v3'}:
        required.add('characters.AbstractPlayer')
    if set(manifest['source_evidence']) != {'com.megacrit.cardcrawl.' + name for name in required}:
        raise ValueError('missing green source evidence')
    with zipfile.ZipFile(stock) as archive:
        for name, row in manifest['source_evidence'].items():
            if hashlib.sha256(archive.read(name.replace('.', '/') + '.class')).hexdigest() != row['class_sha256']:
                raise ValueError('wrong stock class')
    os.environ['CUDA_VISIBLE_DEVICES'] = '-1'
    command = [Path(sys.executable).as_posix(), Path(__file__).with_name('capture_green_key_batch.py').resolve().as_posix(),
               '--manifest', args.manifest.resolve().as_posix(), '--output', args.output.resolve().as_posix()]
    result = runtime_smoke(args.oracle.resolve(), args.output.resolve(), 'validation', None, 900,
                           capture_command=command)
    print(json.dumps(result, indent=2))


if __name__ == '__main__':
    main()
