"""Spawned native environments with centralized model inference."""

from __future__ import annotations

import json
import multiprocessing as mp
import os
import time
from dataclasses import dataclass, field
from multiprocessing.connection import Connection
from pathlib import Path
from typing import Any, Mapping, Sequence

from sls.contracts import Decision, Transition
from sls.curriculum import CurriculumProfile

CRASH_DUMP_SCHEMA = "sls-simulator-crash-v1"


def _semantic_candidates(backend: Any) -> list[Mapping[str, Any] | str]:
    candidates = []
    for candidate_id in getattr(backend, "_candidate_bits", {}):
        try:
            value = json.loads(candidate_id)
        except (TypeError, json.JSONDecodeError):
            value = str(candidate_id)
        candidates.append(value)
    return candidates


def _option_groups(raw: Mapping[str, Any]) -> dict[str, Any]:
    public_screen = raw.get("public_screen") or {}
    public_combat = raw.get("public_combat") or {}
    groups = {
        str(key): value for key, value in public_screen.items()
        if value not in (None, False, 0, "", [], {})
    }
    choice = public_combat.get("choice")
    if choice:
        groups["combat_choice"] = choice
    return groups


def _action_groups(raw: Mapping[str, Any]) -> dict[str, list[Mapping[str, Any]]]:
    groups: dict[str, list[Mapping[str, Any]]] = {}
    for action in raw.get("legal_actions") or ():
        if action.get("domain") == "COMBAT":
            key = f"COMBAT:{int(action.get('action_type', -1))}"
        elif action.get("potion"):
            key = "RUN:POTION"
        else:
            key = f"RUN:REWARD:{int(action.get('reward_type', -1))}"
        groups.setdefault(key, []).append(dict(action))
    return dict(sorted(groups.items()))


def _crash_payload(
    backend: Any,
    *,
    error: BaseException,
    worker_index: int,
    episode_ordinal: int,
    seed: int | None,
    last_semantic_action: str | None,
    profile: CurriculumProfile,
) -> dict[str, Any]:
    try:
        raw = dict(backend.raw_state)
    except (RuntimeError, AttributeError):
        raw = {}
    public_run = raw.get("public_run") or {}
    if seed is None:
        run_state = raw.get("run_state") or {}
        raw_seed = run_state.get("seed")
        seed = int(raw_seed) if raw_seed is not None else None
    try:
        last_action: Mapping[str, Any] | str | None = (
            json.loads(last_semantic_action) if last_semantic_action else None
        )
    except json.JSONDecodeError:
        last_action = last_semantic_action
    return {
        "schema": CRASH_DUMP_SCHEMA,
        "error": {"type": type(error).__name__, "message": str(error)},
        "worker_index": worker_index,
        "worker_episode_ordinal": episode_ordinal,
        "seed": seed,
        "profile": profile.profile_id,
        "last_semantic_action": last_action,
        "screen": {
            "screen_state": public_run.get("screen_state"),
            "event_id": public_run.get("current_event_id"),
        },
        "terminal_flag": bool(public_run and int(public_run.get("outcome", 1)) != 1),
        "raw_backend_state": raw,
        "public_run_state": public_run,
        "public_combat": raw.get("public_combat"),
        "combat_checkpoint": raw.get("combat_checkpoint"),
        "rng_checkpoint": (
            (raw.get("combat_checkpoint") or {}).get("rng")
            if raw.get("public_combat") else raw.get("rng")
        ),
        "inventory": raw.get("public_inventory"),
        "available_option_groups": _option_groups(raw),
        "raw_legal_action_groups": _action_groups(raw),
        "generated_actions": _semantic_candidates(backend),
    }


def _write_crash_dump(directory: Path, payload: Mapping[str, Any]) -> Path:
    directory.mkdir(parents=True, exist_ok=True)
    seed = "unknown" if payload.get("seed") is None else str(payload["seed"])
    stem = (
        f"invalid-decision-worker-{int(payload['worker_index']):02d}"
        f"-episode-{int(payload['worker_episode_ordinal']):08d}-seed-{seed}"
    )
    target = directory / f"{stem}.json"
    suffix = 1
    while target.exists():
        target = directory / f"{stem}-{suffix}.json"
        suffix += 1
    temporary = target.with_suffix(target.suffix + f".{os.getpid()}.tmp")
    temporary.write_text(
        json.dumps(payload, ensure_ascii=False, indent=2, sort_keys=True) + "\n",
        encoding="utf-8",
    )
    os.replace(temporary, target)
    return target


