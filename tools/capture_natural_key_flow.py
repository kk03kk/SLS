"""Bounded normal-start stock/native first-divergence probe; no state writes."""
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
from sls.audit.natural_key_policy import choose_natural_key_action
from sls.backends.original import OriginalBackend
from sls.backends.original.session import OriginalSession
from sls.curriculum import IRONCLAD_A20_HEART
from tools.capture_original_card_batch import _write_completion


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--seed', type=int, required=True)
    parser.add_argument('--output', type=Path, required=True)
    parser.add_argument('--max-actions', type=int, default=96)
    parser.add_argument('--device', choices=('cpu',), default='cpu')
    parser.add_argument('--probe', choices=('ruby-blue','emerald'), default='ruby-blue')
    args = parser.parse_args()
    if not 1 <= args.max_actions <= 256:
        raise ValueError('invalid diagnostic limit')
    if args.output.exists():
        raise FileExistsError('refuse to overwrite natural-flow evidence')
    # CommunicationMod's initial deadline is 10s. Send its handshake before
    # loading native provenance and validating evidence paths.
    session = OriginalSession()
    session.transport.send('ready')
    from sls.backends.simulator import SimulatorBackend, native
    from sls.rl.training_contract import native_source_digest
    from tools.replay_ending_continuation import effective_rng
    if args.probe == 'emerald':
        from sls.audit.natural_emerald_policy import choose_natural_emerald_action
        select_action = choose_natural_emerald_action
        policy_file = ROOT/'src/sls/audit/natural_emerald_policy.py'
    else:
        select_action = choose_natural_key_action
        policy_file = ROOT/'src/sls/audit/natural_key_policy.py'
    session.payload = session.receive_ready()
    if native.NATIVE_SOURCE_SHA256 != native_source_digest():
        raise ValueError('native source identity mismatch')
    result = dict(schema='sls-natural-key-first-divergence-v1', seed=args.seed,
                  native_source_sha256=native.NATIVE_SOURCE_SHA256,
                  native_binary_sha256=hashlib.sha256(Path(native.__file__).read_bytes()).hexdigest(),
                  source_sha256=hashlib.sha256(Path(__file__).read_bytes()).hexdigest(),
                  policy_sha256=hashlib.sha256(policy_file.read_bytes()).hexdigest(), probe=args.probe,
                  natural_start=True, device=args.device, training_eligible=False, training_gate='NOT_QUALIFIED',
                  diagnostic_not_winrate=True, max_actions=args.max_actions, steps=[])
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.open('x', encoding='utf-8').close()

    def flush():
        args.output.write_text(json.dumps(result, indent=2)+'\n', encoding='utf-8')

    backend = OriginalBackend(session=session, profile=IRONCLAD_A20_HEART)
    simulator = SimulatorBackend(profile=IRONCLAD_A20_HEART)
    try:
        decision = backend.reset(args.seed)
        predicted = simulator.reset(args.seed)
        for index in range(args.max_actions+1):
            raw = backend.raw_payload
            if raw.get('_oracle_mode') != 'validation' or raw.get('_parity_scenario'):
                raise ValueError('natural probe requires unconditioned validation run')
            comparison = dict(observation=structured_differences(decision.observation.to_dict(), predicted.observation.to_dict()),
                              legal_actions=structured_differences(sorted(a.candidate_id for a in decision.actions),
                                                                  sorted(a.candidate_id for a in predicted.actions)),
                              rng=structured_differences(raw['_rng'], effective_rng(simulator.raw_state)))
            step = dict(boundary=index, observation=decision.observation.to_dict(),
                        legal_actions=[a.to_dict() for a in decision.actions], before_raw=raw,
                        native_checkpoint=simulator.checkpoint(), comparisons=comparison)
            result['steps'].append(step)
            flush()
            if any(comparison.values()):
                result.update(status='FIRST_DIVERGENCE', first_divergence=index)
                break
            if decision.terminal:
                result.update(status='NATURAL_TERMINAL', terminal_info=result['steps'][-2]['info'])
                break
            if index == args.max_actions:
                result.update(status='DIAGNOSTIC_LIMIT_UNFINISHED', game_failure=False)
                break
            action = select_action(decision)
            step['selected_action'] = action.to_dict()
            flush()
            actual = backend.step(action)
            evidence = backend.last_validation_evidence
            predicted_transition = simulator.step(action, validation_evidence=evidence)
            step.update(commands=backend.last_executed_commands, validation_evidence=evidence,
                        next_observation=actual.decision.observation.to_dict(), after_raw=backend.raw_payload,
                        info=actual.info, reward=actual.reward, terminated=actual.terminated, truncated=actual.truncated,
                        transition_comparisons=dict(reward=structured_differences(actual.reward,predicted_transition.reward),
                                                    terminated=structured_differences(actual.terminated,predicted_transition.terminated),
                                                    truncated=structured_differences(actual.truncated,predicted_transition.truncated),
                                                    reason=structured_differences(actual.info['reason'],predicted_transition.info['reason']),
                                                    success=structured_differences(actual.info['success'],predicted_transition.info['success'])))
            if any(step['transition_comparisons'].values()):
                result.update(status='FIRST_TRANSITION_DIVERGENCE', first_divergence=index)
                break
            decision, predicted = actual.decision, predicted_transition.decision
        backend.return_to_menu()
        result['execution_complete'] = True
        flush()
        _write_completion(0)
        return 0
    except BaseException as error:
        result['execution_error'] = type(error).__name__+': '+str(error)
        flush()
        _write_completion(2, result['execution_error'])
        return 2


if __name__ == '__main__':
    raise SystemExit(main())
