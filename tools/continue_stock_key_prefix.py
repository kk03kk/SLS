"""CPU-only counterfactual continuation of one sealed natural stock key prefix."""
from __future__ import annotations

import argparse
import hashlib
import itertools
import json
import math
import os
import re
import sys
from collections import deque
from dataclasses import asdict
from pathlib import Path

os.environ['CUDA_VISIBLE_DEVICES'] = '-1'
ROOT = Path(__file__).resolve().parents[1]
sys.path[:0] = [str(ROOT/'src'),str(ROOT)]

import torch

from sls.backends.simulator import SimulatorBackend, native
from sls.contracts import Action
from sls.curriculum import CURRICULUM_PROFILES_BY_ID, IRONCLAD_A20_HEART
from sls.diagnostics.canary import _boundary_record, stable_hash
from sls.model import PolicyBatch
from sls.model.encoding import ACTION_TYPE_IDS
from sls.rl.episode_limit import EpisodeLimitState
from sls.rl.reward import curriculum_terminal_reward, shape_curriculum_reward
from sls.rl.training_contract import (
    native_source_digest,
    training_implementation_digest,
)
from sls.runtime import load_policy_artifact
from sls.runtime.artifact import model_state_sha256
from tools.replay_frozen_key_reference import persisted, sha, verify_capture


def load_specs(path):
    specification = json.loads(path.read_text())
    if specification.get('schema') != 'sls-read-only-prefix-models-v1':
        raise ValueError('unsupported read-only model specification')
    result = {}
    for row in specification['models']:
        label = row['label']
        if not re.fullmatch(r'[a-zA-Z0-9_-]+',label) or label in result:
            raise ValueError('model labels must be distinct filename-safe identifiers')
        checkpoint,artifact_path = Path(row['path']),Path(row['artifact'])
        if sha(checkpoint) != row['sha256'] or sha(artifact_path) != row['artifact_sha256']:
            raise ValueError('historical checkpoint/artifact identity mismatch')
        artifact = load_policy_artifact(artifact_path,device='cpu')
        # Trusted, hash-bound historical checkpoint is read for provenance/PPO only.
        # No trainer, optimizer, historical worker, memory or RNG is restored.
        payload = torch.load(checkpoint,map_location='cpu',weights_only=False)
        contract = payload['contract']
        trained_profile = asdict(contract['profile'])
        if (model_state_sha256(payload['model']) != artifact.metadata.model_sha256
                or artifact.metadata.native_source_sha256 != contract['native_source_sha256']
                or artifact.metadata.environment_profile != trained_profile):
            raise ValueError('exported policy does not match historical checkpoint')
        profile = CURRICULUM_PROFILES_BY_ID[trained_profile['profile_id']]
        if asdict(profile) != trained_profile:
            raise ValueError('trained horizon/profile rules changed; transfer is not implicit')
        if not all(bool(torch.isfinite(v).all()) for v in artifact.model.state_dict().values()):
            raise ValueError('nonfinite historical policy weights')
        ppo = dict(contract['ppo'])
        result[label] = dict(artifact=artifact,ppo=ppo,trained_profile=profile,
                             identity=dict(checkpoint=str(checkpoint),checkpoint_sha256=row['sha256'],
                                           artifact=str(artifact_path),artifact_sha256=row['artifact_sha256'],
                                           model_sha256=artifact.metadata.model_sha256,
                                           checkpoint_schema=payload['schema'],steps=payload['trainer']['environment_steps'],
                                           trained_profile=trained_profile,trained_native_source_sha256=contract['native_source_sha256'],
                                           trained_git_commit=contract['git_commit'],ppo=ppo,
                                           transfer='READ_ONLY_POLICY_ARTIFACT_DIAGNOSTIC_MIGRATION_NOT_TRAINING_RESUME'))
        del payload
    if not result:
        raise ValueError('empty read-only model specification')
    return result


def learning_reward(current,transition,profile,ppo,limit=None):
    terminal = transition.terminated or transition.truncated or limit is not None
    reward = float(transition.reward)
    if transition.terminated:
        reward = curriculum_terminal_reward(transition.decision.observation,profile,
                                            success=bool(transition.info['success']),
                                            failure_progress_scale=ppo['failure_progress_scale'])
    elif transition.truncated or limit is not None:
        reward = ppo['limit_failure_reward']
    if ppo['potential_shaping']:
        reward = shape_curriculum_reward(reward,current.observation,transition.decision.observation,profile,
                                         gamma=ppo['gamma'],scale=ppo['potential_scale'],terminal=terminal)
    return float(torch.tensor(reward,dtype=torch.float32))


