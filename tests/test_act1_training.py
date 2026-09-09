"""Act1 environment policy and single-stage training regression coverage."""

from __future__ import annotations

import json
import re
from dataclasses import asdict, replace
from pathlib import Path

import pytest

from sls.backends.simulator import SimulatorBackend
from sls.contracts import Action, ActionKind, PublicEntity, ScreenType
from sls.curriculum import IRONCLAD_A0_ACT1
from sls.rl.best_checkpoint import update_best_checkpoint
from sls.rl.preparation import read_config, workload_contract
from sls.rl.reward import curriculum_potential, curriculum_terminal_reward
from tools.benchmark_workers import select_layout
from tools.submit_slurm import _parser, build_sbatch_command
from tools.train_full_run import _paired_seed_changes

ROOT = Path(__file__).resolve().parents[1]
CONFIG = ROOT / "configs/train/ironclad_a0_act1_5m.toml"


def test_note_auto_leave_matches_manual_leave_and_restores_without_policy_step():
    manual = SimulatorBackend(replace(IRONCLAD_A0_ACT1, note_for_yourself_policy="INTERACTIVE"))
    fixture = json.loads((ROOT / "tests/fixtures/regressions/act1-note-seed-3000000000047.json").read_text())
    decision = manual.reset(fixture["seed"])
    automatic_run = SimulatorBackend(IRONCLAD_A0_ACT1)
    automatic_run.reset(fixture["seed"])
    for raw_action in fixture["actions"]:
        decision = manual.step(Action.from_dict(raw_action)).decision
        automatic_decision = automatic_run.step(Action.from_dict(raw_action)).decision
    checkpoint = manual.checkpoint()
    deck = decision.observation.deck
    expected = manual.step(next(a for a in decision.actions if a.option_id == "event-option:1"))
    automatic = SimulatorBackend(IRONCLAD_A0_ACT1)
    actual = automatic.load_checkpoint(checkpoint)
    assert actual == expected.decision == automatic_decision
    assert actual.observation.deck == deck
    assert actual.observation.screen is ScreenType.MAP
    assert automatic.raw_state["rng"] == manual.raw_state["rng"]
    restored = SimulatorBackend(IRONCLAD_A0_ACT1)
    assert restored.load_checkpoint(automatic.checkpoint()) == actual


def test_act1_keys_have_real_opportunity_cost_and_no_reward_bonus():
    from tests.simulator.test_fullrun_structure import _structural_action
    backend = SimulatorBackend(IRONCLAD_A0_ACT1)
    decision = backend.reset(17)
    backend._native._set_skip_battles_for_testing(True)
    seen = set()
    for _ in range(180):
        if decision.terminal:
            break
        action = next((a for a in decision.actions
                       if a.kind in {ActionKind.RECALL, ActionKind.TAKE_BLUE_KEY}), None)
        action = action or _structural_action(decision)
        before = decision.observation
        decision = backend.step(action).decision
        if action.kind is ActionKind.RECALL:
            seen.add("ruby")
            assert decision.observation.run.has_ruby_key
            assert decision.observation.player.current_hp == before.player.current_hp
            assert decision.observation.deck == before.deck
        if action.kind is ActionKind.TAKE_BLUE_KEY:
            seen.add("sapphire")
            assert decision.observation.run.has_sapphire_key
            assert decision.observation.relics == before.relics
    assert seen == {"ruby", "sapphire"}
    observation = decision.observation
    without = replace(observation, run=replace(observation.run, has_ruby_key=False,
                                              has_sapphire_key=False, has_emerald_key=False))
    assert curriculum_potential(observation, IRONCLAD_A0_ACT1) == curriculum_potential(without, IRONCLAD_A0_ACT1)
    assert curriculum_potential(observation, IRONCLAD_A0_ACT1, terminal=True) == 0
    assert curriculum_terminal_reward(observation, IRONCLAD_A0_ACT1, success=True) > curriculum_terminal_reward(observation, IRONCLAD_A0_ACT1, success=False)


