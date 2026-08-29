from __future__ import annotations

import gzip
import base64
import json
from pathlib import Path
import subprocess
import sys

import pytest
import sls.validation.runtime as runtime_module

from sls.backends.original.adapter import adapt_original
from sls.backends.simulator import IRONCLAD_A0_ACT1, IRONCLAD_A0_HEART, SimulatorBackend
from sls.contracts.continuation import continuation_simulator
from sls.validation.runtime import RuntimeJournal
from sls.validation.evidence import original_evidence_gaps
from sls.validation.diff import differences
from sls.validation.truth import (
    TruthBundleRecorder, autosave_identity, evidence_at_least, load_bundle,
    canonical_json_bytes, continuation_differences, file_hash, recover_partial_bundle,
    resume_verification_boundary, resumable_original_boundary,
)
from sls.validation.truth import _git_metadata


TERMINAL_PAYLOAD = {
    "in_game": True,
    "available_commands": [],
    "game_state": {
        "screen_type": "DEATH", "class": "IRONCLAD", "ascension_level": 0,
        "act": 1, "floor": 0, "current_hp": 0, "max_hp": 80, "gold": 99,
        "act_boss": "INVALID", "deck": [], "relics": [], "potions": [], "map": [],
        "choice_list": [], "_parity_run": {}, "message": "中文 \\ \" 🚀",
    },
    "_rng": {},
}


def make_bundle(tmp_path: Path) -> Path:
    simulator = SimulatorBackend(IRONCLAD_A0_HEART)
    simulator_decision = simulator.reset(0)
    original_decision = adapt_original(TERMINAL_PAYLOAD).decision
    recorder = TruthBundleRecorder(
        tmp_path, seed=0, profile_id=IRONCLAD_A0_HEART.profile_id,
        policy_id="test-policy", evidence_class="LIVE_FULLRUN",
        repository_root=Path(__file__).resolve().parents[2],
    )
    recorder.record_protocol("rx", TERMINAL_PAYLOAD)
    recorder.record_boundary(
        sequence=0, original_payload=TERMINAL_PAYLOAD,
        original_decision=original_decision, simulator_state=simulator.raw_state,
        simulator_decision=simulator_decision, action=None, commands=(),
        observation_diff={"$.player.current_hp": (0, 80)}, action_diff={},
        state_diff={"run": ("original", "simulator")}, checkpoint=simulator.checkpoint(),
        terminal_kind="TERMINAL_MISMATCH",
    )
    return recorder.finalize(complete=False, outcome="TERMINAL_MISMATCH", error=None)


def test_truth_bundle_round_trip_and_recanonicalizes_raw_payload(tmp_path: Path) -> None:
    bundle = make_bundle(tmp_path)
    manifest, boundaries = load_bundle(bundle)
    assert manifest["schema"] == "sls-original-truth-bundle-v2"
    assert manifest["capture_mode"] == "PAIRED"
    assert boundaries[0]["raw_original_payload"]["game_state"]["message"].startswith("中文")
    assert adapt_original(boundaries[0]["raw_original_payload"]).decision.terminal
    assert evidence_at_least("LIVE_FULLRUN", "RESUMED_AUTOSAVE")
    assert not evidence_at_least("ORACLE_SCENARIO", "RESUMED_AUTOSAVE")
    anchor = manifest["anchors"][0]
    metadata = json.loads((bundle / anchor["path"] / "metadata.json").read_text(encoding="utf-8"))
    with gzip.open(bundle / anchor["path"] / "simulator-checkpoint.json.gz", "rt", encoding="utf-8") as stream:
        checkpoint = json.load(stream)
    from sls.validation.truth import value_hash
    assert metadata["checkpoint_state_hash"] == value_hash(checkpoint)


def test_canonical_json_preserves_unpaired_surrogate_as_an_escape() -> None:
    encoded = canonical_json_bytes({"text": "中文\udcaa"})
    assert b"\\udcaa" in encoded
    assert json.loads(encoded)["text"].endswith("\udcaa")


def test_git_dirty_hash_includes_untracked_source(tmp_path: Path) -> None:
    subprocess.run(["git", "init", "-q"], cwd=tmp_path, check=True)
    subprocess.run(["git", "config", "user.email", "truth@example.invalid"], cwd=tmp_path, check=True)
    subprocess.run(["git", "config", "user.name", "Truth Test"], cwd=tmp_path, check=True)
    tracked = tmp_path / "tracked.txt"
    tracked.write_text("base", encoding="utf-8")
    subprocess.run(["git", "add", "tracked.txt"], cwd=tmp_path, check=True)
    subprocess.run(["git", "commit", "-qm", "base"], cwd=tmp_path, check=True)
    _, clean_hash, dirty = _git_metadata(tmp_path)
    assert dirty is False
    untracked = tmp_path / "new.py"
    untracked.write_text("first", encoding="utf-8")
    _, first_hash, dirty = _git_metadata(tmp_path)
    untracked.write_text("second", encoding="utf-8")
    _, second_hash, _ = _git_metadata(tmp_path)
    assert dirty is True
    assert clean_hash != first_hash != second_hash


