"""No model, torch, native backend, rollout, GPU or benchmark execution."""
import ast
import copy
import json
import signal
import subprocess
import sys
import tomllib
from pathlib import Path

import pytest

from sls.rl.best_checkpoint import (
    evaluation_rank,
    passes_progress_guard,
    update_best_checkpoint,
)
from sls.rl.critic_warmup import CriticWarmupConfig, CriticWarmupState
from sls.rl.execution_health import execution_healthy
from tools.act12_critic20m_contract import (
    CONFIRMATION,
    PERIODIC,
    validate_compute_gate,
    validate_config,
)
from tools.analyze_act12_critic20m import decision, verify_bundle
from tools.prepare_act12_long_run import reject_used_seeds
from tools.run_act12_critic20m import allocation_budget, safe_status, training_arguments

ROOT = Path(__file__).resolve().parents[1]


def state(updates=2):
    return CriticWarmupState(CriticWarmupConfig(updates), 2, 8)


@pytest.mark.parametrize("final", [1.0, -1.0])
def test_complete_returns_include_terminal_and_shaped_rewards(final):
    warm = state()
    assert warm.observe(0, "a", .2, False) == []
    assert warm.observe(0, "b", -.3, False) == []
    samples = warm.observe(0, "c", final, True)
    assert [s[0] for s in samples] == ["a", "b", "c"]
    assert [s[1] for s in samples] == pytest.approx([final-.1, final-.3, final])
    assert warm.pending[0] == []
    assert warm.completed_episodes == 1


def test_cross_rollout_restore_and_worker_episode_isolation():
    original = state(3)
    original.observe(0, "old0", .25, False)
    original.observe(1, "worker1", -.5, False)
    original.finish_update()
    restored = state(3)
    restored.load_state_dict(copy.deepcopy(original.state_dict()))
    for warm in (original, restored):
        assert warm.observe(0, "end0", -1, True) == [("old0", -.75), ("end0", -1)]
        assert warm.observe(0, "new0", 1, True) == [("new0", 1)]
        assert warm.observe(1, "end1", 1, True) == [("worker1", .5), ("end1", 1)]
        warm.finish_update()
    assert restored.state_dict() == original.state_dict()


def test_last_warmup_discards_only_target_buffers_and_cannot_repeat():
    warm = state(1)
    warm.observe(0, "unfinished", .2, False)
    warm.finish_update()
    assert not warm.active and warm.discarded_states == 1
    assert warm.pending == [[], []] and warm.state_dict()["phase"] == "PPO"
    restored = state(1)
    restored.load_state_dict(warm.state_dict())
    assert not restored.active
    with pytest.raises(ValueError):
        restored.finish_update()


