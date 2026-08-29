"""Production attachment to an already-running Original-game run."""

from __future__ import annotations

import time

from sls.backends.original.adapter import adapt_original
from sls.backends.original.environment import OriginalBackend
from sls.backends.original.session import OriginalSession
from sls.content.scope import UnsupportedContentPolicy
from sls.contracts import Decision
from sls.curriculum import IRONCLAD_A0_HEART, ironclad_fullrun_profile


class LiveGameBackend(OriginalBackend):
    """Attach without resetting, starting, or resuming the user's run.

    The backend consumes only the canonical public observation and legal action
    contract.  Oracle RNG/continuation fields may be present on the wire for
    diagnostics, but are never exposed through :class:`Decision`.
    """

    def __init__(
        self,
        session: OriginalSession | None = None,
        *,
        content_policy: UnsupportedContentPolicy | None = None,
        wait_for_neow: bool = False,
        wait_timeout_seconds: float = 600.0,
    ) -> None:
        if wait_timeout_seconds <= 0:
            raise ValueError("wait timeout must be positive")
        super().__init__(session=session, profile=IRONCLAD_A0_HEART)
        self.content_policy = content_policy or UnsupportedContentPolicy.ironclad()
        self.require_heart = True
        self.wait_for_neow = wait_for_neow
        self.wait_timeout_seconds = wait_timeout_seconds

    def configure_goal(self, goal: str) -> None:
        if goal not in {"FULLRUN", "HEART"}:
            raise ValueError("live backend requires a FullRun or Heart artifact")
        self.require_heart = goal == "HEART"

    def attach(self) -> Decision:
        payload = self.session.payload or self.session.connect()
        deadline = time.monotonic() + self.wait_timeout_seconds
        while not bool(payload.get("in_game")):
            if not self.wait_for_neow:
                raise RuntimeError("start or continue an Ironclad run before attaching the agent")
            if time.monotonic() >= deadline:
                raise TimeoutError("timed out waiting for a fresh Neow boundary")
            available = {
                str(command).lower()
                for command in payload.get("available_commands") or ()
            }
            if "state" not in available:
                raise RuntimeError("Original main menu does not advertise state polling")
            time.sleep(0.25)
            payload = self.session.execute("state")
        if self.wait_for_neow:
            payload = self._fold_initial_neow_dialog(payload)
        adapted = adapt_original(payload)
        observation = adapted.decision.observation
        if observation.player.character_id != "IRONCLAD":
            raise ValueError(
                f"live agent supports IRONCLAD only, got {observation.player.character_id}"
            )
        ascension = observation.run.ascension
        if not 0 <= ascension <= 20:
            raise ValueError(f"invalid Ironclad ascension: {ascension}")
        self.content_policy.validate_observation(observation)
        self.profile = ironclad_fullrun_profile(
            ascension, require_heart=self.require_heart,
        )
        self._adapted = adapted
        self._last_executed_commands = ()
        self._last_validation_evidence = {}
        return adapted.decision

    def step(self, action):  # type: ignore[no-untyped-def]
        transition = super().step(action)
        self.content_policy.validate_observation(transition.decision.observation)
        return transition