def test_resume_normalization_drops_only_dead_neow_rng_after_floor_zero() -> None:
    payload = json.loads(json.dumps(TERMINAL_PAYLOAD))
    payload["game_state"]["floor"] = 1
    payload["_rng"] = {
        "neow": {"seed0": 1, "seed1": 2, "counter": 3},
        "misc": {"seed0": 4, "seed1": 5, "counter": 6},
    }
    normalized = resumable_original_boundary(payload)
    assert "neow" not in normalized["state"]["rng"]
    assert normalized["state"]["rng"]["misc"]["counter"] == 6
    assert normalized["normalizations"] == ["drop_rng.neow_after_floor0"]


def test_resume_normalization_drops_floor_local_rng_after_combat() -> None:
    payload = json.loads(json.dumps(TERMINAL_PAYLOAD))
    payload["game_state"]["floor"] = 1
    payload["_continuation"] = {"post_combat": True}
    payload["_rng"] = {
        name: {"seed0": 1, "seed1": 2, "counter": 3}
        for name in ("neow", "ai", "shuffle", "card_random", "misc", "monster_hp", "card")
    }
    normalized = resumable_original_boundary(payload)
    assert set(normalized["state"]["rng"]) == {"card"}
    assert normalized["normalizations"] == [
        "drop_rng.neow_after_floor0", "drop_rng.floor_local_after_combat",
    ]


def test_resume_normalization_uses_stock_first_matching_bottle_identity() -> None:
    payload = json.loads(json.dumps(TERMINAL_PAYLOAD))
    payload["game_state"]["deck"] = [
        {"id": "Wild Strike", "upgrades": 0, "misc": 0},
        {"id": "Wild Strike", "upgrades": 0, "misc": 0},
    ]
    payload["_continuation"] = {
        "bottled_cards": [{
            "type": "ATTACK", "deck_index": 1, "id": "Wild Strike",
            "upgrades": 0, "misc": 0,
        }],
    }
    normalized = resumable_original_boundary(payload)
    assert normalized["continuation"]["bottled_cards"][0]["deck_index"] == 0
    assert "normalize_continuation.bottled_identity_for_stock_autosave" in (
        normalized["normalizations"]
    )


def test_resumed_simulator_applies_observed_stock_bottle_identity() -> None:
    from tools.replay_original_segment import _align_simulator_to_stock_autosave

    simulator = SimulatorBackend(IRONCLAD_A0_HEART)
    decision = simulator.reset(0)
    checkpoint = json.loads(json.dumps(simulator.checkpoint()))
    checkpoint["player_state"]["bottle_indices"] = [1, -1, -1]
    decision = simulator.load_checkpoint(checkpoint)
    payload = {"_continuation": {"bottled_cards": [{
        "type": "ATTACK", "deck_index": 2, "id": "Strike_R",
        "upgrades": 0, "misc": 0,
    }]}}
    decision, normalizations = _align_simulator_to_stock_autosave(
        simulator, decision, payload,
    )
    assert simulator.checkpoint()["player_state"]["bottle_indices"] == (2, -1, -1)
    assert normalizations == [{
        "kind": "STOCK_AUTOSAVE_BOTTLE_IDENTITY",
        "before": [1, -1, -1], "after": [2, -1, -1],
    }]


def test_ui_fold_is_recorded_evidence_not_cross_backend_continuation_state() -> None:
    assert not continuation_differences(
        {"continuation_kind": "MAP", "ui_boundary_folded": True},
        {"continuation_kind": 5, "ui_boundary_folded": False},
    )


def test_original_chest_continuation_aliases_canonical_treasure() -> None:
    assert not continuation_differences(
        {"continuation_kind": "CHEST"},
        {"continuation_kind": 6},
    )


def test_treasure_reward_screen_is_not_post_combat() -> None:
    continuation = continuation_simulator({
        "public_run": {"screen_state": 2},
        "progress_state": {"current_room": 5},
        "screen_info": {},
    })
    assert continuation["post_combat"] is False


def test_native_headbutt_continuation_exposes_pending_use_card() -> None:
    continuation = continuation_simulator({
        "public_run": {"screen_state": 9},
        "public_combat": {"choice": {"source": "DISCARD", "task": "HEADBUTT"}},
        "combat_checkpoint": {"action_queue_types": []},
    })
    assert continuation["action_queue_types"] == [
        "com.megacrit.cardcrawl.actions.utility.UseCardAction",
    ]


