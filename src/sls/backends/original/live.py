"""Production attachment to an already-running Original-game run."""

from __future__ import annotations

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
    ) -> None:
        super().__init__(session=session, profile=IRONCLAD_A0_HEART)
        self.content_policy = content_policy or UnsupportedContentPolicy.ironclad()
        self.require_heart = True

    def configure_goal(self, goal: str) -> None:
        if goal not in {"FULLRUN", "HEART"}:
            raise ValueError("live backend requires a FullRun or Heart artifact")
        self.require_heart = goal == "HEART"

    def attach(self) -> Decision:
        payload = self.session.payload or self.session.connect()
        if not bool(payload.get("in_game")):
            raise RuntimeError("start or continue an Ironclad run before attaching the agent")
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
