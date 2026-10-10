"""Behavioral tests of public histories and diagnostic-only targets."""
import gzip
import json
import os
import subprocess
import sys
from dataclasses import replace
from pathlib import Path

import pytest
import torch

from sls.contracts import Action, ActionKind, Decision, Observation
from sls.contracts.observation import Card, Player, RunContext, ScreenType
from sls.diagnostics.cpu import (
    TRAJECTORY_SCHEMA,
    canonical,
    check_seed_collisions,
    complete_returns,
    cpu_runtime,
    decision_from_record,
    decision_record,
    policy_memory_for_prefix,
    read_history,
    safe_path,
    score,
    select_states,
)
from sls.model import ModelConfig, Policy


def decision():
    return Decision(Observation(Player("IRONCLAD", 60, 80, 0, 3, 3),
                                RunContext(20, 1, 1, 99, False, False, False),
                                ScreenType.COMBAT), (Action(ActionKind.END_TURN),))


def test_public_roundtrip_and_duplicate_instances():
    d = decision()
    cards = tuple(Card(f"DECK:{i}", "STRIKE_RED", "DECK", 0, 1, 1) for i in range(2))
    d = replace(d, observation=replace(d.observation, deck=cards))
    restored = decision_from_record(json.loads(canonical(decision_record(d))))
    assert restored == d and restored.observation.deck[0] != restored.observation.deck[1]


def test_duplicate_multiselect_preserves_click_order_and_references():
    from sls.contracts.observation import PublicEntity
    from sls.model import PolicyBatch

    d = decision()
    selected = tuple(PublicEntity(f"SELECTED:{i}", "BASH",
                                  (("deck_index", index), ("selected", True),
                                   ("selected_order", i), ("source", "MASTER_DECK")))
                     for i, index in enumerate((2, 0)))
    options = tuple(PublicEntity(f"CHOICE:{i}", "BASH", (("deck_index", i),)) for i in range(3))
    d = replace(d, observation=replace(d.observation, selected_cards=selected, choice_options=options),
                actions=(Action(ActionKind.SELECT_CARD, subject_id="CHOICE:1"), Action(ActionKind.CONFIRM)))
    restored = decision_from_record(json.loads(canonical(decision_record(d))))
    assert restored == d
    for a, b in zip(PolicyBatch.from_decisions((d,)).model_inputs(),
                    PolicyBatch.from_decisions((restored,)).model_inputs(), strict=True):
        assert torch.equal(a, b)


def test_complete_return_uses_actual_training_float32_rewards():
    reward = 0.1234567890123
    target = complete_returns([{"shaped_reward": reward, "terminal": True}], {"complete": True})
    assert target == [float(torch.tensor(reward, dtype=torch.float32))]


def test_public_history_rejects_hidden_and_schema():
    row = decision_record(decision())
    row["observation"]["rng"] = 7
    with pytest.raises(ValueError, match="hidden"):
        decision_from_record(row)
    row = decision_record(decision())
    row["observation"]["schema_version"] = 500
    with pytest.raises(ValueError, match="schema"):
        decision_from_record(row)


def test_returns_do_not_label_diagnostic_truncation_failure():
    rows = [{"shaped_reward": .1, "terminal": False},
            {"shaped_reward": -1.1, "terminal": True}]
    assert complete_returns(rows, {"complete": True}) == pytest.approx([-1, -1.1])
    assert complete_returns(rows, {"complete": False}) is None
    rows[-1]["terminal"] = False
    with pytest.raises(ValueError, match="terminal"):
        complete_returns(rows, {"complete": True})


def test_cpu_constraint(monkeypatch):
    monkeypatch.setenv("CUDA_VISIBLE_DEVICES", "-1")
    assert cpu_runtime()["device"] == "cpu"
    with pytest.raises(ValueError):
        cpu_runtime("cuda")
    monkeypatch.setenv("CUDA_VISIBLE_DEVICES", "0")
    with pytest.raises(RuntimeError):
        cpu_runtime()


def test_cli_rejects_gpu_before_work(tmp_path):
    root = Path(__file__).resolve().parents[2]
    env = {**os.environ, "CUDA_VISIBLE_DEVICES": "0", "PYTHONDONTWRITEBYTECODE": "1"}
    p = subprocess.run([sys.executable, str(root / "tools/diagnose_cpu.py"), "capture",
                        "--device", "cuda", "--output", str(tmp_path / "out")],
                       env=env, capture_output=True, text=True)
    assert p.returncode != 0 and "invalid choice" in p.stderr
    assert not (tmp_path / "out").exists()


@pytest.mark.parametrize("record", [
    {"seed": 132100001}, {"seed_range": [132100000, 132100002]},
    {"diagnostic_seed_start": 132100000, "diagnostic_seed_count": 10},
    {"seeds": {"start": 132100000, "end": 132100002}},
])
def test_seed_scan_rejects_collision(tmp_path, record):
    (tmp_path / "registration.json").write_text(json.dumps(record))
    with pytest.raises(ValueError, match="collision"):
        check_seed_collisions([tmp_path], 132100000, 32)