def test_original_event_class_aliases_native_event_id() -> None:
    assert not continuation_differences(
        {"event_id": "com.megacrit.cardcrawl.events.exordium.GoldenIdolEvent"},
        {"event_id": "Golden Idol"},
    )
    assert not continuation_differences(
        {"event_id": "com.megacrit.cardcrawl.events.exordium.Cleric"},
        {"event_id": "The Cleric"},
    )
    assert not continuation_differences(
        {"event_phase": "0"}, {"event_phase": 0},
    )


def test_resume_intersection_drops_missing_legacy_event_phase() -> None:
    payload = json.loads(json.dumps(TERMINAL_PAYLOAD))
    payload["_continuation"] = {"event_phase": "1", "event_id": "GoldenIdolEvent"}
    value = resume_verification_boundary(
        payload, ignored_evidence_codes=["MISSING_EVENT_PHASE"],
    )
    assert "event_phase" not in value["continuation"]


def test_terminal_continuation_ignores_inert_original_action_queue() -> None:
    assert not continuation_differences(
        {
            "screen": "DEATH", "continuation_kind": "DEATH",
            "action_phase": "EXECUTING_ACTIONS", "combat_turn": 2,
            "action_queue_types": ["RollMoveAction", "WaitAction"],
            "card_queue_types": [],
        },
        {
            "continuation_kind": "DEATH", "action_phase": None,
            "combat_turn": None, "action_queue_types": [], "card_queue_types": [],
        },
    )


def test_truth_bundle_rejects_tampered_payload(tmp_path: Path) -> None:
    bundle = make_bundle(tmp_path)
    path = bundle / "boundaries.jsonl.gz"
    with gzip.open(path, "rt", encoding="utf-8") as stream:
        values = [json.loads(line) for line in stream]
    values[0]["raw_original_payload"]["game_state"]["gold"] = 1000
    with gzip.open(path, "wt", encoding="utf-8") as stream:
        for value in values:
            stream.write(json.dumps(value, ensure_ascii=False) + "\n")
    with pytest.raises(ValueError, match="hash mismatch"):
        load_bundle(bundle)


def test_v1_truth_bundle_remains_read_only_compatible(tmp_path: Path) -> None:
    bundle = make_bundle(tmp_path)
    boundary_path = bundle / "boundaries.jsonl.gz"
    with gzip.open(boundary_path, "rt", encoding="utf-8") as stream:
        values = [json.loads(line) for line in stream]
    values[0]["schema"] = "sls-original-truth-boundary-v1"
    with boundary_path.open("wb") as raw:
        with gzip.GzipFile(filename="", mode="wb", fileobj=raw, mtime=0) as stream:
            stream.write((json.dumps(values[0], ensure_ascii=False) + "\n").encode("utf-8"))
    manifest_path = bundle / "manifest.json"
    manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    manifest["schema"] = "sls-original-truth-bundle-v1"
    manifest["artifacts"]["boundaries.jsonl.gz"] = file_hash(boundary_path)
    manifest_path.write_text(json.dumps(manifest), encoding="utf-8")
    loaded, boundaries = load_bundle(bundle)
    assert loaded["schema"].endswith("v1")
    assert boundaries[0]["schema"].endswith("v1")


def test_runtime_journal_restores_existing_and_removes_created_files(tmp_path: Path) -> None:
    existing = tmp_path / "save.autosave"
    created = tmp_path / "config.properties"
    existing.write_text("user-save", encoding="utf-8")
    journal = RuntimeJournal(tmp_path / "journal.json")
    journal.backup(existing, tmp_path / "backup")
    journal.backup(created, tmp_path / "backup")
    journal.mark_active()
    existing.write_text("validation-save", encoding="utf-8")
    created.write_text("temporary", encoding="utf-8")
    journal.recover()
    assert existing.read_text(encoding="utf-8") == "user-save"
    assert not created.exists()
    RuntimeJournal.open(journal.path).recover()
    assert existing.read_text(encoding="utf-8") == "user-save"


def test_runtime_journal_can_restore_cloud_files_before_process_exit(tmp_path: Path) -> None:
    preferences = tmp_path / "preferences"
    preferences.mkdir()
    slots = preferences / "STSSaveSlots"
    slots.write_text("user-slots", encoding="utf-8")
    unrelated = tmp_path / "mods" / "oracle.jar"
    unrelated.parent.mkdir()
    unrelated.write_text("user-jar", encoding="utf-8")
    journal = RuntimeJournal(tmp_path / "journal.json")
    journal.backup(slots, tmp_path / "backup")
    journal.backup(unrelated, tmp_path / "backup")
    journal.mark_active()
    slots.write_text("validation-slots", encoding="utf-8")
    unrelated.write_text("validation-jar", encoding="utf-8")

    journal.restore_under((preferences,))

    assert slots.read_text(encoding="utf-8") == "user-slots"
    assert unrelated.read_text(encoding="utf-8") == "validation-jar"
    assert RuntimeJournal.open(journal.path).data["status"] == "ACTIVE"
    journal.recover()
    assert unrelated.read_text(encoding="utf-8") == "user-jar"


