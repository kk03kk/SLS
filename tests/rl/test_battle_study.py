"""Real legal prefixes and actual PPO updates; no injected game resources."""
import copy
import gzip
import json
import os
from dataclasses import asdict
from pathlib import Path

import pytest
import torch

from sls.audit.short_battle import rebuild_context
from sls.backends.simulator import SimulatorBackend
from sls.content.selection import PROJECTION_REVISION
from sls.contracts import Action
from sls.curriculum import IRONCLAD_A20_ACT2
from sls.rl import load_checkpoint, save_checkpoint
from sls.rl.battle_bank import PARENT_SHA, SCHEMA, load_bank, projection_digest
from sls.rl.battle_curriculum import BattleCurriculum
from sls.rl.battle_evaluation import evaluate_roots, paired_result
from sls.rl.battle_outcome import player_escaped
from sls.rl.history_prefill import RootPrefillCache
from sls.rl.ppo import (
    decision_domains,
    normalize_advantages,
    normalize_advantages_by_domain,
)
from sls.rl.training_contract import native_source_digest, sha256_file
from sls.rl.value_adaptation import state_digest
from sls.rl.workers import VectorWorkerPool
from tools.run_battle_study import freeze_endpoint, initialized_model, make_trainer


def test_gpu_entry_import_does_not_import_cpu_collectors_or_hide_allocated_devices(monkeypatch):
    import importlib

    from tools import run_battle_study
    monkeypatch.setenv('CUDA_VISIBLE_DEVICES','allocated-device-marker')
    importlib.reload(run_battle_study)
    assert os.environ['CUDA_VISIBLE_DEVICES']=='allocated-device-marker'


def test_frozen_transfer_loads_identical_bytes_without_remigration_or_rng_change(tmp_path,monkeypatch):
    from tools import run_battle_study
    model=initialized_model({},tiny=True)
    payload={'schema':'sls-frozen-policy-transfer-v1','original_parent_sha256':PARENT_SHA,
        'model_state_sha256':state_digest(model.state_dict()),'model_config':asdict(model.config),
        'model':model.state_dict()}
    path=tmp_path/'frozen.pt'
    torch.save(payload,path)
    def forbidden(*args,**kwargs):
        raise AssertionError('frozen bytes must bypass platform-specific migration')
    monkeypatch.setattr(run_battle_study,'migrate_v5_role_policy',forbidden)
    for seed in (1,999):
        torch.manual_seed(seed)
        rng=torch.get_rng_state().clone()
        clone=initialized_model({'migrated_parent':str(path)})
        assert torch.equal(rng,torch.get_rng_state())
        assert state_digest(clone.state_dict())==payload['model_state_sha256']
    payload['model'][next(iter(payload['model']))].add_(1)
    torch.save(payload,path)
    with pytest.raises(ValueError,match='frozen V6 parent'):
        initialized_model({'migrated_parent':str(path)})


def test_actual_stock_smoke_escape_is_not_a_short_task_victory():
    fixture=json.loads(Path('tests/fixtures/regressions/act2-smoke-victory-heal-131100063.json').read_text())
    backend=SimulatorBackend(IRONCLAD_A20_ACT2)
    before=backend.load_checkpoint(fixture['before'])
    action=next(a for a in before.actions if a.kind.value=='USE_POTION' and a.subject_id=='POTION:0')
    transition=backend.step(action)
    assert transition.decision.observation.screen.value=='MAP'
    assert transition.decision.observation.player.current_hp==fixture['expected_hp']
    assert player_escaped(before,action,transition)
    curriculum=object.__new__(BattleCurriculum)
    curriculum.active={'0':{'suffix':[]}}
    curriculum.outcomes={}
    task=curriculum.observe_transitions([before],[action.candidate_id],[transition])[0]
    assert task.terminated and task.reward==-1 and not task.info['success']
    assert task.info['reason']=='ESCAPE' and curriculum.outcomes['escape']==1
    assert not transition.terminated and transition.reward==0


@pytest.fixture()
def bank(tmp_path):
    recorded=json.loads(Path('tests/fixtures/regressions/battle-study-natural-prefixes.json').read_text())['rows']
    groups={role:[] for role in ('fit','development','confirmation')}
    for record in recorded:
        backend=SimulatorBackend(IRONCLAD_A20_ACT2)
        decision=backend.reset(record['source_seed'])
        for serialized in record['prefix']:
            action=Action.from_dict(serialized)
            assert action in decision.actions
            decision=backend.step(action).decision
        assert decision.observation.run.act==2 and decision.observation.screen.value=='COMBAT'
        row={**record,'sample_id':str(record['source_seed']),'ordinal_in_source':2,
             'native_source_sha256':native_source_digest(),'projection_sha256':projection_digest(),
             'checkpoint':backend.checkpoint()}
        groups[record['split']].append(row)
    directory=tmp_path/'bank'
    directory.mkdir()
    for role,rows in groups.items():
        (directory/f'{role}.json.gz').write_bytes(gzip.compress(json.dumps(rows).encode()))
    manifest={'schema':SCHEMA,'projection_sha256':projection_digest(),'projection_revision':PROJECTION_REVISION,
        'selection':'THIRD_ACT2_COMBAT_STOP_BEFORE_TARGET_ACTION_NO_OUTCOME_FILTER',
        'confirmation_target_actions_executed':0,'weights_unchanged':True,
        'native_source_sha256':native_source_digest(),'quotas':{r:len(v) for r,v in groups.items()},
        'root_sources':{r:[v['source_seed'] for v in values] for r,values in groups.items()},
        'files':{f'{r}.json.gz':sha256_file(directory/f'{r}.json.gz') for r in groups}}
    (directory/'manifest.json').write_text(json.dumps(manifest))
    return directory