def rebuild_prefix(backend,artifact,public,ppo):
    decision = backend.reset(public['seed'])
    memory = artifact.model.initial_memory(1,'cpu')
    previous_actions = torch.zeros(1,dtype=torch.long)
    previous_rewards = torch.zeros(1,dtype=torch.float32)
    limits = EpisodeLimitState.initial(decision)
    records = []
    for expected in public['boundaries'][:-1]:
        if (decision.observation.to_dict() != expected['observation']
                or [a.to_dict() for a in decision.actions] != expected['ordered_candidate_actions']):
            raise ValueError('teacher-forced public prefix/state alignment failure')
        record,_,next_memory = _boundary_record(
            step=expected['step_index'],decision=decision,reward=float(previous_rewards[0]),reason=None,success=False,
            memory=memory,artifact=artifact,previous_action_types=previous_actions,previous_rewards=previous_rewards)
        action = Action.from_dict(expected['executed_action'])
        transition = backend.step(action)
        if (transition.terminated or transition.truncated
                or transition.reward != public['boundaries'][expected['step_index']+1]['reward_from_previous']):
            raise ValueError('reference prefix terminal or base reward mismatch')
        if limits.observe(transition.decision,max_steps=ppo['max_episode_steps'],
                          max_boundary_visits=ppo['max_boundary_visits']) is not None:
            raise ValueError('reference prefix hits this model runtime limit')
        record.update(executed_action=action.to_dict(),teacher_forced=True)
        records.append(record)
        decision,memory = transition.decision,next_memory
        previous_actions = torch.tensor([ACTION_TYPE_IDS[action.kind.value]+1],dtype=torch.long)
        previous_rewards = torch.tensor([transition.reward],dtype=torch.float32)
    final = public['boundaries'][-1]
    if (decision.observation.to_dict() != final['observation']
            or [a.to_dict() for a in decision.actions] != final['ordered_candidate_actions']):
        raise ValueError('continuation initial public state/order mismatch')
    return decision,memory,previous_actions,previous_rewards,limits,records


def initial_distribution(artifact,decision,memory,previous_actions,previous_rewards):
    batch = PolicyBatch.from_decisions((decision,),artifact.model.config)
    with torch.no_grad():
        output = artifact.model(*batch.model_inputs(),memory=memory,
                                previous_action_types=previous_actions,previous_rewards=previous_rewards)
    probabilities = output.logits[0].softmax(-1)
    return [dict(action=a.to_dict(),candidate_id=a.candidate_id,probability=float(probabilities[i]),
                 logit=float(output.logits[0,i])) for i,a in enumerate(decision.actions)]