def _worker(
    connection: Connection,
    profile: CurriculumProfile,
    worker_index: int,
    crash_dump_dir: str | None,
) -> None:
    from sls.backends.simulator import SimulatorBackend

    backend = SimulatorBackend(profile)
    episode_ordinal = 0
    seed: int | None = None
    last_semantic_action: str | None = None
    try:
        while True:
            command, payload = connection.recv()
            if command == "reset":
                episode_ordinal += 1
                seed = int(payload)
                last_semantic_action = None
                connection.send(backend.reset(seed))
            elif command == "step":
                last_semantic_action = str(payload)
                connection.send(backend.step(last_semantic_action))
            elif command == "checkpoint":
                connection.send(backend.checkpoint())
            elif command == "load":
                episode_ordinal += 1
                run_state = payload.get("run_state") or {}
                raw_seed = run_state.get("seed")
                seed = int(raw_seed) if raw_seed is not None else None
                last_semantic_action = None
                connection.send(backend.load_checkpoint(payload))
            elif command == "close":
                break
            else:
                raise RuntimeError(f"unknown worker command: {command}")
    except BaseException as error:
        dump_path = None
        if crash_dump_dir is not None:
            try:
                dump_path = _write_crash_dump(
                    Path(crash_dump_dir),
                    _crash_payload(
                        backend,
                        error=error,
                        worker_index=worker_index,
                        episode_ordinal=episode_ordinal,
                        seed=seed,
                        last_semantic_action=last_semantic_action,
                        profile=profile,
                    ),
                )
            except BaseException:
                # Diagnostics must never replace or suppress the simulator failure.
                dump_path = None
        reported = RuntimeError(
            f"{type(error).__name__}: {error}"
            + (f"; crash dump: {dump_path}" if dump_path is not None else "")
        )
        try:
            connection.send(reported)
        except BaseException:
            pass
        raise
    finally:
        connection.close()


def _vector_worker(
    connection: Connection,
    profile: CurriculumProfile,
    size: int,
    worker_start: int,
    crash_dump_dir: str | None,
) -> None:
    from sls.backends.simulator import SimulatorBackend

    backends = [SimulatorBackend(profile) for _ in range(size)]
    episode_ordinals = [0] * size
    seeds: list[int | None] = [None] * size
    last_actions: list[str | None] = [None] * size

    def execute(index: int, command: str, payload: Any) -> Any:
        backend = backends[index]
        try:
            if command == "reset":
                episode_ordinals[index] += 1
                seeds[index] = int(payload)
                last_actions[index] = None
                return backend.reset(int(payload))
            if command == "step":
                last_actions[index] = str(payload)
                return backend.step(str(payload))
            if command == "checkpoint":
                return backend.checkpoint()
            if command == "load":
                episode_ordinals[index] += 1
                run_state = payload.get("run_state") or {}
                raw_seed = run_state.get("seed")
                seeds[index] = int(raw_seed) if raw_seed is not None else None
                last_actions[index] = None
                return backend.load_checkpoint(dict(payload))
            raise RuntimeError(f"unknown vector environment command: {command}")
        except BaseException as error:
            dump_path = None
            if crash_dump_dir is not None:
                try:
                    dump_path = _write_crash_dump(
                        Path(crash_dump_dir),
                        _crash_payload(
                            backend, error=error,
                            worker_index=worker_start + index,
                            episode_ordinal=episode_ordinals[index],
                            seed=seeds[index],
                            last_semantic_action=last_actions[index],
                            profile=profile,
                        ),
                    )
                except BaseException:
                    dump_path = None
            raise RuntimeError(
                f"{type(error).__name__}: {error}"
                + (f"; crash dump: {dump_path}" if dump_path is not None else "")
            ) from error

    try:
        while True:
            command, payload = connection.recv()
            if command == "reset":
                connection.send([
                    execute(index, "reset", seed)
                    for index, seed in enumerate(payload)
                ])
            elif command == "reset_one":
                index, seed = payload
                connection.send(execute(int(index), "reset", seed))
            elif command == "step":
                connection.send([
                    execute(index, "step", candidate_id)
                    for index, candidate_id in enumerate(payload)
                ])
            elif command == "checkpoint":
                connection.send([
                    execute(index, "checkpoint", None) for index in range(size)
                ])
            elif command == "load":
                connection.send([
                    execute(index, "load", state)
                    for index, state in enumerate(payload)
                ])
            elif command == "load_one":
                index, state = payload
                connection.send(execute(int(index), "load", state))
            elif command == "close":
                break
            else:
                raise RuntimeError(f"unknown vector worker command: {command}")
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
    crash_dump_dir: str | Path | None = None
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
                args=(
                    child, self.profile, index,
                    str(Path(self.crash_dump_dir).resolve())
                    if self.crash_dump_dir is not None else None,
                ),
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
                raise RuntimeError(f"environment worker {index} failed: {value}") from value
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

    def load_one(self, index: int, state: Mapping[str, Any]) -> Decision:
        self._connections[index].send(("load", dict(state)))
        return self._collect((index,))[0]

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