def config(bank,tmp_path):
    path=tmp_path/'config.toml'
    path.write_text('test_engineering_only = true\n')
    return {'bank':str(bank),'prefix_decision_cap':100000,
        'base_config':'configs/train/ironclad_a20_act12_normalstart_a_r1.toml',
        'training_seed':1600002000000,'_path':str(path)}


def test_prefill_matches_live_history_and_cache_invalidates_after_weight_change(bank):
    rows,_=load_bank(bank,'fit')
    model=initialized_model({},tiny=True).eval()
    costs=[]
    cache=RootPrefillCache(rows,costs.append)
    before=state_digest(model.state_dict())
    rng=torch.get_rng_state().clone()
    contexts=cache.root_contexts(model,[0,1])
    assert torch.equal(rng,torch.get_rng_state())
    for row,context in zip(rows,contexts,strict=True):
        _,_,live=rebuild_context(model,row)
        torch.testing.assert_close(live.memory,context.memory,atol=1e-6,rtol=1e-6)
        assert live.previous_action==context.previous_action
        assert live.previous_reward==context.previous_reward
    assert state_digest(model.state_dict())==before
    count=sum(costs)
    cache.root_contexts(model,[1,0])
    assert sum(costs)==count
    model.train()
    with pytest.raises(ValueError,match='eval mode'):
        cache.root_contexts(model,[0])
    model.eval()
    with torch.no_grad():
        model.memory.bias_ih.add_(.25)
    changed=cache.root_contexts(model,[0,1])
    assert sum(costs)==2*count
    assert not torch.equal(changed[0].memory,contexts[0].memory)


def test_real_ppo_update_safe_resume_and_evaluation(bank,tmp_path):
    torch.manual_seed(81)
    cfg=config(bank,tmp_path)
    with VectorWorkerPool(IRONCLAD_A20_ACT2,size=1) as workers:
        trainer=make_trainer(cfg,workers,device='cpu',smoke=True,tiny=True)
        original=state_digest(trainer.model.state_dict())
        first=trainer.collect()
        assert torch.isfinite(first.advantages).all()
        assert decision_domains(first.encoded_decisions).unique().tolist()==[0]
        torch.testing.assert_close(normalize_advantages(first.advantages),
            normalize_advantages_by_domain(first.advantages,first.encoded_decisions))
        chunks=trainer._sequence_chunks(first)
        logprob,_,_=trainer._evaluate_sequences(first,chunks)
        times,environments=trainer._sequence_indices(chunks)
        torch.testing.assert_close(logprob.detach().cpu(),first.old_log_probabilities[times,environments].flatten(),atol=1e-6,rtol=1e-6)
        metrics=trainer.optimize(first)
        assert metrics and state_digest(trainer.model.state_dict())!=original
        endpoint=freeze_endpoint(tmp_path/'endpoint.pt',trainer)
        endpoint_digest=sha256_file(endpoint)
        assert freeze_endpoint(endpoint,trainer)==endpoint
        assert sha256_file(endpoint)==endpoint_digest
        checkpoint=save_checkpoint(tmp_path/'safe.pt',trainer)
        second=trainer.collect()
        trainer.optimize(second)
        with pytest.raises(ValueError,match='immutable endpoint'):
            freeze_endpoint(endpoint,trainer)
        expected=copy.deepcopy(trainer.model.state_dict())
        states=workers.checkpoints()
        load_checkpoint(checkpoint,trainer)
        repeated=trainer.collect()
        assert torch.equal(second.action_indices,repeated.action_indices)
        assert torch.equal(second.returns,repeated.returns)
        trainer.optimize(repeated)
        assert workers.checkpoints()==states
        for key,value in trainer.model.state_dict().items():
            torch.testing.assert_close(value,expected[key],atol=1e-7,rtol=1e-6)
    rows,_=load_bank(bank,'development')
    result=evaluate_roots(trainer.model,rows)
    assert result['episodes']==1 and result['decisions']>0
    assert all(r['root_value'] is not None for r in result['results'])


def test_partial_optimizer_failure_cannot_change_safe_checkpoint(bank,tmp_path,monkeypatch):
    with VectorWorkerPool(IRONCLAD_A20_ACT2,size=1) as workers:
        trainer=make_trainer(config(bank,tmp_path),workers,device='cpu',smoke=True,tiny=True)
        checkpoint=save_checkpoint(tmp_path/'safe.pt',trainer)
        digest=sha256_file(checkpoint)
        before=state_digest(trainer.model.state_dict())
        rollout=trainer.collect()
        original=trainer.optimizer.step
        def fail(*args,**kwargs):
            original(*args,**kwargs)
            raise RuntimeError('injected partial Adam failure')
        monkeypatch.setattr(trainer.optimizer,'step',fail)
        with pytest.raises(RuntimeError,match='partial Adam'):
            trainer.optimize(rollout)
        assert sha256_file(checkpoint)==digest
        load_checkpoint(checkpoint,trainer)
        assert state_digest(trainer.model.state_dict())==before


def test_bank_seal_and_byte_tampering_rejected(bank):
    with pytest.raises(ValueError,match='sealed'):
        load_bank(bank,'confirmation')
    path=bank/'development.json.gz'
    path.write_bytes(path.read_bytes()+b'corrupt')
    with pytest.raises(ValueError,match='hash mismatch'):
        load_bank(bank,'development')


def test_primary_analysis_rejects_repeated_origin_as_independent_evidence():
    report={'results':[{'sample_id':'one','success':True},{'sample_id':'one','success':False}]}
    with pytest.raises(ValueError,match='exactly one paired'):
        paired_result(report,report)
