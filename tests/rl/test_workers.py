from __future__ import annotations

import json
from pathlib import Path

import pytest

from sls.curriculum import IRONCLAD_A0_ACT1
from sls.rl.workers import (
    CRASH_DUMP_SCHEMA, VectorWorkerPool, WorkerPool, _crash_payload,
    _write_crash_dump,
)


class _SilentConnection:
    def poll(self, timeout: float) -> bool:
        return False


class _DeadProcess:
    exitcode = 23

    def is_alive(self) -> bool:
        return False


def test_worker_pool_fails_fast_when_worker_exits_without_a_response() -> None:
    pool = object.__new__(WorkerPool)
    pool.profile = None  # type: ignore[assignment]
    pool.size = 1
    pool.response_timeout_seconds = 0.001
    pool._connections = [_SilentConnection()]  # type: ignore[list-item]
    pool._processes = [_DeadProcess()]  # type: ignore[list-item]

    with pytest.raises(TimeoutError, match="worker 0 exited with code 23"):
        pool._collect((0,))


class _DiagnosticBackend:
    _candidate_bits = {'{"kind":"PROCEED"}': 17}

    @property
    def raw_state(self) -> dict[str, object]:
        return {
            "run_state": {"seed": 997},
            "public_run": {
                "outcome": 1, "screen_state": 2,
                "current_event_id": "INVALID",
            },
            "public_inventory": {"deck": [], "relics": [], "potions": []},
            "public_screen": {"gold": [25]},
            "legal_actions": [{
                "bits": 17, "idx1": 0, "idx2": 0,
                "potion": False, "reward_type": 1,
            }],
            "rng": {"misc": {"counter": 3}},
        }


def test_crash_dump_is_atomic_replay_state_without_backend_mutation(tmp_path: Path) -> None:
    backend = _DiagnosticBackend()
    before = json.loads(json.dumps(backend.raw_state))
    payload = _crash_payload(
        backend,
        error=ValueError("a non-terminal decision must expose a legal action"),
        worker_index=23,
        episode_ordinal=81,
        seed=None,
        last_semantic_action='{"kind":"END_TURN"}',
        profile=IRONCLAD_A0_ACT1,
    )
    path = _write_crash_dump(tmp_path, payload)
    assert backend.raw_state == before
    stored = json.loads(path.read_text(encoding="utf-8"))
    assert stored["schema"] == CRASH_DUMP_SCHEMA
    assert stored["worker_index"] == 23
    assert stored["seed"] == 997
    assert stored["terminal_flag"] is False
    assert stored["last_semantic_action"] == {"kind": "END_TURN"}
    assert stored["generated_actions"] == [{"kind": "PROCEED"}]
    assert stored["raw_legal_action_groups"] == {"RUN:REWARD:1": [before["legal_actions"][0]]}
    assert not list(tmp_path.glob("*.tmp"))


def test_vector_worker_writes_the_same_replayable_crash_schema(tmp_path: Path) -> None:
    pool = object.__new__(VectorWorkerPool)
    pool.profile = IRONCLAD_A0_ACT1
    pool.size = 1
    pool.crash_dump_dir = tmp_path
    pool._backends = [_DiagnosticBackend()]
    pool._episode_ordinals = [6]
    pool._seeds = [8335]
    pool._last_actions = [None]

    with pytest.raises(AttributeError):
        pool._execute(0, "step", '{"kind":"CHOOSE_MAP_NODE"}')

    dumps = list(tmp_path.glob("*.json"))
    assert len(dumps) == 1
    payload = json.loads(dumps[0].read_text(encoding="utf-8"))
    assert payload["schema"] == CRASH_DUMP_SCHEMA
    assert payload["worker_index"] == 0
    assert payload["worker_episode_ordinal"] == 6
    assert payload["seed"] == 8335