@dataclass(slots=True)
class VectorWorkerPool:
    """Host several native environments in the trainer process.

    Native simulation is implemented in C++, so this removes the dominant
    per-action cost of pickling a full public Decision through a process pipe.
    ``WorkerPool`` remains available as an isolation-oriented fallback.
    """

    profile: CurriculumProfile
    size: int
    crash_dump_dir: str | Path | None = None
    _backends: list[Any] = field(init=False, repr=False)
    _episode_ordinals: list[int] = field(init=False, repr=False)
    _seeds: list[int | None] = field(init=False, repr=False)
    _last_actions: list[str | None] = field(init=False, repr=False)

    def __post_init__(self) -> None:
        if self.size <= 0:
            raise ValueError("worker count must be positive")
        from sls.backends.simulator import SimulatorBackend

        self._backends = [SimulatorBackend(self.profile) for _ in range(self.size)]
        self._episode_ordinals = [0] * self.size
        self._seeds = [None] * self.size
        self._last_actions = [None] * self.size

    def _execute(self, index: int, command: str, payload: Any) -> Any:
        backend = self._backends[index]
        try:
            if command == "reset":
                self._episode_ordinals[index] += 1
                self._seeds[index] = int(payload)
                self._last_actions[index] = None
                return backend.reset(int(payload))
            if command == "step":
                self._last_actions[index] = str(payload)
                return backend.step(str(payload))
            if command == "checkpoint":
                return backend.checkpoint()
            if command == "load":
                self._episode_ordinals[index] += 1
                run_state = payload.get("run_state") or {}
                raw_seed = run_state.get("seed")
                self._seeds[index] = int(raw_seed) if raw_seed is not None else None
                self._last_actions[index] = None
                return backend.load_checkpoint(dict(payload))
            raise RuntimeError(f"unknown vector environment command: {command}")
        except BaseException as error:
            if self.crash_dump_dir is not None:
                _write_crash_dump(
                    Path(self.crash_dump_dir),
                    _crash_payload(
                        backend, error=error, worker_index=index,
                        episode_ordinal=self._episode_ordinals[index],
                        seed=self._seeds[index],
                        last_semantic_action=self._last_actions[index],
                        profile=self.profile,
                    ),
                )
            raise

    def reset(self, seeds: Sequence[int]) -> list[Decision]:
        if len(seeds) != self.size:
            raise ValueError("one reset seed is required per environment")
        return [self._execute(index, "reset", seed) for index, seed in enumerate(seeds)]

    def reset_one(self, index: int, seed: int) -> Decision:
        return self._execute(index, "reset", seed)

    def step(self, candidate_ids: Sequence[str]) -> list[Transition]:
        if len(candidate_ids) != self.size:
            raise ValueError("one action is required per environment")
        return [
            self._execute(index, "step", candidate_id)
            for index, candidate_id in enumerate(candidate_ids)
        ]

    def checkpoints(self) -> list[Mapping[str, Any]]:
        return [self._execute(index, "checkpoint", None) for index in range(self.size)]

    def load_checkpoints(self, states: Sequence[Mapping[str, Any]]) -> list[Decision]:
        if len(states) != self.size:
            raise ValueError("checkpoint count does not match environment count")
        return [
            self._execute(index, "load", state)
            for index, state in enumerate(states)
        ]

    def load_one(self, index: int, state: Mapping[str, Any]) -> Decision:
        return self._execute(index, "load", state)

    def close(self) -> None:
        self._backends.clear()

    def __enter__(self) -> "VectorWorkerPool":
        return self

    def __exit__(self, *_: object) -> None:
        self.close()