def test_default_off_and_fixed_budget():
    assert not CriticWarmupState(CriticWarmupConfig(), 1, 4096).active
    assert 32 * 64 * 256 == 524288
    updates = -(-20000000 // (64*256))
    assert 20000000 <= updates*64*256 < 20000000+64*256


@pytest.mark.parametrize("changes", [{"phase": "PPO"}, {"completed_updates": 4},
                                     {"workers": 3}, {"max_steps": 7},
                                     {"config": {"rollout_updates": 3}},
                                     {"discarded_states": -1}, {"completed_episodes": True}])
def test_restore_refuses_phase_contract_or_counter_changes(changes):
    warm = state()
    payload = warm.state_dict() | changes
    with pytest.raises(ValueError):
        warm.load_state_dict(payload)


def test_restore_refuses_pending_buffer_in_ppo_phase_and_nonfinite_reward():
    warm = state()
    payload = warm.state_dict() | {"phase": "PPO", "completed_updates": 2,
                                   "pending": [[("feature", 0.)], []]}
    with pytest.raises(ValueError):
        warm.load_state_dict(payload)
    payload = warm.state_dict() | {"pending": [[("feature", float("nan"))], []]}
    with pytest.raises(ValueError):
        warm.load_state_dict(payload)


@pytest.mark.parametrize("argument", [{"rollout_updates": -1}, {"epochs": 0},
                                      {"batch_size": 0}, {"rollout_updates": True}])
def test_warmup_config_rejects_invalid_parameters(argument):
    with pytest.raises(ValueError):
        CriticWarmupConfig(**argument)


def test_backend_failure_is_never_an_mc_target():
    warm = state()
    with pytest.raises(RuntimeError):
        warm.observe(0, "bad", -1, True, backend_fault=True)
    assert warm.completed_episodes == 0 and not warm.pending[0]


def selection(wins=1, **changes):
    return {"schema": "sls-best-progress-v5", "selection_objective": "HORIZON_CLEAR_COUNT", "successes": wins,
            "episodes": 512, "backend_errors": 0, "backend_truncations": 0,
            "timeouts": 0, "cycle_limits": 12, "step_limits": 3,
            "execution_health_policy": "execution-only-v1", **changes}


def test_new_selection_uses_joint_count_and_keeps_earlier_tie(tmp_path):
    a, b = selection(), selection(2, reached_act2=0, mean_reward=-100)
    assert passes_progress_guard(b, a)
    assert evaluation_rank(b) > evaluation_rank(a)
    assert update_best_checkpoint(tmp_path, a, save=lambda p: p.write_bytes(b"old"))
    assert not update_best_checkpoint(tmp_path, a | {"update": 999}, save=lambda p: p.write_bytes(b"later"))
    assert (tmp_path / "best_progress.pt").read_bytes() == b"old"


def test_legacy_zero_loop_gate_still_rejects():
    record = selection()
    record.pop("execution_health_policy")
    assert not passes_progress_guard(record, record)
    assert execution_healthy(selection())


@pytest.mark.parametrize("change", [{"backend_errors": 1}, {"backend_truncations": 1},
                                    {"timeouts": 1}, {"loss": float("nan")}])
def test_execution_failures_are_hard_unhealthy(change):
    assert not execution_healthy(selection(**change))


def configs():
    def load(name):
        return tomllib.loads((ROOT / "configs/train" / name).read_text())
    return load("ironclad_a20_act12_critic20m_r1.toml"), load("ironclad_a20_act12_lambda098_r1.toml")


@pytest.mark.parametrize("section,key,value", [("ppo", "gae_lambda", 1), ("ppo", "learning_rate", .001),
                                               ("ppo", "reward_schema", "wrong"),
                                               ("model", "recurrent_hidden_dim", 128),
                                               ("run", "worker_layout", [4, 2]),
                                               ("critic_warmup", "rollout_updates", 31)])
def test_recipe_rejects_unapproved_changes(section, key, value):
    config, control = configs()
    validate_config(config, control)
    config[section][key] = value
    with pytest.raises(ValueError):
        validate_config(config, control)


def test_original_ranges_unchanged_and_conflicts_refused(tmp_path):
    assert PERIODIC == (8000012000000, 8000012000512)
    assert CONFIRMATION == (8000013000000, 8000013004096)
    record = tmp_path / "endpoint-evaluation.json"
    record.write_text(json.dumps({"seeds": list(CONFIRMATION)}))
    with pytest.raises(ValueError, match="already evaluated"):
        reject_used_seeds([tmp_path])
    record.write_text(json.dumps({"seed_results": [{"seed": PERIODIC[0]}]}))
    with pytest.raises(ValueError):
        reject_used_seeds([tmp_path])


def test_budget_reserves_confirmation_before_finishing():
    assert allocation_budget(114.7, 48*3600, 16384, 9000000) < 9000000
    assert allocation_budget(114.7, 48*3600, 16384, 1000000) >= 1000000
    with pytest.raises(ValueError):
        allocation_budget(114.7, 20*3600, 16384, 1)


def test_unsafe_exit_not_authorized_for_chaining():
    safe = {"status": "SOAK_COMPLETE", "stages": {"train": {"status": "SOAK_COMPLETE"}}}
    assert safe_status(safe, signalled=None) == "SAFE_INTERRUPTED"
    with pytest.raises(ValueError):
        safe_status(safe, signalled=signal.SIGINT)
    with pytest.raises(ValueError):
        safe_status(safe | {"status": "FAILED"}, signalled=None)


def test_completed_budget_only_resumes_finalization_without_additional_sampling_flag():
    assert training_arguments(110013696, 110018560, 0) == ["--stage", "train"]
    with pytest.raises(ValueError):
        training_arguments(110013696, 90013696, 0)


@pytest.mark.parametrize("policy,requested,restores", [("execution-only-v1", True, True),
                                                     ("execution-only-v1", False, False),
                                                     (None, True, False)])
def test_mock_finalization_interruption_restores_endpoint_only_for_new_safe_shutdown(policy, requested, restores):
    from types import SimpleNamespace
    tree = ast.parse((ROOT / "tools/train_full_run.py").read_text(encoding="utf-8"))
    handler = next(h for n in ast.walk(tree) if isinstance(n, ast.Try) for h in n.handlers
                   if isinstance(h.type, ast.Name) and h.type.id == "InterruptedError"
                   and any(isinstance(c, ast.Name) and c.id == "load_checkpoint" for c in ast.walk(h)))
    probe = ast.Try(body=[ast.Raise(exc=ast.Call(func=ast.Name(id="InterruptedError", ctx=ast.Load()),
                                               args=[], keywords=[]), cause=None)],
                    handlers=[handler], orelse=[], finalbody=[])
    code = compile(ast.fix_missing_locations(ast.Module(body=[probe], type_ignores=[])), "mock-finalization", "exec")
    calls = []
    scope = {"run": {"evaluation_health_policy": policy}, "controller": SimpleNamespace(requested=requested),
             "load_checkpoint": lambda *a: calls.append(a), "latest": "endpoint", "trainer": "mock"}
    if restores:
        exec(code, scope)
        assert calls == [("endpoint", "mock")]
    else:
        with pytest.raises(InterruptedError):
            exec(code, scope)


def test_registered_decision_never_lowers_threshold():
    assert decision({"net": 40, "paired_seeds": 4096, "exact_mcnemar_p": .001}) == "NO_CONFIRMED_GAIN"
    assert decision({"net": 41, "paired_seeds": 4096, "exact_mcnemar_p": .051}) == "NO_CONFIRMED_GAIN"
    assert decision({"net": 41, "paired_seeds": 4096, "exact_mcnemar_p": .05}) == "CONFIRMED_JOINT_GAIN"


def test_mock_acceptance_cannot_omit_real_probe_checks():
    checks = {k: "PASS" for k in ("shared_rules", "grid", "cross-rollout", "last-warmup", "actor_and_gru_frozen", "first-ppo")}
    checks["shared_rule_evidence"] = {"status": "PASS", "synthetic_transition_cases": 22,
                                      "stock_thief_cases": 15,
                                      "native_source_sha256": "6efbb772958c06d1b9133f374d046d8838eaa256da1a146eaf7e1ee530eefd3d"}
    checks["complete_return_states_checked"] = 12
    gate = {"schema": "sls-critic20m-compute-gate-v2", "ok": True,
            "training_identity_sha256": "mock", "production_probe_updates_discarded": True,
            "checks": checks}
    validate_compute_gate(gate, "mock")
    with pytest.raises(ValueError):
        validate_compute_gate(gate, "different source")
    checks.pop("first-ppo")
    with pytest.raises(ValueError):
        validate_compute_gate(gate, "mock")


def test_bundle_cannot_hide_required_missing_files(tmp_path):
    (tmp_path / "training-bundle.json").write_text('{"files": {}}')
    with pytest.raises(ValueError, match="required evidence"):
        verify_bundle(tmp_path)


def test_missing_export_is_optional_but_other_evidence_and_hashes_are_strict(tmp_path):
    from sls.rl.training_contract import sha256_file
    required = ("training-config.toml", "run-manifest.json", "final.pt", "latest.pt",
                "reference-evaluation.json", "endpoint-evaluation.json", "final-evaluation.json",
                "stages/train/metrics.jsonl", "stages/train/selection/best_progress.json",
                "stages/train/selection/best_progress.pt")
    files = {}
    for name in required:
        path = tmp_path / name
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(b"mock evidence; never a model")
        files[name] = sha256_file(path)
    files[tmp_path.name + ".pt"] = "a" * 64
    (tmp_path / "training-bundle.json").write_text(json.dumps({"files": files}))
    assert verify_bundle(tmp_path)[1] == {tmp_path.name + ".pt": "a" * 64}
    (tmp_path / "endpoint-evaluation.json").write_text("corrupted")
    with pytest.raises(ValueError, match="hash mismatch"):
        verify_bundle(tmp_path)


def test_imports_and_operator_entrypoints_do_not_load_torch():
    subprocess.run([sys.executable, "-c", "import sys; import tools.submit_act12_critic20m; "
                    "import tools.analyze_act12_critic20m; import tools.run_act12_critic20m; "
                    "import tools.verify_act12_critic_warmup; assert 'torch' not in sys.modules"], check=True)
    subprocess.run([sys.executable, "-c", "import sys; import tools.check_training_configs; "
                    "assert 'torch' not in sys.modules"], check=True)


@pytest.mark.parametrize('key,value', [('checkpoint_sha256', 'wrong'),
                                      ('parent_environment_steps', 1),
                                      ('transfer_kind', 'environment-migration'),
                                      ('simulator_transition', {'evidence': 'old', 'evidence_sha256': 'wrong'})])
def test_corrective_recipe_keeps_parent_and_exact_migration_binding(key, value):
    config, control = configs()
    validate_config(config, control)
    config['warm_start'][key] = value
    with pytest.raises(ValueError):
        validate_config(config, control)


def test_old_acceptance_and_missing_shared_rules_are_rejected():
    keys = ('shared_rules', 'grid', 'cross-rollout', 'last-warmup', 'actor_and_gru_frozen', 'first-ppo')
    checks = dict.fromkeys(keys, 'PASS') | {'complete_return_states_checked': 1,
        'shared_rule_evidence': {'status': 'PASS', 'synthetic_transition_cases': 22,
                                 'stock_thief_cases': 15,
                                 'native_source_sha256': '6efbb772958c06d1b9133f374d046d8838eaa256da1a146eaf7e1ee530eefd3d'}}
    gate = {'schema': 'sls-critic20m-compute-gate-v2', 'ok': True, 'checks': checks,
            'training_identity_sha256': 'mock', 'production_probe_updates_discarded': True}
    validate_compute_gate(gate, 'mock')
    with pytest.raises(ValueError):
        validate_compute_gate(gate | {'schema': 'sls-critic20m-compute-gate-v1'}, 'mock')
    checks.pop('shared_rules')
    with pytest.raises(ValueError):
        validate_compute_gate(gate, 'mock')


def test_login_node_validation_rejects_corrupt_supporting_proof(tmp_path):
    import shutil

    from tools.act12_critic20m_contract import OPERATOR_PATHS, PLAN, validate
    plan = json.loads((ROOT / PLAN).read_text(encoding='utf-8'))
    paths = set(OPERATOR_PATHS) | {PLAN, plan['config'],
        'configs/train/ironclad_a20_act12_lambda098_r1.toml',
        plan['parent']['simulator_transition']['evidence']}
    migration_path = ROOT / plan['parent']['simulator_transition']['evidence']
    migration = json.loads(migration_path.read_text(encoding='utf-8'))
    paths.update((migration_path.parent / n).resolve().relative_to(ROOT.resolve()).as_posix()
                 for n in migration['evidence'])
    for name in paths:
        destination = tmp_path / name
        destination.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(ROOT / name, destination)
    validate(root=tmp_path, check_sources=False)
    proof = tmp_path / 'docs/results/act12-critic20m-20261007/simulator-qualification-r1.json'
    proof.write_bytes(proof.read_bytes() + b'corruption')
    with pytest.raises(ValueError, match='supporting evidence changed'):
        validate(root=tmp_path, check_sources=False)
