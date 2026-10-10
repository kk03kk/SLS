"""CPU-only frozen-model routes with explicit public key interventions."""
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

import torch

from sls.audit.natural_three_key_policy import choose_natural_three_key_action
from sls.backends.simulator import SimulatorBackend, native
from sls.contracts import ActionKind
from sls.curriculum import IRONCLAD_A20_HEART
from sls.diagnostics.canary import _boundary_record, stable_hash
from sls.model.encoding import ACTION_TYPE_IDS
from sls.rl.training_contract import native_source_digest
from sls.runtime import load_policy_artifact


def intervened_action(decision, model_action):
    if any(a.kind == ActionKind.CHOOSE_MAP_NODE for a in decision.actions):
        return choose_natural_three_key_action(decision),'PUBLIC_JOINT_KEY_ROUTE'
    for action in decision.actions:
        if action.kind in {ActionKind.RECALL,ActionKind.TAKE_BLUE_KEY} or (
            action.kind == ActionKind.TAKE_REWARD and action.reward_id == 'reward-key:emerald'
        ):
            return action,'ACTUAL_LEGAL_KEY_ACQUISITION'
    return model_action,None


def collect_probe(backend,artifact,seed,directory,*,max_actions=256,boundary_record=_boundary_record):
    decision = backend.reset(seed)
    if decision.observation.screen.value != 'NEOW' or decision.observation.run.floor != 0:
        raise ValueError('probe must start at normal Neow')
    memory = artifact.model.initial_memory(1,'cpu')
    previous_actions = torch.zeros(1,dtype=torch.long)
    previous_rewards = torch.zeros(1,dtype=torch.float32)
    reward,reason,success = 0.0,None,False
    count = 0
    interventions = 0
    public = directory/f'{seed}.public.jsonl'
    private = directory/f'{seed}.native.jsonl'
    if public.exists() or private.exists():
        raise FileExistsError('refuse to overwrite probe history')
    status = None
    with public.open('x',encoding='utf-8') as history,private.open('x',encoding='utf-8') as audit:
        for index in range(max_actions+1):
            record,model_action,next_memory = boundary_record(
                step=index,decision=decision,reward=reward,reason=reason,success=success,memory=memory,
                artifact=artifact,previous_action_types=previous_actions,previous_rewards=previous_rewards)
            # Private state is written separately and never passed to inference.
            audit.write(json.dumps(dict(boundary=index,checkpoint=backend.checkpoint()))+'\n')
            audit.flush()
            run = decision.observation.run
            if decision.terminal:
                status = 'ACTUAL_NATIVE_TERMINAL'
            elif run.has_ruby_key and run.has_sapphire_key and run.has_emerald_key:
                status = 'NATURAL_NATIVE_THREE_KEYS_DIAGNOSTIC_STOP'
            elif index == max_actions:
                status = 'DIAGNOSTIC_LIMIT_UNFINISHED'
            if status:
                record.update(probe_status=status,executed_action=None,game_failure=False if not decision.terminal else reason == 'DEATH')
                history.write(json.dumps(record)+'\n')
                history.flush()
                break
            action,intervention = intervened_action(decision,model_action)
            if action.candidate_id not in {a.candidate_id for a in decision.actions}:
                raise ValueError('intervention is not a legal public action')
            changed = action.candidate_id != model_action.candidate_id
            interventions += int(changed)
            record.update(executed_action=action.to_dict(),executed_action_sha256=stable_hash(action.to_dict()),
                          intervention=intervention,changed_model_argmax=changed)
            history.write(json.dumps(record)+'\n')
            history.flush()
            transition = backend.step(action)
            decision = transition.decision
            reward = float(transition.reward)
            reason = str(transition.info.get('reason') or '') or None
            success = bool(transition.info.get('success'))
            memory = next_memory
            previous_actions = torch.tensor([ACTION_TYPE_IDS[action.kind.value]+1],dtype=torch.long)
            previous_rewards = torch.tensor([reward],dtype=torch.float32)
            count += 1
    return dict(seed=seed,status=status,executed_actions=count,changed_argmax_actions=interventions,
                final_public_run=decision.observation.to_dict()['run'],reason=reason,success=success,
                public_history=str(public),public_sha256=hashlib.sha256(public.read_bytes()).hexdigest(),
                private_history=str(private),private_sha256=hashlib.sha256(private.read_bytes()).hexdigest())


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--artifact',type=Path,required=True)
    parser.add_argument('--output-dir',type=Path,required=True)
    parser.add_argument('--seeds',type=int,nargs='+',required=True)
    parser.add_argument('--max-actions',type=int,default=256)
    parser.add_argument('--device',choices=('cpu',),default='cpu')
    args = parser.parse_args()
    if not 1 <= args.max_actions <= 256 or len(args.seeds) != len(set(args.seeds)):
        raise ValueError('invalid bounded seed/budget request')
    if args.output_dir.exists():
        raise FileExistsError('refuse to overwrite probe output')
    if native.NATIVE_SOURCE_SHA256 != native_source_digest():
        raise ValueError('stale native source')
    conflicts = [str(p) for folder in ['native/oracle/resources','tests','tools','configs','docs/results']
                 for p in (ROOT/folder).rglob('*') if p.is_file()
                 and any(str(seed).encode() in p.read_bytes() for seed in args.seeds)]
    if conflicts:
        raise ValueError('diagnostic seed namespace conflict: '+str(conflicts))
    torch.set_num_threads(1)
    artifact = load_policy_artifact(args.artifact,device='cpu')
    args.output_dir.mkdir(parents=True,exist_ok=False)
    report = dict(schema='sls-frozen-three-key-route-probe-v1',scope='DIAGNOSTIC_ENVIRONMENT_MIGRATION_WITH_PUBLIC_ROUTE_INTERVENTIONS',
                  source_sha256=hashlib.sha256(Path(__file__).read_bytes()).hexdigest(),
                  artifact_path=str(args.artifact.resolve()),artifact_sha256=hashlib.sha256(args.artifact.read_bytes()).hexdigest(),
                  model_sha256=artifact.metadata.model_sha256,trained_profile=artifact.metadata.environment_profile,
                  trained_native_source_sha256=artifact.metadata.native_source_sha256,current_native_source_sha256=native.NATIVE_SOURCE_SHA256,
                  current_native_binary_sha256=hashlib.sha256(Path(native.__file__).read_bytes()).hexdigest(),
                  route_policy_sha256=hashlib.sha256((ROOT/'src/sls/audit/natural_three_key_policy.py').read_bytes()).hexdigest(),
                  seeds=args.seeds,namespace_conflicts=conflicts,device='cpu',max_actions=args.max_actions,
                  previous_reward_input='ACTUAL_BACKEND_BASE_REWARD',previous_action_input='ACTUAL_EXECUTED_ACTION_TYPE',
                  training=False,training_eligible=False,training_gate='NOT_QUALIFIED',runs=[])
    path = args.output_dir/'report.json'
    path.open('x',encoding='utf-8').close()
    for seed in args.seeds:
        try:
            row = collect_probe(SimulatorBackend(profile=IRONCLAD_A20_HEART),artifact,seed,args.output_dir,max_actions=args.max_actions)
            report['runs'].append(row)
            print(json.dumps({k:row[k] for k in ['seed','status','executed_actions','final_public_run']}),flush=True)
        except Exception as error:
            report['execution_error'] = type(error).__name__+': '+str(error)
            path.write_text(json.dumps(report,indent=2),encoding='utf-8')
            raise
        path.write_text(json.dumps(report,indent=2),encoding='utf-8')
    report['execution_complete'] = True
    path.write_text(json.dumps(report,indent=2),encoding='utf-8')
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
