"""Stdlib-only study deployment, inspection, single-allocation pipeline and export.

No remote access is initiated by this tool. Run inspect/submit on the login
host yourself. PyTorch and native inference only run in an allocated job.
"""
from __future__ import annotations

import argparse
import hashlib
import importlib.metadata
import importlib.util
import json
import os
import shlex
import shutil
import subprocess
import sys
import tarfile
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
CONFIG='configs/train/ironclad_a20_battle_study_r1.toml'


def sha(path):
    digest=hashlib.sha256()
    with Path(path).open('rb') as stream:
        for block in iter(lambda:stream.read(1024*1024),b''):
            digest.update(block)
    return digest.hexdigest()


def command(args,*,required=True):
    process=subprocess.run(args,cwd=ROOT,text=True,capture_output=True)
    if required and process.returncode:
        raise RuntimeError(f'{shlex.join(map(str,args))}: {process.stderr.strip()}')
    return {'command':list(map(str,args)),'exit_code':process.returncode,
            'stdout':process.stdout,'stderr':process.stderr}


def pack(output,validation):
    if output.exists():
        raise FileExistsError('bundle is immutable')
    validation=validation.resolve()
    if not validation.is_relative_to(ROOT/'local/reports'):
        raise ValueError('validation evidence must be inside local/reports')
    validation=validation.relative_to(ROOT)
    command([sys.executable,'-m','tools.prepare_battle_study','--check-ready','--output',str(validation)])
    import tomllib
    config=tomllib.loads((ROOT/CONFIG).read_text())
    paths=[Path(config['parent']),Path(config['migrated_parent']),Path(config['migrated_parent']).parent/'identity.json']
    paths += [Path(config['bank'])/name for name in
              ('manifest.json','contract.json','fit.json.gz','development.json.gz','confirmation.json.gz','source-ledger.json',
               'collector-source.py.txt','bank-source.py.txt','collection-source-receipt.json')]
    paths += [validation/name for name in ('readiness.json','validation-source.json','full-cpu.xml',
                'bank-audit.json','tiny-smoke/result.json','parent-smoke/result.json','diagnostics.json','power.json',
                'seed-reservations.json')]
    # Source is delivered by Git, never a zip overlay on the server checkout.
    source=command(['git','rev-parse','HEAD'])['stdout'].strip()
    tracked=command(['git','ls-files','src','native','tools','configs','tests','pyproject.toml'])['stdout'].splitlines()
    dirty=command(['git','diff','HEAD','--name-only','--','src','native','tools','configs','tests','pyproject.toml'])['stdout'].strip()
    if dirty:
        raise ValueError('commit validated execution sources before freezing deployment identity')
    required=('tools/run_battle_study.py','tools/battle_server.py','src/sls/rl/battle_curriculum.py',
              'src/sls/rl/history_prefill.py','src/sls/rl/battle_evaluation.py',CONFIG)
    if not set(required)<=set(tracked):
        raise ValueError('required study sources must be delivered by Git before packaging')
    code={p:hashlib.sha256((ROOT/p).read_bytes().replace(b'\r\n',b'\n')).hexdigest()
          for p in tracked if (ROOT/p).is_file()}
    manifest={'schema':'sls-battle-deployment-v1','git_commit':source,'code':code,
              'files':{p.as_posix():sha(ROOT/p) for p in paths},'config':CONFIG}
    metadata=ROOT/'local/reports/act2-study-r1/deployment-manifest.json'
    if metadata.exists():
        if json.loads(metadata.read_text())!=manifest:
            raise ValueError('deployment identity already frozen with different bytes')
    else:
        metadata.write_text(json.dumps(manifest,indent=2)+'\n',encoding='utf-8')
    output.parent.mkdir(parents=True,exist_ok=True)
    with tarfile.open(output,'w:gz') as archive:
        for p in paths:
            archive.add(ROOT/p,arcname=p.as_posix(),recursive=False)
        archive.add(metadata,arcname='local/reports/act2-study-r1/deployment-manifest.json',recursive=False)
    receipt={'bundle':str(output.resolve()),'sha256':sha(output),'bytes':output.stat().st_size,
             'git_commit':source,'remote_execution':False}
    output.with_suffix(output.suffix+'.json').write_text(json.dumps(receipt,indent=2)+'\n')
    return receipt


