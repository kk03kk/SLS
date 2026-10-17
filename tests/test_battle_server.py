import argparse
import json
from pathlib import Path

import pytest

from tools import battle_server


def test_one_allocation_contains_smoke_then_training_entry_and_safe_signal(tmp_path,monkeypatch):
    monkeypatch.setattr(battle_server,'ROOT',tmp_path)
    args=argparse.Namespace(partition='gpu-long',gpu_type='a100-40',account='allusers',qos='normal',
        walltime='12:00:00',run='local/runs/example')
    command=battle_server.sbatch_command(args)
    assert '--signal=B:TERM@300' in command
    assert '--gres=gpu:a100-40:1' in command
    assert 'tools.battle_server pipeline' in command[-1]
    assert command[-1].startswith('--wrap=exec ')


@pytest.mark.parametrize('field,value',[('partition',None),('gpu_type',None),('partition','gpu;bad'),('run','../outside')])
def test_submission_requires_explicit_safe_resources(tmp_path,monkeypatch,field,value):
    monkeypatch.setattr(battle_server,'ROOT',tmp_path)
    args=argparse.Namespace(partition='gpu-long',gpu_type='a100-40',account='allusers',qos='normal',
        walltime='12:00:00',run='local/runs/example')
    setattr(args,field,value)
    with pytest.raises(ValueError):
        battle_server.sbatch_command(args)


def test_verify_rejects_data_or_code_changes_without_importing_runtime(tmp_path,monkeypatch):
    monkeypatch.setattr(battle_server,'ROOT',tmp_path)
    artifact=tmp_path/'local/runs/parent.pt'
    artifact.parent.mkdir(parents=True)
    artifact.write_bytes(b'unique existing parent')
    source=tmp_path/'tools/source.py'
    source.parent.mkdir()
    source.write_bytes(b'print(1)\n')
    manifest={'files':{'local/runs/parent.pt':battle_server.sha(artifact)},
        'code':{'tools/source.py':battle_server.sha(source)},'git_commit':'recorded'}
    path=tmp_path/'local/reports/act2-study-r1/deployment-manifest.json'
    path.parent.mkdir(parents=True)
    path.write_text(json.dumps(manifest))
    assert battle_server.verify_install()['status']=='PASS'
    source.write_text('print(2)\n')
    with pytest.raises(ValueError,match='code changed'):
        battle_server.verify_install()
    assert artifact.read_bytes()==b'unique existing parent'


def test_pipeline_is_for_allocated_jobs_only(monkeypatch):
    monkeypatch.delenv('SLURM_JOB_ID',raising=False)
    with pytest.raises(ValueError,match='allocated'):
        battle_server.pipeline(Path('local/runs/example'))
