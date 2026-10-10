"""Actual stock replay of a version-bound normal-start frozen-model route."""
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
from sls.backends.original import OriginalBackend
from sls.backends.original.session import OriginalSession
from sls.contracts import Action
from sls.curriculum import IRONCLAD_A20_HEART
from tools.capture_original_card_batch import _write_completion


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--reference-report',type=Path,required=True)
    parser.add_argument('--seed',type=int,required=True)
    parser.add_argument('--output',type=Path,required=True)
    parser.add_argument('--device',choices=('cpu',),default='cpu')
    args = parser.parse_args()
    if args.output.exists():
        raise FileExistsError('refuse to overwrite original reference evidence')
    session = OriginalSession()
    session.transport.send('ready')
    reference = json.loads(args.reference_report.read_text())
    if not reference.get('execution_complete') or reference.get('execution_error'):
        raise ValueError('incomplete native reference')
    rows = [r for r in reference['runs'] if r['seed'] == args.seed]
    if len(rows) != 1:
        raise ValueError('seed not uniquely owned by reference')
    row = rows[0]
    public = ROOT/row['public_history']
    private = ROOT/row['private_history']
    if (hashlib.sha256(public.read_bytes()).hexdigest() != row['public_sha256']
            or hashlib.sha256(private.read_bytes()).hexdigest() != row['private_sha256']):
        raise ValueError('reference history identity mismatch')
    expected = [json.loads(line) for line in public.read_text().splitlines()]
    checkpoints = [json.loads(line)['checkpoint'] for line in private.read_text().splitlines()]
    if not 2 <= len(expected) <= 257 or len(expected) != len(checkpoints):
        raise ValueError('reference is not a bounded complete prefix')
    session.payload = session.receive_ready()
    backend = OriginalBackend(session=session,profile=IRONCLAD_A20_HEART)
    result = dict(schema='sls-stock-frozen-key-reference-v1',seed=args.seed,
                  reference_sha256=hashlib.sha256(args.reference_report.read_bytes()).hexdigest(),
                  public_reference_sha256=row['public_sha256'],private_reference_sha256=row['private_sha256'],
                  model_sha256=reference['model_sha256'],source_sha256=hashlib.sha256(Path(__file__).read_bytes()).hexdigest(),
                  native_reference_source_sha256=reference['current_native_source_sha256'],
                  scope='ACTUAL_NORMAL_START_STOCK_EXECUTED_REFERENCE_ACTIONS_NOT_STOCK_MODEL_INFERENCE',
                  training_eligible=False,training_gate='NOT_QUALIFIED',steps=[])
    args.output.open('x',encoding='utf-8').close()

    def flush():
        args.output.write_text(json.dumps(result,indent=2)+'\n',encoding='utf-8')

    try:
        decision = backend.reset(args.seed)
        for index,(record,state) in enumerate(zip(expected,checkpoints,strict=True)):
            raw = backend.raw_payload
            if raw.get('_oracle_mode') != 'validation' or raw.get('_parity_scenario'):
                raise ValueError('reference replay requires an unconditioned stock start')
            native_rng = state['combat_checkpoint']['rng'] if state.get('combat_checkpoint') else state['rng']
            differences = dict(observation=structured_differences(record['observation'],decision.observation.to_dict()),
                               legal_actions=structured_differences(sorted(Action.from_dict(a).candidate_id for a in record['candidate_actions']),
                                                                   sorted(a.candidate_id for a in decision.actions)),
                               rng=structured_differences(native_rng,raw['_rng']))
            step = dict(boundary=index,observation=decision.observation.to_dict(),legal_actions=[a.to_dict() for a in decision.actions],
                        before_raw=raw,comparisons=differences)
            result['steps'].append(step)
            flush()
            if any(differences.values()):
                result.update(status='FIRST_DIVERGENCE',first_divergence=index)
                break
            if record.get('executed_action') is None:
                result.update(status='REFERENCE_PREFIX_COMPLETE',actual_final_run=decision.observation.to_dict()['run'],game_failure=False)
                break
            action = Action.from_dict(record['executed_action'])
            step['selected_action'] = action.to_dict()
            flush()
            transition = backend.step(action)
            step.update(commands=backend.last_executed_commands,validation_evidence=backend.last_validation_evidence,
                        after_raw=backend.raw_payload,next_observation=transition.decision.observation.to_dict(),
                        reward=transition.reward,info=transition.info,terminated=transition.terminated,truncated=transition.truncated)
            following = expected[index+1]
            step['transition_comparisons'] = dict(reward=structured_differences(following['reward_from_previous'],transition.reward),
                                                  terminal=structured_differences(following['terminal'],transition.terminated),
                                                  reason=structured_differences(following['terminal_reason'],transition.info.get('reason') or None),
                                                  success=structured_differences(following['success'],transition.info['success']))
            flush()
            if any(step['transition_comparisons'].values()) or transition.truncated:
                result.update(status='FIRST_TRANSITION_DIVERGENCE',first_divergence=index)
                break
            decision = transition.decision
        backend.return_to_menu()
        result['execution_complete'] = True
        flush()
        _write_completion(0)
        return 0
    except BaseException as error:
        result['execution_error'] = type(error).__name__+': '+str(error)
        flush()
        _write_completion(2,result['execution_error'])
        return 2


if __name__ == '__main__':
    raise SystemExit(main())