def install(bundle):
    # Reject links/traversal/overwrites. Existing identical parent bytes reuse
    # their original path; no historical checkpoint is replaced.
    with tarfile.open(bundle,'r:gz') as archive:
        members=archive.getmembers()
        manifests=[m for m in members if m.name=='local/reports/act2-study-r1/deployment-manifest.json']
        if len(manifests)!=1:
            raise ValueError('missing deployment identity')
        manifest=json.load(archive.extractfile(manifests[0]))
        allowed={*manifest['files'],'local/reports/act2-study-r1/deployment-manifest.json'}
        if {m.name for m in members}!=allowed or len(members)!=len(allowed):
            raise ValueError('unexpected or duplicate bundle member')
        for member in members:
            target=(ROOT/member.name).resolve()
            if not member.isfile() or not target.is_relative_to(ROOT):
                raise ValueError('unsafe bundle path/link')
            data=archive.extractfile(member).read()
            expected=manifest['files'].get(member.name)
            if expected and hashlib.sha256(data).hexdigest()!=expected:
                raise ValueError('bundle content hash mismatch')
            if target.exists():
                if target.read_bytes()!=data:
                    raise FileExistsError(f'refuse to replace existing artifact: {member.name}')
            else:
                target.parent.mkdir(parents=True,exist_ok=True)
                with target.open('xb') as stream:
                    stream.write(data)
    return verify_install()


def verify_install():
    manifest=json.loads((ROOT/'local/reports/act2-study-r1/deployment-manifest.json').read_text())
    for name,digest in manifest['files'].items():
        if sha(ROOT/name)!=digest:
            raise ValueError(f'artifact changed: {name}')
    for name,digest in manifest['code'].items():
        if hashlib.sha256((ROOT/name).read_bytes().replace(b'\r\n',b'\n')).hexdigest()!=digest:
            raise ValueError(f'code changed or not pulled: {name}')
    return {'status':'PASS','git_commit':manifest['git_commit'],'artifact_files':len(manifest['files']),
            'code_files':len(manifest['code'])}


def inspect_server():
    if os.name=='nt':
        raise ValueError('inspect is for the Linux login host, not a remote SSH client')
    packages={}
    spec=importlib.util.find_spec('sls')
    for name in ('torch','numpy','pytest'):
        try:
            packages[name]=importlib.metadata.version(name)
        except importlib.metadata.PackageNotFoundError:
            packages[name]='MISSING'
    return {'python':sys.executable,'version':sys.version,'python_312':sys.version_info[:2]==(3,12),
        'sls_import_origin':spec.origin if spec else None,
        'expected_source':str(ROOT/'src/sls/__init__.py'),
        'package_metadata_only':packages,'disk':list(shutil.disk_usage(ROOT)),
        'tools':{name:shutil.which(name) for name in ('git','cmake','clang++','g++','sbatch','srun','sinfo')},
        'sinfo':command(['sinfo','-o','%P %G %l %a']),
        'associations':command(['sacctmgr','-n','show','assoc',f'user={os.environ.get("USER","")}',
                                'format=Account,Partition,QOS'],required=False),
        'files':verify_install()}


def sbatch_command(args):
    if not args.partition or not args.gpu_type:
        raise ValueError('choose verified partition and GPU type explicitly after inspect')
    for value in (args.partition,args.gpu_type,args.account,args.qos,args.walltime):
        if not value or any(c not in 'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-_:.' for c in value):
            raise ValueError('invalid scheduler identifier')
    run=Path(args.run)
    if run.is_absolute() or '..' in run.parts or not run.as_posix().startswith('local/runs/'):
        raise ValueError('study run must be inside local/runs')
    (ROOT/'local/runs/slurm-logs').mkdir(parents=True,exist_ok=True)
    invocation=[sys.executable,'-m','tools.battle_server','pipeline','--run',str(run)]
    return ['sbatch','--parsable',f'--account={args.account}',f'--qos={args.qos}',
        f'--partition={args.partition}',f'--gres=gpu:{args.gpu_type}:1','--cpus-per-task=16','--mem=64G',
        f'--time={args.walltime}','--signal=B:TERM@300','--job-name=sls-battle-r1',f'--chdir={ROOT}',
        '--export=ALL,CUBLAS_WORKSPACE_CONFIG=:4096:8',
        '--output=local/runs/slurm-logs/battle-%j.out','--error=local/runs/slurm-logs/battle-%j.err',
        '--wrap=exec '+shlex.join(invocation)]


