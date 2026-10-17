"""CPU-only qualification, real training smoke and readiness verification."""
from __future__ import annotations

import argparse
import json
import os
import subprocess
import sys
import xml.etree.ElementTree as ET
from dataclasses import asdict
from pathlib import Path

os.environ['CUDA_VISIBLE_DEVICES']='-1'
os.environ['SLS_TEST_CUDA']='0'
for key in ('OMP_NUM_THREADS','MKL_NUM_THREADS','OPENBLAS_NUM_THREADS'):
    os.environ[key]='2'

from sls.rl.battle_bank import load_bank, write_json
from sls.rl.normalstart_release import RELEASE, release_bindings
from sls.rl.training_contract import (
    ROOT,
    local_source_digest,
    sha256_file,
    training_validation_digest,
)

CONFIG=Path('configs/train/ironclad_a20_battle_study_r1.toml')
PREPARATION_SOURCES=('tools/prepare_battle_study.py','tools/collect_battle_bank.py',
    'tools/audit_battle_bank.py','tools/diagnose_battle_bank.py','tools/power_battle_study.py',
    'tools/prepare_act12_normalstart_study.py','src/sls/audit/development_seeds.py')
IMPACT=('test_chest_key_interactions','test_dynamic_permanent_deck','test_revival_boundaries',
        'test_event_constructor_timing','test_battle_master_deck_restore','test_large_combat_piles',
        'test_exploder_fatal_stock','test_suicide_order','test_flight_headbutt_order',
        'test_nested_callback_restore','test_sparse_monster_extent_restore','test_dormant_selector_restore')


def invoke(root,name,arguments):
    log=root/f'{name}.log'
    with log.open('w',encoding='utf-8') as stream:
        result=subprocess.run([sys.executable,*map(str,arguments)],cwd=ROOT,env=os.environ,
                              stdout=stream,stderr=subprocess.STDOUT)
    if result.returncode:
        raise RuntimeError(f'{name} failed; inspect {log}')
    print(f'{name}: PASS',flush=True)


def ready(root):
    from tools.run_battle_study import identity, load_config
    config=load_config(CONFIG)
    expected=identity(config,CONFIG)
    lock=json.loads((root/'validation-source.json').read_text())
    if (lock['training_validation_sha256']!=training_validation_digest()
            or lock['preparation_tool_sha256']!=sha256_file(Path(__file__))
            or lock['preparation_sources_sha256']!=local_source_digest(PREPARATION_SOURCES)
            or lock['test_source_sha256']!=local_source_digest(('tests',))
            or lock['junit_sha256']!=sha256_file(root/'full-cpu.xml')):
        raise ValueError('executed validation source or JUnit changed')
    _,manifest=load_bank(Path(config['bank']),'fit')
    if manifest['quotas']!={'fit':64,'development':64,'confirmation':384}:
        raise ValueError('formal quota incomplete')
    reports={name:json.loads((root/name).read_text()) for name in
             ('bank-audit.json','tiny-smoke/result.json','parent-smoke/result.json','diagnostics.json')}
    plan=json.loads((ROOT/'configs/experiments/ironclad_a20_battle_study_r1.json').read_text())
    reservation=json.loads((root/'seed-reservations.json').read_text())
    if (reservation['source_reservations']!=plan['source_reservations']
            or reservation['final_set_used'] or not reservation['scanned_inputs']):
        raise ValueError('external seed-history scan missing or inconsistent')
    for name in ('tiny-smoke/result.json','parent-smoke/result.json'):
        if reports[name]['status']!='PASS' or reports[name]['device']!='cpu' or reports[name]['identity']!=expected:
            raise ValueError('CPU pipeline smoke stale or failed')
    if reports['parent-smoke/result.json']['tiny'] or not reports['tiny-smoke/result.json']['tiny']:
        raise ValueError('real-parent and small-model CPU checks both required')
    audit=reports['bank-audit.json']
    if (audit['status']!='PASS' or audit['bank_sha256']!=expected['bank_sha256'] or audit['confirmation_target_actions_executed']
            or audit.get('parent_memory_checks')!=512
            or len(audit.get('battle_continuations',[]))!=32
            or sum(r['complete'] for r in audit['battle_continuations'])<8):
        raise ValueError('bank legality/seal audit missing')
    baseline=reports['diagnostics.json']['development_greedy']
    if baseline['episodes']!=64 or not .2<=baseline['success_rate']<=.8:
        raise ValueError('fresh development baseline saturated or incomplete; redesign task before GPU')
    suite=ET.parse(root/'full-cpu.xml').getroot()
    cases=list(suite.iter('testcase'))
    if not cases or any(c.find(tag) is not None for c in cases for tag in ('failure','error')):
        raise ValueError('full CPU test suite failed')
    for c in cases:
        skip=c.find('skipped')
        if skip is not None and not any(s in skip.attrib.get('message','') for s in
                ('CUDA','local exported Act1 policy is unavailable')):
            raise ValueError('unexpected missing CPU evidence')
    receipt={'status':'READY_FOR_SERVER_SMOKE','identity':expected,
        'cpu_cases':len(cases),'cpu_skips':sum(c.find('skipped') is not None for c in cases),
        'excluded_four_environment_tests':2,'device':'cpu','local_gpu_used':False,
        'server_execution':'NOT_RUN','learning_effect':'NOT_RUN',
        'formal_training_requires':'SAME_ALLOCATION_REAL_PARENT_FORMAL_SHAPE_CUDA_SMOKE_PASS',
        'evidence':{str(p.relative_to(ROOT)):sha256_file(p) for p in
            [root/'full-cpu.xml',root/'bank-audit.json',root/'tiny-smoke/result.json',root/'parent-smoke/result.json',
             root/'diagnostics.json',root/'power.json',root/'seed-reservations.json',ROOT/RELEASE/'qualification.json']}}
    return receipt


