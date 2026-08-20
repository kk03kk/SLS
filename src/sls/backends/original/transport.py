"""Newline-delimited CommunicationMod transport."""

from __future__ import annotations

import json
import sys
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
    ) -> None:
        self.stdin = stdin
        self.stdout = stdout
        self.log_path = log_path
        self.event_sink = event_sink

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
        while True:
            line = self.stdin.readline()
            if line == "":
                raise EOFError("CommunicationMod closed stdin")
            line = line.strip()
            if not line:
                continue
            payload = json.loads(line)
            if not isinstance(payload, dict):
                raise ValueError("CommunicationMod message must be a JSON object")
            self._log("rx", payload)
            return payload
