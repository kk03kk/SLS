from __future__ import annotations

import gzip
import hashlib
import json
from types import SimpleNamespace

import pytest

from sls.curriculum import IRONCLAD_A20_ACT2
from sls.rl.start_curriculum import SCHEMA, StartCurriculum
from sls.rl.training_contract import native_source_digest
from tools.train_full_run import StopController


def bank(tmp_path, **changes):
    payload = {"schema": SCHEMA, "profile": IRONCLAD_A20_ACT2.profile_id,
               "native_source_sha256": native_source_digest(),
               "selection": "ALL_REACHED_ENTRIES_NO_OUTCOME_FILTER",
               "policy_memory": "RESET_AT_NEW_EPISODE",
               "entries": [{"seed": 10, "actions": ["legal-prefix"], "checkpoint": {"test": 1}},
                           {"seed": 11, "actions": ["legal-prefix"], "checkpoint": {"test": 2}}]}
    payload.update(changes)
    raw = gzip.compress(json.dumps(payload).encode())
    path = tmp_path / "bank.json.gz"
    path.write_bytes(raw)
    return path, hashlib.sha256(raw).hexdigest()


def test_episode_mix_continues_across_sparse_resets_and_resume(tmp_path):
    path, digest = bank(tmp_path)
    curriculum = StartCurriculum(path, digest)
    entry = SimpleNamespace(terminal=False, observation=SimpleNamespace(
        run=SimpleNamespace(act=2), screen=SimpleNamespace(value="MAP")))
    loaded = []
    workers = SimpleNamespace(profile=IRONCLAD_A20_ACT2,
                              load_one=lambda i, state: loaded.append((i, state)) or entry)
    result = curriculum.apply(workers, [0, 1, 2], [20, 21, 22], ["normal"] * 3)
    assert result == ["normal", entry, "normal"]
    resumed = StartCurriculum(path, digest)
    resumed.load_state_dict(curriculum.state_dict())
    assert resumed.apply(workers, [2], [23], ["normal"]) == [entry]
    state = resumed.state_dict()
    assert [state[k] for k in ("started", "normal_started", "entry_started")] == [4, 2, 2]
    resumed.note_terminal(2, "success")
    assert resumed.state_dict()["outcomes"] == {"normal": {}, "entry": {"success": 1}}
    assert [i for i, _ in loaded] == [1, 2]
    assert [state["test"] for _, state in loaded] == [1, 2]


@pytest.mark.parametrize("changes", [
    {"native_source_sha256": "stale"}, {"selection": "WINNERS_ONLY"},
    {"policy_memory": "TEACHER_MEMORY"}, {"entries": []},
])
def test_rejects_stale_or_biased_bank(tmp_path, changes):
    path, digest = bank(tmp_path, **changes)
    with pytest.raises(ValueError):
        StartCurriculum(path, digest)


def test_rejects_bank_bytes_and_invalid_continuation(tmp_path):
    path, digest = bank(tmp_path)
    with pytest.raises(ValueError, match="SHA256"):
        StartCurriculum(path, "wrong")
    curriculum = StartCurriculum(path, digest)
    with pytest.raises(ValueError, match="counters"):
        curriculum.load_state_dict({"started": 4, "normal_started": 3, "entry_started": 1})


def test_wall_deadline_requests_safe_shutdown(monkeypatch):
    monkeypatch.setattr("tools.train_full_run.time.monotonic", lambda: 100.0)
    controller = StopController()
    controller.set_wall_budget(10)
    assert not controller.requested
    monkeypatch.setattr("tools.train_full_run.time.monotonic", lambda: 111.0)
    assert controller.requested
    assert controller.signal_name == "WALL_TIME_BUDGET"


def test_mixed_training_cannot_omit_foundation_qualification():
    from sls.rl.preparation import require_preparation

    with pytest.raises(ValueError, match="foundation qualification"):
        require_preparation({"run": {}, "start_curriculum": {}}, None)
