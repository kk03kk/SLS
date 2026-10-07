"""Strict, state-preserving A20 Act1+Act2 completed-endpoint continuation."""
from __future__ import annotations

import argparse
import json
from pathlib import Path

from sls.rl.preparation import read_config
from sls.rl.training_contract import (
    ROOT,
    native_source_digest,
    sha256_file,
    training_implementation_digest,
)
from tools.initialize_act1_continuation import initialize as initialize_parent


def initialize(config_path: Path, *, root: Path = ROOT) -> dict:
    import torch

    config = read_config(config_path)
    run = config['run']
    parent = (root / run['continuation_from']).resolve()
    original = read_config(parent / 'training-config.toml')
    if (set(config) - set(original)
            or ('warm_start' in config and config['warm_start'] != original.get('warm_start'))):
        raise ValueError('unapproved continuation configuration section')
    manifest = json.loads((parent / 'run-manifest.json').read_text(encoding='utf-8'))
    bundle = json.loads((parent / 'training-bundle.json').read_text(encoding='utf-8'))
    endpoint = json.loads((parent / 'endpoint-evaluation.json').read_text(encoding='utf-8'))
    if any(endpoint.get('result', {}).get(key, -1) != 0 for key in
           ('backend_errors', 'backend_truncations', 'step_limits', 'cycle_limits', 'timeouts')):
        raise ValueError('fixed endpoint evaluation is incomplete or unhealthy')
    if (run['profile'] != 'IRONCLAD_A20_ACT2'
            or original['run']['profile'] != run['profile']
            or manifest['native_source_sha256'] != native_source_digest()
            or manifest['training_implementation_sha256'] != training_implementation_digest()
            or run.get('continuation_selection_evidence') != 'completed-endpoint'):
        raise ValueError('requires same-source completed A20 Act2 endpoint')
    for name in ('final.pt', 'latest.pt', 'endpoint-evaluation.json'):
        if sha256_file(parent / name) != bundle['files'][name]:
            raise ValueError('endpoint bundle evidence changed: ' + name)
    if endpoint['checkpoint_sha256'] != bundle['files']['final.pt']:
        raise ValueError('evaluation is not the fixed endpoint')
    if ((root / run['development_reference_checkpoint']).resolve() != parent / 'final.pt'
            or run['development_reference_sha256'] != bundle['files']['final.pt']
            or run['development_reference_profile'] != 'IRONCLAD_A20_ACT2'):
        raise ValueError('long-run development reference must be the chosen Act2 endpoint')
    left = torch.load(parent / 'latest.pt', map_location='cpu', weights_only=False)
    right = torch.load(parent / 'final.pt', map_location='cpu', weights_only=False)
    from tools.verify_act12_diagnostics import assert_same
    for key in ('model', 'optimizer', 'trainer', 'environments', 'python_rng', 'torch_rng', 'cuda_rng'):
        assert_same(left[key], right[key])
    if endpoint['checkpoint_environment_steps'] != left['trainer']['environment_steps']:
        raise ValueError('endpoint evaluation step mismatch')
    if config['stages']['train']['target_environment_steps'] != left['trainer']['environment_steps'] + 20_000_000:
        raise ValueError('registered continuation requires exactly 20M additional decisions')
    # Existing Act1 half-LR and source-transition permissions are NOT inherited.
    if config['ppo'] != original['ppo'] or config['model'] != original['model']:
        raise ValueError('Act2 continuation cannot change learning recipe')
    return initialize_parent(config_path, root=root, _act2_endpoint=True)


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--config', type=Path, required=True)
    args = parser.parse_args()
    print(json.dumps(initialize(args.config), indent=2))


if __name__ == '__main__':
    main()
