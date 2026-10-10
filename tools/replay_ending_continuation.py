"""Strict first divergence of public Act4 decisions after a sealed boss flow."""
from __future__ import annotations

import argparse
import hashlib
import json
import os
from pathlib import Path

os.environ['CUDA_VISIBLE_DEVICES'] = '-1'

from sls.audit.card_parity import structured_differences
from sls.backends.simulator import SimulatorBackend
from sls.contracts import Action
from sls.curriculum import IRONCLAD_A20_HEART
from tools.verify_boss_flow_suffix import verify


def action_from_record(value):
    value = dict(value)
    value['metadata'] = dict(value.get('metadata', {}))
    return Action.from_dict(value)


def effective_rng(raw):
    # Combat owns streams until native commits them back at room completion.
    combat = raw.get('combat_checkpoint')
    return combat['rng'] if combat else raw['rng']


def replay(capture, build, entry, flow, *, diagnostic_continue=False):
    proof = verify(capture, build, entry, flow)
    stock = json.loads(capture.read_text())
    traces = json.loads(flow.read_text())
    results = []
    for row, previous in zip(stock['runs'], traces['runs'], strict=True):
        record = row['ending_continuation']
        if (record['initial_raw'] != row['act4_entry'] or not record['history']
                or record['history'][0]['before_raw'] != record['initial_raw']):
            raise ValueError('continuous entry witness differs')
        backend = SimulatorBackend(profile=IRONCLAD_A20_HEART)
        decision = backend.load_checkpoint(previous['native_final'])
        checkpoints = [backend.checkpoint()]
        steps = []
        first_difference = None
        selected_actions = []
        stop_reason = None
        for index, step in enumerate(record['history']):
            if index and record['history'][index-1]['after_raw'] != step['before_raw']:
                raise ValueError('public history has a discontinuous raw boundary')
            selected = action_from_record(step['selected_action'])
            legal = sorted(action_from_record(a).candidate_id for a in step['legal_actions'])
            if selected.candidate_id not in legal:
                raise ValueError('recorded original action was not legal')
            comparisons = dict(observation=structured_differences(step['observation'], decision.observation.to_dict()),
                               legal_actions=structured_differences(legal, sorted(a.candidate_id for a in decision.actions)),
                               rng=structured_differences(step['before_raw']['_rng'], effective_rng(backend.raw_state)))
            boundary = dict(index=index, comparisons=comparisons, selected_action=step['selected_action'],
                            native_checkpoint=backend.checkpoint())
            steps.append(boundary)
            if any(comparisons.values()):
                if first_difference is None:
                    first_difference = index
                if not diagnostic_continue:
                    break
            if selected.candidate_id not in {a.candidate_id for a in decision.actions}:
                stop_reason = 'RECORDED_PUBLIC_ACTION_UNAVAILABLE'
                break
            transition = backend.step(selected, validation_evidence=step.get('validation_evidence'))
            selected_actions.append((selected,step.get('validation_evidence')))
            checkpoints.append(backend.checkpoint())
            boundary['after_comparisons'] = dict(
                observation=structured_differences(step['next_observation'], transition.decision.observation.to_dict()),
                rng=structured_differences(step['after_raw']['_rng'], effective_rng(backend.raw_state)),
                terminated=structured_differences(step['terminated'], transition.terminated),
                truncated=structured_differences(step['truncated'], transition.truncated),
                success=structured_differences(step['info']['success'], transition.info['success']),
                reason=structured_differences(step['info']['reason'], transition.info['reason']))
            if any(boundary['after_comparisons'].values()):
                if first_difference is None:
                    first_difference = index
                if not diagnostic_continue:
                    break
            decision = transition.decision
        suffixes = []
        for start, state in enumerate(checkpoints):
            restored = SimulatorBackend(profile=IRONCLAD_A20_HEART)
            try:
                restored.load_checkpoint(state)
            except (ValueError,RuntimeError) as error:
                # Preserve rejected restores; do not drop an incomplete screen
                # or replay from an invented replacement origin.
                suffixes.append(dict(boundary=start,full_native_suffix_equal=False,
                                     restoration_error=type(error).__name__+': '+str(error),
                                     screen_info=state['screen_info'],replay_required=state['replay_required']))
                continue
            equal = restored.checkpoint() == state
            for offset,(action,evidence) in enumerate(selected_actions[start:],start+1):
                restored.step(action,validation_evidence=evidence)
                if restored.checkpoint() != checkpoints[offset]:
                    equal = False
                    break
            suffixes.append(dict(boundary=start,full_native_suffix_equal=equal))
        results.append(dict(seed=row['seed'], first_difference=first_difference, steps=steps,
                            executed_actions=len(checkpoints)-1, stock_decisions=len(record['history']),
                            stock_status=record['status'], native_checkpoints=checkpoints, native_suffixes=suffixes,
                            stop_reason=stop_reason,
                            complete_public_trajectory_equal=first_difference is None and len(steps)==len(record['history'])))
    return dict(schema='sls-ending-continuation-first-divergence-v1', runs=results,
                capture_sha256=proof['capture_sha256'], oracle_sha256=proof['oracle_sha256'],
                native_source_sha256=proof['native_source_sha256'], native_binary_sha256=proof['native_binary_sha256'],
                source_sha256=hashlib.sha256(Path(__file__).read_bytes()).hexdigest(),
                scope=('DIAGNOSTIC_RECORDED_ACTION_TRANSFER_WITH_ALL_DIFFERENCES_RETAINED'
                       if diagnostic_continue else 'FULL_PUBLIC_OBSERVATION_LEGAL_ACTIONS_RNG_AND_HORIZON_UNTIL_FIRST_DIFFERENCE'),
                training_gate='NOT_QUALIFIED', training_eligible=False, natural_trajectory=False)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    for name in ('capture','oracle-build','entry-report','flow-report','output'):
        parser.add_argument('--'+name, type=Path, required=True)
    parser.add_argument('--diagnostic-continue',action='store_true',help='Retain all differences and transfer available recorded actions')
    args = parser.parse_args()
    if args.output.exists():
        raise FileExistsError('refuse to overwrite continuation evidence')
    result = replay(args.capture,args.oracle_build,args.entry_report,args.flow_report,
                    diagnostic_continue=args.diagnostic_continue)
    with args.output.open('x', encoding='utf-8') as stream:
        json.dump(result,stream,indent=2)
    print(json.dumps([dict(seed=r['seed'],first_difference=r['first_difference'],executed_actions=r['executed_actions'])
                      for r in result['runs']]))
    return 0 if all(r['complete_public_trajectory_equal'] and all(s['full_native_suffix_equal'] for s in r['native_suffixes'])
                    for r in result['runs']) else 2


if __name__ == '__main__':
    raise SystemExit(main())
