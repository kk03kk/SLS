"""Canonical backend driven by Original Game plus CommunicationMod."""

from __future__ import annotations

from typing import Any

from sls.backends.original.adapter import AdaptedOriginalDecision, adapt_original
from sls.backends.original.session import OriginalSession
from sls.content.seed import long_to_seed_string
from sls.contracts import Action, ActionKind, Decision, Transition, ValidationSnapshot
from sls.curriculum import CurriculumProfile, IRONCLAD_A0_HEART, evaluate_horizon


class OriginalBackend:
    """Original-game backend used for validation, never high-throughput training."""

    def __init__(
        self,
        session: OriginalSession | None = None,
        profile: CurriculumProfile = IRONCLAD_A0_HEART,
    ) -> None:
        if profile.character_id != "IRONCLAD":
            raise ValueError("the canonical baseline currently supports IRONCLAD only")
        self.session = session or OriginalSession()
        self.profile = profile
        self._adapted: AdaptedOriginalDecision | None = None
        self._last_executed_commands: tuple[str, ...] = ()

    @property
    def raw_payload(self) -> dict[str, Any]:
        if self.session.payload is None:
            raise RuntimeError("backend has not been reset")
        return self.session.payload

    def reset(self, seed: int) -> Decision:
        payload = self.session.payload or self.session.connect()
        if payload.get("in_game"):
            available = {str(item).lower() for item in payload.get("available_commands") or ()}
            if "reset_run" not in available:
                raise RuntimeError("Oracle Mod reset_run command is unavailable")
            payload = self.session.execute("reset_run")
        if payload.get("in_game"):
            raise RuntimeError("Original game did not return to the main menu")
        available = {str(item).lower() for item in payload.get("available_commands") or ()}
        if "start" not in available:
            raise RuntimeError(f"Original main menu does not advertise start: {sorted(available)}")
        payload = self.session.execute(
            f"start {self.profile.character_id} {self.profile.ascension} "
            f"{long_to_seed_string(int(seed))}"
        )
        payload = self._fold_initial_neow_dialog(payload)
        self._adapted = adapt_original(payload)
        return self._adapted.decision

    def resume(self) -> Decision:
        """Resume an official autosave through the Oracle's stock-menu trigger."""

        payload = self.session.payload or self.session.connect()
        if payload.get("in_game"):
            available = {str(item).lower() for item in payload.get("available_commands") or ()}
            if "reset_run" not in available:
                raise RuntimeError("cannot resume while Original is in a run")
            payload = self.session.execute("reset_run")
        available = {str(item).lower() for item in payload.get("available_commands") or ()}
        if "parity_continue" not in available:
            raise RuntimeError(f"parity_continue is unavailable: {sorted(available)}")
        payload = self.session.execute("parity_continue")
        payload = self._fold_protocol_only_boundaries(
            payload, fold_single_event=False,
        )
        payload = self._settle_debug_intents(payload)
        self._adapted = adapt_original(payload)
        return self._adapted.decision

    def return_to_menu(self) -> None:
        """End the validation run before protected user files are restored."""

        payload = self.session.payload
        if not payload or not payload.get("in_game"):
            return
        available = {str(item).lower() for item in payload.get("available_commands") or ()}
        if "reset_run" not in available:
            raise RuntimeError("validation run cannot return to the main menu")
        self.session.execute("reset_run")
        self._adapted = None

    def step(self, action: Action | str) -> Transition:
        if self._adapted is None:
            raise RuntimeError("reset must be called before step")
        candidate_id = action if isinstance(action, str) else action.candidate_id
        try:
            commands = self._adapted.commands[candidate_id]
        except KeyError as error:
            raise ValueError("action is not legal at the current Original decision") from error
        payload = self.raw_payload
        executed: list[str] = []
        for index, command in enumerate(commands):
            payload = self.session.execute(command)
            executed.append(command)
            if index + 1 < len(commands):
                payload = self._settle_reward_intermediate(payload, executed)
        if not isinstance(action, str) and action.kind in {
            ActionKind.CHOOSE_CARD_REWARD,
            ActionKind.SKIP_CARD_REWARD,
            ActionKind.TAKE_SINGING_BOWL,
        }:
            payload = self._settle_reward_completion(payload, executed)
        fold_single_event = (
            not isinstance(action, str)
            and action.kind in {ActionKind.CHOOSE_EVENT_OPTION, ActionKind.CHOOSE_NEOW_OPTION}
        )
        payload = self._fold_protocol_only_boundaries(
            payload, executed, fold_single_event=fold_single_event,
        )
        payload = self._settle_debug_intents(payload, executed)
        self._last_executed_commands = tuple(executed)
        self._adapted = adapt_original(payload)
        horizon = evaluate_horizon(self.profile, self._adapted.decision.observation)
        decision = self._adapted.decision
        if horizon.terminated != decision.terminal:
            decision = Decision(
                decision.observation,
                () if horizon.terminated else decision.actions,
                horizon.terminated,
            )
            self._adapted = AdaptedOriginalDecision(decision, {} if horizon.terminated else self._adapted.commands)
        return Transition(
            decision,
            1.0 if horizon.success else -1.0 if horizon.reason == "DEATH" else 0.0,
            horizon.terminated,
            False,
            {"reason": horizon.reason, "success": horizon.success},
        )

    def _wait_for_screen_change(
        self, payload: dict[str, Any], *, while_screen: str,
        executed: list[str], limit: int = 60,
    ) -> dict[str, Any]:
        for _ in range(limit):
            screen = str((payload.get("game_state") or {}).get("screen_type") or "").upper()
            if screen != while_screen:
                return payload
            available = {str(item).lower() for item in payload.get("available_commands") or ()}
            if "wait" not in available:
                raise RuntimeError(
                    f"Original UI remained on {while_screen} without an advertised wait command"
                )
            payload = self.session.execute("wait 1")
            executed.append("wait 1")
        raise RuntimeError(f"Original UI did not leave {while_screen} within {limit} frames")

    def _settle_reward_intermediate(
        self, payload: dict[str, Any], executed: list[str],
    ) -> dict[str, Any]:
        return self._wait_for_screen_change(
            payload, while_screen="COMBAT_REWARD", executed=executed,
        )

    def _settle_reward_completion(
        self, payload: dict[str, Any], executed: list[str],
    ) -> dict[str, Any]:
        return self._wait_for_screen_change(
            payload, while_screen="CARD_REWARD", executed=executed,
        )

    def command_sequence(self, action: Action | str) -> tuple[str, ...]:
        """Return validation-only wire commands without exposing them to policy code."""

        if self._adapted is None:
            raise RuntimeError("reset must be called before command_sequence")
        candidate_id = action if isinstance(action, str) else action.candidate_id
        try:
            return self._adapted.commands[candidate_id]
        except KeyError as error:
            raise ValueError("action is not legal at the current Original decision") from error

    @property
    def last_executed_commands(self) -> tuple[str, ...]:
        return self._last_executed_commands

    def validation_snapshot(self) -> ValidationSnapshot:
        return self.session.validation_snapshot()

    def _fold_initial_neow_dialog(self, payload: dict[str, Any]) -> dict[str, Any]:
        game = payload.get("game_state") or {}
        choices = game.get("choice_list") or []
        if int(game.get("floor", 0) or 0) == 0 and len(choices) == 1:
            available = {str(item).lower() for item in payload.get("available_commands") or ()}
            if "choose" in available:
                return self.session.execute("choose 0")
        return payload

    def _fold_protocol_only_boundaries(
        self, payload: dict[str, Any], executed: list[str] | None = None,
        *, fold_single_event: bool = True,
    ) -> dict[str, Any]:
        """Fold confirmations that carry no player choice or gameplay semantics."""

        folded = False
        for _ in range(8):
            game = payload.get("game_state") or {}
            available = {str(item).lower() for item in payload.get("available_commands") or ()}
            choices = game.get("choice_list")
            screen = str(game.get("screen_type") or "").upper()
            if screen == "GRID" and not choices and "confirm" in available:
                payload = self.session.execute("confirm")
                if executed is not None:
                    executed.append("confirm")
                folded = True
                continue
            if screen == "SHOP_ROOM" and len(choices or ()) == 1 and "choose" in available:
                payload = self.session.execute("choose 0")
                if executed is not None:
                    executed.append("choose 0")
                folded = True
                continue
            if (
                fold_single_event
                and screen == "EVENT" and len(choices or ()) == 1
                and "choose" in available
            ):
                payload = self.session.execute("choose 0")
                if executed is not None:
                    executed.append("choose 0")
                folded = True
                available_after = {
                    str(item).lower() for item in payload.get("available_commands") or ()
                }
                if "wait" in available_after:
                    payload = self.session.execute("wait 30")
                    if executed is not None:
                        executed.append("wait 30")
                continue
            break
        if folded:
            continuation = payload.get("_continuation")
            if isinstance(continuation, dict):
                continuation["ui_boundary_folded"] = True
        return payload

    def _settle_debug_intents(
        self, payload: dict[str, Any], executed: list[str] | None = None,
    ) -> dict[str, Any]:
        """Advance presentation frames until stock monster intents are materialized."""

        frames = 0
        for _ in range(8):
            game = payload.get("game_state") or {}
            if not game.get("combat_state"):
                break
            intents = payload.get("_monster_intents") or []
            if intents and all(str(item.get("intent") or "").upper() != "DEBUG" for item in intents):
                break
            available = {str(item).lower() for item in payload.get("available_commands") or ()}
            if "wait" not in available:
                break
            payload = self.session.execute("wait 1")
            frames += 1
            if executed is not None:
                executed.append("wait 1")
        return payload