def test_seed_scan_preserves_master_namespace(tmp_path):
    (tmp_path / "config.toml").write_text('[run]\nseed=130000000\ntraining_seed_limit=2000000000000\n')
    assert len(check_seed_collisions([tmp_path], 132100000, 32)) == 1


def test_evidence_paths_cannot_escape(tmp_path):
    with pytest.raises(ValueError, match="path"):
        safe_path(tmp_path, "../secret", "0" * 64)


def test_prefix_memory_uses_each_models_history_not_teacher_memory():
    d = decision()
    torch.manual_seed(1)
    model = Policy(ModelConfig(embedding_dim=16, transformer_layers=1, attention_heads=2,
                               feedforward_dim=32, recurrent_hidden_dim=16)).eval()
    memory = model.initial_memory(1)
    next_memory, pa, pr = policy_memory_for_prefix(model, d, memory, 0, 0,
                                                  d.actions[0].to_dict(), 0, True)
    assert next_memory.shape == memory.shape and not torch.equal(next_memory, memory)
    assert pa > 0 and pr == 0 and torch.equal(memory, torch.zeros_like(memory))


def test_history_sequence_and_selection_are_deterministic(tmp_path):
    records = [{"record_type": "metadata", "schema": TRAJECTORY_SCHEMA}]
    for step in range(3):
        records.append({"record_type": "decision", "step": step,
                        **decision_record(decision()), "chosen_action": decision().actions[0].to_dict(),
                        "terminal": step == 2, "shaped_reward": -1 if step == 2 else 0})
    records.append({"record_type": "outcome", "complete": True})
    path = tmp_path / "history.gz"
    with gzip.open(path, "wt") as f:
        for row in records:
            f.write(json.dumps(row) + "\n")
    rows, _ = read_history(path)
    assert len(rows) == 3
    trajectories = [{"model": "a", "seed": 1, "id": "a-1", "public_path": path.name}]
    assert select_states(trajectories, tmp_path, 2) == select_states(trajectories[::-1], tmp_path, 2)
    records[2]["step"] = 900
    with gzip.open(path, "wt") as f:
        for row in records:
            f.write(json.dumps(row) + "\n")
    with pytest.raises(ValueError, match="contiguous"):
        read_history(path)


def test_natural_state_alignment_and_truncated_continuations(tmp_path, monkeypatch):
    from sls.diagnostics.cpu import capture, compare, validated_corpus

    monkeypatch.setenv("CUDA_VISIBLE_DEVICES", "-1")
    runtime = cpu_runtime()
    config = {"gamma": 1, "failure_progress_scale": 0, "potential_shaping": True,
              "potential_scale": .2, "max_episode_steps": 4096,
              "max_boundary_visits": 4, "limit_failure_reward": -1}
    models = {}
    for label, seed in (("a", 1), ("b", 2)):
        torch.manual_seed(seed)
        model = Policy(ModelConfig(embedding_dim=16, transformer_layers=1, attention_heads=2,
                                   feedforward_dim=32, recurrent_hidden_dim=16)).eval()
        models[label] = {"model": model, "ppo": config, "identity": {"test": label}}
    corpus = tmp_path / "corpus"
    manifest = capture(corpus, models, runtime, seed_start=132100101, seed_count=1,
                       max_steps=6, max_states=2)
    result = compare(corpus, tmp_path / "comparison.json", models, runtime, max_steps=1)
    assert len(result["states"]) == 4
    for state in manifest["states"]:
        matched = [r for r in result["states"] if r["state"] == state["id"]]
        assert matched[0]["actions"] == matched[1]["actions"]
        assert all(not r["continuation"]["complete"] and
                   r["continuation"]["shaped_return"] is None for r in matched)
        trajectory = next(t for t in manifest["trajectories"] if t["id"] == state["trajectory"])
        history, _ = read_history(corpus / trajectory["public_path"])
        for row in matched:
            model = models[row["model"]]["model"]
            memory, pa, pr = model.initial_memory(1, "cpu"), 0, 0.0
            for prefix in history[:state["step"]]:
                memory, pa, pr = policy_memory_for_prefix(model, decision_from_record(prefix),
                    memory, pa, pr, prefix["chosen_action"], prefix["raw_reward"], prefix["step"] == 0)
            predicted = score(model, decision_from_record(history[state["step"]]),
                              memory, pa, pr, state["step"] == 0)
            assert row["value_shaped"] == float(predicted.value[0])
            assert row["probabilities"] == predicted.logits.softmax(1)[0].tolist()
    path = corpus / "manifest.json"
    manifest["native_source_sha256"] = "0" * 64
    path.write_text(json.dumps(manifest), encoding="utf-8")
    with pytest.raises(ValueError, match="native source"):
        validated_corpus(corpus)
