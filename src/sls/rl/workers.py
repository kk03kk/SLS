"""Spawned native environments with centralized model inference."""

from __future__ import annotations

from dataclasses import dataclass, field
import multiprocessing as mp
from multiprocessing.connection import Connection
import time
from typing import Any, Mapping, Sequence

from sls.contracts import Decision, Transition
from sls.curriculum import CurriculumProfile


def _worker(connection: Connection, profile: CurriculumProfile) -> None:
    from sls.backends.simulator import SimulatorBackend

    backend = SimulatorBackend(profile)
    try:
        while True:
            command, payload = connection.recv()
            if command == "reset":
                connection.send(backend.reset(int(payload)))
            elif command == "step":
                connection.send(backend.step(str(payload)))
            elif command == "checkpoint":
                connection.send(backend.checkpoint())
            elif command == "load":
                connection.send(backend.load_checkpoint(payload))
            elif command == "close":
                break
            else:
                raise RuntimeError(f"unknown worker command: {command}")
    except BaseException as error:
        try:
            connection.send(error)
        except BaseException:
            pass
        raise
    finally:
        connection.close()


@dataclass(slots=True)
class WorkerPool:
    profile: CurriculumProfile
    size: int
    response_timeout_seconds: float = 120.0
    _connections: list[Connection] = field(init=False, repr=False)
    _processes: list[mp.Process] = field(init=False, repr=False)

    def __post_init__(self) -> None:
        if self.size <= 0:
            raise ValueError("worker count must be positive")
        if self.response_timeout_seconds <= 0:
            raise ValueError("worker response timeout must be positive")
        context = mp.get_context("spawn")
        self._connections: list[Connection] = []
        self._processes: list[mp.Process] = []
        for index in range(self.size):
            parent, child = context.Pipe()
            process = context.Process(
                target=_worker,
                args=(child, self.profile),
                name=f"sls-env-{index}",
            )
            process.start()
            child.close()
            self._connections.append(parent)
            self._processes.append(process)

    def _collect(self, indices: Sequence[int]) -> list[Any]:
        values = []
        deadline = time.monotonic() + self.response_timeout_seconds
        for index in indices:
            connection = self._connections[index]
            remaining = deadline - time.monotonic()
            if remaining <= 0 or not connection.poll(remaining):
                process = self._processes[index]
                state = (
                    f"exited with code {process.exitcode}"
                    if not process.is_alive()
                    else f"did not respond within {self.response_timeout_seconds:g}s"
                )
                raise TimeoutError(f"environment worker {index} {state}")
            try:
                value = connection.recv()
            except EOFError as error:
                process = self._processes[index]
                raise RuntimeError(
                    f"environment worker {index} closed its pipe "
                    f"(exit code {process.exitcode})"
                ) from error
            if isinstance(value, BaseException):
                raise RuntimeError(f"environment worker {index} failed") from value
            values.append(value)
        return values

    def reset(self, seeds: Sequence[int]) -> list[Decision]:
        if len(seeds) != self.size:
            raise ValueError("one reset seed is required per worker")
        indices = tuple(range(self.size))
        for index, seed in enumerate(seeds):
            self._connections[index].send(("reset", int(seed)))
        return self._collect(indices)

    def reset_one(self, index: int, seed: int) -> Decision:
        self._connections[index].send(("reset", int(seed)))
        return self._collect((index,))[0]

    def step(self, candidate_ids: Sequence[str]) -> list[Transition]:
        if len(candidate_ids) != self.size:
            raise ValueError("one action is required per worker")
        indices = tuple(range(self.size))
        for index, candidate_id in enumerate(candidate_ids):
            self._connections[index].send(("step", candidate_id))
        return self._collect(indices)

    def checkpoints(self) -> list[Mapping[str, Any]]:
        indices = tuple(range(self.size))
        for connection in self._connections:
            connection.send(("checkpoint", None))
        return self._collect(indices)

    def load_checkpoints(self, states: Sequence[Mapping[str, Any]]) -> list[Decision]:
        if len(states) != self.size:
            raise ValueError("checkpoint count does not match worker count")
        indices = tuple(range(self.size))
        for connection, state in zip(self._connections, states):
            connection.send(("load", dict(state)))
        return self._collect(indices)

    def close(self) -> None:
        for connection in getattr(self, "_connections", ()):
            try:
                connection.send(("close", None))
            except (BrokenPipeError, EOFError):
                pass
        for process in getattr(self, "_processes", ()):
            process.join(timeout=5)
            if process.is_alive():
                process.terminate()
                process.join(timeout=5)
        for connection in getattr(self, "_connections", ()):
            connection.close()

    def __enter__(self) -> "WorkerPool":
        return self

    def __exit__(self, *_: object) -> None:
        self.close()
