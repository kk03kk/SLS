"""Reject damaged identities and keep genuine diagnostic outcomes distinct."""
import hashlib
import json
import os
import subprocess
import sys
import zipfile
from pathlib import Path

import pytest

from tools import replay_frozen_key_reference as replay


@pytest.fixture
def sealed_prefix(tmp_path,monkeypatch):
    # Synthetic identity envelope around real public regression steps; no game launch.
    fixture = json.loads(Path('tests/fixtures/regressions/natural-three-key-stock-r1.json').read_text())
    steps = []
    public,private = [],[]
    for index,step in enumerate(fixture['steps']):
        expected = dict(observation=step['observation'],candidate_actions=step['legal_actions'],
                        executed_action=step.get('selected_action'),terminal=False,terminal_reason=None,success=False,
                        reward_from_previous=fixture['steps'][index-1]['reward'] if index else 0.0)
        public.append(expected)
        private.append(dict(checkpoint=dict(rng=step['before_rng'])))
        actual = dict(step)
        actual['before_raw'] = dict(_oracle_mode='validation',_rng=step['before_rng'])
        actual['comparisons'] = dict(observation=[],legal_actions=[],rng=[])
        if 'selected_action' in step:
            actual['transition_comparisons'] = dict(reward=[],terminal=[],reason=[],success=[])
        steps.append(actual)
    (tmp_path/'tools').mkdir()
    producer = tmp_path/'tools/capture_frozen_key_reference.py'
    producer.write_bytes(b'synthetic producer identity')
    public_path,private_path = tmp_path/'public.jsonl',tmp_path/'private.jsonl'
    for path,rows in [(public_path,public),(private_path,private)]:
        path.write_text('\n'.join(json.dumps(row) for row in rows)+'\n')
    reference = dict(schema='sls-frozen-three-key-route-probe-v1',execution_complete=True,
                     model_sha256='synthetic-model',current_native_source_sha256=replay.native.NATIVE_SOURCE_SHA256,
                     current_native_binary_sha256=replay.sha(Path(replay.native.__file__)),
                     runs=[dict(seed=fixture['seed'],public_history=str(public_path),private_history=str(private_path),
                                public_sha256=replay.sha(public_path),private_sha256=replay.sha(private_path))])
    reference_path = tmp_path/'reference.json'
    reference_path.write_text(json.dumps(reference))
    source = dict(schema='sls-stock-frozen-key-reference-v1',execution_complete=True,status='REFERENCE_PREFIX_COMPLETE',
                  scope='ACTUAL_NORMAL_START_STOCK_EXECUTED_REFERENCE_ACTIONS_NOT_STOCK_MODEL_INFERENCE',
                  source_sha256=replay.sha(producer),reference_sha256=replay.sha(reference_path),
                  model_sha256=reference['model_sha256'],seed=fixture['seed'],
                  native_reference_source_sha256=reference['current_native_source_sha256'],
                  public_reference_sha256=reference['runs'][0]['public_sha256'],
                  private_reference_sha256=reference['runs'][0]['private_sha256'],
                  actual_final_run=steps[-1]['observation']['run'],steps=steps)
    capture = tmp_path/'capture.json'
    capture.write_text(json.dumps(source))
    oracle = tmp_path/'oracle.jar'
    with zipfile.ZipFile(oracle,'w') as archive:
        archive.writestr('Oracle.class',b'synthetic compiled oracle')
    (tmp_path/'game').mkdir()
    (tmp_path/'game/desktop-1.0.jar').write_bytes(b'synthetic game identity')
    build = dict(schema='sls-oracle-build-v1',used_existing_oracle=False,sources={'Oracle.java':'a'*64},
                 output_sha256=replay.sha(oracle),
                 members={'Oracle.class':hashlib.sha256(b'synthetic compiled oracle').hexdigest()},
                 dependencies={'game':replay.sha(tmp_path/'game/desktop-1.0.jar')})
    oracle.with_suffix('.build.json').write_text(json.dumps(build))
    capture.with_suffix('.launch.json').write_text(json.dumps(dict(mode='validation',oracle_sha256=build['output_sha256'],
                                                                  recovery_status='RECOVERED',completion={'exit_code':0})))
    monkeypatch.setattr(replay,'ROOT',tmp_path)
    monkeypatch.setattr(replay,'original_runtime_paths',lambda _: (tmp_path,tmp_path/'game'))
    return capture,reference_path,oracle,source,reference