def test_calling_bell_discarded_reward_roll_and_fountain_eligibility_match_stock():
    fixture = json.loads((ROOT / "tests/fixtures/regressions/act1-note-seed-3000000000025.json").read_text())
    backend = SimulatorBackend(IRONCLAD_A0_ACT1)
    backend.reset(fixture["seed"])
    for i, raw in enumerate(fixture["actions"]):
        backend.step(Action.from_dict(raw))
        if i == 0:
            assert backend.raw_state["rng"]["card"]["counter"] == 9
            assert not backend.raw_state["public_screen"].get("card_rewards")
        if i == 15:
            cards = backend.raw_state["public_screen"]["card_rewards"][0]
            assert [c["content_id"] for c in cards] == ["SENTINEL", "SHRUG_IT_OFF", "SEEING_RED"]
            assert backend.raw_state["rng"]["card"]["counter"] == 18
    # Bell's unremovable curse must not add Fountain to the eligible shrine pool.
    assert backend.raw_state["public_run"]["current_event_id"] == "Transmorgrifier"


def test_prismatic_only_in_shop_pool_and_owned_state_rejected():
    source = (ROOT / "native/simulator/include/constants/RelicPools.h").read_text()
    source = re.sub(r"//[^\n]*", "", source)
    pools = re.findall(r"(\w+RelicPool)\s*=\s*\{([^}]+)\}", source)
    containing = [name for name, contents in pools if "PRISMATIC_SHARD" in contents]
    assert containing and set(containing) == {"shopRelicPool"}
    game = (ROOT / "native/simulator/src/game/GameContext.cpp").read_text()
    assert not re.search(r"obtainRelic\(\s*RelicId::PRISMATIC_SHARD", game)
    from sls.content.scope import UnsupportedContentPolicy
    observation = SimulatorBackend(IRONCLAD_A0_ACT1).reset(0).observation
    with pytest.raises(ValueError, match="already owns unsupported"):
        UnsupportedContentPolicy.ironclad().validate_observation(replace(
            observation, relics=(PublicEntity("RELIC:0", "PRISMATIC_SHARD"),),
        ))


def test_act1_best_ties_keep_earlier_and_seed_changes_are_paired(tmp_path):
    saved = []
    record = {"schema": "sls-best-progress-v4", "selection_objective": "ACT1_CLEAR_COUNT",
              "successes": 10, "mean_reward": 0}
    assert update_best_checkpoint(tmp_path, record, save=saved.append)
    assert not update_best_checkpoint(tmp_path, {**record, "mean_reward": 100}, save=saved.append)
    assert update_best_checkpoint(tmp_path, {**record, "successes": 11}, save=saved.append)
    assert len(saved) == 2
    assert _paired_seed_changes(
        {"seed_results": [{"seed": 1, "success": True}, {"seed": 2, "success": False}]},
        {"seed_results": [{"seed": 2, "success": True}, {"seed": 1, "success": False}]},
    ) == {"compared_seeds": 2, "win_to_loss": 1, "loss_to_win": 1}


def test_act1_config_and_submission_bind_actual_workload():
    config = read_config(CONFIG)
    assert config["run"]["workflow"] == "single-stage"
    assert set(config["stages"]) == {"train"}
    assert config["stages"]["train"]["target_environment_steps"] == 5_000_000
    changed = {**config, "ppo": {**config["ppo"], "gamma": .99}}
    assert workload_contract(config) != workload_contract(changed)
    args = _parser().parse_args(["train", "--config", str(CONFIG), "--prepare", "--dry-run"])
    command = build_sbatch_command(args)
    assert "prepare_and_train.py" in command[-1]
    assert "CUBLAS_WORKSPACE_CONFIG=:4096:8" in " ".join(command)
    assert select_layout([{"workers": 32, "shards": 4, "decisions_per_second": 96},
                          {"workers": 64, "shards": 8, "decisions_per_second": 100}]) == (32, 4)


