import copy
import json
import tomllib
from pathlib import Path

import pytest

from sls.rl.checkpoint import RUNTIME_REBIND_FIELDS, checkpoint_contract_diff
from sls.rl.training_contract import (
    training_validation_digest,
    validate_training_sources,
)
from tools.train_full_run import _load_benchmark, _training_identity


def contract():
    return {
        "git_commit": "old", "native_source_sha256": "native",
        "encoding_schema": "v4", "vocabulary_sha256": "vocab",
        "model": {"embedding_dim": 128}, "ppo": {"gamma": 1.0, "reward_schema": "v3"},
        "workers": 48, "worker_shards": 16, "training_config_sha256": "schedule",
        "runtime": {"cuda_device": "NVIDIA A100 80GB PCIe MIG 3g.40gb",
                    "cuda_device_count": 1, "torch": "2.6.0+cu124", "cuda": "12.4",
                    "cudnn": 90100, "python_cache_tag": "cpython-312",
                    "deterministic_algorithms": True, "float32_matmul_precision": "high"},
    }


def diff(old, new):
    return checkpoint_contract_diff(old, new, allowed_runtime_rebind_fields=RUNTIME_REBIND_FIELDS)


def test_job_821775_marketing_name_is_recorded_rebind():
    old = contract()
    new = copy.deepcopy(old)
    new["runtime"]["cuda_device"] = "NVIDIA A100-PCIE-40GB"
    new["git_commit"] = "reviewed-fix"
    changes = diff(old, new)
    assert {d["path"] for d in changes} == {"runtime.cuda_device", "git_commit"}
    assert all(d["runtime_rebind_allowed"] for d in changes)


@pytest.mark.parametrize("path,value", [
    ("native_source_sha256", "different"), ("encoding_schema", "v5"),
    ("vocabulary_sha256", "different"), ("model.embedding_dim", 256),
    ("ppo.gamma", 0.99), ("ppo.reward_schema", "different"),
    ("workers", 24), ("worker_shards", 8), ("training_config_sha256", "different"),
    ("runtime.cuda_device_count", 2), ("runtime.torch", "2.7"),
    ("runtime.cuda", "13"), ("runtime.cudnn", 999),
    ("runtime.python_cache_tag", "cpython-313"),
    ("runtime.deterministic_algorithms", False), ("runtime.float32_matmul_precision", "highest"),
])
def test_semantic_or_unvalidated_numerical_changes_remain_strict(path, value):
    old = contract()
    new = copy.deepcopy(old)
    target = new
    parts = path.split(".")
    for part in parts[:-1]:
        target = target[part]
    target[parts[-1]] = value
    assert any(not item["runtime_rebind_allowed"] for item in diff(old, new))


def test_legacy_unrecorded_precision_is_explicitly_recorded_not_invented():
    new = contract()
    old = copy.deepcopy(new)
    del old["runtime"]["float32_matmul_precision"]
    changes = diff(old, new)
    assert changes[0]["checkpoint"] == "<MISSING>"
    assert changes[0]["runtime_rebind_allowed"]
    # Removing a recorded field, or the entire runtime, is not permitted.
    assert not diff(new, old)[0]["runtime_rebind_allowed"]
    del old["runtime"]
    assert not diff(old, new)[0]["runtime_rebind_allowed"]


def test_validation_scope_and_reviewed_transition(tmp_path):
    source = tmp_path / "src/sls/rl/ppo.py"
    source.parent.mkdir(parents=True)
    source.write_text("learning = 1")
    old = {"source_tree_sha256": "legacy-report"}
    transition = tmp_path / "configs/compatibility/training-validation-transitions.json"
    transition.parent.mkdir(parents=True)
    transition.write_text(json.dumps([{
        "from_source_tree_sha256": "legacy-report",
        "to_training_validation_sha256": training_validation_digest(root=tmp_path),
        "reason": "reviewed loader-only fix",
    }]))
    assert validate_training_sources(old, root=tmp_path).startswith("reviewed-transition")
    unrelated = tmp_path / "tools/analyze_training_history.py"
    unrelated.parent.mkdir()
    unrelated.write_text("print('report')")
    assert validate_training_sources(old, root=tmp_path).startswith("reviewed-transition")
    with pytest.raises(ValueError):
        validate_training_sources({"source_tree_sha256": "unknown"}, root=tmp_path)
    source.write_text("learning = 2")
    with pytest.raises(ValueError):
        validate_training_sources(old, root=tmp_path)


def test_benchmark_rebuild_is_advisory_but_environment_and_layout_are_strict(tmp_path):
    path = tmp_path / "benchmark.json"
    payload = {"schema": "sls-worker-benchmark-v2", "native_source_sha256": "same",
               "native_artifact": {"sha256": "old-binary"}, "selected_workers": 48, "selected_shards": 16}
    path.write_text(json.dumps(payload))
    with pytest.warns(RuntimeWarning, match="throughput is advisory"):
        assert _load_benchmark(path, native_digest="same", native_binary_sha256="rebuilt") == (48, 16)
    with pytest.raises(ValueError, match="different simulator sources"):
        _load_benchmark(path, native_digest="different", native_binary_sha256="old-binary")
    payload["selected_shards"] = 49
    path.write_text(json.dumps(payload))
    with pytest.raises(ValueError, match="invalid layout"):
        _load_benchmark(path, native_digest="same", native_binary_sha256="old-binary")


def test_identity_allows_snapshot_cadence_and_paths_but_not_learning_or_evaluation():
    root = Path(__file__).resolve().parents[2]
    config = tomllib.loads((root / "configs/train/ironclad_a0_fullrun_15m.toml").read_text())
    identity = _training_identity(config, workers=48, shards=16)
    changed = copy.deepcopy(config)
    changed["run"]["output"] = "relocated"
    changed["run"]["benchmark"] = "relocated-benchmark.json"
    changed["stages"]["train"]["checkpoint_every_steps"] = 250_000
    assert _training_identity(changed, workers=48, shards=16, checkpoint_reference=config) == identity
    changed["stages"]["train"]["evaluate_every_steps"] = 500_000
    assert _training_identity(changed, workers=48, shards=16, checkpoint_reference=config) != identity
    changed = copy.deepcopy(config)
    changed["ppo"]["learning_rate"] *= 2
    assert _training_identity(changed, workers=48, shards=16, checkpoint_reference=config) != identity