def test_replay_rejects_capture_producer_version_error(sealed_prefix):
    capture,reference,oracle,source,_ = sealed_prefix
    source['source_sha256'] = '0'*64
    capture.write_text(json.dumps(source))
    with pytest.raises(ValueError,match='producer version'):
        replay.verify_capture(capture,reference,oracle)


def test_replay_rejects_private_reference_tampering(sealed_prefix):
    capture,reference_path,oracle,_,reference = sealed_prefix
    path = Path(reference['runs'][0]['private_history'])
    path.write_bytes(path.read_bytes()+b'\n')
    with pytest.raises(ValueError,match='history identity'):
        replay.verify_capture(capture,reference_path,oracle)


def test_replay_does_not_trust_stored_zero_difference_claim(sealed_prefix):
    capture,reference,oracle,source,_ = sealed_prefix
    source['steps'][0]['comparisons']['rng'] = [{'path':'$.counter','original':0,'simulator':1}]
    capture.write_text(json.dumps(source))
    with pytest.raises(ValueError,match='comparison disagrees'):
        replay.verify_capture(capture,reference,oracle)


def test_replay_rejects_implicit_native_environment_migration(sealed_prefix):
    capture,reference_path,oracle,source,reference = sealed_prefix
    reference['current_native_source_sha256'] = source['native_reference_source_sha256'] = '0'*64
    reference_path.write_text(json.dumps(reference))
    source['reference_sha256'] = replay.sha(reference_path)
    capture.write_text(json.dumps(source))
    with pytest.raises(ValueError,match='migration is not implicit'):
        replay.verify_capture(capture,reference_path,oracle)


def test_first_transition_divergence_keeps_actual_executed_action_and_checkpoint(sealed_prefix):
    capture,reference,oracle,source,_ = sealed_prefix
    source['steps'] = source['steps'][:1]
    source['steps'][0]['reward'] = 42.0
    # Obtain the real reference reward rather than inventing the historical baseline.
    public = Path(json.loads(reference.read_text())['runs'][0]['public_history'])
    expected = [json.loads(line) for line in public.read_text().splitlines()]
    source['steps'][0]['transition_comparisons']['reward'] = [
        {'path':'$','original':expected[1]['reward_from_previous'],'simulator':42.0}]
    source.update(status='FIRST_TRANSITION_DIVERGENCE',first_divergence=0)
    capture.write_text(json.dumps(source))
    validated,_,_,_ = replay.verify_capture(capture,reference,oracle)
    result = replay.replay_prefix(validated)
    assert not result['observed_stock_prefix_fully_matched'] and result['failed_boundaries'] == [0]
    assert result['executed_replay_actions'] == 1 and len(result['native_checkpoints']) == 2
    assert result['all_full_native_suffixes_equal']


def test_cpu_replayer_overrides_inherited_cuda_and_rejects_gpu_device():
    environment = {**os.environ,'CUDA_VISIBLE_DEVICES':'0'}
    check = subprocess.run([sys.executable,'-c',
                            'import tools.replay_frozen_key_reference; import os; '
                            'assert os.environ["CUDA_VISIBLE_DEVICES"] == "-1"'],
                           env=environment,capture_output=True,text=True,check=False)
    assert check.returncode == 0,check.stderr
    request = subprocess.run([sys.executable,'tools/replay_frozen_key_reference.py',
                              '--capture','unused','--reference-report','unused','--oracle','unused',
                              '--output','unused','--device','cuda'],
                             env=environment,capture_output=True,text=True,check=False)
    assert request.returncode == 2 and 'invalid choice' in request.stderr
