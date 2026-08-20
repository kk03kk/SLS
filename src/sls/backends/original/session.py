"""Validated command session for Original-game differential testing."""

from __future__ import annotations

from typing import Any

from sls.backends.original.transport import StdioTransport, Transport
from sls.contracts import ValidationSnapshot


class OriginalSession:
    """Low-level validation session; policy code must not depend on this API."""

    def __init__(self, transport: Transport | None = None) -> None:
        self.transport = transport or StdioTransport()
        self.payload: dict[str, Any] | None = None

    def connect(self) -> dict[str, Any]:
        self.transport.send("ready")
        self.payload = self.receive_ready()
        return self.payload

    def receive_ready(self) -> dict[str, Any]:
        for _ in range(20_000):
            payload = self.transport.receive()
            if payload.get("error"):
                raise RuntimeError(f"CommunicationMod error: {payload['error']}")
            if payload.get("ready_for_command"):
                self.payload = payload
                return payload
        raise RuntimeError("Original game did not reach a command boundary")

    def execute(self, command: str) -> dict[str, Any]:
        if self.payload is None:
            raise RuntimeError("connect must be called before execute")
        base = command.split(maxsplit=1)[0].lower()
        available = {
            str(item).lower() for item in self.payload.get("available_commands") or ()
        }
        if base not in available:
            raise RuntimeError(
                f"unadvertised Original command {command!r}; available={sorted(available)}"
            )
        self.transport.send(command)
        return self.receive_ready()

    def validation_snapshot(self) -> ValidationSnapshot:
        if self.payload is None:
            raise RuntimeError("connect must be called before validation_snapshot")
        game = self.payload.get("game_state") or {}
        return ValidationSnapshot(
            public_state=game,
            rng_streams=self.payload.get("_rng") or game.get("_rng") or {},
        )
