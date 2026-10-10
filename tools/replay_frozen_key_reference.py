"""CPU replay of a sealed real stock reference prefix, including failed prefixes."""
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

from sls.audit.card_parity import structured_differences
from sls.audit.stock_clock import verify_sealed_oracle
from sls.backends.simulator import SimulatorBackend, native
from sls.contracts import Action
from sls.curriculum import IRONCLAD_A20_HEART
from sls.rl.training_contract import native_source_digest
from tools.replay_ending_continuation import effective_rng
from tools.run_original_canary import original_runtime_paths


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def persisted(value):
    return json.dumps(value,sort_keys=True,separators=(',',':'))


def verify_capture(capture,reference_path,oracle):
    source = json.loads(capture.read_text())
    reference = json.loads(reference_path.read_text())
    launch = json.loads(capture.with_suffix('.launch.json').read_text())
    build = json.loads(oracle.with_suffix('.build.json').read_text())
    verify_sealed_oracle(oracle,build)
    _,game = original_runtime_paths(None)
    if sha(game/'desktop-1.0.jar') != build['dependencies']['game']:
        raise ValueError('stock game identity mismatch')
    if (source.get('schema') != 'sls-stock-frozen-key-reference-v1'
            or not source.get('execution_complete') or source.get('execution_error')
            or source.get('status') not in {'FIRST_DIVERGENCE','FIRST_TRANSITION_DIVERGENCE','REFERENCE_PREFIX_COMPLETE'}
            or not 1 <= len(source.get('steps',[])) <= 257
            or source.get('scope') != 'ACTUAL_NORMAL_START_STOCK_EXECUTED_REFERENCE_ACTIONS_NOT_STOCK_MODEL_INFERENCE'):
        raise ValueError('invalid or incomplete stock prefix')
    if (reference.get('schema') != 'sls-frozen-three-key-route-probe-v1'
            or not reference.get('execution_complete') or reference.get('execution_error')
            or source.get('reference_sha256') != sha(reference_path)
            or source.get('model_sha256') != reference.get('model_sha256')):
        raise ValueError('reference identity mismatch')
    if source['source_sha256'] != sha(ROOT/'tools/capture_frozen_key_reference.py'):
        raise ValueError('capture producer version mismatch')
    if (source['native_reference_source_sha256'] != reference['current_native_source_sha256']
            or reference['current_native_source_sha256'] != native.NATIVE_SOURCE_SHA256
            or native.NATIVE_SOURCE_SHA256 != native_source_digest()
            or reference['current_native_binary_sha256'] != sha(Path(native.__file__))):
        raise ValueError('native environment identity mismatch; migration is not implicit')
    if (launch['mode'] != 'validation' or launch['oracle_sha256'] != build['output_sha256']
            or launch['recovery_status'] != 'RECOVERED' or launch.get('execution_error')
            or launch['completion']['exit_code'] != 0):
        raise ValueError('stock launch/recovery identity failure')
    rows = [r for r in reference['runs'] if r['seed'] == source['seed']]
    if len(rows) != 1:
        raise ValueError('reference seed is not uniquely owned')
    row = rows[0]
    public,private = ROOT/row['public_history'],ROOT/row['private_history']
    if (sha(public) != row['public_sha256'] or sha(private) != row['private_sha256']
            or source['public_reference_sha256'] != row['public_sha256']
            or source['private_reference_sha256'] != row['private_sha256']):
        raise ValueError('reference history identity mismatch')
    expected = [json.loads(line) for line in public.read_text().splitlines()]
    checkpoints = [json.loads(line)['checkpoint'] for line in private.read_text().splitlines()]
    if len(expected) != len(checkpoints):
        raise ValueError('public/private reference boundary alignment failure')
    if len(source['steps']) > len(expected):
        raise ValueError('stock prefix exceeds actual reference')
    failures = []
    for index,step in enumerate(source['steps']):
        if step['boundary'] != index or step['before_raw'].get('_oracle_mode') != 'validation' or step['before_raw'].get('_parity_scenario'):
            raise ValueError('stock prefix is not a normal contiguous validation history')
        if 'selected_action' in step:
            if (index+1 >= len(expected)
                    or step['selected_action'] != expected[index].get('executed_action')
                    or (index+1 < len(source['steps']) and step['next_observation'] != source['steps'][index+1]['observation'])
                    or (index+1 == len(source['steps']) and source['status'] != 'FIRST_TRANSITION_DIVERGENCE')):
                raise ValueError('executed action or public history alignment failure')
        elif index != len(source['steps'])-1:
            raise ValueError('missing executed action inside stock prefix')
        state = checkpoints[index]
        reference_rng = state['combat_checkpoint']['rng'] if state.get('combat_checkpoint') else state['rng']
        computed = dict(observation=structured_differences(expected[index]['observation'],step['observation']),
                        legal_actions=structured_differences(sorted(Action.from_dict(a).candidate_id for a in expected[index]['candidate_actions']),
                                                            sorted(Action.from_dict(a).candidate_id for a in step['legal_actions'])),
                        rng=structured_differences(reference_rng,step['before_raw']['_rng']))
        if computed != step['comparisons']:
            raise ValueError('recorded comparison disagrees with sealed actual data')
        if any(computed.values()):
            failures.append(('FIRST_DIVERGENCE',index))
        if 'selected_action' in step:
            following = expected[index+1]
            computed_transition = dict(reward=structured_differences(following['reward_from_previous'],step['reward']),
                                       terminal=structured_differences(following['terminal'],step['terminated']),
                                       reason=structured_differences(following['terminal_reason'],step['info'].get('reason') or None),
                                       success=structured_differences(following['success'],step['info']['success']))
            if computed_transition != step['transition_comparisons']:
                raise ValueError('recorded transition comparison disagrees with sealed actual data')
            if any(computed_transition.values()) or step['truncated']:
                failures.append(('FIRST_TRANSITION_DIVERGENCE',index))
    if source['status'] != 'REFERENCE_PREFIX_COMPLETE' and (
        failures != [(source['status'],source.get('first_divergence'))]
        or source['first_divergence'] != source['steps'][-1]['boundary']
    ):
        raise ValueError('first-divergence status does not describe actual comparisons')
    if source['status'] == 'REFERENCE_PREFIX_COMPLETE' and (
        failures or source.get('first_divergence') is not None
        or len(source['steps']) != len(expected) or expected[-1].get('executed_action') is not None
        or source.get('actual_final_run') != source['steps'][-1]['observation']['run']
    ):
        raise ValueError('completed stock prefix does not match reference stop')
    return source,reference,launch,build


