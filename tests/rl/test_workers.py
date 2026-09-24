from __future__ import annotations

import json
from pathlib import Path

import pytest

import sls.rl.workers as workers_module
from sls.curriculum import IRONCLAD_A0_ACT1
from sls.rl.workers import (
    CRASH_DUMP_SCHEMA,
    ShardedWorkerPool,
    VectorWorkerPool,
    WorkerPool,
    _crash_payload,
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


def test_sharded_reset_many_preserves_requested_index_and_seed_order() -> None:
    with ShardedWorkerPool(IRONCLAD_A0_ACT1, 4, shard_count=2) as pool:
        pool.reset((10, 11, 12, 13))
        decisions = pool.reset_many((3, 1), (103, 101))
        checkpoints = pool.checkpoints()
    assert len(decisions) == 2
    assert checkpoints[0]["run_state"]["seed"] == 10
    assert checkpoints[1]["run_state"]["seed"] == 101
    assert checkpoints[2]["run_state"]["seed"] == 12
    assert checkpoints[3]["run_state"]["seed"] == 103


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


def test_vector_diagnostic_failure_preserves_original_error(tmp_path: Path) -> None:
    blocked = tmp_path / "not-a-directory"
    blocked.write_text("occupied", encoding="utf-8")
    with VectorWorkerPool(IRONCLAD_A0_ACT1, 1, crash_dump_dir=blocked) as pool:
        pool._backends = [_DiagnosticBackend()]
        with pytest.raises(AttributeError, match="step"):
            pool.step(("invalid",))


@pytest.mark.parametrize("pool_type", [WorkerPool, ShardedWorkerPool])
def test_pool_start_failure_closes_pipes_and_started_processes(monkeypatch, pool_type) -> None:
    connections = []
    started = []

    class Connection:
        closed = False

        def close(self):
            self.closed = True

        def send(self, _message):
            pass

    class Process:
        joined = False

        def join(self, timeout):
            self.joined = True

        def is_alive(self):
            return False

    class Context:
        def Pipe(self):
            pair = (Connection(), Connection())
            connections.extend(pair)
            return pair

        def Process(self, **_kwargs):
            return Process()

    def start(process):
        if started:
            raise OSError("spawn failed")
        started.append(process)

    monkeypatch.setattr(workers_module.mp, "get_context", lambda _method: Context())
    monkeypatch.setattr(workers_module, "_start_importable_worker", start)
    with pytest.raises(OSError, match="spawn failed"):
        pool_type(IRONCLAD_A0_ACT1, 2)
    assert len(connections) == 4
    assert all(connection.closed for connection in connections)
    assert started[0].joined


@pytest.mark.parametrize("pool_type", [WorkerPool, ShardedWorkerPool])
def test_process_pool_close_is_idempotent(pool_type) -> None:
    pool = pool_type(IRONCLAD_A0_ACT1, 1)
    pool.close()
    pool.close()
    assert not pool._connections
    assert not pool._processes


@pytest.mark.parametrize("pool_type", [WorkerPool, ShardedWorkerPool])
@pytest.mark.parametrize("timeout", [0.0, -1.0, float("nan"), float("inf")])
def test_process_pool_rejects_invalid_timeout(pool_type, timeout) -> None:
    with pytest.raises(ValueError, match="timeout"):
        pool_type(IRONCLAD_A0_ACT1, 1, response_timeout_seconds=timeout)


@pytest.mark.parametrize("pool_type", [WorkerPool, VectorWorkerPool, ShardedWorkerPool])
def test_negative_single_worker_indices_cannot_mutate_last_environment(pool_type) -> None:
    with pool_type(IRONCLAD_A0_ACT1, 1) as pool:
        pool.reset((42,))
        before = pool.checkpoints()
        with pytest.raises(IndexError):
            pool.reset_one(-1, 99)
        with pytest.raises(IndexError):
            pool.load_one(-1, before[0])
        assert pool.checkpoints() == before
