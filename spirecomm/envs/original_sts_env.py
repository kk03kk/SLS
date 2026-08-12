"""Gymnasium environment backed by the original game via CommunicationMod."""

from __future__ import annotations

import json
import sys
from pathlib import Path
from typing import Any, Protocol, TextIO

from spirecomm.envs.base_sts_env import BaseSTSEnv
from spirecomm.envs.codec import (
    generate_legal_actions,
    is_combat_payload,
    parse_battle_observation,
    rich_battle_state,
)
from spirecomm.envs.contracts import LegalAction
from spirecomm.spire.game import Game


class Transport(Protocol):
    def send(self, command: str) -> None: ...
    def receive(self) -> dict[str, Any]: ...


class StdioTransport:
    """Newline-delimited CommunicationMod transport.

    stdout is protocol-only. Optional diagnostics are written to a file.
    """

    def __init__(
        self,
        stdin: TextIO = sys.stdin,
        stdout: TextIO = sys.stdout,
        log_path: Path | None = None,
    ) -> None:
        self.stdin = stdin
        self.stdout = stdout
        self.log_path = log_path

    def _log(self, direction: str, data: Any) -> None:
        if self.log_path is None:
            return
        self.log_path.parent.mkdir(parents=True, exist_ok=True)
        with self.log_path.open("a", encoding="utf-8") as stream:
            stream.write(
                # CommunicationMod can expose Java strings containing lone UTF-16
                # surrogates when localized text is decoded incorrectly. Escaping
                # non-ASCII keeps the diagnostic JSONL lossless and prevents those
                # optional logs from killing the protocol process.
                json.dumps({"direction": direction, "data": data}, ensure_ascii=True) + "\n"
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
            self._log("rx", payload)
            return payload


class OriginalSTSEnv(BaseSTSEnv):
    """Controls one original-game battle and terminates when combat ends."""

    def __init__(self, transport: Transport | None = None, player_class: str = "IRONCLAD") -> None:
        super().__init__()
        self.transport = transport or StdioTransport()
        self.player_class = player_class.upper()
        self.game: Game | None = None
        self._ready_sent = False

    @staticmethod
    def _is_combat(payload: dict[str, Any]) -> bool:
        return is_combat_payload(payload)

    def _receive_ready_state(self) -> dict[str, Any]:
        while True:
            payload = self.transport.receive()
            if payload.get("error"):
                raise RuntimeError(f"CommunicationMod rejected a command: {payload['error']}")
            if payload.get("ready_for_command", False):
                return payload

    def _send_validated(self, command: str, payload: dict[str, Any]) -> None:
        base_command = command.split(maxsplit=1)[0].lower()
        advertised = {str(item).lower() for item in payload.get("available_commands", [])}
        if not payload.get("ready_for_command", False):
            raise RuntimeError("Refusing to send while the game is not ready")
        if base_command not in advertised:
            raise RuntimeError(
                f"Refusing unadvertised command {command!r}; available={sorted(advertised)}"
            )
        self.transport.send(command)

    def _parse(self, payload: dict[str, Any]) -> None:
        game_state = payload.get("game_state") or {}
        if isinstance(game_state.get("_rng"), dict):
            payload["_rng"] = game_state["_rng"]
        self._accept_payload(payload)
        if payload.get("in_game") and payload.get("game_state"):
            self.game = Game.from_json(
                payload["game_state"], payload.get("available_commands", [])
            )
        else:
            self.game = None

    def _autopilot_to_combat(self, payload: dict[str, Any]) -> dict[str, Any]:
        while not self._is_combat(payload):
            available = {str(item).lower() for item in payload.get("available_commands", [])}
            if not payload.get("in_game") and "start" in available:
                command = f"start {self.player_class} 0"
            else:
                candidates = generate_legal_actions(payload)
                preferred = [
                    action for action in candidates if action.kind in {"choose", "proceed"}
                ]
                if not preferred:
                    raise RuntimeError(
                        f"Cannot reach combat safely; available={sorted(available)}, "
                        f"screen={(payload.get('game_state') or {}).get('screen_type')}"
                    )
                command = preferred[0].command
            assert command is not None
            self._send_validated(command, payload)
            payload = self._receive_ready_state()
        return payload

    def reset(self, *, seed: int | None = None, options: dict[str, Any] | None = None):
        super().reset(seed=seed)
        if not self._ready_sent:
            self.transport.send("ready")
            self._ready_sent = True
        payload = self._autopilot_to_combat(self._receive_ready_state())
        self._parse(payload)
        if not self.legal_actions:
            raise RuntimeError("Combat state contains no safe legal actions")
        self._begin_reward_tracking()
        return self._observation(), self._info()

    def step(self, action: int):
        selected = self._validate_action_index(action)
        assert self.payload is not None
        if selected.command is None:
            raise RuntimeError(f"Original backend action has no command: {selected}")
        self._send_validated(selected.command, self.payload)
        payload = self._receive_ready_state()
        terminated = not self._is_combat(payload)

        self._parse(payload)
        if not terminated:
            if not self.legal_actions:
                raise RuntimeError("Combat state contains no safe legal actions")
            reward = self._combat_reward()
        else:
            self.game = None
            self.legal_actions = []
            reward = 0.0

        return self._observation(), reward, terminated, False, self._info()


__all__ = [
    "LegalAction",
    "OriginalSTSEnv",
    "StdioTransport",
    "Transport",
    "generate_legal_actions",
    "parse_battle_observation",
    "rich_battle_state",
]
