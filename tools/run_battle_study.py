"""Run the source-bound third-battle study, including real CPU/GPU smoke."""
from __future__ import annotations

import argparse
import copy
import json
import math
import os
import signal
import time
import tomllib
from pathlib import Path

os.environ.setdefault('CUBLAS_WORKSPACE_CONFIG',':4096:8')

import torch

from sls.curriculum import IRONCLAD_A20_ACT2
from sls.model import ModelConfig, Policy
from sls.rl import PPOConfig, PPOTrainer, save_checkpoint
from sls.rl.battle_bank import PARENT_SHA, load_bank, write_json
from sls.rl.battle_curriculum import BattleCurriculum
from sls.rl.battle_evaluation import evaluate_roots, paired_result
from sls.rl.checkpoint import load_checkpoint, load_checkpoint_runtime_rebind
from sls.rl.model_migration import migrate_v5_role_policy
from sls.rl.normalstart_release import RELEASE, release_bindings, validate_qualification
from sls.rl.training_contract import (
    git_state,
    native_source_digest,
    sha256_file,
    training_validation_digest,
)
from sls.rl.value_adaptation import state_digest
from sls.rl.workers import ShardedWorkerPool, VectorWorkerPool

STOP=False


def stop(signum,frame):
    global STOP
    STOP=True


def load_config(path):
    config=tomllib.loads(path.read_text(encoding='utf-8'))
    if config['task']!='NATURAL_THIRD_ACT2_BATTLE' or config['target_decisions']!=500000:
        raise ValueError('unexpected registered study task/budget')
    if (not 0<=config['training_seed']<2_000_000_000_000
            or not 1<=config['shards']<=config['workers']<=64
            or not 1<=config['evaluation_batch']<=config['workers']
            or config['prefix_decision_cap']!=8_000_000
            or not 0<config['max_wall_seconds']<=39600):
        raise ValueError('invalid registered study resource/seed contract')
    return config


def identity(config,path):
    bank=Path(config['bank'])
    _,manifest=load_bank(bank,'fit')
    load_bank(bank,'development')
    parent=Path(config['parent'])
    if sha256_file(parent)!=PARENT_SHA:
        raise ValueError('parent identity mismatch')
    if (manifest['parent_sha256']!=PARENT_SHA
            or manifest['parent_model_sha256']!=state_digest(initialized_model(config).state_dict())):
        raise ValueError('explicit V6 migration initialization differs from bank parent')
    qualification=Path(RELEASE)/'qualification.json'
    validate_qualification(qualification,sha256_file(qualification))
    release_bindings(Path.cwd())
    return {'config_sha256':sha256_file(path),'bank_sha256':sha256_file(bank/'manifest.json'),
        'migrated_parent_sha256':sha256_file(Path(config['migrated_parent'])),
        'base_config_sha256':sha256_file(Path(config['base_config'])),
        'preregistration_sha256':sha256_file(Path('configs/experiments/ironclad_a20_battle_study_r1.json')),
        'native_source_sha256':native_source_digest(),'parent_sha256':PARENT_SHA,
        'training_validation_sha256':training_validation_digest(),
        'qualification_sha256':sha256_file(qualification),'quotas':manifest['quotas']}


def initialized_model(config,*,tiny=False):
    if tiny:
        return Policy(ModelConfig(embedding_dim=32,transformer_layers=1,attention_heads=4,
            feedforward_dim=64,recurrent_hidden_dim=32,architecture='sls-recurrent-relational-policy-v6'))
    with torch.random.fork_rng(devices=[]):
        # Only CPU construction consumes randomness; do not reseed an allocated
        # CUDA stream when creating a frozen reference during PPO evaluation.
        torch.random.default_generator.manual_seed(39171)
        if config.get('migrated_parent'):
            frozen=torch.load(config['migrated_parent'],map_location='cpu',weights_only=False)
            if (frozen.get('schema')!='sls-frozen-policy-transfer-v1'
                    or frozen.get('original_parent_sha256')!=PARENT_SHA
                    or frozen.get('model_state_sha256')!=state_digest(frozen['model'])
                    or frozen['model_config']['architecture']!='sls-recurrent-relational-policy-v6'):
                raise ValueError('frozen V6 parent identity mismatch')
            model=Policy(ModelConfig(**frozen['model_config']))
            model.load_state_dict(frozen['model'],strict=True)
        else:
            model,_=migrate_v5_role_policy(torch.load(config['parent'],map_location='cpu',weights_only=False))
    return model


