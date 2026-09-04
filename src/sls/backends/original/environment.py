"""Canonical backend driven by Original Game plus CommunicationMod."""

from __future__ import annotations

import time
from typing import Any

from sls.backends.original.adapter import AdaptedOriginalDecision, adapt_original
from sls.backends.original.session import OriginalSession
from sls.content.seed import long_to_seed_string
from sls.contracts import Action, ActionKind, Decision, Transition, ValidationSnapshot
from sls.contracts.continuation import continuation_original
from sls.curriculum import (
    IRONCLAD_A0_HEART,
    CurriculumProfile,
    EpisodeHorizon,
    completed_act_between,
    evaluate_horizon,
)


def _completed_curriculum_act(
    profile: CurriculumProfile, payload: dict[str, Any], decision: Decision,
) -> int | None:
    """Recognize a public Original boss-clear boundary before boss rewards."""

    if profile.horizon not in {
        EpisodeHorizon.ACT_1, EpisodeHorizon.ACT_2, EpisodeHorizon.ACT_3,
    }:
        return None
    game = payload.get("game_state") or {}
    current_act = int(decision.observation.run.act)
    if decision.observation.screen.value == "BOSS_REWARD":
        return current_act
    room_class = str(game.get("room_class") or game.get("room_type") or "")
    room_phase = str(game.get("room_phase") or "").upper()
    if room_class.endswith("MonsterRoomBoss") and room_phase == "COMPLETE":
        return current_act
    return None


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
        self._card_reward_preview_seconds = 0.0

    def set_card_reward_preview_seconds(self, seconds: float) -> None:
        """Delay the second half of a folded card-reward UI transaction."""

        value = float(seconds)
        if not 0.0 <= value <= 10.0:
            raise ValueError("card reward preview must be between zero and ten seconds")
        self._card_reward_preview_seconds = value

    @property
    def raw_payload(self) -> dict[str, Any]:
        if self.session.payload is None:
            raise RuntimeError("backend has not been reset")
        return self.session.payload

    def _adapt(self, payload: dict[str, Any]) -> AdaptedOriginalDecision:
        return adapt_original(
            payload,
            allow_key_acquisition=self.profile.horizon is EpisodeHorizon.HEART,
        )

    def reset(self, seed: int) -> Decision:
        self._last_validation_evidence = {}
        payload = self.session.payload or self.session.connect()
        if payload.get("in_game"):
            available = {str(item).lower() for item in payload.get("available_commands") or ()}
            if "reset_run" not in available:
                raise RuntimeError("Oracle Mod reset_run command is unavailable")
            payload = self.session.execute("reset_run")
            payload = self._wait_for_main_menu(payload)
        if payload.get("in_game"):
            raise RuntimeError("Original game did not return to the main menu")
        available = {str(item).lower() for item in payload.get("available_commands") or ()}
        if "start" not in available:
            raise RuntimeError(f"Original main menu does not advertise start: {sorted(available)}")
        payload = self.session.execute(
            f"start {self.profile.character_id} {self.profile.ascension} "
            f"{long_to_seed_string(int(seed))}"
        )
        payload = self._wait_for_run_start(payload)
        payload = self._fold_initial_neow_dialog(payload)
        self._adapted = self._adapt(payload)
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
            payload = self._wait_for_main_menu(payload)
        available = {str(item).lower() for item in payload.get("available_commands") or ()}
        if "parity_continue" not in available:
            raise RuntimeError(f"parity_continue is unavailable: {sorted(available)}")
        payload = self.session.execute("parity_continue")
        payload = self._fold_protocol_only_boundaries(
            payload, fold_single_event=False,
        )
        payload = self._settle_debug_intents(payload)
        self._adapted = self._adapt(payload)
        return self._adapted.decision

    def return_to_menu(self) -> None:
        """End the validation run before protected user files are restored."""

        payload = self.session.payload
        if not payload or not payload.get("in_game"):
            return
        available = {str(item).lower() for item in payload.get("available_commands") or ()}
        if "reset_run" not in available:
            raise RuntimeError("validation run cannot return to the main menu")
        self._wait_for_main_menu(self.session.execute("reset_run"))
        self._adapted = None

    def _wait_for_main_menu(
        self, payload: dict[str, Any], *, limit: int = 240,
    ) -> dict[str, Any]:
        """Wait through the asynchronous stock dungeon-to-menu transition."""

        for _ in range(limit):
            if not payload.get("in_game"):
                return payload
            available = {
                str(item).lower() for item in payload.get("available_commands") or ()
            }
            # ``state`` remains valid on both sides of the asynchronous menu
            # transition; ``wait`` can be advertised by the last dungeon
            # payload and become invalid before it reaches CommunicationMod.
            command = "state" if "state" in available else (
                "wait 1" if "wait" in available else None
            )
            if command is None:
                raise RuntimeError(
                    "Original is leaving the run without an advertised wait/state command"
                )
            payload = self.session.execute(command)
        raise RuntimeError("Original game did not reach the main menu after reset_run")

    def _wait_for_run_start(
        self, payload: dict[str, Any], *, limit: int = 240,
    ) -> dict[str, Any]:
        """Wait through CommunicationMod's asynchronous start acknowledgement."""

        for _ in range(limit):
            if payload.get("in_game") and payload.get("game_state"):
                return payload
            available = {
                str(item).lower() for item in payload.get("available_commands") or ()
            }
            if "state" not in available:
                raise RuntimeError(
                    "Original start transition does not advertise the state command"
                )
            payload = self.session.execute("state")
        raise RuntimeError("Original game did not enter the run after start")

    def step(self, action: Action | str) -> Transition:
        if self._adapted is None:
            raise RuntimeError("reset must be called before step")
        previous_observation = self._adapted.decision.observation
        candidate_id = action if isinstance(action, str) else action.candidate_id
        try:
            commands = self._adapted.commands[candidate_id]
        except KeyError as error:
            raise ValueError("action is not legal at the current Original decision") from error
        resolved_action = next(
            candidate for candidate in self._adapted.decision.actions
            if candidate.candidate_id == candidate_id
        )
        payload = self.raw_payload
        pending_discard_souls: set[str] = set()
        liquid_memories = False
        if resolved_action.kind is ActionKind.USE_POTION:
            liquid_memories = any(
                potion.instance_id == resolved_action.subject_id
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
        selection_task_before = str(
            (payload.get("_continuation") or {}).get("card_selection_task") or ""
        ).upper()
        starting_deck_size = len((payload.get("game_state") or {}).get("deck") or ())
        executed: list[str] = []
        for index, command in enumerate(commands):
            payload = self.session.execute(command)
            executed.append(command)
            if index + 1 < len(commands):
                payload = self._settle_reward_intermediate(payload, executed)
                if (
                    index == 0
                    and resolved_action.kind in {
                        ActionKind.CHOOSE_CARD_REWARD,
                        ActionKind.TAKE_SINGING_BOWL,
                    }
                    and self._card_reward_preview_seconds > 0.0
                ):
                    # The policy decision is already fixed.  Sleeping here only
                    # keeps stock's stable CardRewardScreen visible before the
                    # second wire command completes the folded transaction.
                    time.sleep(self._card_reward_preview_seconds)
        if resolved_action.kind in {
            ActionKind.CHOOSE_CARD_REWARD,
            ActionKind.SKIP_CARD_REWARD,
            ActionKind.TAKE_SINGING_BOWL,
        }:
            payload = self._settle_reward_completion(payload, executed)
        if resolved_action.kind is ActionKind.CHOOSE_CARD_REWARD:
            payload = self._wait_for_deck_growth(
                payload, starting_size=starting_deck_size, executed=executed,
            )
        expected_key = (
            "emerald_key" if resolved_action.kind is ActionKind.TAKE_REWARD
            and resolved_action.reward_id == "reward-key:emerald"
            else "sapphire_key" if resolved_action.kind is ActionKind.TAKE_BLUE_KEY
            else "ruby_key" if resolved_action.kind is ActionKind.RECALL
            else None
        )
        if expected_key is not None:
            payload = self._wait_for_key_acquisition(
                payload, key=expected_key, executed=executed,
            )
        fold_single_event = (
            resolved_action.kind in {ActionKind.CHOOSE_EVENT_OPTION, ActionKind.CHOOSE_NEOW_OPTION}
        )
        payload = self._fold_protocol_only_boundaries(
            payload, executed, fold_single_event=fold_single_event,
            leaving_shop=(
                resolved_action.kind is ActionKind.LEAVE_SHOP
            ),
        )
        payload = self._settle_combat_terminal(payload, executed)
        payload = self._wait_for_actionable_combat_boundary(payload, executed)
        payload = self._settle_debug_intents(payload, executed)
        payload = self._settle_command_boundary(payload, executed)
        if (
            resolved_action.kind is ActionKind.CHOOSE_EVENT_OPTION
            and str(resolved_action.option_id or "").startswith("match-pair:")
        ):
            payload = self._settle_match_completion(payload, executed)
        # Some events (notably Match and Keep) materialize their initial
        # Continue dialog only after the room-entry command has settled.  Fold
        # those protocol-only dialogs, then settle the actual interactive
        # screen before exposing a policy boundary.
        payload = self._fold_protocol_only_boundaries(
            payload, executed, fold_single_event=fold_single_event,
        )
        payload = self._settle_command_boundary(payload, executed)
        if resolved_action.kind in {
            ActionKind.UPGRADE_CARD, ActionKind.REMOVE_CARD, ActionKind.SELECT_CARD,
        }:
            payload = self._wait_for_selection_completion(
                payload,
                executed,
                limit=180 if selection_task_before == "DISCOVERY" else 30,
            )
            payload = self._fold_terminal_selection_event(payload, executed)
            payload = self._fold_protocol_only_boundaries(
                payload, executed, fold_single_event=fold_single_event,
            )
            payload = self._fold_protocol_only_boundaries(
                payload, executed, fold_single_event=fold_single_event,
            )
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
        self._adapted = self._adapt(payload)
        observed_completion = _completed_curriculum_act(
            self.profile, payload, self._adapted.decision,
        )
        horizon = evaluate_horizon(
            self.profile,
            self._adapted.decision.observation,
            act_completed=observed_completion or completed_act_between(
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

    def _settle_match_completion(
        self, payload: dict[str, Any], executed: list[str], *, limit: int = 20,
    ) -> dict[str, Any]:
        """Fold stock Match cleanup animation and its forced Continue dialog."""

        for _ in range(limit):
            game = payload.get("game_state") or {}
            screen = str(game.get("screen_type") or "").upper()
            if screen != "EVENT":
                return payload
            if payload.get("_match_slots"):
                return payload
            event_id = str(
                (payload.get("_continuation") or {}).get("event_id")
                or (game.get("screen_state") or {}).get("event_id")
                or ""
            )
            if event_id != "Match and Keep!":
                return payload
            choices = game.get("choice_list") or ()
            available = {
                str(item).lower() for item in payload.get("available_commands") or ()
            }
            if len(choices) == 1 and "choose" in available:
                payload = self.session.execute("choose 0")
                executed.append("choose 0")
                continue
            if "wait" not in available:
                raise RuntimeError("Match cleanup has no wait or forced Continue command")
            payload = self.session.execute("wait 30")
            executed.append("wait 30")
        raise RuntimeError("Match cleanup did not settle within the bounded wait")

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

    def _wait_for_selection_completion(
        self, payload: dict[str, Any], executed: list[str], *, limit: int = 30,
    ) -> dict[str, Any]:
        """Advance stock frames past transient GRID/NONE selection teardown."""

        continuation = payload.get("_continuation") or {}
        effective_limit = max(limit, 180) if str(
            continuation.get("card_selection_task") or ""
        ).upper() == "DISCOVERY" else limit
        for _ in range(effective_limit):
            game = payload.get("game_state") or {}
            screen = str(game.get("screen_type") or "NONE").upper()
            continuation = payload.get("_continuation") or {}
            available = {str(item).lower() for item in payload.get("available_commands") or ()}
            if screen == "HAND_SELECT":
                state = game.get("screen_state") or {}
                selected = state.get("selected") or ()
                required = int(state.get("max_cards", 0) or 0)
                protocol_only_confirm = bool(
                    required > 0 and len(selected) >= required
                    and not bool(state.get("can_pick_zero", False))
                )
                if protocol_only_confirm and "confirm" in available:
                    payload = self.session.execute("confirm")
                    executed.append("confirm")
                    continue
                # A partial or optional multi-selection remains a semantic
                # boundary; do not choose or confirm it automatically.
                return payload
            if (
                screen == "NONE"
                and str(continuation.get("action_phase") or "").upper() == "WAITING_ON_USER"
                and str(game.get("room_phase") or "").upper() == "COMBAT"
            ):
                return payload
            if screen not in {"GRID", "NONE", "CARD_REWARD", "HAND_SELECT"}:
                return payload
            if "wait" not in available:
                raise RuntimeError(
                    f"selection remained on transient {screen} without wait"
                )
            payload = self.session.execute("wait 1")
            executed.append("wait 1")
        raise RuntimeError(
            f"selection did not reach a semantic screen within {effective_limit} frames"
        )

    def _fold_terminal_selection_event(
        self, payload: dict[str, Any], executed: list[str],
    ) -> dict[str, Any]:
        """Fold the sole event completion revealed after a card selection."""

        game = payload.get("game_state") or {}
        choices = game.get("choice_list") or ()
        if str(game.get("screen_type") or "").upper() != "EVENT" or len(choices) != 1:
            return payload
        available = {str(item).lower() for item in payload.get("available_commands") or ()}
        if "choose" not in available:
            raise RuntimeError("terminal post-selection event does not advertise choose")
        payload = self.session.execute("choose 0")
        executed.append("choose 0")
        available = {str(item).lower() for item in payload.get("available_commands") or ()}
        if "wait" in available:
            payload = self.session.execute("wait 30")
            executed.append("wait 30")
        continuation = payload.get("_continuation")
        if isinstance(continuation, dict):
            continuation["ui_boundary_folded"] = True
        return payload

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

    def _fold_initial_neow_dialog(
        self, payload: dict[str, Any], *, limit: int = 240,
    ) -> dict[str, Any]:
        """Settle Neow's animation and fold its non-semantic Continue dialog."""

        folded_continue = False
        for _ in range(limit):
            game = payload.get("game_state") or {}
            if int(game.get("floor", 0) or 0) != 0:
                return payload
            choices = game.get("choice_list") or []
            available = {
                str(item).lower() for item in payload.get("available_commands") or ()
            }
            if "choose" in available and choices:
                if len(choices) == 1 and not folded_continue:
                    payload = self.session.execute("choose 0")
                    folded_continue = True
                    continue
                return payload
            # Merely requesting state does not advance Neow's opening
            # animation. ``wait`` is the protocol's explicit frame advance.
            if "wait" in available:
                payload = self.session.execute("wait 1")
            elif "state" in available:
                payload = self.session.execute("state")
            else:
                raise RuntimeError(
                    "Neow opening has no choose, wait, or state command"
                )
        raise RuntimeError("Neow opening did not become actionable")

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
            neow_terminal = bool(
                screen == "EVENT"
                and (
                    room_class.endswith("NeowRoom")
                    or str(
                        continuation_original(payload).get("event_id") or ""
                    ).endswith("NeowEvent")
                )
            )
            event_state = game.get("screen_state") or {}
            event_options = event_state.get("options") or ()
            event_choice_texts = [str(choices[0]).lower()] if len(choices or ()) == 1 else []
            if len(event_options) == 1:
                event_choice_texts.extend(
                    str(event_options[0].get(field) or "").lower()
                    for field in ("label", "text")
                )
            terminal_event_choice = bool(
                screen == "EVENT"
                and len(choices or ()) == 1
                and any(
                    token in text
                    for text in event_choice_texts
                    for token in ("leave", "proceed", "continue", "离开", "继续")
                )
            )
            match_intro = bool(
                screen == "EVENT"
                and str(continuation_original(payload).get("event_id") or "")
                == "Match and Keep!"
                and not (payload.get("_match_slots") or ())
            )
            if (
                (
                    screen == "NEOW" or neow_terminal
                    or terminal_event_choice
                    or match_intro
                    or (fold_single_event and screen == "EVENT")
                )
                and len(choices or ()) == 1
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
                    wait_command = "wait 120" if match_intro else "wait 30"
                    payload = self.session.execute(wait_command)
                    if executed is not None:
                        executed.append(wait_command)
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

    def _wait_for_actionable_combat_boundary(
        self, payload: dict[str, Any], executed: list[str] | None = None,
        *, limit: int = 30,
    ) -> dict[str, Any]:
        """Advance a torn post-card boundary until stock exposes its choice.

        CommunicationMod can publish ``ready_for_command`` after a card leaves
        the hand but before cards such as Armaments create their selection UI.
        Such a payload has a live combat, no policy command, and only the
        validation ``wait/state`` commands.  It is not a policy boundary.
        """

        for _ in range(limit):
            game = payload.get("game_state") or {}
            continuation = payload.get("_continuation") or game.get("_continuation") or {}
            if not game.get("combat_state") or str(
                continuation.get("screen") or ""
            ).upper() in {"DEATH", "VICTORY", "GAME_OVER", "COMPLETE"}:
                return payload
            try:
                self._adapt(payload)
            except ValueError as error:
                if str(error) != "a non-terminal decision must expose a legal action":
                    raise
            else:
                return payload
            available = {str(item).lower() for item in payload.get("available_commands") or ()}
            if "wait" not in available:
                raise RuntimeError("live Original combat has no policy command or advertised wait")
            payload = self.session.execute("wait 1")
            if executed is not None:
                executed.append("wait 1")
        game = payload.get("game_state") or {}
        combat = game.get("combat_state") or {}
        continuation = payload.get("_continuation") or game.get("_continuation") or {}
        raise RuntimeError(
            "Original combat did not expose a policy command after 30 frames: "
            f"available={payload.get('available_commands')}, "
            f"screen={game.get('screen_type')}, "
            f"continuation={continuation}, "
            f"screen_state={game.get('screen_state')}, "
            f"card_select={combat.get('card_select')}"
        )

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
            combat_player = combat.get("player") or {}
            player_dead = int(
                combat_player.get("current_hp", game.get("current_hp", 0)) or 0
            ) <= 0
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
