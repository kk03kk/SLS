"""Canonical backend driven by Original Game plus CommunicationMod."""

from __future__ import annotations

from typing import Any

from sls.backends.original.adapter import AdaptedOriginalDecision, adapt_original
from sls.backends.original.session import OriginalSession
from sls.content.seed import long_to_seed_string
from sls.contracts import Action, ActionKind, Decision, Transition, ValidationSnapshot
from sls.contracts.continuation import continuation_original
from sls.curriculum import (
    CurriculumProfile,
    IRONCLAD_A0_HEART,
    completed_act_between,
    evaluate_horizon,
)


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
        self._last_validation_evidence: dict[str, Any] = {}

    @property
    def raw_payload(self) -> dict[str, Any]:
        if self.session.payload is None:
            raise RuntimeError("backend has not been reset")
        return self.session.payload

    def reset(self, seed: int) -> Decision:
        self._last_validation_evidence = {}
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

        self._last_validation_evidence = {}

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
        previous_observation = self._adapted.decision.observation
        candidate_id = action if isinstance(action, str) else action.candidate_id
        try:
            commands = self._adapted.commands[candidate_id]
        except KeyError as error:
            raise ValueError("action is not legal at the current Original decision") from error
        payload = self.raw_payload
        pending_discard_souls: set[str] = set()
        liquid_memories = False
        if not isinstance(action, str) and action.kind is ActionKind.USE_POTION:
            liquid_memories = any(
                potion.instance_id == action.subject_id
                and potion.content_id == "LIQUID_MEMORIES"
                for potion in self._adapted.decision.observation.potions
            )
            if liquid_memories:
                pending_discard_souls = {
                    str(item.get("card_uuid"))
                    for item in continuation_original(payload).get("active_card_souls") or ()
                    if isinstance(item, dict)
                    and str(item.get("destination") or "").upper() == "DISCARD_PILE"
                    and item.get("card_uuid")
                }
        timing_before = payload.get("_timing_evidence") or {}
        starting_deck_size = len((payload.get("game_state") or {}).get("deck") or ())
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
        if not isinstance(action, str) and action.kind is ActionKind.CHOOSE_CARD_REWARD:
            payload = self._wait_for_deck_growth(
                payload, starting_size=starting_deck_size, executed=executed,
            )
        if not isinstance(action, str):
            expected_key = (
                "emerald_key" if action.kind is ActionKind.TAKE_REWARD
                and action.reward_id == "reward-key:emerald"
                else "sapphire_key" if action.kind is ActionKind.TAKE_BLUE_KEY
                else "ruby_key" if action.kind is ActionKind.RECALL
                else None
            )
            if expected_key is not None:
                payload = self._wait_for_key_acquisition(
                    payload, key=expected_key, executed=executed,
                )
        fold_single_event = (
            not isinstance(action, str)
            and action.kind in {ActionKind.CHOOSE_EVENT_OPTION, ActionKind.CHOOSE_NEOW_OPTION}
        )
        payload = self._fold_protocol_only_boundaries(
            payload, executed, fold_single_event=fold_single_event,
            leaving_shop=(
                not isinstance(action, str) and action.kind is ActionKind.LEAVE_SHOP
            ),
        )
        payload = self._settle_combat_terminal(payload, executed)
        payload = self._settle_debug_intents(payload, executed)
        payload = self._settle_command_boundary(payload, executed)
        self._last_executed_commands = tuple(executed)
        timing_after = payload.get("_timing_evidence") or {}
        self._last_validation_evidence = {}
        if int(timing_after.get("discovery_completion_serial", 0)) != int(
            timing_before.get("discovery_completion_serial", 0)
        ):
            self._last_validation_evidence["discovery_retrieval_updates"] = int(
                timing_after["discovery_retrieval_updates"]
            )
        if liquid_memories and pending_discard_souls:
            hand = (((payload.get("game_state") or {}).get("combat_state") or {}).get("hand") or ())
            reset_count = sum(
                str(card.get("uuid")) in pending_discard_souls
                for card in hand if isinstance(card, dict)
            )
            if reset_count:
                self._last_validation_evidence["card_soul_cost_reset_count"] = reset_count
        self._adapted = adapt_original(payload)
        horizon = evaluate_horizon(
            self.profile,
            self._adapted.decision.observation,
            act_completed=completed_act_between(
                previous_observation, self._adapted.decision.observation,
            ),
        )
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

    def _wait_for_deck_growth(
        self, payload: dict[str, Any], *, starting_size: int,
        executed: list[str], limit: int = 60,
    ) -> dict[str, Any]:
        for _ in range(limit):
            deck = (payload.get("game_state") or {}).get("deck") or ()
            if len(deck) > starting_size:
                return payload
            available = {str(item).lower() for item in payload.get("available_commands") or ()}
            if "wait" not in available:
                raise RuntimeError("card reward closed before the selected card entered the deck")
            payload = self.session.execute("wait 1")
            executed.append("wait 1")
        raise RuntimeError(f"selected card did not enter the deck within {limit} frames")

    def _wait_for_key_acquisition(
        self, payload: dict[str, Any], *, key: str,
        executed: list[str], limit: int = 60,
    ) -> dict[str, Any]:
        """Wait for stock ObtainKeyEffect to materialize the selected key."""

        for _ in range(limit):
            run = payload.get("_parity_run") or (
                (payload.get("game_state") or {}).get("_parity_run") or {}
            )
            if run.get(key) is True:
                return payload
            available = {str(item).lower() for item in payload.get("available_commands") or ()}
            if "wait" not in available:
                raise RuntimeError(f"selected {key} before its stock effect materialized")
            payload = self.session.execute("wait 1")
            executed.append("wait 1")
        raise RuntimeError(f"selected {key} did not materialize within {limit} frames")

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

    @property
    def last_validation_evidence(self) -> dict[str, Any]:
        return dict(self._last_validation_evidence)

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
        *, fold_single_event: bool = True, leaving_shop: bool = False,
    ) -> dict[str, Any]:
        """Fold confirmations that carry no player choice or gameplay semantics."""

        folded = False
        for _ in range(8):
            game = payload.get("game_state") or {}
            available = {str(item).lower() for item in payload.get("available_commands") or ()}
            choices = game.get("choice_list")
            screen = str(game.get("screen_type") or "").upper()
            room_class = str(game.get("room_class") or game.get("room_type") or "")
            chest_state = game.get("screen_state") or {}
            if (
                screen == "CHEST"
                and room_class.endswith("TreasureRoomBoss")
            ):
                # Clicking the Act boss chest carries no gameplay choice; the
                # semantic decisions are the relic screen and next Act map.
                opened = bool(chest_state.get("chest_open", False))
                command = "proceed" if opened and "proceed" in available else (
                    "choose 0" if not opened and "choose" in available else None
                )
                if command is None:
                    break
                payload = self.session.execute(command)
                if executed is not None:
                    executed.append(command)
                folded = True
                continue
            if screen == "GRID" and not choices and "confirm" in available:
                payload = self.session.execute("confirm")
                if executed is not None:
                    executed.append("confirm")
                folded = True
                continue
            if screen == "SHOP_ROOM" and len(choices or ()) == 1:
                command = "proceed" if leaving_shop and "proceed" in available else "choose 0"
                if command == "choose 0" and "choose" not in available:
                    break
                payload = self.session.execute(command)
                if executed is not None:
                    executed.append(command)
                folded = True
                continue
            rest_state = game.get("screen_state") or {}
            if (
                screen == "REST"
                and bool(rest_state.get("has_rested"))
                and not (rest_state.get("rest_options") or ())
                and "proceed" in available
            ):
                payload = self.session.execute("proceed")
                if executed is not None:
                    executed.append("proceed")
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
            if intents and all(
                str(item.get("intent") or "").upper() != "DEBUG"
                and (
                    not str(item.get("intent") or "").upper().startswith("ATTACK")
                    or (int(item.get("damage", -1)) >= 0 and int(item.get("hits", 0)) >= 1)
                )
                for item in intents
            ):
                break
            available = {str(item).lower() for item in payload.get("available_commands") or ()}
            if "wait" not in available:
                break
            payload = self.session.execute("wait 1")
            frames += 1
            if executed is not None:
                executed.append("wait 1")
        return payload

    def _settle_command_boundary(
        self, payload: dict[str, Any], executed: list[str] | None = None,
        *, limit: int = 6,
    ) -> dict[str, Any]:
        """Require two identical non-advancing snapshots at a command boundary.

        CommunicationMod state conversion can run concurrently with a stock
        action update. A single ready payload can therefore expose a torn
        transition (for example, a Liquid Memories card after it enters the
        hand but before ``setCostForTurn(0)``). At a choice-free idle combat
        boundary, all active card ``Soul`` continuations first finish. Two
        subsequent ``state`` replies then establish transport quiescence.
        Selection screens are never advanced here because rendering can be
        causally relevant to RNG consumption.
        """

        available = {str(item).lower() for item in payload.get("available_commands") or ()}
        if "state" not in available:
            return payload
        game = payload.get("game_state") or {}
        continuation = payload.get("_continuation") or game.get("_continuation") or {}
        combat = game.get("combat_state") or {}
        safe_idle_combat = (
            bool(combat)
            and str(game.get("screen_type") or "NONE").upper() in {"", "NONE"}
            and str(continuation.get("action_phase") or "").upper() == "WAITING_ON_USER"
            and not (continuation.get("action_queue_types") or ())
            and not (continuation.get("card_queue_types") or ())
            and "wait" in available
        )
        if safe_idle_combat:
            souls_known = "active_card_souls" in continuation
            for _ in range(limit):
                active_souls = continuation.get("active_card_souls") or ()
                if souls_known and not active_souls:
                    break
                command = "wait 30" if souls_known else "wait 1"
                payload = self.session.execute(command)
                if executed is not None:
                    executed.append(command)
                continuation = payload.get("_continuation") or (
                    (payload.get("game_state") or {}).get("_continuation") or {}
                )
                if not souls_known:
                    break
            else:
                raise RuntimeError(
                    f"Original card Soul continuation did not settle after {limit} waits"
                )
        previous: dict[str, Any] | None = None
        for _ in range(limit):
            current = self.session.execute("state")
            if executed is not None:
                executed.append("state")
            if previous is not None and current == previous:
                return current
            previous = current
        raise RuntimeError(
            f"Original command boundary did not stabilize after {limit} state snapshots"
        )

    def _settle_combat_terminal(
        self, payload: dict[str, Any], executed: list[str] | None = None,
        *, limit: int = 8,
    ) -> dict[str, Any]:
        """Advance stock victory/death presentation to an actionable boundary."""

        for _ in range(limit):
            game = payload.get("game_state") or {}
            continuation = payload.get("_continuation") or game.get("_continuation") or {}
            if str(continuation.get("screen") or "").upper() in {
                "DEATH", "VICTORY", "GAME_OVER", "COMPLETE",
            }:
                return payload
            combat = game.get("combat_state") or {}
            if not combat:
                return payload
            monsters = combat.get("monsters") or ()
            player_dead = int(game.get("current_hp", 0) or 0) <= 0
            monsters_dead = bool(monsters) and all(
                int(monster.get("current_hp", 0) or 0) <= 0
                or bool(monster.get("is_gone", False))
                for monster in monsters
            )
            if not player_dead and not monsters_dead:
                return payload
            available = {str(item).lower() for item in payload.get("available_commands") or ()}
            if "wait" not in available:
                raise RuntimeError("terminal combat presentation has no advertised wait command")
            payload = self.session.execute("wait 30")
            if executed is not None:
                executed.append("wait 30")
        raise RuntimeError("Original combat terminal presentation did not settle")