def test_runtime_journal_refuses_corrupt_backup(tmp_path: Path) -> None:
    existing = tmp_path / "save.autosave"
    existing.write_text("user-save", encoding="utf-8")
    journal = RuntimeJournal(tmp_path / "journal.json")
    backup = tmp_path / "backup"
    journal.backup(existing, backup)
    next(backup.iterdir()).write_text("corrupt", encoding="utf-8")
    existing.write_text("validation-save", encoding="utf-8")
    with pytest.raises(RuntimeError, match="recovery failed"):
        journal.recover()
    assert RuntimeJournal.open(journal.path).data["status"] == "RECOVERY_FAILED"


def test_pending_recovery_refuses_a_live_owned_process(tmp_path: Path, monkeypatch) -> None:
    root = tmp_path / "journals" / "one"
    journal = RuntimeJournal(root / "journal.json")
    executable = tmp_path / "javaw.exe"
    executable.write_bytes(b"java")
    journal.record_process(pid=123, executable=executable, command=(str(executable),))
    journal.mark_active()
    monkeypatch.setattr(runtime_module, "process_identity", lambda pid: {
        "ProcessId": pid, "ExecutablePath": str(executable), "CommandLine": str(executable),
    })
    with pytest.raises(RuntimeError, match="while owned game process is alive"):
        runtime_module.recover_pending(tmp_path / "journals")


def test_partial_truth_recovery_is_aborted_and_not_eligible(tmp_path: Path) -> None:
    simulator = SimulatorBackend(IRONCLAD_A0_HEART)
    simulator_decision = simulator.reset(0)
    original_decision = adapt_original(TERMINAL_PAYLOAD).decision
    recorder = TruthBundleRecorder(
        tmp_path, seed=0, profile_id=IRONCLAD_A0_HEART.profile_id,
        policy_id="interrupted", repository_root=Path(__file__).resolve().parents[2],
    )
    recorder.record_boundary(
        sequence=0, original_payload=TERMINAL_PAYLOAD,
        original_decision=original_decision, simulator_state=simulator.raw_state,
        simulator_decision=simulator_decision, action=None, commands=(),
        observation_diff={}, action_diff={}, state_diff={},
        checkpoint=simulator.checkpoint(), terminal_kind=None,
    )
    with recorder._boundary_stage.open("ab") as stream:
        stream.write(b'{"truncated":')
    recovered = recover_partial_bundle(recorder.path)
    manifest, boundaries = load_bundle(recovered)
    assert len(boundaries) == 1
    assert manifest["aborted"] is True
    assert manifest["acceptance_eligible"] is False


def test_staged_action_updates_merge_before_finalize(tmp_path: Path) -> None:
    simulator = SimulatorBackend(IRONCLAD_A0_HEART)
    simulator_decision = simulator.reset(0)
    recorder = TruthBundleRecorder(
        tmp_path, seed=0, profile_id=IRONCLAD_A0_HEART.profile_id,
        policy_id="updates", repository_root=Path(__file__).resolve().parents[2],
    )
    recorder.record_boundary(
        sequence=0, original_payload=TERMINAL_PAYLOAD,
        original_decision=adapt_original(TERMINAL_PAYLOAD).decision,
        simulator_state=simulator.raw_state, simulator_decision=simulator_decision,
        action=None, commands=(), observation_diff={}, action_diff={}, state_diff={},
        checkpoint=simulator.checkpoint(), terminal_kind=None,
    )
    action = simulator_decision.actions[0]
    recorder.select_last_action(action, ("choose 0",))
    recorder.mark_last_action_executed(("choose 0", "wait 1"))
    _, boundaries = load_bundle(recorder.finalize(complete=False, outcome=None, error=None))
    assert boundaries[0]["selected_action"] == action.to_dict()
    assert boundaries[0]["commands"] == ["choose 0", "wait 1"]
    assert boundaries[0]["action_executed"] is True