def test_act1_artifact_explicit_rules_are_required():
    from tests.runtime.test_policy_runtime import _runtime_artifact
    metadata = replace(_runtime_artifact().metadata, goal="ACT1", ascension_max=0)
    with pytest.raises(ValueError, match="lacks explicit environment"):
        metadata.validate()
    replace(metadata, environment_profile=json.loads(json.dumps(asdict(IRONCLAD_A0_ACT1)))).validate()
    with pytest.raises(ValueError, match="environment rules"):
        replace(metadata, environment_profile={**asdict(IRONCLAD_A0_ACT1), "version": 3}).validate()


def test_original_note_auto_leave_uses_option_index_not_translated_text():
    from sls.backends.original.environment import OriginalBackend
    from tests.original.test_adapter import base_game
    payload = {"in_game": True, "available_commands": ["choose"], "game_state": base_game(
        screen_type="EVENT", choice_list=["localized exchange", "localized leave"],
        screen_state={"event_id": "NoteForYourself"},
    )}
    following = {"in_game": True, "available_commands": [], "game_state": base_game(screen_type="MAP")}
    class Session:
        def execute(self, command):
            assert command == "choose 1"
            return following
    backend = OriginalBackend(Session(), IRONCLAD_A0_ACT1)
    assert backend._fold_protocol_only_boundaries(payload) == following


def test_preparation_ignores_gpu_label_but_protects_workload(monkeypatch):
    import sls.rl.preparation as preparation
    runtime = {"cuda_device": "MIG", "cuda_device_count": 1, "torch": "2.6"}
    monkeypatch.setattr(preparation, "runtime_contract", lambda _: dict(runtime))
    config = read_config(CONFIG)
    before = preparation.preparation_contract(config, None)
    runtime["cuda_device"] = "A100-PCIE-40GB"
    assert preparation.preparation_contract(config, None) == before
    runtime["torch"] = "different"
    assert preparation.preparation_contract(config, None) != before


