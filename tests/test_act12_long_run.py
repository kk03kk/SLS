from __future__ import annotations

import copy
import json
import signal
import subprocess
import sys
from pathlib import Path

import pytest
import torch

from sls.curriculum import IRONCLAD_A20_ACT2
from sls.model import ModelConfig, Policy
from sls.rl import PPOConfig, PPOTrainer, WorkerPool, load_checkpoint, save_checkpoint
from sls.rl.training_contract import (
    ROOT,
    native_source_digest,
    sha256_file,
    training_implementation_digest,
)
from tools.initialize_act12_continuation import initialize
from tools.prepare_act12_long_run import (
    PERIODIC,
    reject_used_seeds,
    select_arm,
    toml_text,
)
from tools.run_act12_long_run import chunk_budget, safe_status
from tools.train_full_run import MANIFEST_SCHEMA, _training_identity
from tools.verify_act12_diagnostics import assert_same


def fixture(tmp_path):
    import tomllib
    source = tmp_path / 'parent'
    source.mkdir()
    config = tomllib.loads((ROOT / 'configs/train/ironclad_a20_act12_lambda100_r1.toml').read_text())
    config.pop('warm_start')
    config['model'].update(embedding_dim=32, transformer_layers=1, feedforward_dim=64, recurrent_hidden_dim=32)
    config['ppo'].update(rollout_steps=2, recurrent_sequence_length=1, minibatch_sequences=1)
    benchmark = tmp_path / 'parent-preparation/benchmark.json'
    benchmark.parent.mkdir()
    benchmark.write_text(json.dumps({'selected_workers': 1, 'selected_shards': 1}))
    config['run'].update(output=source.as_posix(), device='cpu', worker_layout=[1, 1], benchmark=benchmark.as_posix())
    parent_path = source / 'training-config.toml'
    parent_path.write_text(toml_text(config), encoding='utf-8')
    with WorkerPool(IRONCLAD_A20_ACT2, 1) as workers:
        trainer = PPOTrainer(Policy(ModelConfig(**config['model'])), workers, PPOConfig(**config['ppo']),
                             seed=config['run']['seed'], training_seed_limit=config['run']['training_seed_limit'],
                             training_config_digest=_training_identity(config, workers=1, shards=1))
        trainer.train_update()
        trainer.environment_steps = 94_027_776
        save_checkpoint(source / 'latest.pt', trainer)
        save_checkpoint(source / 'final.pt', trainer)
    manifest = {'schema': MANIFEST_SCHEMA, 'status': 'COMPLETE', 'stages': {'train': {'status': 'COMPLETE'}},
                'environment_steps': 94_027_776, 'updates': 1, 'native_source_sha256': native_source_digest(),
                'training_implementation_sha256': training_implementation_digest(),
                'training_identity_sha256': _training_identity(config, workers=1, shards=1)}
    (source / 'run-manifest.json').write_text(json.dumps(manifest))
    (source / 'endpoint-evaluation.json').write_text(json.dumps({'checkpoint_sha256': sha256_file(source / 'final.pt'),
                                                               'checkpoint_environment_steps': 94_027_776,
                                                               'result': dict.fromkeys(('backend_errors', 'backend_truncations', 'step_limits', 'cycle_limits', 'timeouts'), 0)}))
    (source / 'training-bundle.json').write_text(json.dumps({'files': {name: sha256_file(source / name)
        for name in ('latest.pt', 'final.pt', 'run-manifest.json', 'training-config.toml', 'endpoint-evaluation.json')}}))
    child = copy.deepcopy(config)
    child['run'].update(output=(tmp_path / 'child').as_posix(), benchmark=(tmp_path / 'child-preparation/benchmark.json').as_posix(),
                        development_reference_checkpoint=(source / 'final.pt').as_posix(),
                        development_reference_sha256=sha256_file(source / 'final.pt'),
                        development_reference_profile='IRONCLAD_A20_ACT2',
                        continuation_from=source.as_posix(), continuation_checkpoint_sha256=sha256_file(source / 'latest.pt'),
                        continuation_selection_evidence='completed-endpoint', periodic_evaluation_seed_start=PERIODIC[0],
                        periodic_evaluation_seed_count=512, final_evaluation_seed_start=8000013000000,
                        final_evaluation_seed_count=4096)
    child['stages']['train'].update(target_environment_steps=114_027_776, evaluate_every_steps=2_000_000,
                                  checkpoint_every_steps=1_000_000, minimum_evaluation_episodes=512,
                                  minimum_final_evaluation_episodes=4096)
    path = tmp_path / 'child.toml'
    path.write_text(toml_text(child), encoding='utf-8')
    return source, config, child, path