def continue_model(spec,public,reference_state,directory,profile,max_actions):
    directory.mkdir(parents=True,exist_ok=False)
    backend = SimulatorBackend(profile=profile)
    artifact,ppo = spec['artifact'],spec['ppo']
    decision,memory,previous_actions,previous_rewards,limits,prefix = rebuild_prefix(backend,artifact,public,ppo)
    initial_state = backend.checkpoint()
    if persisted(initial_state) != persisted(reference_state):
        raise ValueError('continuation initial complete native state differs from sealed reference')
    (directory/'prefix.public.jsonl').write_text(''.join(json.dumps(r)+'\n' for r in prefix),encoding='utf-8')
    distribution = initial_distribution(artifact,decision,memory,previous_actions,previous_rewards)
    prefix_count = len(prefix)
    reward,reason,success = float(previous_rewards[0]),None,False
    count,shaped,base_rewards = 0,[],[]
    tail = deque(maxlen=32)
    max_act,max_floor = decision.observation.run.act,decision.observation.run.floor
    status = None
    initial_value,initial_input,initial_record = None,None,None
    with (directory/'continuation.public.jsonl').open('x',encoding='utf-8') as stream,(
        directory/'continuation.native.jsonl').open('x',encoding='utf-8') as private:
        for index in range(max_actions+1):
            record,action,next_memory = _boundary_record(
                step=prefix_count+index,decision=decision,reward=reward,reason=reason,success=success,
                memory=memory,artifact=artifact,previous_action_types=previous_actions,previous_rewards=previous_rewards)
            private.write(json.dumps(dict(boundary=index,checkpoint=backend.checkpoint()))+'\n')
            private.flush()
            if index == 0:
                initial_record = dict(record)
                initial_value,initial_input = record['value'],record['policy_input_sha256']
            if status is not None or decision.terminal or index == max_actions:
                if status is None:
                    status = 'NATIVE_TERMINAL' if decision.terminal else 'DIAGNOSTIC_LIMIT_UNFINISHED'
                record.update(continuation_status=status,executed_action=None)
                stream.write(json.dumps(record)+'\n')
                break
            current = decision
            transition = backend.step(action)
            limit = None
            if not transition.terminated and not transition.truncated:
                limit = limits.observe(transition.decision,max_steps=ppo['max_episode_steps'],
                                       max_boundary_visits=ppo['max_boundary_visits'])
            learned = learning_reward(current,transition,profile,ppo,limit)
            record.update(executed_action=action.to_dict(),raw_reward=float(transition.reward),
                          shaped_reward_float32=learned,transition_info=transition.info,
                          terminated=transition.terminated,truncated=transition.truncated,policy_limit=limit)
            stream.write(json.dumps(record)+'\n')
            stream.flush()
            tail.append(dict(boundary=prefix_count+index,screen=record['screen'],executed_action=action.to_dict(),
                             choice_options=record['observation']['choice_options'],
                             selected_cards=record['observation']['selected_cards'],
                             public_context=record['observation']['public_context']))
            shaped.append(learned)
            base_rewards.append(float(transition.reward))
            decision,memory = transition.decision,next_memory
            reward,reason,success = float(transition.reward),transition.info.get('reason') or None,bool(transition.info['success'])
            previous_actions = torch.tensor([ACTION_TYPE_IDS[action.kind.value]+1],dtype=torch.long)
            previous_rewards = torch.tensor([reward],dtype=torch.float32)
            max_act,max_floor = max(max_act,decision.observation.run.act),max(max_floor,decision.observation.run.floor)
            count += 1
            if transition.terminated:
                status = 'NATIVE_TERMINAL'
            elif transition.truncated:
                status = 'BACKEND_TRUNCATED'
            elif limit is not None:
                status,reason = 'POLICY_'+limit.upper(),limit
    complete = status != 'DIAGNOSTIC_LIMIT_UNFINISHED'
    mc_return = None
    if complete:
        mc_return = 0.0
        for value in reversed(shaped):
            mc_return = value+ppo['gamma']*mc_return
    files = {name:dict(path=str(directory/name),sha256=sha(directory/name)) for name in [
        'prefix.public.jsonl','continuation.public.jsonl','continuation.native.jsonl']}
    return dict(status=status,executed_continuation_actions=count,public_prefix_actions=prefix_count,
                initial_complete_native_sha256=stable_hash(initial_state),initial_record=initial_record,
                initial_policy_input_sha256=initial_input,initial_action_distribution=distribution,
                initial_value=initial_value,realized_complete_shaped_return=mc_return,
                partial_shaped_sum=sum(shaped),raw_reward_sum=sum(base_rewards),
                value_minus_realized_return=initial_value-mc_return if mc_return is not None else None,
                diagnostic_truncation_is_game_failure=False,training_objective_episode_complete=complete,
                actual_backend_terminal=decision.terminal,reason=reason,success=success,
                max_act=max_act,max_floor=max_floor,final_public_run=decision.observation.to_dict()['run'],
                final_hp=decision.observation.player.current_hp,cycle_tail=list(tail) if status=='POLICY_CYCLE_LIMIT' else [],
                final_action_tail=list(tail),files=files,
                target=dict(profile=asdict(profile),policy='GREEDY_AFTER_TEACHER_FORCED_PUBLIC_PREFIX',
                            gamma=ppo['gamma'],potential_shaping=ppo['potential_shaping'],potential_scale=ppo['potential_scale'],
                            failure_progress_scale=ppo['failure_progress_scale'],limit_failure_reward=ppo['limit_failure_reward'],
                            training_value_target='STOCHASTIC_V_PLUS_GAE_WITH_ROLLOUT_BOOTSTRAP_NOT_THIS_GREEDY_MC',
                            original_training_profile=asdict(spec['trained_profile']),
                            horizon_transfer=profile != spec['trained_profile']))