@pytest.mark.parametrize("interrupt_evaluation", [False, True])
def test_single_stage_real_ppo_soak_resume_and_finalization(tmp_path, monkeypatch, interrupt_evaluation):
    import sys

    import torch

    import sls.rl.preparation as preparation
    import tools.train_full_run as train
    from sls.rl.training_contract import native_artifact, native_source_digest
    config_text = CONFIG.read_text().replace('device = "cuda"', 'device = "cpu"')
    replacements = {
        '"local/runs/preparation/ironclad-a0-act1-v4-5m/benchmark.json"': '"benchmark.json"',
        '"local/runs/ironclad-a0-act1-v4-5m"': '"run"',
        "target_environment_steps = 5000000": "target_environment_steps = 8",
        "evaluate_every_steps = 500000": "evaluate_every_steps = 4",
        "checkpoint_every_steps = 250000": "checkpoint_every_steps = 4",
        "= 512": "= 2", "= 1024": "= 2", "= 4096": "= 8",
        "embedding_dim = 128": "embedding_dim = 32",
        "transformer_layers = 4": "transformer_layers = 1",
        "feedforward_dim = 256": "feedforward_dim = 64",
        "recurrent_hidden_dim = 256": "recurrent_hidden_dim = 32",
        "rollout_steps = 256": "rollout_steps = 2",
        "recurrent_sequence_length = 64": "recurrent_sequence_length = 1",
        "minibatch_sequences = 16": "minibatch_sequences = 2", "epochs = 2": "epochs = 1",
    }
    for old, new in replacements.items():
        config_text = config_text.replace(old, new)
    config_path = tmp_path / "config.toml"
    config_path.write_text(config_text)
    (tmp_path / "benchmark.json").write_text(json.dumps({
        "schema": "sls-worker-benchmark-v2", "selected_workers": 2, "selected_shards": 1,
        "native_source_sha256": native_source_digest(), "native_artifact": native_artifact(),
    }))
    monkeypatch.setattr(train, "ROOT", tmp_path)
    monkeypatch.setattr(preparation, "require_preparation", lambda *_: {})
    if interrupt_evaluation:
        controllers = []
        original_controller = train.StopController
        original_evaluate = train.evaluate
        calls = []
        def controller_factory():
            controller = original_controller()
            controllers.append(controller)
            return controller
        def interrupt_second(*args, **kwargs):
            calls.append(True)
            if len(calls) == 2:
                controllers[-1].requested = True
                raise InterruptedError("injected Slurm evaluation interruption")
            return original_evaluate(*args, **kwargs)
        monkeypatch.setattr(train, "StopController", controller_factory)
        monkeypatch.setattr(train, "evaluate", interrupt_second)
    argv = ["train_full_run", "--stage", "train", "--config", str(config_path)]
    monkeypatch.setattr(sys, "argv", [*argv, "--stop-after-additional-steps", "4"])
    assert train.main() == 0
    output = tmp_path / "run"
    manifest = json.loads((output / "run-manifest.json").read_text())
    assert manifest["status"] == ("INTERRUPTED" if interrupt_evaluation else "SOAK_COMPLETE")
    assert manifest["environment_steps"] == 4
    assert (output / "training-config.toml").read_text() == config_text
    monkeypatch.setattr(sys, "argv", argv)
    assert train.main() == 0
    manifest = json.loads((output / "run-manifest.json").read_text())
    assert manifest["status"] == "COMPLETE" and manifest["environment_steps"] == 8
    assert (output / "final.pt").exists() and (output / "final-evaluation.json").exists()
    from sls.runtime.artifact import load_policy_artifact
    assert load_policy_artifact(output / "run.pt").metadata.goal == "ACT1"
    saved = torch.load(output / "latest.pt", weights_only=False)
    assert saved["trainer"]["environment_steps"] == 8
    records = [json.loads(line) for line in (output / "stages/train/metrics.jsonl").read_text().splitlines()]
    assert records[-1]["paired_seed_changes"]["compared_seeds"] == 2
    assert [r["environment_steps"] for r in records if "evaluation" in r] == [0, 4, 8]
    with pytest.raises(ValueError, match="already completed"):
        train.main()


@pytest.mark.parametrize("failed_tool", ["preflight_training.py", "benchmark_workers.py"])
def test_prepare_failure_never_executes_training(tmp_path, monkeypatch, failed_tool):
    import sys
    from types import SimpleNamespace

    import sls.rl.preparation as preparation
    import tools.prepare_and_train as prepare
    config = read_config(CONFIG)
    config["run"]["benchmark"] = str(tmp_path / "benchmark.json")
    config["run"]["output"] = str(tmp_path / "run")
    monkeypatch.setattr(prepare, "read_config", lambda _: config)
    monkeypatch.setenv("SLURM_JOB_ID", "test")
    monkeypatch.setitem(sys.modules, "fcntl", SimpleNamespace(LOCK_EX=1, LOCK_NB=2, flock=lambda *_: None))
    monkeypatch.setattr(prepare.subprocess, "run", lambda *a, **kw: SimpleNamespace(returncode=0))
    import torch
    monkeypatch.setattr(torch.cuda, "is_available", lambda: True)
    def missing(*_):
        raise FileNotFoundError("no preparation yet")
    monkeypatch.setattr(preparation, "require_preparation", missing)
    calls = []
    def run_tool(name, *_):
        calls.append(name)
        if name == failed_tool:
            raise RuntimeError("injected preparation failure")
    monkeypatch.setattr(prepare, "run_tool", run_tool)
    monkeypatch.setattr(prepare.os, "execv", lambda *_: pytest.fail("training must not start"))
    monkeypatch.setattr(sys, "argv", ["prepare", "--config", str(CONFIG)])
    with pytest.raises(RuntimeError, match="injected preparation failure"):
        prepare.main()
    assert calls[-1] == failed_tool


