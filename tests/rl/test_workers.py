from __future__ import annotations

import pytest

from sls.rl.workers import WorkerPool


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