def test_verified_resume_promotes_initial_native_anchor(tmp_path: Path) -> None:
    simulator = SimulatorBackend(IRONCLAD_A0_HEART)
    simulator_decision = simulator.reset(0)
    recorder = TruthBundleRecorder(
        tmp_path / "truth", seed=0, profile_id=IRONCLAD_A0_HEART.profile_id,
        policy_id="resume", repository_root=Path(__file__).resolve().parents[2],
    )
    recorder.record_boundary(
        sequence=0, original_payload=TERMINAL_PAYLOAD,
        original_decision=adapt_original(TERMINAL_PAYLOAD).decision,
        simulator_state=simulator.raw_state, simulator_decision=simulator_decision,
        action=None, commands=(), observation_diff={}, action_diff={}, state_diff={},
        checkpoint=simulator.checkpoint(), terminal_kind=None,
    )
    save_value = {"seed": 0, "floor_num": 0, "current_room": "MonsterRoom"}
    raw = json.dumps(save_value).encode("utf-8")
    encoded = bytes(item ^ b"key"[index % 3] for index, item in enumerate(raw))
    save = tmp_path / "IRONCLAD.autosave"
    save.write_bytes(base64.b64encode(encoded))
    recorder.mark_initial_resume_verified(save, source_run_id="source", source_anchor_id="a1")
    bundle = recorder.finalize(complete=False, outcome="RESUMED_WINDOW", error=None)
    manifest, _ = load_bundle(bundle)
    assert manifest["anchors"][0]["capability"] == "RESUME_VERIFIED"
    metadata = json.loads(
        (bundle / manifest["anchors"][0]["path"] / "metadata.json").read_text(encoding="utf-8")
    )
    assert metadata["derived_from"] == {"source_run_id": "source", "source_anchor_id": "a1"}
    assert metadata["files"]["original.autosave"] == file_hash(save)


def test_missing_original_fields_are_inconclusive_evidence() -> None:
    gaps = original_evidence_gaps(TERMINAL_PAYLOAD, canonical_screen="GAME_OVER")
    codes = {item["code"] for item in gaps}
    assert "MISSING_CONTINUATION_EVIDENCE" in codes
    assert "MISSING_BURNING_ELITE_X" in codes


def test_hidden_combat_reward_cards_are_an_evidence_gap_until_oracle_supplies_them() -> None:
    payload = json.loads(json.dumps(TERMINAL_PAYLOAD))
    payload["available_commands"] = ["choose", "proceed"]
    payload["game_state"].update({
        "screen_type": "COMBAT_REWARD", "current_hp": 80,
        "screen_state": {"rewards": [
            {"reward_type": "GOLD", "gold": 11}, {"reward_type": "CARD"},
        ]},
    })
    codes = {
        item["code"] for item in original_evidence_gaps(payload, canonical_screen="COMBAT_REWARD")
    }
    assert "MISSING_COMBAT_REWARD_CARD_OPTIONS" in codes
    payload["_combat_reward_cards"] = [[
        {"id": "Anger", "upgrades": 0},
        {"id": "Clothesline", "upgrades": 0},
        {"id": "Whirlwind", "upgrades": 0},
    ]]
    decision = adapt_original(payload).decision
    # The flattened policy boundary omits stock's reversible child-popup
    # ``skip``; the parent SKIP_REWARD remains the irreversible abandon action.
    assert len(decision.actions) == 5
    assert [item.content_id for item in decision.observation.reward_options] == [
        "ANGER", "CLOTHESLINE", "WHIRLWIND", "GOLD",
    ]


def test_attack_intent_requires_adjusted_damage_and_hit_evidence() -> None:
    payload = json.loads(json.dumps(TERMINAL_PAYLOAD))
    payload["game_state"].update({
        "screen_type": "COMBAT", "combat_state": {
            "monsters": [{"id": "JawWorm", "current_hp": 44, "max_hp": 44}],
        },
    })
    payload["_monster_intents"] = [{"intent": "ATTACK", "base_damage": 11}]
    codes = {
        item["code"] for item in original_evidence_gaps(payload, canonical_screen="COMBAT")
    }
    assert "MISSING_ADJUSTED_MONSTER_INTENT_DAMAGE" in codes
    payload["_monster_intents"][0].update({"damage": -1, "hits": 1})
    codes = {
        item["code"] for item in original_evidence_gaps(payload, canonical_screen="COMBAT")
    }
    assert "UNSETTLED_ADJUSTED_MONSTER_INTENT_DAMAGE" in codes
    payload["_monster_intents"][0]["damage"] = 11
    codes = {
        item["code"] for item in original_evidence_gaps(payload, canonical_screen="COMBAT")
    }
    assert "MISSING_ADJUSTED_MONSTER_INTENT_DAMAGE" not in codes


def test_combat_dynamic_cost_is_required_evidence() -> None:
    payload = json.loads(json.dumps(TERMINAL_PAYLOAD))
    payload["game_state"].update({
        "screen_type": "COMBAT", "combat_state": {
            "hand": [{"id": "Wild Strike", "cost": 1}], "monsters": [],
        },
    })
    payload["_monster_intents"] = []

    codes = {
        item["code"] for item in original_evidence_gaps(payload, canonical_screen="COMBAT")
    }
    assert "MISSING_DYNAMIC_CARD_COSTS" in codes
    payload["game_state"]["combat_state"]["hand"][0]["cost_for_turn"] = 0
    codes = {
        item["code"] for item in original_evidence_gaps(payload, canonical_screen="COMBAT")
    }
    assert "MISSING_DYNAMIC_CARD_COSTS" not in codes


