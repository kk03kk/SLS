"""Newline-delimited CommunicationMod transport."""

from __future__ import annotations

import json
import os
import queue
import sys
import threading
from pathlib import Path
from typing import Any, Callable, Protocol, TextIO


class Transport(Protocol):
    def send(self, command: str) -> None: ...
    def receive(self) -> dict[str, Any]: ...


class StdioTransport:
    """Protocol-only stdout transport with optional JSONL diagnostics."""

    def __init__(
        self,
        stdin: TextIO = sys.stdin,
        stdout: TextIO = sys.stdout,
        log_path: Path | None = None,
        event_sink: Callable[[str, Any], None] | None = None,
        read_timeout_seconds: float | None = None,
    ) -> None:
        self.stdin = stdin
        self.stdout = stdout
        self.log_path = log_path
        self.event_sink = event_sink
        self.read_timeout_seconds = (
            float(os.environ.get("SLS_ORIGINAL_COMMAND_TIMEOUT", "60"))
            if read_timeout_seconds is None else read_timeout_seconds
        )
        if self.read_timeout_seconds <= 0:
            raise ValueError("Original command timeout must be positive")
        self._lines: queue.Queue[str | BaseException] = queue.Queue()
        self._reader_started = False

    def _start_reader(self) -> None:
        if self._reader_started:
            return
        self._reader_started = True

        def read_lines() -> None:
            try:
                while True:
                    line = self.stdin.readline()
                    if line == "":
                        self._lines.put(EOFError("CommunicationMod closed stdin"))
                        return
                    self._lines.put(line)
            except BaseException as error:
                self._lines.put(error)

        threading.Thread(
            target=read_lines, name="sls-original-stdin", daemon=True,
        ).start()

    def _log(self, direction: str, data: Any) -> None:
        if self.event_sink is not None:
            self.event_sink(direction, data)
        if self.log_path is None:
            return
        self.log_path.parent.mkdir(parents=True, exist_ok=True)
        with self.log_path.open("a", encoding="utf-8") as stream:
            stream.write(
                json.dumps(
                    {"direction": direction, "data": data},
                    ensure_ascii=True,
                )
                + "\n"
            )

    def send(self, command: str) -> None:
        self.stdout.write(command + "\n")
        self.stdout.flush()
        self._log("tx", command)

    def receive(self) -> dict[str, Any]:
        self._start_reader()
        while True:
            try:
                line = self._lines.get(timeout=self.read_timeout_seconds)
            except queue.Empty as error:
                raise TimeoutError(
                    "Original game did not return a command boundary within "
                    f"{self.read_timeout_seconds:g}s"
                ) from error
            if isinstance(line, BaseException):
                raise line
            line = line.strip()
            if not line:
                continue
            payload = json.loads(line)
            if not isinstance(payload, dict):
                raise ValueError("CommunicationMod message must be a JSON object")
            self._log("rx", payload)
            return payload
