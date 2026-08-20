from __future__ import annotations

import gzip
import json
from pathlib import Path
import subprocess
import sys

import pytest

from sls.backends.original.adapter import adapt_original
from sls.backends.simulator import IRONCLAD_A0_HEART, SimulatorBackend
from sls.validation.runtime import RuntimeJournal
from sls.validation.diff import differences
from sls.validation.truth import (
    TruthBundleRecorder, evidence_at_least, load_bundle, resumable_original_boundary,
)


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
    assert manifest["schema"] == "sls-original-truth-bundle-v1"
    assert boundaries[0]["raw_original_payload"]["game_state"]["message"].startswith("中文")
    assert adapt_original(boundaries[0]["raw_original_payload"]).decision.terminal
    assert evidence_at_least("LIVE_FULLRUN", "RESUMED_AUTOSAVE")
    assert not evidence_at_least("ORACLE_SCENARIO", "RESUMED_AUTOSAVE")


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
    initial = (output / "truth-test.json.gz").read_bytes()
    second = subprocess.run(command, cwd=root, text=True, capture_output=True)
    assert second.returncode == 0, second.stderr
    assert (output / "truth-test.json.gz").read_bytes() == initial


def test_committed_adapter_regressions_match_expected_canonical_output() -> None:
    root = Path(__file__).resolve().parents[2]
    for path in (root / "tests" / "fixtures" / "regressions").glob("*.json.gz"):
        with gzip.open(path, "rt", encoding="utf-8") as stream:
            fixture = json.load(stream)
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