def test_act_two_combat_requires_authoritative_max_energy() -> None:
    payload = json.loads(json.dumps(TERMINAL_PAYLOAD))
    payload["game_state"].update({
        "act": 2,
        "screen_type": "COMBAT",
        "combat_state": {"player": {"energy": 4}, "monsters": []},
    })
    payload["_monster_intents"] = []
    codes = {
        item["code"] for item in original_evidence_gaps(payload, canonical_screen="COMBAT")
    }
    assert "MISSING_MAX_ENERGY" in codes
    payload["_parity_run"] = {**payload.get("_parity_run", {}), "max_energy": 4}
    codes = {
        item["code"] for item in original_evidence_gaps(payload, canonical_screen="COMBAT")
    }
    assert "MISSING_MAX_ENERGY" not in codes


def test_resume_intersection_ignores_only_unsettled_adjusted_intent_fields() -> None:
    payload = json.loads(json.dumps(TERMINAL_PAYLOAD))
    payload["game_state"].update({
        "screen_type": "COMBAT", "combat_state": {"turn": 1, "monsters": [{
            "id": "JawWorm", "current_hp": 44, "max_hp": 44, "block": 0,
            "move_adjusted_damage": -1, "move_hits": 1,
        }]},
    })
    payload["_monster_intents"] = [{"intent": "ATTACK", "damage": -1, "hits": 1}]
    refreshed = json.loads(json.dumps(payload))
    refreshed["_monster_intents"][0]["damage"] = 11
    code = ["UNSETTLED_ADJUSTED_MONSTER_INTENT_DAMAGE"]
    assert resume_verification_boundary(
        payload, ignored_evidence_codes=code,
    ) == resume_verification_boundary(refreshed, ignored_evidence_codes=code)


def test_autosave_identity_decodes_stock_envelope(tmp_path: Path) -> None:
    value = {
        "seed": 7, "floor_num": 3, "current_room": "MonsterRoom",
        "room_x": 2, "room_y": 1,
    }
    raw = json.dumps(value).encode("utf-8")
    encoded = bytes(item ^ b"key"[index % 3] for index, item in enumerate(raw))
    path = tmp_path / "IRONCLAD.autosave"
    path.write_bytes(base64.b64encode(encoded))
    assert autosave_identity(path) == {
        "seed": 7, "floor": 3, "character": "IRONCLAD",
        "room_class": "MonsterRoom", "map_x": 2, "map_y": 1,
    }


def test_offline_replay_reproduces_difference_and_extractor_is_stable(tmp_path: Path) -> None:
    bundle = make_bundle(tmp_path / "truth")
    root = Path(__file__).resolve().parents[2]
    replay = subprocess.run(
        [sys.executable, str(root / "tools" / "replay_truth.py"), str(bundle)],
        cwd=root, text=True, capture_output=True,
    )
    assert replay.returncode == 1, replay.stderr
    detail = json.loads(replay.stdout)
    assert detail["difference"]["step"] == 0
    assert detail["difference"]["signature"]
    output = tmp_path / "fixtures"
    command = [
        sys.executable, str(root / "tools" / "extract_regression.py"), str(bundle),
        "--step", "0", "--issue", "truth-test", "--output-root", str(output),
    ]
    first = subprocess.run(command, cwd=root, text=True, capture_output=True)
    assert first.returncode == 0, first.stderr
    target = output / "truth-test.instrumentation-request.json.gz"
    initial = target.read_bytes()
    second = subprocess.run(command, cwd=root, text=True, capture_output=True)
    assert second.returncode == 0, second.stderr
    assert target.read_bytes() == initial


def test_regression_extractor_falls_back_from_unloadable_nearest_anchor(
    tmp_path: Path,
) -> None:
    from sls.validation.truth import value_hash, write_json_gz
    from tools.extract_regression import _simulator_at_step

    simulator = SimulatorBackend(IRONCLAD_A0_HEART)
    decision = simulator.reset(0)
    checkpoint = simulator.checkpoint()
    action = decision.actions[0]
    expected = simulator.step(action).decision
    older = tmp_path / "anchors" / "older"
    newer = tmp_path / "anchors" / "newer"
    write_json_gz(older / "simulator-checkpoint.json.gz", checkpoint)
    newer.mkdir(parents=True)
    (newer / "simulator-checkpoint.json.gz").write_bytes(b"not-gzip")
    manifest = {
        "profile_id": IRONCLAD_A0_HEART.profile_id,
        "anchors": [
            {"anchor_id": "older", "sequence": 0, "path": "anchors/older"},
            {"anchor_id": "newer", "sequence": 1, "path": "anchors/newer"},
        ],
    }
    boundaries = [{"selected_action": action.to_dict()}, {"selected_action": None}]
    _, actual, anchor, restored, suffix = _simulator_at_step(
        tmp_path, manifest, boundaries, 1,
    )
    assert anchor["anchor_id"] == "older"
    assert value_hash(restored) == value_hash(checkpoint)
    assert suffix == [action.to_dict()]
    assert actual.observation.to_dict() == expected.observation.to_dict()