def replay_prefix(source,*,condition_on_stock_clock=False):
    backend = SimulatorBackend(profile=IRONCLAD_A20_HEART)
    decision = backend.reset(source['seed'])
    states = [backend.checkpoint()]
    rows,actions,evidences = [],[],[]
    for step in source['steps']:
        differences = dict(observation=structured_differences(step['observation'],decision.observation.to_dict()),
                           legal_actions=structured_differences(sorted(Action.from_dict(a).candidate_id for a in step['legal_actions']),
                                                               sorted(a.candidate_id for a in decision.actions)),
                           rng=structured_differences(step['before_raw']['_rng'],effective_rng(backend.raw_state)))
        rows.append(dict(boundary=step['boundary'],differences=differences))
        if any(differences.values()):
            break
        if 'selected_action' in step:
            action = Action.from_dict(step['selected_action'])
            evidence = step['validation_evidence'] if condition_on_stock_clock else None
            transition = backend.step(action,validation_evidence=evidence)
            rows[-1]['transition_differences'] = dict(
                observation=structured_differences(step['next_observation'],transition.decision.observation.to_dict()),
                reward=structured_differences(step['reward'],transition.reward),
                terminated=structured_differences(step['terminated'],transition.terminated),
                truncated=structured_differences(step['truncated'],transition.truncated),
                reason=structured_differences(step['info']['reason'],transition.info['reason']),
                success=structured_differences(step['info']['success'],transition.info['success']))
            actions.append(action)
            evidences.append(evidence)
            states.append(backend.checkpoint())
            decision = transition.decision
            if any(rows[-1]['transition_differences'].values()):
                break
    suffixes = []
    for start,state in enumerate(states):
        restored = SimulatorBackend(profile=IRONCLAD_A20_HEART)
        try:
            restored.load_checkpoint(state)
            equal = persisted(restored.checkpoint()) == persisted(state)
            for offset,(action,evidence) in enumerate(zip(actions[start:],evidences[start:],strict=True),start+1):
                restored.step(action,validation_evidence=evidence)
                equal = equal and persisted(restored.checkpoint()) == persisted(states[offset])
            suffixes.append(dict(boundary=start,full_native_suffix_equal=equal))
        except (ValueError,RuntimeError) as error:
            suffixes.append(dict(boundary=start,full_native_suffix_equal=False,error=type(error).__name__+': '+str(error)))
    failed = [r['boundary'] for r in rows if any(r['differences'].values()) or any(r.get('transition_differences',{}).values())]
    return dict(conditioned_on_stock_clock=condition_on_stock_clock,rows=rows,failed_boundaries=failed,
                observed_stock_prefix_fully_matched=len(rows)==len(source['steps']) and not failed,
                executed_replay_actions=len(actions),native_checkpoints=states,suffixes=suffixes,
                all_full_native_suffixes_equal=all(s['full_native_suffix_equal'] for s in suffixes),
                replay_final_public_run=decision.observation.to_dict()['run'])


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--capture',type=Path,required=True)
    parser.add_argument('--reference-report',type=Path,required=True)
    parser.add_argument('--oracle',type=Path,required=True)
    parser.add_argument('--output',type=Path,required=True)
    parser.add_argument('--condition-on-stock-clock',action='store_true')
    parser.add_argument('--device',choices=('cpu',),default='cpu')
    args = parser.parse_args()
    if args.output.exists():
        raise FileExistsError('refuse to overwrite replay evidence')
    source,reference,launch,build = verify_capture(args.capture,args.reference_report,args.oracle)
    result = replay_prefix(source,condition_on_stock_clock=args.condition_on_stock_clock)
    result.update(schema='sls-stock-frozen-key-reference-replay-v1',
                  scope='OBSERVED_STOCK_PREFIX_ONLY_NOT_A20H_QUALIFICATION',
                  source_sha256=sha(Path(__file__)),capture_sha256=sha(args.capture),
                  reference_sha256=sha(args.reference_report),stock_capture_status=source['status'],
                  native_source_sha256=reference['current_native_source_sha256'],
                  native_binary_sha256=reference['current_native_binary_sha256'],
                  oracle_sha256=build['output_sha256'],original_jar_sha256=build['dependencies']['game'],
                  launch=launch,device='cpu',execution_complete=True,training=False,training_eligible=False,training_gate='NOT_QUALIFIED')
    args.output.parent.mkdir(parents=True,exist_ok=True)
    with args.output.open('x',encoding='utf-8') as stream:
        json.dump(result,stream,indent=2)
    print(json.dumps({k:result[k] for k in ['observed_stock_prefix_fully_matched','failed_boundaries','executed_replay_actions','all_full_native_suffixes_equal']}))
    # A known stock divergence remains a diagnostic outcome, not a game loss or matching certificate.
    return 0 if result['all_full_native_suffixes_equal'] else 2


if __name__ == '__main__':
    raise SystemExit(main())