def pipeline(run):
    if not os.environ.get('SLURM_JOB_ID'):
        raise ValueError('pipeline requires allocated Slurm job')
    verify_install()
    if sys.version_info[:2]!=(3,12):
        raise ValueError('validated study requires Python 3.12 environment')
    env={**os.environ,'OMP_NUM_THREADS':'2','MKL_NUM_THREADS':'2','OPENBLAS_NUM_THREADS':'2',
         'PYTHONPATH':str(ROOT/'src'),
         'PYTHONPYCACHEPREFIX':str(ROOT/'local/build/python-cache'),'CUBLAS_WORKSPACE_CONFIG':':4096:8'}
    subprocess.run([sys.executable,'tools/build_native.py','--jobs','2'],cwd=ROOT,env=env,check=True)
    subprocess.run(['nvidia-smi'],cwd=ROOT,env=env,check=True)
    subprocess.run([sys.executable,'-m','pytest','-q','tests/simulator/test_nested_callback_restore.py',
                    'tests/simulator/test_sparse_monster_extent_restore.py',
                    'tests/simulator/test_dormant_selector_restore.py','tests/model/test_selection_quantity.py'],
                   cwd=ROOT,env={**env,'CUDA_VISIBLE_DEVICES':'-1','SLS_TEST_CUDA':'0'},check=True,timeout=300)
    smoke=Path(run).with_name(Path(run).name+f'-smoke-{os.environ["SLURM_JOB_ID"]}')
    subprocess.run([sys.executable,'-m','tools.run_battle_study','smoke','--device','cuda',
                    '--output',str(smoke)],cwd=ROOT,env=env,check=True,timeout=1800)
    proof=json.loads((ROOT/smoke/'result.json').read_text())
    # Conservative planning estimate, not a throughput guarantee. Fail before
    # long training if even measured first-update/evaluation cost cannot fit.
    import tomllib
    config=tomllib.loads((ROOT/CONFIG).read_text())
    estimated=123*proof['first_update_seconds']+2600*proof['evaluation_seconds']/max(proof['evaluation']['episodes'],1)
    if estimated>config['max_wall_seconds']:
        raise RuntimeError(f'BUDGET_BLOCK: conservative smoke projection {estimated:.0f}s exceeds registered wall budget')
    # exec preserves Slurm's early TERM delivery to the safe-stop controller.
    os.environ.update(env)
    os.execv(sys.executable,[sys.executable,'-m','tools.run_battle_study','train','--device','cuda',
                '--output',str(run),'--smoke-receipt',str(smoke/'result.json')])


def export(run,output):
    run=Path(run)
    if not run.resolve().is_relative_to(ROOT/'local/runs') or output.exists():
        raise ValueError('invalid export source or existing output')
    files=[p for p in run.rglob('*') if p.is_file() and p.suffix in ('.json','.jsonl')]
    with tarfile.open(output,'w:gz') as archive:
        for path in files:
            archive.add(path,arcname=str(path.relative_to(ROOT)))
    return {'export':str(output),'sha256':sha(output),'files':len(files),'weights_included':False}


def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('mode',choices=('pack','install','verify','inspect','submit','pipeline','status','export'))
    parser.add_argument('--bundle',type=Path)
    parser.add_argument('--output',type=Path)
    parser.add_argument('--validation',type=Path,default=Path('local/reports/act2-study-r1/validation'))
    parser.add_argument('--run',default='local/runs/ironclad-a20-battle-r1')
    parser.add_argument('--partition')
    parser.add_argument('--gpu-type')
    parser.add_argument('--account',default='allusers')
    parser.add_argument('--qos',default='normal')
    parser.add_argument('--walltime',default='12:00:00')
    parser.add_argument('--dry-run',action='store_true')
    args=parser.parse_args()
    os.chdir(ROOT)
    if args.mode=='pack':
        result=pack(args.output,args.validation)
    elif args.mode=='install':
        result=install(args.bundle)
    elif args.mode=='verify':
        result=verify_install()
    elif args.mode=='inspect':
        result=inspect_server()
    elif args.mode=='submit':
        cmd=sbatch_command(args)
        if args.dry_run:
            result={'command':shlex.join(cmd),'submitted':False}
        else:
            verify_install()
            inventory=command(['sinfo','-h','-p',args.partition,'-o','%G'])['stdout']
            if args.gpu_type not in inventory:
                raise ValueError('requested GPU type not advertised in selected partition; inspect actual resources')
            result=command(cmd)
    elif args.mode=='pipeline':
        pipeline(args.run)
        return 0
    elif args.mode=='export':
        result=export(ROOT/args.run,args.output)
    else:
        manifest=ROOT/args.run/'run-manifest.json'
        metrics=ROOT/args.run/'metrics.jsonl'
        latest=None
        if metrics.exists():
            for line in reversed(metrics.read_text().splitlines()):
                try:
                    latest=json.loads(line)
                    break
                except json.JSONDecodeError:
                    continue
        result={'manifest':json.loads(manifest.read_text()) if manifest.exists() else 'NOT_STARTED',
                'latest_metrics':latest}
    print(json.dumps(result,ensure_ascii=False,indent=2))
    return 0


if __name__=='__main__':
    raise SystemExit(main())
