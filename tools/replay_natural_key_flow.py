"""Verify and replay a bounded normal-start key probe, including every suffix."""
from __future__ import annotations

import argparse
import hashlib
import json
import os
import sys
from pathlib import Path

os.environ['CUDA_VISIBLE_DEVICES'] = '-1'
ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT))
sys.path.insert(0,str(ROOT/'src'))

from sls.audit.stock_clock import verify_sealed_oracle
from sls.backends.simulator import SimulatorBackend, native
from sls.contracts import Action
from sls.curriculum import IRONCLAD_A20_HEART
from sls.rl.training_contract import native_source_digest
from tools.replay_ending_continuation import effective_rng
from tools.run_original_canary import original_runtime_paths


def persisted(value):
    return json.dumps(value, sort_keys=True, separators=(',', ':'))


def verify_capture(capture, oracle):
    source = json.loads(capture.read_text())
    launch = json.loads(capture.with_suffix('.launch.json').read_text())
    build = json.loads(oracle.with_suffix('.build.json').read_text())
    verify_sealed_oracle(oracle, build)
    _, game = original_runtime_paths(None)
    if hashlib.sha256((game/'desktop-1.0.jar').read_bytes()).hexdigest() != build['dependencies']['game']:
        raise ValueError('stock game identity mismatch')
    if (source.get('schema') != 'sls-natural-key-first-divergence-v1' or not source.get('natural_start')
            or not source.get('execution_complete') or source.get('execution_error')
            or source.get('status') not in {'NATURAL_TERMINAL','DIAGNOSTIC_LIMIT_UNFINISHED'}
            or source.get('first_divergence') is not None or not source.get('steps')):
        raise ValueError('incomplete or divergent natural probe is not a matching trajectory')
    if (launch['mode'] != 'validation' or launch['oracle_sha256'] != build['output_sha256']
            or launch['recovery_status'] != 'RECOVERED' or launch['completion']['exit_code'] != 0):
        raise ValueError('natural capture launch/recovery identity failure')
    if (source['native_source_sha256'] != native.NATIVE_SOURCE_SHA256
            or native.NATIVE_SOURCE_SHA256 != native_source_digest()
            or source['native_binary_sha256'] != hashlib.sha256(Path(native.__file__).read_bytes()).hexdigest()):
        raise ValueError('native identity mismatch; do not rebind historical evidence')
    steps = source['steps']
    for index, step in enumerate(steps):
        if step['boundary'] != index or any(step['comparisons'].values()):
            raise ValueError('invalid or differing public boundary')
        raw = step['before_raw']
        if raw.get('_oracle_mode') != 'validation' or raw.get('_parity_scenario'):
            raise ValueError('conditioned scenario cannot count as a normal-start probe')
        if index and raw != steps[index-1]['after_raw']:
            raise ValueError('discontinuous stock raw history')
        if 'selected_action' in step:
            if Action.from_dict(step['selected_action']).candidate_id not in {
                Action.from_dict(a).candidate_id for a in step['legal_actions']
            } or any(step['transition_comparisons'].values()):
                raise ValueError('invalid or differing recorded transition')
        elif index != len(steps)-1:
            raise ValueError('missing intermediate action')
    if 'selected_action' in steps[-1]:
        raise ValueError('missing final observed boundary')
    if source['status'] == 'DIAGNOSTIC_LIMIT_UNFINISHED' and (source.get('game_failure') is not False
                                                            or len(steps) != source['max_actions']+1):
        raise ValueError('invalid diagnostic truncation')
    return source, launch, build


def replay(capture, oracle):
    source, launch, build = verify_capture(capture, oracle)
    backend = SimulatorBackend(profile=IRONCLAD_A20_HEART)
    decision = backend.reset(source['seed'])
    states = []
    steps = source['steps']
    for step in steps:
        if (decision.observation.to_dict() != step['observation']
                or sorted(a.candidate_id for a in decision.actions) != sorted(
                    Action.from_dict(a).candidate_id for a in step['legal_actions'])
                or effective_rng(backend.raw_state) != step['before_raw']['_rng']
                or persisted(backend.checkpoint()) != persisted(step['native_checkpoint'])):
            raise ValueError('fresh normal-start replay boundary differs')
        states.append(backend.checkpoint())
        if 'selected_action' in step:
            transition = backend.step(Action.from_dict(step['selected_action']),
                                      validation_evidence=step['validation_evidence'])
            if (transition.reward != step['reward'] or transition.info['reason'] != step['info']['reason']
                    or transition.info['success'] != step['info']['success']
                    or transition.terminated != step['terminated'] or transition.truncated != step['truncated']
                    or transition.decision.observation.to_dict() != step['next_observation']):
                raise ValueError('fresh normal-start transition differs')
            decision = transition.decision
    if source['status'] == 'NATURAL_TERMINAL':
        if not decision.terminal or source.get('terminal_info') != steps[-2]['info']:
            raise ValueError('declared terminal does not match actual native horizon')
    elif decision.terminal:
        raise ValueError('terminal outcome cannot be relabeled as diagnostic truncation')
    suffixes = []
    for start, state in enumerate(states):
        restored = SimulatorBackend(profile=IRONCLAD_A20_HEART)
        restored.load_checkpoint(state)
        equal = restored.checkpoint() == state
        for offset, step in enumerate(steps[start:-1], start+1):
            restored.step(Action.from_dict(step['selected_action']),validation_evidence=step['validation_evidence'])
            equal = equal and restored.checkpoint() == states[offset]
        suffixes.append(dict(boundary=start, full_native_suffix_equal=equal))
    return dict(schema='sls-natural-key-replay-v1', seed=source['seed'], capture_status=source['status'],
                capture_sha256=hashlib.sha256(capture.read_bytes()).hexdigest(),
                oracle_sha256=build['output_sha256'],stock_game_sha256=build['dependencies']['game'],
                launch_sha256=hashlib.sha256(capture.with_suffix('.launch.json').read_bytes()).hexdigest(),
                native_source_sha256=native.NATIVE_SOURCE_SHA256,native_binary_sha256=source['native_binary_sha256'],
                source_sha256=hashlib.sha256(Path(__file__).read_bytes()).hexdigest(),
                public_boundaries=len(states), executed_actions=len(states)-1, suffixes=suffixes,
                actual_key_actions=[dict(boundary=s['boundary'], action=s['selected_action']) for s in steps[:-1]
                                    if s['selected_action']['kind'] in {'RECALL','TAKE_BLUE_KEY'}],
                timing_conditioned_actions=[s['boundary'] for s in steps[:-1] if s['validation_evidence']],
                final_public_run=steps[-1]['observation']['run'], final_screen=steps[-1]['observation']['screen'],
                natural_start=True,training_eligible=False,training_gate='NOT_QUALIFIED',diagnostic_not_winrate=True)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--capture', type=Path, required=True)
    parser.add_argument('--oracle', type=Path, required=True)
    parser.add_argument('--output', type=Path, required=True)
    parser.add_argument('--device', choices=('cpu',), default='cpu')
    args = parser.parse_args()
    if args.output.exists():
        raise FileExistsError('refuse to overwrite replay evidence')
    result = replay(args.capture, args.oracle)
    with args.output.open('x', encoding='utf-8') as stream:
        json.dump(result, stream, indent=2)
    print({k:result[k] for k in ('seed','capture_status','public_boundaries','actual_key_actions','final_public_run')})
    return 0 if all(s['full_native_suffix_equal'] for s in result['suffixes']) else 2


if __name__ == '__main__':
    raise SystemExit(main())