def pairwise_initial_distributions(runs):
    result = []
    for left,right in itertools.combinations(runs,2):
        if left['target_name'] != right['target_name'] or left['target_name'] != 'heart':
            continue
        a,b = left['result']['initial_action_distribution'],right['result']['initial_action_distribution']
        if [x['candidate_id'] for x in a] != [x['candidate_id'] for x in b]:
            raise ValueError('model action distributions are not state-aligned')
        jsd = 0.0
        for x,y in zip(a,b,strict=True):
            p,q = x['probability'],y['probability']
            m = (p+q)/2
            jsd += (p*math.log(p/m) if p else 0)/2+(q*math.log(q/m) if q else 0)/2
        result.append(dict(models=[left['model_label'],right['model_label']],js_divergence_nats=jsd))
    return result


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--models',type=Path,required=True)
    parser.add_argument('--capture',type=Path,required=True)
    parser.add_argument('--reference-report',type=Path,required=True)
    parser.add_argument('--oracle',type=Path,required=True)
    parser.add_argument('--output-dir',type=Path,required=True)
    parser.add_argument('--max-actions',type=int,default=256)
    parser.add_argument('--targets',nargs='+',choices=('heart','trained'),default=['heart','trained'])
    parser.add_argument('--device',choices=('cpu',),default='cpu')
    args = parser.parse_args()
    if args.output_dir.exists():
        raise FileExistsError('refuse to overwrite continuation evidence')
    if not 1 <= args.max_actions <= 256 or len(args.targets) != len(set(args.targets)):
        raise ValueError('invalid diagnostic continuation budget/targets')
    code = Path(__file__).read_bytes()
    identity = dict(source_sha256=hashlib.sha256(code).hexdigest(),native_source_sha256=native_source_digest(),
                    native_binary_sha256=sha(Path(native.__file__)),training_implementation_sha256=training_implementation_digest(),
                    models_specification_sha256=sha(args.models),capture_sha256=sha(args.capture),reference_sha256=sha(args.reference_report))
    capture,reference,_,_ = verify_capture(args.capture,args.reference_report,args.oracle)
    if capture['status'] != 'REFERENCE_PREFIX_COMPLETE' or not all(capture['actual_final_run'][k] for k in [
        'has_ruby_key','has_emerald_key','has_sapphire_key']):
        raise ValueError('continuation requires a sealed actually completed normal-start three-key prefix')
    row = next(r for r in reference['runs'] if r['seed']==capture['seed'])
    public = dict(seed=capture['seed'],boundaries=[json.loads(line) for line in (ROOT/row['public_history']).read_text().splitlines()])
    reference_state = json.loads((ROOT/row['private_history']).read_text().splitlines()[-1])['checkpoint']
    torch.set_num_threads(1)
    specs = load_specs(args.models)
    args.output_dir.mkdir(parents=True,exist_ok=False)
    (args.output_dir/'executed-source.py').write_bytes(code)
    report = dict(schema='sls-stock-key-prefix-capability-v1',scope='ONE_SHARED_NATURAL_PREFIX_COUNTERFACTUAL_MODEL_DIAGNOSTIC_NOT_WINRATE',
                  identity=identity,model_identities={k:v['identity'] for k,v in specs.items()},seed=capture['seed'],
                  seed_reuse='EXPLICIT_SEALED_REFERENCE_CONTINUATION_NOT_NEW_EVALUATION_SEEDS',max_actions=args.max_actions,
                  original_stock_verified_prefix_only=True,continuations_native_only=True,previous_rewards='ACTUAL_BASE_REWARD',
                  previous_actions='ACTUAL_EXECUTED_ACTION_TYPES',device='cpu',training=False,training_eligible=False,
                  training_gate='NOT_QUALIFIED',runs=[])
    report_path = args.output_dir/'report.json'
    with report_path.open('x',encoding='utf-8') as stream:
        json.dump(report,stream,indent=2)
    try:
        for label,spec in specs.items():
            for target in args.targets:
                profile = IRONCLAD_A20_HEART if target=='heart' else spec['trained_profile']
                result = continue_model(spec,public,reference_state,args.output_dir/label/target,profile,args.max_actions)
                report['runs'].append(dict(model_label=label,target_name=target,result=result))
                report_path.write_text(json.dumps(report,indent=2),encoding='utf-8')
                print(json.dumps(dict(model=label,target=target,status=result['status'],max_act=result['max_act'],
                                      max_floor=result['max_floor'],actions=result['executed_continuation_actions'])),flush=True)
        if (Path(__file__).read_bytes() != code or native_source_digest()!=identity['native_source_sha256']
                or training_implementation_digest()!=identity['training_implementation_sha256']):
            raise ValueError('execution source/environment changed during diagnostic run')
        report['pairwise_initial_distributions'] = pairwise_initial_distributions(report['runs'])
        report['execution_complete'] = True
        report_path.write_text(json.dumps(report,indent=2),encoding='utf-8')
    except BaseException as error:
        report['execution_error'] = type(error).__name__+': '+str(error)
        report_path.write_text(json.dumps(report,indent=2),encoding='utf-8')
        raise
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