def validate(root):
    root.mkdir(parents=True,exist_ok=False)
    import tomllib
    config=tomllib.loads((ROOT/CONFIG).read_text())
    source=training_validation_digest()
    preparation_source=local_source_digest(PREPARATION_SOURCES)
    test_source=local_source_digest(('tests',))
    import torch

    torch.set_num_threads(2)
    torch.set_num_interop_threads(2)

    from sls.rl.value_adaptation import state_digest
    from tools.run_battle_study import initialized_model
    # Freeze migration bytes locally. Cross-platform RNG equality is not an
    # assumption of the server launch contract, even with a fixed seed.
    migrated=ROOT/config['migrated_parent']
    expected=json.loads((ROOT/config['bank']/'manifest.json').read_text())['parent_model_sha256']
    if not migrated.exists():
        original={k:v for k,v in config.items() if k!='migrated_parent'}
        model=initialized_model(original).eval()
        if state_digest(model.state_dict())!=expected:
            raise ValueError('local migration differs from collected bank parent')
        migrated.parent.mkdir(parents=True,exist_ok=True)
        temporary=migrated.with_suffix('.tmp')
        torch.save({'schema':'sls-frozen-policy-transfer-v1',
            'original_parent_sha256':sha256_file(ROOT/config['parent']),
            'model_state_sha256':expected,'model_config':asdict(model.config),
            'migration_seed':39171,'model':model.state_dict()},temporary)
        temporary.replace(migrated)
    model=initialized_model(config).eval()
    if state_digest(model.state_dict())!=expected:
        raise ValueError('frozen migration differs from bank parent')
    receipt=migrated.parent/'identity.json'
    payload={'schema':'sls-frozen-policy-transfer-identity-v1',
        'original_parent_sha256':sha256_file(ROOT/config['parent']),
        'model_state_sha256':expected,'file_sha256':sha256_file(migrated),
        'migration_seed':39171,'server_gpu_and_learning':'NOT_RUN'}
    if receipt.exists():
        if json.loads(receipt.read_text())!=payload:
            raise ValueError('frozen migration receipt changed')
    else:
        write_json(receipt,payload)
    del model
    from tools.prepare_act12_normalstart_study import check_seed_reservations
    plan_path=ROOT/'configs/experiments/ironclad_a20_battle_study_r1.json'
    plan=json.loads(plan_path.read_text())
    own=ROOT/'local/reports/act2-study-r1'
    scan=check_seed_reservations(excluded=[*own.rglob('*'),plan_path,ROOT/CONFIG],
        allocations=[(role,*bounds) for role,bounds in plan['source_reservations'].items()])
    write_json(root/'seed-reservations.json',{'source_reservations':plan['source_reservations'],
        'scanned_inputs':scan['scanned_inputs'],'final_set_used':False,
        'excluded_own_source_replays':str(own),'same_seed_replays_are_not_independent_samples':True,
        'timing':'FINAL_EXTERNAL_HISTORY_AUDIT_AFTER_SOURCE_COLLECTION_BEFORE_TARGET_CONFIRMATION_OR_GPU'})
    # Pre-publication regression does not pretend missing new release is a
    # pass. Its portable release tests run in the final, unfiltered CPU suite.
    common=['-m','pytest','-q','--tb=short','-k',
        'not test_sharded_worker_hosts_multiple_environments_per_process and not test_sharded_reset_many_preserves_requested_index_and_seed_order']
    if not (ROOT/RELEASE/'qualification.json').exists():
        invoke(root,'prepublication',[*common,'--ignore=tests/rl/test_normalstart_release.py',
                                     f'--junitxml={root}/prepublication.xml'])
        qualification=root/'qualification'
        qualification.mkdir()
        from tools.prepare_act12_normalstart_study import check_seed_reservations
        scan=check_seed_reservations(excluded=list(root.rglob('*')),
            allocations=[('battle-qualification',8000105000000,8000105010000)])
        allocation={'profile':'IRONCLAD_A20_ACT2','seed_range':[8000105000000,8000105010000],
            'purpose':'CURRENT_SOURCE_DEVELOPMENT_QUALIFICATION_NOT_TRAINING_OR_CONFIRMATION',
            'scanned_inputs':scan['scanned_inputs'],'final_set_used':False}
        write_json(qualification/'development-seeds.json',allocation)
        invoke(root,'boundaries',['-m','tools.verify_act12_foundation','--checkpoint',config['parent'],
            '--output',qualification/'boundaries.json','--seed-start','8000105000000'])
        invoke(root,'rule-replay',['-m','tools.revalidate_act12_simulator','--foundation','local/reports/act12-foundation',
            '--oracle-build','local/build/oracle/Act12Foundation.build.json','--output-directory',qualification/'replay'])
        invoke(root,'impact',['-m','pytest','-q',*[f'tests/simulator/{name}.py' for name in IMPACT],
            'tests/model/test_selection_quantity.py','tests/audit/test_public_projection_migration.py',
            f'--junitxml={qualification}/impact-tests.xml'])
        invoke(root,'qualify',['-m','tools.qualify_normalstart_act12','--root',qualification,'--output',qualification/'qualification.json'])
        invoke(root,'publish',['-m','tools.publish_normalstart_release','--qualification',qualification/'qualification.json',
            '--previous-release','tests/fixtures/evidence/act12-normalstart'])
    release_bindings(ROOT)
    invoke(root,'full-cpu',[*common,f'--junitxml={root}/full-cpu.xml'])
    write_json(root/'validation-source.json',{'training_validation_sha256':training_validation_digest(),
        'preparation_tool_sha256':sha256_file(Path(__file__)),
        'preparation_sources_sha256':local_source_digest(PREPARATION_SOURCES),
        'test_source_sha256':local_source_digest(('tests',)),'junit_sha256':sha256_file(root/'full-cpu.xml')})
    invoke(root,'configuration',['-m','tools.check_training_configs'])
    invoke(root,'bank-audit',['-m','tools.audit_battle_bank','--bank',config['bank'],'--output',root/'bank-audit.json'])
    for tiny,name in ((True,'tiny-smoke'),(False,'parent-smoke')):
        invoke(root,name,['-m','tools.run_battle_study','smoke','--device','cpu','--output',root/name,
                         *(['--tiny'] if tiny else [])])
    invoke(root,'diagnostics',['-m','tools.diagnose_battle_bank','--bank',config['bank'],'--output',root/'diagnostics.json'])
    invoke(root,'power',['-m','tools.power_battle_study','--output',root/'power.json'])
    if (training_validation_digest()!=source
            or local_source_digest(PREPARATION_SOURCES)!=preparation_source
            or local_source_digest(('tests',))!=test_source):
        raise ValueError('training sources changed while validating; rerun final sources')
    result=ready(root)
    write_json(root/'readiness.json',result)
    return result


def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--output',type=Path,default=Path('local/reports/act2-study-r1/validation'))
    parser.add_argument('--check-ready',action='store_true')
    args=parser.parse_args()
    os.chdir(ROOT)
    args.output=args.output.resolve()
    result=ready(args.output) if args.check_ready else validate(args.output)
    print(json.dumps(result,indent=2))


if __name__=='__main__':
    main()