def ppo_config(config,*,smoke=False):
    base=tomllib.loads(Path(config['base_config']).read_text())['ppo']
    base.update(potential_shaping=False,failure_progress_scale=0.,reward_schema='sls-curriculum-win-v1',
        max_episode_steps=512)
    if smoke:
        base.update(rollout_steps=16,recurrent_sequence_length=8,minibatch_sequences=1,epochs=1,
                    gradient_diagnostic_interval=0)
    return PPOConfig(**base)


def zero_task_value(model):
    linear=[m for m in model.value_head.modules() if isinstance(m,torch.nn.Linear)][-1]
    with torch.no_grad():
        linear.weight.zero_()
        linear.bias.zero_()


def finite(model):
    if any(not torch.isfinite(p).all() for p in model.parameters()):
        raise RuntimeError('non-finite policy parameter')


def freeze_endpoint(path,trainer):
    """Keep the original endpoint bytes through evaluation-only resumption."""
    if path.exists():
        payload=torch.load(path,map_location='cpu',weights_only=False)
        if (payload['trainer']['environment_steps']!=trainer.environment_steps
                or state_digest(payload['model'])!=state_digest(trainer.model.state_dict())):
            raise ValueError('immutable endpoint differs from resumed fixed-budget model')
        return path
    return save_checkpoint(path,trainer)


def make_trainer(config,workers,*,device,smoke=False,tiny=False,seed=None):
    model=initialized_model(config,tiny=tiny)
    zero_task_value(model)
    curriculum=BattleCurriculum(config['bank'],prefix_budget=config['prefix_decision_cap'])
    trainer=PPOTrainer(model,workers,ppo_config(config,smoke=smoke),device=device,
        seed=config['training_seed'] if seed is None else seed,start_curriculum=curriculum,
        git_commit=git_state().get('commit','UNAVAILABLE'),
        training_config_digest=sha256_file(Path(config['_path'])))
    return trainer


def smoke(config,output,*,device,tiny=False):
    output.mkdir(parents=True,exist_ok=False)
    before=time.perf_counter()
    pool=(ShardedWorkerPool(IRONCLAD_A20_ACT2,size=config['workers'],shard_count=config['shards'])
          if device=='cuda' else VectorWorkerPool(IRONCLAD_A20_ACT2,size=1))
    with pool as workers:
        torch.manual_seed(391719)
        trainer=make_trainer(config,workers,device=device,smoke=device=='cpu',tiny=tiny)
        initial=state_digest(trainer.model.state_dict())
        first=trainer.collect()
        assert torch.isfinite(first.advantages).all() and torch.isfinite(first.returns).all()
        with torch.no_grad():
            chunks=trainer._sequence_chunks(first)[:trainer.config.minibatch_sequences]
            reproduced,_,_=trainer._evaluate_sequences(first,chunks)
            old=trainer._select_sequences(first.old_log_probabilities,chunks).to(device)
            torch.testing.assert_close(reproduced,old,atol=2e-5 if device=='cuda' else 1e-6,rtol=1e-6)
            old_logprob_error=float((reproduced-old).abs().max())
        trainer.optimize(first)
        first_update_seconds=time.perf_counter()-before
        finite(trainer.model)
        checkpoint=save_checkpoint(output/'safe.pt',trainer)
        second=trainer.collect()
        trainer.optimize(second)
        expected={k:v.detach().cpu().clone() for k,v in trainer.model.state_dict().items()}
        expected_optimizer=copy.deepcopy(trainer.optimizer.state_dict())
        expected_rng=torch.get_rng_state().clone()
        expected_cuda_rng=torch.cuda.get_rng_state_all() if device=='cuda' else None
        expected_environments=workers.checkpoints()
        load_checkpoint(checkpoint,trainer)
        repeated=trainer.collect()
        assert torch.equal(second.action_indices,repeated.action_indices)
        assert torch.equal(second.returns,repeated.returns)
        trainer.optimize(repeated)
        finite(trainer.model)
        from tools.verify_act12_diagnostics import assert_same
        assert_same(expected_optimizer,trainer.optimizer.state_dict())
        assert torch.equal(expected_rng,torch.get_rng_state())
        if expected_cuda_rng is not None:
            assert all(torch.equal(a,b) for a,b in zip(expected_cuda_rng,torch.cuda.get_rng_state_all(),strict=True))
        assert expected_environments==workers.checkpoints()
        maximum=0.
        for name,value in trainer.model.state_dict().items():
            maximum=max(maximum,float((value.detach().cpu()-expected[name]).abs().max()))
            torch.testing.assert_close(value.detach().cpu(),expected[name],atol=1e-7,rtol=1e-6)
        changed=state_digest(trainer.model.state_dict())!=initial
        assert changed
        final=save_checkpoint(output/'final.pt',trainer)
        ppo_decisions=trainer.environment_steps
        prefill_decisions=trainer.start_curriculum.prefix_decisions
        # No extra policy memory enters the environment checkpoint.
    rows,_=load_bank(Path(config['bank']),'development')
    evaluation_started=time.perf_counter()
    evaluation=evaluate_roots(trainer.model,rows[:2],batch_size=1)
    evaluation_seconds=time.perf_counter()-evaluation_started
    report={'status':'PASS','device':str(device),'tiny':tiny,'torch':str(torch.__version__),
        'cuda_name':torch.cuda.get_device_name() if device=='cuda' else None,
        'cuda_total_memory':torch.cuda.get_device_properties(0).total_memory if device=='cuda' else None,
        'identity':identity(config,Path(config['_path'])),'ppo_decisions':ppo_decisions,
        'prefix_decisions':prefill_decisions,'weight_changed':changed,
        'replayed_update_weight_max_error':maximum,'checkpoint_sha256':sha256_file(final),
        'optimizer_and_rng_replay_equal':True,
        'pre_update_old_logprob_max_error':old_logprob_error,
        'first_update_seconds':first_update_seconds,'formal_shapes':device=='cuda',
        'evaluation':evaluation,'elapsed_seconds':time.perf_counter()-before,
        'evaluation_seconds':evaluation_seconds,
        'max_cuda_allocated':torch.cuda.max_memory_allocated() if device=='cuda' else 0}
    write_json(output/'result.json',report)
    return report