def test_endpoint_continuation_preserves_next_rollout_and_update(tmp_path):
    source, old, new, path = fixture(tmp_path)
    parent_hash = sha256_file(source / 'latest.pt')
    proof = initialize(path)
    assert proof['schema'] == 'sls-act12-endpoint-continuation-v1'
    assert proof['configuration_changes']['stages']['train']['new']['target_environment_steps'] == 114_027_776
    child = Path(new['run']['output']) / 'latest.pt'
    a = torch.load(source / 'latest.pt', weights_only=False)
    b = torch.load(child, weights_only=False)
    from tools.train_full_run import _validate_frozen_reference_profile
    assert _validate_frozen_reference_profile(a, IRONCLAD_A20_ACT2, new) is False
    for key in ('model', 'optimizer', 'trainer', 'environments', 'python_rng', 'torch_rng', 'cuda_rng'):
        assert_same(a[key], b[key])
    results = []
    for config, checkpoint in ((old, source / 'latest.pt'), (new, child)):
        with WorkerPool(IRONCLAD_A20_ACT2, 1) as workers:
            trainer = PPOTrainer(Policy(ModelConfig(**config['model'])), workers, PPOConfig(**config['ppo']),
                                 seed=config['run']['seed'], training_seed_limit=config['run']['training_seed_limit'],
                                 training_config_digest=_training_identity(config, workers=1, shards=1))
            load_checkpoint(checkpoint, trainer)
            rollout = trainer.collect()
            metrics = trainer.optimize(rollout)
            results.append((rollout, copy.deepcopy(trainer.model.state_dict()), copy.deepcopy(trainer.optimizer.state_dict()), metrics))
    for name in results[0][0].__dataclass_fields__:
        left, right = getattr(results[0][0], name), getattr(results[1][0], name)
        if name == 'encoded_decisions':
            for left_step, right_step in zip(left, right, strict=True):
                for left_item, right_item in zip(left_step, right_step, strict=True):
                    for key in left_item.__dataclass_fields__:
                        assert_same(getattr(left_item, key), getattr(right_item, key))
        else:
            assert_same(left, right)
    for left, right in zip(results[0][1:], results[1][1:], strict=True):
        assert_same(left, right)
    assert sha256_file(source / 'latest.pt') == parent_hash
    assert not (child.parent / 'stages/train/selection').exists()


@pytest.mark.parametrize('section,key,value', [('ppo', 'gae_lambda', .98), ('ppo', 'learning_rate', 1e-5),
                                               ('model', 'embedding_dim', 64), ('run', 'worker_layout', [2, 1])])
def test_continuation_rejects_core_changes(tmp_path, section, key, value):
    _, _, child, path = fixture(tmp_path)
    child[section][key] = value
    path.write_text(toml_text(child), encoding='utf-8')
    with pytest.raises(ValueError):
        initialize(path)
    assert not Path(child['run']['output']).exists()


def test_selection_and_runtime_failure_gates():
    windows = [{'successes': 1}, {'successes': 2}]
    pair = {'net': 30, 'paired_seeds': 2048, 'exact_mcnemar_p': .01}
    assert select_arm({'research_decision': 'INCONCLUSIVE'}, pair, windows) == 'control'
    assert select_arm({'research_decision': 'SUPPORTS_SECOND_TRAINING_SEED_REPLICATION'}, pair, windows) == 'experimental'
    assert select_arm({'research_decision': 'INCONCLUSIVE'}, {**pair, 'net': 1}, windows) is None
    assert select_arm({'research_decision': 'INCONCLUSIVE'}, pair, [{'successes': 0}]) is None
    assert chunk_budget(100, 48*3600, 16384) > 9_000_000
    with pytest.raises(ValueError):
        chunk_budget(float('nan'), 48*3600, 16384)
    with pytest.raises(ValueError):
        safe_status({'status': 'FAILED', 'stages': {'train': {'status': 'FAILED'}}}, signalled=None)
    safe = {'status': 'INTERRUPTED', 'stages': {'train': {'status': 'INTERRUPTED', 'stop_signal': 'SIGTERM'}}}
    assert safe_status(safe, signalled=signal.SIGTERM) == 'SAFE_INTERRUPTED'
    with pytest.raises(ValueError):
        safe_status(safe, signalled=None)
    with pytest.raises(ValueError):
        safe_status(safe, signalled=signal.SIGINT)


def test_actual_seed_collision(tmp_path):
    path = tmp_path / 'endpoint-evaluation.json'
    path.write_text(json.dumps({'seeds': list(PERIODIC)}))
    with pytest.raises(ValueError, match='already evaluated'):
        reject_used_seeds([tmp_path])


def test_login_submit_import_does_not_load_torch():
    result = subprocess.run([sys.executable, '-c',
                             "import sys; import tools.submit_act12_long_run; assert 'torch' not in sys.modules"],
                            cwd=ROOT, capture_output=True, text=True)
    assert result.returncode == 0, result.stderr


def test_missing_or_corrupt_endpoint_evidence_blocks_creation(tmp_path):
    source, _, child, path = fixture(tmp_path)
    endpoint = source / 'endpoint-evaluation.json'
    data = json.loads(endpoint.read_text())
    data['result']['backend_errors'] = 1
    endpoint.write_text(json.dumps(data))
    with pytest.raises(ValueError, match='unhealthy'):
        initialize(path)
    assert not Path(child['run']['output']).exists()


def test_completed_allocation_requires_actual_budget():
    record = {'status': 'COMPLETE', 'environment_steps': 10,
              'stages': {'train': {'status': 'COMPLETE', 'target_environment_steps': 20}}}
    with pytest.raises(ValueError):
        safe_status(record, signalled=None)


def test_slurm_chain_builds_single_recipe_runner(tmp_path):
    from tools.submit_act12_long_run import commands
    path = tmp_path / 'example-plan.json'
    path.write_text('{}')
    rows = commands(path, 3, python='/venv/bin/python')
    assert len(rows) == 3
    for index, row in enumerate(rows):
        assert '--time=2-00:00:00' in row
        assert '--signal=B:TERM@300' in row
        assert '--gres=gpu:a100-40:1' in row
        assert '--allocation ' + str(index) in row[row.index('--wrap') + 1]
