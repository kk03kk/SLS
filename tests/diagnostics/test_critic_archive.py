"""Evidence failure modes must fail closed; partial budget stays partial."""
import io
import tarfile
from dataclasses import replace
from types import SimpleNamespace

import pytest

from sls.contracts import Action, ActionKind, Decision, Observation, ScreenType
from sls.contracts.observation import Player, RunContext
from sls.diagnostics.battle_probe import battle_outcome, select_battle_states
from sls.diagnostics.critic_archive import extract_archive, paired_binary, verify_curve
from tools.qualify_stopped_critic import combine_chunks


@pytest.mark.parametrize("name,kind", [("../escape", "file"), ("/absolute", "file"),
    ("C:/escape", "file"), ("a\\b", "file"), ("link", "link"), ("duplicate", "duplicate")])
def test_archive_rejects_unsafe_or_ambiguous_members(tmp_path, name, kind):
    archive = tmp_path / "archive.tar.gz"
    with tarfile.open(archive, "w:gz") as stream:
        info = tarfile.TarInfo(name)
        if kind == "link":
            info.type = tarfile.SYMTYPE
            info.linkname = "../escape"
            stream.addfile(info)
        else:
            info.size = 1
            stream.addfile(info, io.BytesIO(b"x"))
            if kind == "duplicate":
                stream.addfile(info, io.BytesIO(b"y"))
    with pytest.raises(ValueError):
        extract_archive(archive, tmp_path / "extracted")
    assert not (tmp_path / "escape").exists()


def test_archive_roundtrip_no_overwrite(tmp_path):
    archive = tmp_path / "archive.tar.gz"
    with tarfile.open(archive, "w:gz") as stream:
        info = tarfile.TarInfo("run/metrics.jsonl")
        info.size = 2
        stream.addfile(info, io.BytesIO(b"{}"))
    inventory = extract_archive(archive, tmp_path / "out")
    assert len(inventory["files"]) == 1
    assert (tmp_path / "out/run/metrics.jsonl").read_bytes() == b"{}"
    with pytest.raises(ValueError, match="already exists"):
        extract_archive(archive, tmp_path / "out")
    assert extract_archive(archive, tmp_path / "out", verify_existing=True) == inventory
    (tmp_path / "out/run/metrics.jsonl").write_bytes(b"[]")
    with pytest.raises(ValueError, match="hash mismatch"):
        extract_archive(archive, tmp_path / "out", verify_existing=True)


def test_paired_binary_uses_discordant_seeds():
    a = {i: {"success": True} for i in range(10)}
    b = {i: {"success": i >= 8} for i in range(10)}
    result = paired_binary(a, b, "success")
    assert result["lost"] == 8 and result["gained"] == 0
    assert result["exact_mcnemar_p"] == 2 / 256
    with pytest.raises(ValueError, match="seeds"):
        paired_binary(a, {0: b[0]}, "success")


def test_curve_rejects_missing_updates_before_analysis():
    with pytest.raises(ValueError, match="missing or duplicate"):
        verify_curve([{"update": 2, "update_seconds": 1}], 90013696, 16384, (1, 3))


def transition(screen, **kwargs):
    decision = Decision(Observation(Player("IRONCLAD", 10, 80, 0, 3, 3),
        RunContext(20, 2, 25, 99, False, False, False), screen), (Action(ActionKind.END_TURN),))
    return SimpleNamespace(decision=decision, terminated=False, truncated=False, info={}, **kwargs)


def test_battle_boundary_is_not_full_run_return_or_success():
    t = transition(ScreenType.COMBAT_REWARD)
    assert battle_outcome(t, None, False) == "SURVIVED_BATTLE_EXIT"
    assert battle_outcome(t, None, True) == "ESCAPED_BATTLE"
    assert battle_outcome(transition(ScreenType.COMBAT), None, False) is None
    t.truncated = True
    assert battle_outcome(t, None, False) == "BACKEND_TRUNCATED"
    t.truncated, t.terminated = False, True
    assert battle_outcome(t, None, False) == "DEATH"
    t.info["success"] = True
    assert battle_outcome(t, None, False) == "RUN_SUCCESS"