@dataclass(slots=True)
class ShardedWorkerPool:
    """Parallel shards where each process hosts multiple native environments."""

    profile: CurriculumProfile
    size: int
    shard_count: int = 8
    response_timeout_seconds: float = 120.0
    crash_dump_dir: str | Path | None = None
    _connections: list[Connection] = field(init=False, repr=False)
    _processes: list[mp.Process] = field(init=False, repr=False)
    _ranges: list[tuple[int, int]] = field(init=False, repr=False)

    def __post_init__(self) -> None:
        if self.size <= 0 or self.shard_count <= 0:
            raise ValueError("environment and shard counts must be positive")
        self.shard_count = min(self.size, self.shard_count)
        base, extra = divmod(self.size, self.shard_count)
        context = mp.get_context("spawn")
        self._connections, self._processes, self._ranges = [], [], []
        start = 0
        for shard in range(self.shard_count):
            count = base + int(shard < extra)
            parent, child = context.Pipe()
            process = context.Process(
                target=_vector_worker,
                args=(
                    child, self.profile, count, start,
                    str(Path(self.crash_dump_dir).resolve())
                    if self.crash_dump_dir is not None else None,
                ),
                name=f"sls-vector-shard-{shard}",
            )
            process.start()
            child.close()
            self._connections.append(parent)
            self._processes.append(process)
            self._ranges.append((start, start + count))
            start += count

    def _receive(self, shard: int) -> Any:
        connection = self._connections[shard]
        if not connection.poll(self.response_timeout_seconds):
            raise TimeoutError(f"environment shard {shard} did not respond")
        value = connection.recv()
        if isinstance(value, BaseException):
            raise RuntimeError(f"environment shard {shard} failed") from value
        return value

    def _all(self, command: str, values: Sequence[Any] | None = None) -> list[Any]:
        for shard, (start, end) in enumerate(self._ranges):
            payload = None if values is None else list(values[start:end])
            self._connections[shard].send((command, payload))
        result: list[Any] = []
        for shard in range(self.shard_count):
            result.extend(self._receive(shard))
        return result

    def _locate(self, index: int) -> tuple[int, int]:
        if not 0 <= index < self.size:
            raise IndexError(index)
        for shard, (start, end) in enumerate(self._ranges):
            if start <= index < end:
                return shard, index - start
        raise AssertionError("environment shard index was not assigned")

    def reset(self, seeds: Sequence[int]) -> list[Decision]:
        if len(seeds) != self.size:
            raise ValueError("one reset seed is required per environment")
        return self._all("reset", seeds)

    def reset_one(self, index: int, seed: int) -> Decision:
        shard, local = self._locate(index)
        self._connections[shard].send(("reset_one", (local, int(seed))))
        return self._receive(shard)

    def step(self, candidate_ids: Sequence[str]) -> list[Transition]:
        if len(candidate_ids) != self.size:
            raise ValueError("one action is required per environment")
        return self._all("step", candidate_ids)

    def checkpoints(self) -> list[Mapping[str, Any]]:
        return self._all("checkpoint")

    def load_checkpoints(self, states: Sequence[Mapping[str, Any]]) -> list[Decision]:
        if len(states) != self.size:
            raise ValueError("checkpoint count does not match environment count")
        return self._all("load", states)

    def load_one(self, index: int, state: Mapping[str, Any]) -> Decision:
        shard, local = self._locate(index)
        self._connections[shard].send(("load_one", (local, dict(state))))
        return self._receive(shard)

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

    def __enter__(self) -> "ShardedWorkerPool":
        return self

    def __exit__(self, *_: object) -> None:
        self.close()