def test_committed_adapter_regressions_match_expected_canonical_output() -> None:
    root = Path(__file__).resolve().parents[2]
    for path in (root / "tests" / "fixtures" / "regressions").glob("*.json.gz"):
        with gzip.open(path, "rt", encoding="utf-8") as stream:
            fixture = json.load(stream)
        if fixture["category"] == "simulator_adapter":
            from sls.contracts import Action

            simulator = SimulatorBackend(IRONCLAD_A0_HEART)
            decision = simulator.load_checkpoint(fixture["simulator_checkpoint"])
            evidence_suffix = fixture.get("action_evidence_suffix", [])
            for index, action in enumerate(fixture.get("action_suffix", [])):
                evidence = evidence_suffix[index] if index < len(evidence_suffix) else {}
                decision = simulator.step(
                    Action.from_dict(action), validation_evidence=evidence,
                ).decision
            durable_checkpoint = json.loads(json.dumps(simulator.checkpoint()))
            restored = SimulatorBackend(IRONCLAD_A0_HEART)
            restored_decision = restored.load_checkpoint(durable_checkpoint)
            assert restored_decision.observation.to_dict() == decision.observation.to_dict()
            actual = {
                "observation": decision.observation.to_dict(),
                "actions": [action.to_dict() for action in decision.actions],
                "terminal": decision.terminal,
            }
            assert not differences(actual, fixture["expected"]), path.name
            continue
        if fixture["category"] == "transition":
            from sls.contracts import Action
            from sls.contracts.continuation import continuation_simulator
            from sls.validation.compare import canonical_simulator

            simulator = SimulatorBackend(IRONCLAD_A0_HEART)
            simulator.load_checkpoint(fixture["simulator_checkpoint"])
            evidence_suffix = fixture.get("action_evidence_suffix", [])
            for index, prefix_action in enumerate(fixture.get("action_suffix", [])):
                evidence = evidence_suffix[index] if index < len(evidence_suffix) else {}
                simulator.step(Action.from_dict(prefix_action), validation_evidence=evidence)
            decision = simulator.step(
                Action.from_dict(fixture["action"]),
                validation_evidence=fixture.get("action_evidence") or {},
            ).decision
            actual = {
                "canonical_public_state": canonical_simulator(simulator.raw_state),
                "canonical_decision": {
                    "observation": decision.observation.to_dict(),
                    "actions": [action.to_dict() for action in decision.actions],
                    "terminal": decision.terminal,
                },
                "rng": canonical_simulator(simulator.raw_state)["rng"],
                "continuation": continuation_simulator(simulator.raw_state),
            }
            # Official resumes do not preserve the exhausted Neow stream;
            # LIVE_FULLRUN fixtures retain and compare it.
            if fixture.get("provenance", {}).get("evidence_class") == "RESUMED_AUTOSAVE":
                for value in (actual["canonical_public_state"].get("rng", {}), actual["rng"]):
                    value.pop("neow", None)
            expected = fixture["expected"]
            assert not differences(
                {key: actual[key] for key in ("canonical_public_state", "canonical_decision", "rng")},
                {key: expected[key] for key in ("canonical_public_state", "canonical_decision", "rng")},
            ), path.name
            from sls.validation.truth import continuation_differences
            assert not continuation_differences(
                expected["continuation"], actual["continuation"],
            ), path.name
            continue
        if fixture["category"] == "rng":
            from sls.contracts import Action
            from sls.validation.compare import canonical_simulator

            simulator = SimulatorBackend(IRONCLAD_A0_HEART)
            simulator.load_checkpoint(fixture["simulator_checkpoint"])
            evidence_suffix = fixture.get("action_evidence_suffix", [])
            for index, prefix_action in enumerate(fixture.get("action_suffix", [])):
                evidence = evidence_suffix[index] if index < len(evidence_suffix) else {}
                simulator.step(Action.from_dict(prefix_action), validation_evidence=evidence)
            before = canonical_simulator(simulator.raw_state)["rng"]
            before.pop("neow", None)
            assert not differences(before, fixture["before"]["original"]), path.name
            simulator.step(
                Action.from_dict(fixture["action"]),
                validation_evidence=fixture.get("action_evidence") or {},
            )
            after = canonical_simulator(simulator.raw_state)["rng"]
            after.pop("neow", None)
            assert not differences(after, fixture["after"]["original"]), path.name
            for stream, expected_delta in fixture["counter_delta"].items():
                if expected_delta["original"] is None:
                    continue
                assert (
                    after[stream]["counter"] - before[stream]["counter"]
                    == expected_delta["original"]
                ), (path.name, stream)
            continue
        if fixture["category"] != "adapter":
            continue
        decision = adapt_original(fixture["raw_original_payload"]).decision
        actual = {
            "observation": decision.observation.to_dict(),
            "actions": [action.to_dict() for action in decision.actions],
            "terminal": decision.terminal,
        }
        current = differences(actual, fixture["expected"])
        for expected_path in fixture.get("expected_paths", {}):
            category, field_path = expected_path.split(":", 1)
            prefix = "$.observation" if category == "observation" else "$.actions"
            assert prefix + field_path[1:] not in current, (path.name, expected_path)