def train(config,output,*,device,smoke_receipt):
    if device!='cuda':
        raise ValueError('formal study requires allocated server GPU; local CPU only runs smoke')
    expected_identity=identity(config,Path(config['_path']))
    receipt=json.loads(smoke_receipt.read_text())
    if (receipt.get('status')!='PASS' or receipt.get('device')!='cuda' or receipt.get('tiny')
            or not receipt.get('formal_shapes')
            or receipt['identity']!=expected_identity or receipt['torch']!=str(torch.__version__)):
        raise ValueError('missing/stale real-parent GPU smoke; long job blocked')
    if expected_identity['quotas']!={'fit':64,'development':64,'confirmation':384}:
        raise ValueError('formal data quotas incomplete')
    output.mkdir(parents=True,exist_ok=True)
    manifest_file=output/'run-manifest.json'
    if manifest_file.exists():
        previous=json.loads(manifest_file.read_text())
        if previous['identity']!=expected_identity or previous['status']=='COMPLETE':
            raise ValueError('run identity differs or trial is already closed')
    else:
        if any(output.iterdir()):
            raise FileExistsError('new trial cannot reuse a nonempty directory without its own manifest')
        write_json(manifest_file,{'status':'INITIALIZING','identity':expected_identity})
    for number in (signal.SIGTERM,signal.SIGINT):
        signal.signal(number,stop)
    bank=Path(config['bank'])
    development,_=load_bank(bank,'development')
    fit_rows,_=load_bank(bank,'fit')
    latest=output/'latest.pt'
    started=time.perf_counter()
    def should_stop():
        return STOP or time.perf_counter()-started>=config['max_wall_seconds']
    with ShardedWorkerPool(IRONCLAD_A20_ACT2,size=config['workers'],shard_count=config['shards'],
                          crash_dump_dir=output/'failures') as workers:
        torch.manual_seed(config['training_seed'])
        trainer=make_trainer(config,workers,device=device)
        if latest.exists():
            load_checkpoint_runtime_rebind(latest,trainer)
        else:
            save_checkpoint(latest,trainer)
        baseline_path=output/'reference-development.json'
        if baseline_path.exists():
            baseline=json.loads(baseline_path.read_text())
            if baseline['identity']!=expected_identity:
                raise ValueError('development reference identity changed')
        else:
            parent=initialized_model(config).to(device).eval()
            baseline={'identity':expected_identity,
                'evaluation':evaluate_roots(parent,development,batch_size=config['evaluation_batch'],stop_requested=should_stop)}
            write_json(baseline_path,baseline)
            write_json(output/'reference-fit.json',{'identity':expected_identity,
                'evaluation':evaluate_roots(parent,fit_rows,batch_size=config['evaluation_batch'],stop_requested=should_stop)})
            del parent
        if not (output/'reference-fit.json').exists():
            parent=initialized_model(config).to(device).eval()
            write_json(output/'reference-fit.json',{'identity':expected_identity,
                'evaluation':evaluate_roots(parent,fit_rows,batch_size=config['evaluation_batch'],stop_requested=should_stop)})
            del parent
        if not .2<=baseline['evaluation']['success_rate']<=.8:
            write_json(manifest_file,{'status':'BLOCKED','identity':expected_identity,
                'reason':'BASELINE_SATURATED','decisions':trainer.environment_steps})
            raise RuntimeError('BASELINE_SATURATED: registered task cannot proceed; do not filter individual roots')
        target=math.ceil(config['target_decisions']/(workers.size*trainer.config.rollout_steps))*(workers.size*trainer.config.rollout_steps)
        write_json(manifest_file,{'status':'RUNNING','identity':expected_identity,'target':target,
            'decisions':trainer.environment_steps,'smoke_receipt_sha256':sha256_file(smoke_receipt)})
        try:
            while trainer.environment_steps<target and not STOP:
                if time.perf_counter()-started>config['max_wall_seconds']:
                    break
                tick=time.perf_counter()
                rollout=trainer.collect()
                metrics=trainer.optimize(rollout)
                finite(trainer.model)
                if any(not math.isfinite(v) for v in metrics.values() if isinstance(v,(int,float))):
                    raise RuntimeError('non-finite training diagnostic')
                save_checkpoint(latest,trainer)  # only completed updates publish
                row={'update':trainer.update,'decisions':trainer.environment_steps,
                    'prefix_decisions':trainer.start_curriculum.prefix_decisions,
                    'seconds':time.perf_counter()-tick,**metrics}
                with (output/'metrics.jsonl').open('a',encoding='utf-8') as stream:
                    stream.write(json.dumps(row,allow_nan=False)+'\n')
                print(json.dumps({'update':trainer.update,'decisions':trainer.environment_steps,
                                  'prefix_decisions':row['prefix_decisions']}),flush=True)
                if trainer.update%config['development_interval_updates']==0 and not STOP:
                    report=evaluate_roots(trainer.model,development,batch_size=config['evaluation_batch'],stop_requested=should_stop)
                    write_json(output/f'development-{trainer.update:04d}.json',report)
        except InterruptedError:
            raise
        except BaseException as error:
            failure=output/f'failure-{trainer.update}-{time.time_ns()}.json'
            write_json(failure,{'error':repr(error),'last_update_context':trainer.last_update_context,
                'unpublished_environment_steps':trainer.environment_steps,
                'unpublished_prefix_decisions':trainer.start_curriculum.prefix_decisions,
                'safe_checkpoint_sha256':sha256_file(latest),'unsafe_state_not_published':True})
            write_json(manifest_file,{'status':'FAILED','identity':expected_identity,'error':repr(error)})
            raise
        if trainer.environment_steps<target:
            write_json(manifest_file,{'status':'STOPPED','identity':expected_identity,
                                      'decisions':trainer.environment_steps,'target':target})
            return {'status':'STOPPED'}
        endpoint=freeze_endpoint(output/'endpoint.pt',trainer)
        endpoint_sha=sha256_file(endpoint)
        write_json(output/'endpoint-fit.json',{'identity':expected_identity,
            'evaluation':evaluate_roots(trainer.model,fit_rows,batch_size=config['evaluation_batch'],stop_requested=should_stop)})
    # Only the fixed endpoint may unseal confirmation. Evaluations are paired,
    # greedy, and use each model's own current-weight history reconstruction.
    confirmation,_=load_bank(bank,'confirmation',allow_confirmation=True)
    seal=output/'confirmation-unsealed.json'
    if seal.exists():
        if json.loads(seal.read_text())['endpoint_sha256']!=endpoint_sha:
            raise ValueError('confirmation cannot select a different candidate')
    else:
        write_json(seal,{'endpoint_sha256':endpoint_sha,'selection':'FIXED_BUDGET_ENDPOINT_ONLY'})
    parent=initialized_model(config).to(device).eval()
    write_json(manifest_file,{'status':'EVALUATING','identity':expected_identity,
        'decisions':trainer.environment_steps,'target':target,'endpoint_sha256':endpoint_sha})
    reference=evaluate_roots(parent,confirmation,batch_size=config['evaluation_batch'],stop_requested=should_stop)
    candidate=evaluate_roots(trainer.model,confirmation,batch_size=config['evaluation_batch'],stop_requested=should_stop)
    comparison=paired_result(reference,candidate)
    write_json(output/'reference-evaluation.json',reference)
    write_json(output/'endpoint-evaluation.json',candidate)
    write_json(output/'comparison.json',comparison)
    write_json(output/'development-stochastic.json',{
        'reference':evaluate_roots(parent,development[:32],greedy=False,replicates=4,
            batch_size=config['evaluation_batch'],stop_requested=should_stop),
        'endpoint':evaluate_roots(trainer.model,development[:32],greedy=False,replicates=4,
            batch_size=config['evaluation_batch'],stop_requested=should_stop),
        'interpretation':'Development-only secondary; 32 origins, not 128 independent samples. Primary gate unchanged.'})
    if comparison['pass_learnability']:
        suffix_parent=evaluate_roots(parent,development,batch_size=config['evaluation_batch'],suffix=True,max_steps=4096,stop_requested=should_stop)
        suffix_candidate=evaluate_roots(trainer.model,development,batch_size=config['evaluation_batch'],suffix=True,max_steps=4096,stop_requested=should_stop)
        write_json(output/'development-full-suffix.json',{'reference':suffix_parent,'candidate':suffix_candidate,
            'interpretation':'Development-only diagnostic, not independent evidence of full-Act2 transfer.'})
    write_json(manifest_file,{'status':'COMPLETE','identity':expected_identity,
        'decisions':trainer.environment_steps,'target':target,'endpoint_sha256':endpoint_sha,
        'elapsed_seconds':time.perf_counter()-started,'comparison':comparison})
    return {'status':'COMPLETE','comparison':comparison}


