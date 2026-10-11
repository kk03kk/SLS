"""Offline original-bytecode probability audit; never original/native runtime parity."""

from __future__ import annotations

import argparse
import hashlib
import itertools
import json
import zipfile
from dataclasses import asdict
from pathlib import Path

from sls.audit.stock_potion_reward import PotionRewardContext, stock_potion_chance


def digest(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--stock-jar', type=Path, required=True)
    parser.add_argument('--identity', type=Path, required=True)
    parser.add_argument('--bytecode', type=Path, required=True)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    if args.output.exists():
        raise FileExistsError('refuse to overwrite evidence')
    identity = json.loads(args.identity.read_text(encoding='utf-8'))
    name = 'com.megacrit.cardcrawl.rooms.AbstractRoom'
    evidence = identity['classes'][name]
    if (digest(args.stock_jar) != identity['stock_jar_sha256']
            or digest(args.bytecode) != evidence['bytecode_sha256']):
        raise ValueError('stale original source identity')
    with zipfile.ZipFile(args.stock_jar) as archive:
        if hashlib.sha256(archive.read(name.replace('.', '/') + '.class')).hexdigest() != evidence['class_sha256']:
            raise ValueError('stock class identity mismatch')
    bytecode = args.bytecode.read_text(encoding='utf-8')
    rows = []
    for room, escaped, modifier, statue, count in itertools.product(
            ('MONSTER', 'ELITE', 'BOSS', 'EVENT', 'OTHER'), (False, True),
            (-50, -20, 0, 10, 50), (False, True), (0, 1, 3, 4)):
        context = PotionRewardContext(room, escaped, modifier, statue, count)
        chance = stock_potion_chance(bytecode, context)
        rows.append({'context': asdict(context), 'stock_chance': chance,
                     'successful_rolls_0_to_99': sum(roll < chance for roll in range(100))})
    result = {'schema': 'sls-stock-potion-probability-prefix-v1',
              'scope': 'OFFLINE_BYTECODE_BRANCHES_ONLY',
              'stock_jar_sha256': digest(args.stock_jar),
              'stock_class_sha256': evidence['class_sha256'],
              'bytecode_sha256': digest(args.bytecode), 'identity_sha256': digest(args.identity),
              'interpreter_sha256': digest(Path(__file__).resolve().parents[1] / 'src/sls/audit/stock_potion_reward.py'),
              'rows': rows, 'original_game_executed': False, 'native_executed': False,
              'limits': ['RNG advancement not executed', 'reward generation not executed',
                         'monster escape/death and whole-room flow not executed',
                         'all-escaped ELITE/BOSS combinations are structural branches, not reachability claims']}
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(result, indent=2) + '\n', encoding='utf-8')
    print(json.dumps({'contexts_checked': len(rows), 'scope': result['scope']}))
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
