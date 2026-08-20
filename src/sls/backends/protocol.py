"""Backend interface implemented by the original game and native simulator."""

from __future__ import annotations

from typing import Any, Mapping, Protocol

from sls.contracts import Action, Decision, Transition, ValidationSnapshot


class Backend(Protocol):
    def reset(self, seed: int) -> Decision: ...
    def step(self, action: Action | str) -> Transition: ...
    def validation_snapshot(self) -> ValidationSnapshot: ...


class CheckpointableBackend(Backend, Protocol):
    def checkpoint(self) -> Mapping[str, Any]: ...
    def load_checkpoint(self, state: Mapping[str, Any]) -> Decision: ...