def test_job_833382_note_leave_does_not_alias_second_potion_slot():
    # Natural server trajectory, seed 10004912; no fabricated event state.
    backend = SimulatorBackend(replace(IRONCLAD_A0_ACT1, note_for_yourself_policy="INTERACTIVE"))
    backend.reset(10004912)
    for bits in [1, 402653184, 402653185, 805306368, 0, 0, 3, 537264129, 536870912, 536870914]:
        backend._native.step(bits)
    before = backend._native.snapshot()
    manual = SimulatorBackend(backend.profile)
    decision = manual.load_checkpoint(before)
    enter = next(a for a in decision.actions if a.kind is ActionKind.CHOOSE_MAP_NODE)
    event = manual.step(enter).decision
    event_checkpoint = manual.checkpoint()
    assert event.observation.screen is ScreenType.EVENT
    assert sum(a['idx1'] == 1 for a in manual.raw_state['legal_actions']) == 2
    expected = manual.step(next(a for a in event.actions if a.option_id == 'event-option:1'))
    automatic = SimulatorBackend(IRONCLAD_A0_ACT1)
    automatic.load_checkpoint(before)
    transition = automatic.step(enter)
    assert transition.decision == expected.decision
    assert transition.info['automatic_actions'] == ['NOTE_FOR_YOURSELF:LEAVE']
    assert automatic.raw_state['player_state']['potions'] == before['player_state']['potions']
    assert automatic.raw_state['rng'] == manual.raw_state['rng']
    restored = SimulatorBackend(IRONCLAD_A0_ACT1)
    assert restored.load_checkpoint(event_checkpoint) == expected.decision
    assert restored.load_checkpoint(automatic.checkpoint()) == expected.decision


def test_reviewed_source_fix_is_exact_directional_pair_only():
    from sls.rl.training_contract import (
        native_source_digest,
        state_preserving_source_transition,
    )
    previous = '8ac099425f0bf2a4ecc1282cef2a10d4551386e8086f577425528879e5bb7ceb'
    current = native_source_digest()
    assert state_preserving_source_transition(previous, current)
    assert state_preserving_source_transition(current, previous) is None
    assert state_preserving_source_transition(previous, 'unreviewed') is None
    assert state_preserving_source_transition('unreviewed', current) is None


def test_resume_archives_updates_after_checkpoint_without_losing_history(tmp_path):
    from tools.train_full_run import _archive_uncheckpointed_metrics
    path = tmp_path / 'metrics.jsonl'
    original = ''.join(json.dumps({'environment_steps': n}) + '\n' for n in [0, 16, 32])
    path.write_text(original)
    _archive_uncheckpointed_metrics(path, 16)
    assert [json.loads(line)['environment_steps'] for line in path.read_text().splitlines()] == [0, 16]
    assert next(tmp_path.glob('metrics.before-resume-*.jsonl')).read_text() == original
    _archive_uncheckpointed_metrics(path, 16)
    assert len(list(tmp_path.glob('metrics.before-resume-*.jsonl'))) == 1


def test_reviewed_note_fix_reuses_layout_but_rejects_unknown_environment(tmp_path):
    from sls.rl.training_contract import native_source_digest
    from tools.train_full_run import _load_benchmark
    path = tmp_path / 'benchmark.json'
    data = {'schema': 'sls-worker-benchmark-v2', 'selected_workers': 64,
            'selected_shards': 8, 'native_artifact': {'sha256': 'old-binary'},
            'native_source_sha256': '8ac099425f0bf2a4ecc1282cef2a10d4551386e8086f577425528879e5bb7ceb'}
    path.write_text(json.dumps(data))
    with pytest.warns(RuntimeWarning, match='Reviewed compatible'):
        assert _load_benchmark(path, native_digest=native_source_digest(),
                               native_binary_sha256='rebuilt') == (64, 8)
    data['native_source_sha256'] = 'unreviewed'
    path.write_text(json.dumps(data))
    with pytest.raises(ValueError, match='different simulator'):
        _load_benchmark(path, native_digest=native_source_digest(), native_binary_sha256='rebuilt')