def test_battle_selection_includes_available_bosses_and_is_fixed():
    states = [{"id": "a", "stratum": "act2:COMBAT:SNECKO"},
              {"id": "z", "stratum": "act2:COMBAT:THE_COLLECTOR"},
              {"id": "b", "stratum": "act1:COMBAT:HEXAGHOST"}]
    assert select_battle_states(states, 1) == [states[1]]
    assert select_battle_states(list(reversed(states)), 2) == [states[1], states[0]]


def test_chunk_aggregation_rejects_missing_seeds_and_faults():
    row = {"seed": 1, "success": False, "reason": "cycle_limit", "act_entries": {"2": {}}}
    chunks = [{"result": {"seed_results": [row], "backend_errors": 0}}]
    result = combine_chunks(chunks, [1])
    assert result["reached_act2"] == 1 and result["cycle_limits"] == 1
    with pytest.raises(ValueError, match="missing/duplicate"):
        combine_chunks(chunks, [1, 2])
    chunks[0]["result"]["backend_errors"] = 1
    with pytest.raises(ValueError, match="execution"):
        combine_chunks(chunks, [1])


def test_value_head_intervention_cannot_change_actor_or_memory():
    import torch

    from sls.diagnostics.cpu import score
    from sls.model import ModelConfig, Policy

    torch.set_num_threads(1)
    torch.manual_seed(91)
    model = Policy(ModelConfig(embedding_dim=16, attention_heads=4, transformer_layers=1,
        feedforward_dim=32, recurrent_hidden_dim=16)).eval()
    d = transition(ScreenType.COMBAT).decision
    d = replace(d, actions=(Action(ActionKind.END_TURN), Action(ActionKind.PROCEED)))
    memory = model.initial_memory(1, "cpu")
    before = score(model, d, memory)
    with torch.no_grad():
        for p in model.value_head.parameters():
            p.add_(3.)
    after = score(model, d, memory)
    assert torch.equal(before.logits, after.logits)
    assert torch.equal(before.next_memory, after.next_memory)
    assert not torch.equal(before.value, after.value)


def test_qualification_plan_protects_holdout_and_checkpoint_hash(tmp_path):
    from sls.diagnostics.critic_archive import sha
    from tools.qualify_stopped_critic import validate_plan

    parent = tmp_path / "parent.pt"
    parent.write_bytes(b"historical parent")
    plan = {"schema": "sls-stopped-critic-qualification-v1",
        "role": "STOPPED_CHECKPOINT_DEVELOPMENT_CONFIRMATION", "seed_range": [8000013000000, 8000013004096],
        "profile": "IRONCLAD_A20_ACT2", "batch_size": 128, "environment_shards": 16,
        "models": [{"label": "parent90", "path": "FROZEN_PARENT", "sha256": sha(parent)}]}
    assert validate_plan(plan, tmp_path, parent) == {"parent90": parent}
    plan["seed_range"] = [8000014000000, 8000014004096]
    with pytest.raises(ValueError, match="sealed or unregistered"):
        validate_plan(plan, tmp_path, parent)
    plan["seed_range"] = [8000013000000, 8000013004096]
    parent.write_bytes(b"mutated")
    with pytest.raises(ValueError, match="hash mismatch"):
        validate_plan(plan, tmp_path, parent)


def test_submission_command_only_runs_separate_qualification(tmp_path):
    from tools.submit_stopped_critic_qualification import command

    args = SimpleNamespace(python="/home/h/hengzhi/venvs/sls/bin/python", source_root=tmp_path)
    cmd = command(args, tmp_path / "plan.json", tmp_path / "run", tmp_path / "parent", tmp_path / "output")
    wrap = cmd[cmd.index("--wrap") + 1]
    assert "--execute-in-allocation" in wrap and "--source-root" in wrap
    assert "--job-name=sls-critic-stopped-eval" in cmd
    assert "train_full_run.py" not in wrap and "--time=12:00:00" in cmd
