"""Record one frozen stock actor's system script, then replay it in native."""

from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path

from sls.backends.original import ORIGINAL_EXECUTION_CONTRACT
from sls.backends.original.session import OriginalSession
from sls.curriculum import IRONCLAD_A20_ACT2
from tools.capture_act2_production_batch import ProductionBackend
from tools.capture_original_card_batch import _write_completion
from tools.run_original_canary import original_runtime_paths


class ValidationPolicyBackend(ProductionBackend):
    @staticmethod
    def require_isolation(raw):
        if raw.get('_oracle_mode') != 'validation' or '_rng' not in raw:
            raise ValueError('controlled stock policy requires validation RNG evidence')
        combat = raw['game_state'].get('combat_state')
        if combat and '_stock_direct' not in raw:
            raise ValueError('controlled combat lacks independent stock objects')

    def checkpoint(self):
        return {'stock_payload': self.raw_payload,
                'previous_action_validation_evidence': self.last_validation_evidence}


def convert_trajectory(path: Path, output: Path) -> int:
    """Keep the complete stock payload and semantic script; do not alter values."""
    records = [json.loads(line) for line in path.read_text(encoding='utf-8').splitlines()]
    boundaries = records[1:]
    with output.open('x', encoding='utf-8') as stream:
        for index, record in enumerate(boundaries):
            state = record['diagnostic_state']
            stream.write(json.dumps({'boundary': index, 'observation': record['observation'],
                'actions': record['candidate_actions'], 'terminal': record['terminal'],
                'requested_action': record['chosen_action'], 'stock_raw': state['stock_payload'],
                'previous_action_validation_evidence': state['previous_action_validation_evidence']},
                sort_keys=True) + '\n')
    return len(boundaries)


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--artifact', type=Path, required=True)
    parser.add_argument('--output', type=Path, required=True)
    parser.add_argument('--seeds', type=int, nargs='+', required=True)
    args = parser.parse_args()
    if (args.output.exists() or len(set(args.seeds)) != len(args.seeds)
            or any(seed not in range(131100063, 131100072) for seed in args.seeds)):
        raise ValueError('requires a new output and the fixed nine system seeds')
    session = OriginalSession()
    session.transport.send('ready')
    import torch

    from sls.diagnostics import capture_policy_trajectory
    from sls.runtime import load_policy_artifact

    torch.set_num_threads(1)
    artifact = load_policy_artifact(args.artifact, device='cpu')
    if (artifact.metadata.environment_profile['profile_id'] != 'IRONCLAD_A20_ACT1'
            or artifact.metadata.model_sha256 !=
                'ed9068343c8d628a13595a3919d8a15c5f9a19840be1042e07e5f12fcc8ca30c'):
        raise ValueError('reference must be the frozen90 model with Act1 training provenance')
    session.payload = session.receive_ready()
    _, game = original_runtime_paths(None)
    identity = {'profile_id': 'IRONCLAD_A20_ACT2', 'trained_profile_id': 'IRONCLAD_A20_ACT1',
                'evaluation_contract': 'sls-frozen-act1-reference-act2-v1',
                'stock_jar_sha256': hashlib.sha256((game / 'desktop-1.0.jar').read_bytes()).hexdigest(),
                'oracle_mode': 'validation',
                'frozen_model_sha256': artifact.metadata.model_sha256}
    result = {'schema': 'sls-act2-stock-system-capture-v1', 'runs': [],
              'initial_state': 'NORMAL_A20_NEOW', 'expected_source': 'STOCK_RAW_OBJECTS_AND_ACTIONS',
              'original_execution_contract': ORIGINAL_EXECUTION_CONTRACT,
              'script_contract': 'sls-stock-recorded-frozen-policy-v1', 'environment': identity,
              'scope': 'Controlled validation only; native replays stock actions, never runs its own actor'}
    try:
        backend = ValidationPolicyBackend(session=session, profile=IRONCLAD_A20_ACT2)
        for seed in args.seeds:
            output = args.output.with_name(args.output.stem + f'-{seed}.jsonl')
            trajectory = output.with_suffix('.actor.jsonl')
            if output.exists() or trajectory.exists() or output.with_suffix('.actions.jsonl').exists():
                raise FileExistsError('refuse to overwrite stock evidence')
            row = capture_policy_trajectory(backend, artifact, backend_name='original', seed=seed,
                output=trajectory, journal=output.with_suffix('.actions.jsonl'), max_actions=4096,
                diagnostic_state=True, environment_identity=identity)
            if not row['terminal']:
                raise RuntimeError('incomplete stock-controlled system trajectory')
            count = convert_trajectory(trajectory, output)
            result['runs'].append({'seed': seed, 'stock_capture': output.as_posix(),
                'script_action_unavailable': None, 'boundaries': count, 'terminal': True,
                'reached_act': row['act'], 'sha256': hashlib.sha256(output.read_bytes()).hexdigest(),
                'actor_trajectory_sha256': hashlib.sha256(trajectory.read_bytes()).hexdigest(),
                'script_sha256': hashlib.sha256(output.read_bytes()).hexdigest()})
            args.output.write_text(json.dumps(result, indent=2) + '\n', encoding='utf-8')
        backend.return_to_menu()
        result['execution_complete'] = True
        args.output.write_text(json.dumps(result, indent=2) + '\n', encoding='utf-8')
        _write_completion(0)
        return 0
    except BaseException as error:
        result['execution_error'] = f'{type(error).__name__}: {error}'
        with args.output.with_suffix('.failure.json').open('x', encoding='utf-8') as stream:
            json.dump(session.payload, stream, indent=2)
        args.output.write_text(json.dumps(result, indent=2) + '\n', encoding='utf-8')
        _write_completion(2, result['execution_error'])
        return 2


if __name__ == '__main__':
    raise SystemExit(main())
