"""Production attachment to an already-running Original-game run."""

from __future__ import annotations

import time

from sls.backends.original.adapter import adapt_original
from sls.backends.original.environment import OriginalBackend
from sls.backends.original.session import OriginalSession
from sls.content.scope import UnsupportedContentPolicy
from sls.contracts import Decision
from sls.curriculum import (
    CURRICULUM_PROFILES_BY_ID,
    IRONCLAD_A0_HEART,
    ironclad_fullrun_profile,
)


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
        allow_curriculum_goals: bool = False,
    ) -> None:
        if wait_timeout_seconds <= 0:
            raise ValueError("wait timeout must be positive")
        super().__init__(session=session, profile=IRONCLAD_A0_HEART)
        self.content_policy = content_policy or UnsupportedContentPolicy.ironclad()
        self.require_heart = True
        self.wait_for_neow = wait_for_neow
        self.wait_timeout_seconds = wait_timeout_seconds
        self.allow_curriculum_goals = allow_curriculum_goals
        self._curriculum_profile = None

    def configure_goal(self, goal: str) -> None:
        if goal in {"ACT1", "ACT2", "ACT3"}:
            if not self.allow_curriculum_goals:
                raise ValueError("live backend requires a FullRun or Heart artifact")
            self._curriculum_profile = CURRICULUM_PROFILES_BY_ID[f"IRONCLAD_A0_{goal}"]
            self.require_heart = False
            return
        if goal not in {"FULLRUN", "HEART"}:
            raise ValueError("live backend requires a FullRun or Heart artifact")
        self._curriculum_profile = None
        self.require_heart = goal == "HEART"

    def attach(self) -> Decision:
        payload = self.session.payload or self.session.connect()
        deadline = time.monotonic() + self.wait_timeout_seconds
        while not bool(payload.get("in_game")):
            if not self.wait_for_neow:
                raise RuntimeError("start or continue an Ironclad run before attaching the agent")
            if time.monotonic() >= deadline:
                raise TimeoutError("timed out waiting for a fresh Neow boundary")
            # CommunicationMod publishes menu/dungeon state changes without a
            # command. Polling ``state`` races AbstractDungeon initialization:
            # during the few frames after Embark, the dungeon exists but its
            # current room does not. Wait passively for the next published
            # stable boundary instead of probing that unsafe transition.
            try:
                payload = self.session.receive_ready()
            except TimeoutError:
                continue
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
        if self._curriculum_profile is not None:
            if ascension != self._curriculum_profile.ascension:
                raise ValueError(
                    f"{self._curriculum_profile.profile_id} requires ascension "
                    f"{self._curriculum_profile.ascension}, got {ascension}"
                )
            self.profile = self._curriculum_profile
        else:
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
