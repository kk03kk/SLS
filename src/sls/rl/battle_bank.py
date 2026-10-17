"""Immutable single-root, source-separated natural-prefix research banks."""
from __future__ import annotations

import gzip
import json
from pathlib import Path

from sls.content.selection import PROJECTION_REVISION
from sls.rl.training_contract import (
    local_source_digest,
    native_source_digest,
    sha256_file,
)

SCHEMA='sls-natural-third-battle-bank-v1'
PARENT_SHA='274963f4fe32b75003c5a1b4ccd394b5185304156e6ea22a76aee2764d54f1e0'
PROJECTION_FILES=('src/sls/backends/simulator/environment.py','src/sls/content/selection.py',
                  'src/sls/model/batching.py','src/sls/model/encoding.py')


def projection_digest():
    return local_source_digest(PROJECTION_FILES)


def write_json(path,value):
    path=Path(path)
    temporary=path.with_suffix(path.suffix+'.tmp')
    temporary.write_text(json.dumps(value,ensure_ascii=False,indent=2,allow_nan=False)+'\n',encoding='utf-8')
    temporary.replace(path)


def load_bank(directory,split,*,allow_confirmation=False):
    if split not in ('fit','development','confirmation') or (split=='confirmation' and not allow_confirmation):
        raise ValueError('confirmation bank is sealed during development')
    directory=Path(directory)
    manifest=json.loads((directory/'manifest.json').read_text(encoding='utf-8'))
    if (manifest['schema']!=SCHEMA or manifest['native_source_sha256']!=native_source_digest()
            or manifest['projection_sha256']!=projection_digest()
            or manifest['projection_revision']!=PROJECTION_REVISION):
        raise ValueError('battle bank source/projection mismatch')
    if (manifest.get('selection')!='THIRD_ACT2_COMBAT_STOP_BEFORE_TARGET_ACTION_NO_OUTCOME_FILTER'
            or manifest.get('confirmation_target_actions_executed')!=0 or manifest.get('weights_unchanged') is not True):
        raise ValueError('battle bank provenance or confirmation seal missing')
    all_seeds=[]
    for role in ('fit','development','confirmation'):
        all_seeds.extend(manifest['root_sources'][role])
        if any(type(seed) is not int or not (0<=seed<2_000_000_000_000 if role=='fit'
               else 8_000_000_000_000<=seed<9_000_000_000_000) for seed in manifest['root_sources'][role]):
            raise ValueError('battle origin namespace mismatch')
    if len(set(all_seeds))!=len(all_seeds):
        raise ValueError('origin run leaked between banks')
    file=directory/f'{split}.json.gz'
    if sha256_file(file)!=manifest['files'][file.name]:
        raise ValueError('battle bank hash mismatch')
    rows=json.loads(gzip.decompress(file.read_bytes()))
    if (len(rows)!=manifest['quotas'][split] or [r['source_seed'] for r in rows]!=manifest['root_sources'][split]
            or any(r['split']!=split or r['ordinal_in_source']!=2 for r in rows)
            or any(r['native_source_sha256']!=manifest['native_source_sha256']
                   or r['projection_sha256']!=manifest['projection_sha256'] for r in rows)
            or any(not {'monster_count','selector_memory'} <= r['checkpoint'].get('combat_checkpoint',{}).get(
                'game_state',{}).get('combat_state',{}).get('_internal',{}).keys() for r in rows)
            or len({r['sample_id'] for r in rows})!=len(rows)):
        raise ValueError('battle bank source/count/role mismatch')
    return rows,manifest
