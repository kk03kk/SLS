import copy
import json
from pathlib import Path

import pytest

from tools.capture_held_key_map import validate

MANIFEST = Path('native/oracle/resources/spirecomm/parity/fullrun-held-key-map-r1.json')


def test_stock_map_manifest_declares_exact_same_seed_pairs():
    manifest = json.loads(MANIFEST.read_text())
    validate(manifest)
    assert manifest['repeated_seed_policy'] == 'EXPLICIT_SAME_SEED_PAIRED_FLAGS_AND_ACTS'
    assert len({r['seed'] for r in manifest['scenes']}) == 1


@pytest.mark.parametrize('change', ['namespace', 'seed', 'scene', 'injection', 'bool', 'eligibility', 'policy', 'float_act'])
def test_stock_map_manifest_rejects_different_or_injected_inputs(change):
    manifest = copy.deepcopy(json.loads(MANIFEST.read_text()))
    if change == 'namespace':
        manifest['seed_namespace'] = [131200411, 131200412]
    elif change == 'seed':
        manifest['scenes'][0]['seed'] += 1
    elif change == 'scene':
        manifest['scenes'][0] = manifest['scenes'][1]
    elif change == 'injection':
        manifest['scenes'][0]['burning_elite_x'] = 3
    elif change == 'bool':
        manifest['scenes'][0]['emerald_key'] = 0
    elif change == 'policy':
        manifest['repeated_seed_policy'] = 'IMPLICIT_REUSE'
    elif change == 'float_act':
        manifest['scenes'][0]['act'] = 2.0
    else:
        manifest['training_eligible'] = True
    with pytest.raises(ValueError):
        validate(manifest)
