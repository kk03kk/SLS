"""Run sealed stock map probes through owned-process backup and recovery."""
from __future__ import annotations

import argparse
import hashlib
import json
import os
import sys
import zipfile
from pathlib import Path

from tools.capture_held_key_map import validate
from tools.run_original_canary import original_runtime_paths
from tools.verify_oracle import inspect, runtime_smoke


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--oracle', required=True, type=Path)
    parser.add_argument('--manifest', required=True, type=Path)
    parser.add_argument('--output', required=True, type=Path)
    args = parser.parse_args()
    if args.output.exists() or args.output.with_suffix('.launch.json').exists():
        raise FileExistsError('refuse to overwrite evidence')
    manifest = json.loads(args.manifest.read_text())
    validate(manifest)
    build = inspect(args.oracle)
    member = 'spirecomm/parity/' + args.manifest.name
    if hashlib.sha256(args.manifest.read_bytes()).hexdigest() != build['members'].get(member):
        raise ValueError('manifest differs from sealed Oracle')
    _, game = original_runtime_paths(None)
    stock = game / 'desktop-1.0.jar'
    if not hashlib.sha256(stock.read_bytes()).hexdigest() == manifest['stock_jar_sha256'] == build['dependencies']['game']:
        raise ValueError('stock JAR identity mismatch')
    expected = {'com.megacrit.cardcrawl.' + name for name in
                ('dungeons.AbstractDungeon', 'dungeons.TheCity', 'dungeons.TheBeyond',
                 'map.MapRoomNode', 'rooms.RestRoom')}
    if set(manifest['source_evidence']) != expected:
        raise ValueError('missing stock constructor evidence')
    with zipfile.ZipFile(stock) as archive:
        for name, row in manifest['source_evidence'].items():
            if hashlib.sha256(archive.read(name.replace('.', '/') + '.class')).hexdigest() != row['class_sha256']:
                raise ValueError('stock class identity mismatch')
    os.environ['CUDA_VISIBLE_DEVICES'] = '-1'
    command = [Path(sys.executable).as_posix(),
               Path(__file__).with_name('capture_held_key_map.py').resolve().as_posix(),
               '--manifest', args.manifest.resolve().as_posix(), '--output', args.output.resolve().as_posix()]
    result = runtime_smoke(args.oracle.resolve(), args.output.resolve(), 'validation', None, 600,
                           capture_command=command)
    print(json.dumps(result, indent=2))


if __name__ == '__main__':
    main()