def test_legacy_golden_idol_checkpoint_migrates_to_exact_continuation() -> None:
    root = Path(__file__).resolve().parents[2]
    path = root / "tests" / "fixtures" / "regressions" / (
        "curse-unplayable-cost.json.gz"
    )
    with gzip.open(path, "rt", encoding="utf-8") as stream:
        fixture = json.load(stream)

    legacy = fixture["simulator_checkpoint"]
    assert legacy["screen_info"] == {
        "complete": False, "event_data": 0, "screen_state": 1,
    }
    simulator = SimulatorBackend(IRONCLAD_A0_HEART)
    decision = simulator.load_checkpoint(legacy)
    migrated = json.loads(json.dumps(simulator.checkpoint()))
    assert migrated["screen_info"] == {
        "complete": True,
        "continuation": "map",
        "event_data": 0,
        "hp_amount_0": 20,
        "hp_amount_1": 6,
        "screen_state": 1,
    }

    restored = SimulatorBackend(IRONCLAD_A0_HEART)
    assert restored.load_checkpoint(migrated) == decision
    assert restored.raw_state == simulator.raw_state


def test_upgrade_shrine_checkpoint_migrates_to_exact_continuation() -> None:
    from sls.contracts import ActionKind

    simulator = SimulatorBackend(IRONCLAD_A0_ACT1)
    simulator.reset(3)
    simulator._native.reset_event_probe(3, "Upgrade Shrine", simulator.raw_state["rng"])
    decision = simulator._adapt(simulator._native.snapshot())
    checkpoint = json.loads(json.dumps(simulator.checkpoint()))
    assert checkpoint["screen_info"] == {
        "complete": True,
        "continuation": "map",
        "event_data": 0,
        "screen_state": 1,
    }

    # Historical checkpoints marked this deterministic event dialog as an
    # incomplete continuation and attempted to reconstruct it from the entire
    # seed-local action history.  It is now restored from its exact state.
    legacy = json.loads(json.dumps(checkpoint))
    legacy["screen_info"] = {
        "complete": False, "event_data": 0, "screen_state": 1,
    }
    legacy["progress_state"]["screen_continuation_serialized"] = False
    restored = SimulatorBackend(IRONCLAD_A0_ACT1)
    assert restored.load_checkpoint(legacy) == decision
    migrated = json.loads(json.dumps(restored.checkpoint()))
    assert migrated["screen_info"] == checkpoint["screen_info"]

    choose_upgrade = next(
        action for action in decision.actions
        if action.kind == ActionKind.CHOOSE_EVENT_OPTION and action.option_id == "event-option:0"
    )
    selection = restored.step(choose_upgrade).decision
    assert selection.actions
    assert all(action.kind == ActionKind.UPGRADE_CARD for action in selection.actions)
    final = restored.step(selection.actions[0]).decision
    assert final.observation.screen.value == "MAP"


def test_discovery_timing_evidence_replays_the_observed_fifteen_update_variant() -> None:
    # Original bundle 20260820T175146.915871Z-seed-0 recorded 15 retrieval
    # updates and card_random counter 50 from the same room-entry autosave.
    root = Path(__file__).resolve().parents[2]
    path = root / "tests" / "fixtures" / "regressions" / (
        "original-colorless-potion-card-rng-order.json.gz"
    )
    with gzip.open(path, "rt", encoding="utf-8") as stream:
        fixture = json.load(stream)
    from sls.contracts import Action
    from sls.validation.compare import canonical_simulator

    simulator = SimulatorBackend(IRONCLAD_A0_HEART)
    simulator.load_checkpoint(fixture["simulator_checkpoint"])
    simulator.step(Action.from_dict(fixture["action_suffix"][0]))
    simulator.step(
        Action.from_dict(fixture["action"]),
        validation_evidence={"discovery_retrieval_updates": 15},
    )
    assert canonical_simulator(simulator.raw_state)["rng"]["card_random"] == {
        "counter": 50,
        "seed0": 8409297769953316801,
        "seed1": 6897397330030733022,
    }
