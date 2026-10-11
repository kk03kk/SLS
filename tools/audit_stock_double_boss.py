"""Export local legal stock bytecode and bind double-boss audit obligations.

This is source evidence, never a runtime parity or win-rate certificate.
Original bytecode remains in the requested local output directory.
"""

import argparse
import hashlib
import itertools
import json
import subprocess
import zipfile
from pathlib import Path


def sha(data):
    return hashlib.sha256(data).hexdigest()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--stock-jar', type=Path, required=True)
    parser.add_argument('--javap', type=Path, required=True)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    if args.output.exists():
        raise ValueError('refuse to overwrite source evidence')
    stock_sha = sha(args.stock_jar.read_bytes())
    expected = 'cfad868ac8d65a88e71a0bf096fb09f78811e553effe0787c5309a655e081673'
    if stock_sha != expected:
        raise ValueError('stock identity differs: re-audit rather than reuse expectations')
    args.output.mkdir(parents=True)
    classes = {}
    with zipfile.ZipFile(args.stock_jar) as jar:
        for suffix in ('ui.buttons.ProceedButton', 'rooms.MonsterRoomBoss',
                       'dungeons.AbstractDungeon', 'dungeons.TheBeyond'):
            name = 'com.megacrit.cardcrawl.' + suffix
            command = [str(args.javap), '-classpath', str(args.stock_jar), '-c', '-p', name]
            result = subprocess.run(command, capture_output=True, timeout=45, check=True)
            (args.output / (suffix + '.txt')).write_bytes(result.stdout)
            classes[name] = {'class_sha256': sha(jar.read(name.replace('.', '/') + '.class')),
                             'bytecode_sha256': sha(result.stdout), 'command': command}
    bosses = ['TIME_EATER', 'AWAKENED_ONE', 'DONU_AND_DECA']
    report = {
        'schema': 'sls-stock-double-boss-source-v1', 'stock_jar_sha256': stock_sha,
        'status': 'SOURCE_EXPORTED_REQUIRES_MANUAL_REVIEW_AND_RUNTIME', 'classes': classes,
        'obligations': [{'first': first, 'second': second, 'status': 'RUNTIME_UNVERIFIED',
                         'boundaries': ['first boss initial', 'first victory',
                                        'second boss initial', 'second victory'],
                         'compare': ['boss identity and remaining boss list', 'floor and room',
                                     'HP/maxHP/gold', 'permanent deck and potions',
                                     'relic counters and victory/entry callbacks',
                                     'reward construction versus rewards actually granted',
                                     'all auditable RNG streams', 'legal semantic actions',
                                     'checkpoint restore and next action']}
                        for first, second in itertools.permutations(bosses, 2)],
        'restriction': 'No controlled setup or skip-battle result is a natural trajectory.'}
    (args.output / 'identity.json').write_text(json.dumps(report, indent=2) + '\n', encoding='utf-8')
    print(json.dumps({'stock_sha256': stock_sha, 'classes': len(classes),
                      'ordered_pairs': len(report['obligations']), 'runtime_verified': False}))
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