def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('mode',choices=('smoke','train','status'))
    parser.add_argument('--config',type=Path,default=Path('configs/train/ironclad_a20_battle_study_r1.toml'))
    parser.add_argument('--output',type=Path,required=True)
    parser.add_argument('--device',choices=('cpu','cuda'),default='cpu')
    parser.add_argument('--tiny',action='store_true')
    parser.add_argument('--smoke-receipt',type=Path)
    args=parser.parse_args()
    if args.mode=='train' and (args.tiny or args.device!='cuda' or args.smoke_receipt is None):
        parser.error('formal training requires CUDA, real parent and --smoke-receipt')
    if args.mode=='status':
        print((args.output/'run-manifest.json').read_text())
        metrics=args.output/'metrics.jsonl'
        if metrics.exists():
            print(metrics.read_text().splitlines()[-1])
        return 0
    if args.device=='cpu':
        os.environ['CUDA_VISIBLE_DEVICES']='-1'
    torch.set_num_threads(2)
    torch.set_num_interop_threads(2)
    torch.use_deterministic_algorithms(True)
    if args.device=='cuda' and (not os.environ.get('SLURM_JOB_ID') or not torch.cuda.is_available()):
        raise ValueError('GPU work requires an allocated Slurm job; local GPU forbidden')
    config=load_config(args.config)
    config['_path']=str(args.config)
    identity(config,args.config)
    try:
        report=smoke(config,args.output,device=args.device,tiny=args.tiny) if args.mode=='smoke' else train(
            config,args.output,device=args.device,smoke_receipt=args.smoke_receipt)
    except InterruptedError:
        manifest=args.output/'run-manifest.json'
        previous=json.loads(manifest.read_text())
        write_json(manifest,{**previous,'status':'STOPPED','reason':'SIGNAL_DURING_EVALUATION'})
        report={'status':'STOPPED'}
    print(json.dumps(report))
    return 0 if report['status'] in ('PASS','COMPLETE') else 2


if __name__=='__main__':
    raise SystemExit(main())
